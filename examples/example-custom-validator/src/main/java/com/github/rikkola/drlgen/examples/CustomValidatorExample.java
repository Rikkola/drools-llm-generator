package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.validation.CompositeValidator;
import com.github.rikkola.drlgen.validation.DRLValidator;
import com.github.rikkola.drlgen.validation.ValidationResult;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Custom Validator Example - Implement business-specific validation rules.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating custom validators with business rules</li>
 *   <li>Using CompositeValidator to combine multiple validators</li>
 *   <li>Processing ValidationResult messages by severity</li>
 *   <li>Adding warnings vs errors to validation results</li>
 * </ul>
 *
 * <p>Custom validators are useful for:
 * <ul>
 *   <li>Enforcing coding standards</li>
 *   <li>Security checks</li>
 *   <li>Business rule compliance</li>
 *   <li>Quality gates</li>
 * </ul>
 *
 * <p>Run with: {@code mvn compile exec:java}
 */
public class CustomValidatorExample {

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Custom Validator Example ===\n");

        // Simple requirement
        String requirement = """
            Create a rule that marks orders over $100 as priority orders.
            """;

        String factTypes = """
            Order: orderId (String), total (double), priority (boolean)
            """;

        System.out.println("Requirement:");
        System.out.println(requirement);

        // Create custom validators
        System.out.println("\n=== Creating Custom Validators ===");
        System.out.println("1. Comment Validator - Warns if no comments present");
        System.out.println("2. Naming Validator - Errors on generic rule names");
        System.out.println("3. Security Validator - Errors on dangerous patterns\n");

        // Create composite validator combining Drools + custom validators
        DRLValidator compositeValidator = createCompositeValidator();

        // Create the AI model
        ChatModel model = ModelConfiguration.createModel("granite4");

        // Build generator with custom validation
        DRLGenerator generator = DRLGenerator.builder()
                .chatModel(model)
                .validator(compositeValidator)
                .build();

        System.out.println("Generating DRL...\n");

        // Generate DRL
        GenerationResult result = generator.generate(requirement, factTypes);

        // Print generated DRL
        System.out.println("=== Generated DRL ===");
        System.out.println(result.generatedDrl());
        System.out.println();

        // Show detailed validation results
        System.out.println("=== Validation Details ===");
        System.out.println("Overall: " + (result.validationPassed() ? "PASSED" : "FAILED"));
        System.out.println("Message: " + result.validationMessage());
        System.out.println();

        // Demonstrate direct validation with detailed results
        System.out.println("=== Direct Validation (for more detail) ===");
        ValidationResult validationResult = compositeValidator.validate(result.generatedDrl());

        if (!validationResult.getErrors().isEmpty()) {
            System.out.println("ERRORS:");
            for (var error : validationResult.getErrors()) {
                System.out.println("  - " + error.message());
                if (error.lineNumber() != null) {
                    System.out.println("    Line: " + error.lineNumber());
                }
            }
        }

        if (!validationResult.getWarnings().isEmpty()) {
            System.out.println("WARNINGS:");
            for (var warning : validationResult.getWarnings()) {
                System.out.println("  - " + warning.message());
            }
        }

        if (!validationResult.getNotes().isEmpty()) {
            System.out.println("NOTES:");
            for (var note : validationResult.getNotes()) {
                System.out.println("  - " + note.message());
            }
        }

        if (validationResult.isValid()) {
            System.out.println("\nAll validation checks passed!");
        }
    }

    /**
     * Creates a composite validator with custom business rules.
     */
    private static DRLValidator createCompositeValidator() {
        // 1. Default Drools syntax validator
        DRLValidator syntaxValidator = DRLValidator.createDefault();

        // 2. Comment validator - warns if rules don't have comments
        DRLValidator commentValidator = drlCode -> {
            if (!drlCode.contains("//") && !drlCode.contains("/*")) {
                return ValidationResult.builder()
                        .message(new ValidationResult.ValidationMessage(
                                ValidationResult.Severity.WARNING,
                                "Best practice: Rules should have comments explaining their purpose",
                                null,
                                null))
                        .build();
            }
            return ValidationResult.success();
        };

        // 3. Naming convention validator - errors on generic names
        DRLValidator namingValidator = drlCode -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            boolean hasIssues = false;

            if (drlCode.contains("rule \"Rule1\"") ||
                drlCode.contains("rule \"Rule 1\"") ||
                drlCode.contains("rule \"Test\"")) {
                builder.message(new ValidationResult.ValidationMessage(
                        ValidationResult.Severity.ERROR,
                        "Naming: Use descriptive rule names instead of 'Rule1' or 'Test'",
                        null,
                        null));
                hasIssues = true;
            }

            return hasIssues ? builder.build() : ValidationResult.success();
        };

        // 4. Security validator - errors on dangerous patterns
        DRLValidator securityValidator = drlCode -> {
            ValidationResult.Builder builder = ValidationResult.builder();
            boolean hasIssues = false;

            if (drlCode.contains("System.exit")) {
                builder.message(new ValidationResult.ValidationMessage(
                        ValidationResult.Severity.ERROR,
                        "Security: System.exit() calls are not allowed in rules",
                        null,
                        null));
                hasIssues = true;
            }

            if (drlCode.contains("Runtime.getRuntime()")) {
                builder.message(new ValidationResult.ValidationMessage(
                        ValidationResult.Severity.ERROR,
                        "Security: Runtime.getRuntime() is not allowed in rules",
                        null,
                        null));
                hasIssues = true;
            }

            if (drlCode.contains("ProcessBuilder")) {
                builder.message(new ValidationResult.ValidationMessage(
                        ValidationResult.Severity.ERROR,
                        "Security: ProcessBuilder is not allowed in rules",
                        null,
                        null));
                hasIssues = true;
            }

            return hasIssues ? builder.build() : ValidationResult.success();
        };

        // 5. Package validator - note if non-standard package
        DRLValidator packageValidator = drlCode -> {
            if (!drlCode.contains("package rules")) {
                return ValidationResult.builder()
                        .message(new ValidationResult.ValidationMessage(
                                ValidationResult.Severity.NOTE,
                                "Info: Consider using 'package rules;' for consistency",
                                null,
                                null))
                        .build();
            }
            return ValidationResult.success();
        };

        // Combine all validators
        return new CompositeValidator(
                syntaxValidator,
                commentValidator,
                namingValidator,
                securityValidator,
                packageValidator
        );
    }
}

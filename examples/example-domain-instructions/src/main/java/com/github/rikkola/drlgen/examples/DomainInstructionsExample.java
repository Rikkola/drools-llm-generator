package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Domain Instructions Example - Add domain-specific knowledge for better generation.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Loading domain instructions from a markdown file</li>
 *   <li>Combining domain knowledge with the default DRL guide</li>
 *   <li>Generating rules that follow domain conventions</li>
 * </ul>
 *
 * <p>Domain instructions are especially useful for:
 * <ul>
 *   <li>Defining business terminology</li>
 *   <li>Specifying calculation formulas</li>
 *   <li>Documenting constraints and valid values</li>
 *   <li>Establishing naming conventions</li>
 * </ul>
 *
 * <p>Run with: {@code mvn compile exec:java}
 */
public class DomainInstructionsExample {

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Domain Instructions Example ===\n");

        // Insurance domain requirement
        String requirement = """
            Create rules for insurance risk assessment:

            1. Classify applicants by age:
               - Under 18: cannot be approved
               - 18-25: YOUNG_ADULT (higher risk)
               - 26-64: ADULT (standard risk)
               - 65+: SENIOR (elevated risk)

            2. Calculate risk factor:
               - Start with base risk by age category
               - Add 0.5 if smoker
               - Subtract 0.2 if exercises regularly

            3. Flag high-risk applicants (risk factor > 2.0) for manual review
            """;

        // Define fact types
        String factTypes = """
            Applicant: name (String), age (int), healthScore (int), smoker (boolean),
                       exercisesRegularly (boolean), ageCategory (String), approved (boolean)
            RiskAssessment: applicantName (String), riskFactor (double),
                           requiresReview (boolean), riskCategory (String)
            """;

        System.out.println("Requirement:");
        System.out.println(requirement);

        // Path to domain instructions
        // In a real application, this could be loaded from configuration
        Path domainInstructions = Paths.get("src/main/resources/insurance-rules.md");
        System.out.println("Loading domain instructions from: " + domainInstructions);
        System.out.println();

        // Create the AI model
        // Using larger model for complex domain scenarios
        // Note: Complex multi-rule scenarios may require retries
        ChatModel model = ModelConfiguration.createModel("qwen2.5-coder:14b-instruct-q4_K_M");

        // Build generator with domain instructions
        // The domainInstructions() method appends domain-specific knowledge
        // to the default DRL reference guide
        DRLGenerator generator = DRLGenerator.builder()
                .chatModel(model)
                .domainInstructions(domainInstructions)
                .build();

        System.out.println("Generating DRL with domain instructions...\n");

        // Generate DRL
        GenerationResult result = generator.generate(requirement, factTypes);

        // Print results
        System.out.println("=== Generated DRL ===");
        System.out.println(result.generatedDrl());
        System.out.println();

        if (result.validationPassed()) {
            System.out.println("=== Validation: PASSED ===");
            System.out.println("Generation time: " + result.generationTime().toMillis() + "ms");
            System.out.println();

            // Highlight domain-specific patterns in the generated rules
            String drl = result.generatedDrl();
            System.out.println("=== Domain Pattern Analysis ===");

            if (drl.contains("YOUNG_ADULT") || drl.contains("SENIOR")) {
                System.out.println("- Age categories used correctly");
            }
            if (drl.contains("requiresReview")) {
                System.out.println("- Manual review flagging implemented");
            }
            if (drl.contains("smoker")) {
                System.out.println("- Smoking risk factor considered");
            }
            if (drl.contains("exercisesRegularly")) {
                System.out.println("- Exercise benefit considered");
            }
            if (drl.contains("< 18") || drl.contains("<= 17")) {
                System.out.println("- Minor restriction enforced");
            }
        } else {
            System.out.println("=== Validation: FAILED ===");
            System.out.println("Error: " + result.validationMessage());
        }
    }
}

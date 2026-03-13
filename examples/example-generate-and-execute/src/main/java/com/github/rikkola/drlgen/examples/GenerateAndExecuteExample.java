package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Generate and Execute Example - Generate DRL and run with test facts.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Generating DRL rules for a business scenario</li>
 *   <li>Executing the rules with JSON-formatted test facts</li>
 *   <li>Verifying execution results (rules fired, resulting facts)</li>
 * </ul>
 *
 * <p>Run with: {@code mvn compile exec:java}
 */
public class GenerateAndExecuteExample {

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Generate and Execute Example ===\n");

        // Define the business requirement
        String requirement = """
            Create a rule for order discounts:
            - If order total is greater than 100, set discountPercent to 10
            - If order total is greater than 500, set discountPercent to 20
            """;

        // Define fact types
        String factTypes = """
            Order: orderId (String), total (double), discountPercent (int)
            """;

        // Test facts in JSON format
        // The _type field tells the engine which declared type to use
        String factsJson = """
            [
                {"_type": "Order", "orderId": "ORD001", "total": 150.0, "discountPercent": 0},
                {"_type": "Order", "orderId": "ORD002", "total": 50.0, "discountPercent": 0},
                {"_type": "Order", "orderId": "ORD003", "total": 750.0, "discountPercent": 0}
            ]
            """;

        System.out.println("Requirement:");
        System.out.println(requirement);
        System.out.println("Test Facts:");
        System.out.println(factsJson);
        System.out.println("Generating and executing DRL...\n");

        // Create the AI model
        ChatModel model = ModelConfiguration.createModel("granite4");

        // Build the DRL generator
        DRLGenerator generator = DRLGenerator.builder()
                .chatModel(model)
                .build();

        // Generate AND execute in one call
        // This will:
        // 1. Generate DRL from the requirement
        // 2. Validate the generated DRL
        // 3. Execute the rules with the provided facts
        // 4. Return results including fired rules and modified facts
        GenerationResult result = generator.generateAndExecute(requirement, factTypes, factsJson);

        // Print generated DRL
        System.out.println("=== Generated DRL ===");
        System.out.println(result.generatedDrl());
        System.out.println();

        // Print execution results
        System.out.println("=== Execution Results ===");
        System.out.println("Validation: " + (result.validationPassed() ? "PASSED" : "FAILED"));
        System.out.println("Execution: " + (result.executionPassed() ? "PASSED" : "FAILED"));
        System.out.println("Rules fired: " + result.rulesFired());
        System.out.println("Generation time: " + result.generationTime().toMillis() + "ms");
        System.out.println("Total time: " + result.totalTime().toMillis() + "ms");
        System.out.println();

        // Print resulting facts
        if (result.executionPassed() && !result.resultingFacts().isEmpty()) {
            System.out.println("=== Resulting Facts ===");
            for (Object fact : result.resultingFacts()) {
                System.out.println(fact);
            }
        }

        // Explain the results
        System.out.println();
        if (result.executionPassed()) {
            System.out.println("SUCCESS! The generated rules correctly processed " +
                    result.resultingFacts().size() + " orders.");
            System.out.println("- ORD001 ($150) should have 10% discount");
            System.out.println("- ORD002 ($50) should have no discount");
            System.out.println("- ORD003 ($750) should have 20% discount");
        } else {
            System.out.println("FAILED: " + result.executionMessage());
        }
    }
}

package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;

/**
 * Hello World Example - The simplest possible DRL generation.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating a chat model using ModelConfiguration</li>
 *   <li>Building a DRLGenerator with default settings</li>
 *   <li>Generating DRL from a natural language requirement</li>
 *   <li>Checking the validation result</li>
 * </ul>
 *
 * <p>Run with: {@code mvn compile exec:java}
 */
public class HelloWorldExample {

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Hello World Example ===\n");

        // Define what we want the rules to do
        String requirement = """
            If person age >= 18, set adult to true.
            """;

        // Define the fact types available
        String factTypes = """
            Person: name (String), age (int), adult (boolean)
            """;

        System.out.println("Requirement:");
        System.out.println(requirement);
        System.out.println("Generating DRL...\n");

        // 1. Create the AI model
        // Uses Ollama by default, model configured via TEST_OLLAMA_MODEL env var
        // or defaults to granite4
        ChatModel model = ModelConfiguration.createModel("granite4");

        // 2. Build the DRL generator with all defaults
        // This uses:
        // - Default prompts (from classpath)
        // - Default DRL reference guide
        // - Default cleanup strategy (removes markdown artifacts)
        // - Default Drools validator
        DRLGenerator generator = DRLGenerator.builder()
                .chatModel(model)
                .build();

        // 3. Generate DRL from natural language
        GenerationResult result = generator.generate(requirement, factTypes);

        // 4. Print results
        System.out.println("=== Generated DRL ===");
        System.out.println(result.generatedDrl());
        System.out.println();

        if (result.validationPassed()) {
            System.out.println("=== Validation: PASSED ===");
            System.out.println("Generation time: " + result.generationTime().toMillis() + "ms");
        } else {
            System.out.println("=== Validation: FAILED ===");
            System.out.println("Error: " + result.validationMessage());
        }
    }
}

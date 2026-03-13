package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.prompt.PromptProvider;
import dev.langchain4j.model.chat.ChatModel;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Custom Prompts Example - Override default AI prompts.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Loading custom system and user prompts from files</li>
 *   <li>Using PromptProvider.fromDirectory() to load prompts</li>
 *   <li>How custom prompts affect the generated output</li>
 * </ul>
 *
 * <p>Custom prompts are useful when you need to:
 * <ul>
 *   <li>Change the AI's behavior or focus</li>
 *   <li>Enforce specific code style guidelines</li>
 *   <li>Add company-specific instructions</li>
 *   <li>Optimize prompts for different AI models</li>
 * </ul>
 *
 * <p>Run with: {@code mvn compile exec:java}
 */
public class CustomPromptsExample {

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Custom Prompts Example ===\n");

        // Financial services requirement
        String requirement = """
            Create rules for bank account overdraft protection:

            1. Flag accounts with negative balance as overdrawn
            2. If account is overdrawn and balance < -500, flag for review
            3. If account has overdraft protection enabled, allow up to -1000 before flagging
            """;

        // Define fact types
        String factTypes = """
            Account: accountId (String), balance (double), overdrawn (boolean),
                     overdraftProtection (boolean), requiresReview (boolean)
            """;

        System.out.println("Requirement:");
        System.out.println(requirement);

        // Load custom prompts from the prompts directory
        // This directory should contain system.txt and user.txt
        Path promptsDir = Paths.get("src/main/resources/prompts");
        System.out.println("Loading custom prompts from: " + promptsDir);

        // Create a PromptProvider that loads from the filesystem
        PromptProvider customPrompts = PromptProvider.fromDirectory(promptsDir);

        // Show what we loaded
        System.out.println("\n=== Custom System Prompt (first 200 chars) ===");
        String systemPrompt = customPrompts.getSystemMessage();
        System.out.println(systemPrompt.substring(0, Math.min(200, systemPrompt.length())) + "...\n");

        // Create the AI model
        ChatModel model = ModelConfiguration.createModel("granite4");

        // Build generator with custom prompts
        DRLGenerator generator = DRLGenerator.builder()
                .chatModel(model)
                .promptProvider(customPrompts)  // Use our custom prompts
                .build();

        System.out.println("Generating DRL with custom prompts...\n");

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

            // Check if our custom prompt requirements were followed
            String drl = result.generatedDrl();
            System.out.println("=== Custom Prompt Compliance ===");

            if (drl.contains("//") || drl.contains("/*")) {
                System.out.println("- Comments included (as requested)");
            } else {
                System.out.println("- No comments found (custom prompt requested them)");
            }

            if (drl.contains("salience")) {
                System.out.println("- Salience used for rule ordering");
            }

            if (drl.contains("\"")) {
                System.out.println("- Descriptive rule names in quotes");
            }
        } else {
            System.out.println("=== Validation: FAILED ===");
            System.out.println("Error: " + result.validationMessage());
        }
    }
}

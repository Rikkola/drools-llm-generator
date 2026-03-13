package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;

import java.time.Duration;

/**
 * Error Handling Example - Robust patterns for production error handling.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Handling generation failures gracefully</li>
 *   <li>Processing validation failures</li>
 *   <li>Implementing retry logic</li>
 *   <li>Providing meaningful error messages</li>
 * </ul>
 *
 * <p>Run with: {@code mvn compile exec:java@error-handling}
 */
public class ErrorHandlingExample {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Error Handling Example ===\n");

        // Create generator
        ChatModel model = ModelConfiguration.createModel("granite4");
        DRLGenerator generator = DRLGenerator.builder()
                .chatModel(model)
                .build();

        // Test various scenarios
        System.out.println("=== Scenario 1: Valid Requirement ===");
        handleGeneration(generator,
                "If person age >= 18, set adult to true",
                "Person: name (String), age (int), adult (boolean)");

        System.out.println("\n=== Scenario 2: Complex Requirement (may need retry) ===");
        handleGenerationWithRetry(generator,
                """
                    Create a complex multi-rule system:
                    1. Classify customers by spending tier
                    2. Apply appropriate discounts
                    3. Calculate loyalty points
                    """,
                "Customer: name (String), totalSpending (double), tier (String), discount (int), loyaltyPoints (int)");

        System.out.println("\n=== Scenario 3: Empty Requirement (error case) ===");
        handleGeneration(generator, "", "Person: name (String)");
    }

    /**
     * Basic generation with error handling.
     */
    private static void handleGeneration(DRLGenerator generator,
                                          String requirement,
                                          String factTypes) {
        try {
            // Validate inputs
            if (requirement == null || requirement.isBlank()) {
                System.out.println("ERROR: Requirement cannot be empty");
                return;
            }

            // Generate DRL
            System.out.println("Generating DRL...");
            GenerationResult result = generator.generate(requirement, factTypes);

            // Handle result
            if (result.validationPassed()) {
                System.out.println("SUCCESS: DRL generated and validated");
                System.out.println("Generation time: " + result.generationTime().toMillis() + "ms");
                System.out.println("DRL preview: " +
                        result.generatedDrl().substring(0, Math.min(100, result.generatedDrl().length())) + "...");
            } else {
                System.out.println("VALIDATION FAILED: " + result.validationMessage());
                System.out.println("Generated DRL (invalid):");
                System.out.println(result.generatedDrl());
            }

        } catch (Exception e) {
            System.out.println("EXCEPTION: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            handleException(e);
        }
    }

    /**
     * Generation with retry logic for transient failures.
     */
    private static void handleGenerationWithRetry(DRLGenerator generator,
                                                   String requirement,
                                                   String factTypes) {
        int attempt = 0;
        GenerationResult lastResult = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            System.out.println("Attempt " + attempt + " of " + MAX_RETRIES + "...");

            try {
                GenerationResult result = generator.generate(requirement, factTypes);

                if (result.validationPassed()) {
                    System.out.println("SUCCESS on attempt " + attempt);
                    System.out.println("Generation time: " + result.generationTime().toMillis() + "ms");
                    return;
                }

                // Validation failed - might succeed on retry
                lastResult = result;
                System.out.println("Validation failed, will retry...");

            } catch (Exception e) {
                System.out.println("Exception on attempt " + attempt + ": " + e.getMessage());

                if (isRetryable(e) && attempt < MAX_RETRIES) {
                    System.out.println("Retrying in " + RETRY_DELAY_MS + "ms...");
                    sleep(RETRY_DELAY_MS);
                } else {
                    System.out.println("Non-retryable exception or max retries reached");
                    handleException(e);
                    return;
                }
            }

            // Wait before retry
            if (attempt < MAX_RETRIES) {
                sleep(RETRY_DELAY_MS);
            }
        }

        // All retries exhausted
        System.out.println("FAILED after " + MAX_RETRIES + " attempts");
        if (lastResult != null) {
            System.out.println("Last validation error: " + lastResult.validationMessage());
        }
    }

    /**
     * Determines if an exception is retryable.
     */
    private static boolean isRetryable(Exception e) {
        // Connection/timeout issues are often transient
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("refused") ||
               message.contains("unavailable");
    }

    /**
     * Handles exceptions with appropriate logging.
     */
    private static void handleException(Exception e) {
        String message = e.getMessage() != null ? e.getMessage() : "Unknown error";

        if (message.contains("Connection refused")) {
            System.out.println("HINT: Is Ollama running? Try: ollama serve");
        } else if (message.contains("model not found")) {
            System.out.println("HINT: Pull the model first: ollama pull granite4");
        } else if (message.contains("timeout")) {
            System.out.println("HINT: Try a smaller model or increase timeout");
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

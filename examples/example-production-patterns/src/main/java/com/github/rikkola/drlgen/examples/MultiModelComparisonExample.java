package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Multi-Model Comparison Example - Compare DRL generation across AI models.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Testing the same requirement with multiple models</li>
 *   <li>Comparing generation times and success rates</li>
 *   <li>Implementing model fallback strategies</li>
 *   <li>Finding the best model for your use case</li>
 * </ul>
 *
 * <p>Run with: {@code mvn compile exec:java@multi-model}
 */
public class MultiModelComparisonExample {

    private static final Map<String, DRLGenerator> generatorCache = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Multi-Model Comparison Example ===\n");

        // Models to compare (must be available in Ollama)
        List<String> modelsToTest = List.of(
                "granite4",
                "qwen3"
                // Add more models as needed:
                // "granite4:small-h",
                // "qwen2.5-coder:14b-instruct-q4_K_M"
        );

        // Test requirement
        String requirement = """
            Create rules for a loyalty program:
            1. If customer totalPurchases > 1000, set tier to "GOLD"
            2. If customer totalPurchases > 500, set tier to "SILVER"
            3. Otherwise set tier to "BRONZE"
            4. GOLD members get 20% discount, SILVER 10%, BRONZE 5%
            """;

        String factTypes = """
            Customer: customerId (String), totalPurchases (double),
                      tier (String), discountPercent (int)
            """;

        System.out.println("Requirement:");
        System.out.println(requirement);
        System.out.println("Testing with models: " + modelsToTest);
        System.out.println();

        // Collect results
        List<ModelResult> results = new ArrayList<>();

        for (String modelName : modelsToTest) {
            System.out.println("Testing model: " + modelName);

            try {
                DRLGenerator generator = getOrCreateGenerator(modelName);
                long startTime = System.currentTimeMillis();

                GenerationResult result = generator.generate(requirement, factTypes);

                long totalTime = System.currentTimeMillis() - startTime;

                results.add(new ModelResult(
                        modelName,
                        result.validationPassed(),
                        result.generationTime().toMillis(),
                        totalTime,
                        result.generatedDrl(),
                        result.validationMessage()
                ));

                System.out.printf("  Result: %s | Gen: %dms | Total: %dms%n",
                        result.validationPassed() ? "SUCCESS" : "FAILED",
                        result.generationTime().toMillis(),
                        totalTime);

            } catch (Exception e) {
                System.out.println("  ERROR: " + e.getMessage());
                results.add(new ModelResult(
                        modelName,
                        false,
                        0,
                        0,
                        null,
                        "Exception: " + e.getMessage()
                ));
            }

            System.out.println();
        }

        // Print comparison summary
        printComparisonSummary(results);

        // Show best result
        showBestResult(results);
    }

    private static void printComparisonSummary(List<ModelResult> results) {
        System.out.println("=== Comparison Summary ===");
        System.out.println();
        System.out.printf("%-30s | %-8s | %-10s | %-10s%n",
                "Model", "Status", "Gen Time", "Total Time");
        System.out.println("-".repeat(70));

        for (ModelResult result : results) {
            System.out.printf("%-30s | %-8s | %-10s | %-10s%n",
                    truncate(result.modelName, 30),
                    result.success ? "SUCCESS" : "FAILED",
                    result.success ? result.generationTimeMs + "ms" : "-",
                    result.success ? result.totalTimeMs + "ms" : "-");
        }

        System.out.println();

        // Statistics
        long successCount = results.stream().filter(r -> r.success).count();
        System.out.printf("Success rate: %d/%d (%.0f%%)%n",
                successCount, results.size(),
                (double) successCount / results.size() * 100);

        if (successCount > 0) {
            double avgGenTime = results.stream()
                    .filter(r -> r.success)
                    .mapToLong(r -> r.generationTimeMs)
                    .average()
                    .orElse(0);
            System.out.printf("Average generation time: %.0fms%n", avgGenTime);
        }
    }

    private static void showBestResult(List<ModelResult> results) {
        // Find fastest successful model
        results.stream()
                .filter(r -> r.success)
                .min(Comparator.comparingLong(r -> r.generationTimeMs))
                .ifPresent(best -> {
                    System.out.println("\n=== Best Result: " + best.modelName + " ===");
                    System.out.println("Generation time: " + best.generationTimeMs + "ms");
                    System.out.println("\nGenerated DRL:");
                    System.out.println(best.generatedDrl);
                });
    }

    private static DRLGenerator getOrCreateGenerator(String modelName) {
        return generatorCache.computeIfAbsent(modelName, name -> {
            ChatModel chatModel = ModelConfiguration.createModel(name);
            return DRLGenerator.builder()
                    .chatModel(chatModel)
                    .build();
        });
    }

    private static String truncate(String s, int maxLength) {
        return s.length() <= maxLength ? s : s.substring(0, maxLength - 3) + "...";
    }

    /**
     * Record to hold comparison results for each model.
     */
    record ModelResult(
            String modelName,
            boolean success,
            long generationTimeMs,
            long totalTimeMs,
            String generatedDrl,
            String message
    ) {}
}

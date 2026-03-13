package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests multiple Ollama models to find the best one for CPU-based DRL generation.
 */
public class ModelComparisonTest {

    public static void main(String[] args) {
        // Models to test, ordered by size (smallest first)
        List<String> models = List.of(
            "granite4",                              // 2.1 GB - smallest
            "qwen3",                                 // 5.2 GB
            "codellama:13b",                         // 7.4 GB
            "qwen2.5-coder:14b-instruct-q4_K_M",     // 9.0 GB
            "phi4"                                   // 9.1 GB
        );

        String requirement = "If person age >= 18, set adult to true";
        String factTypes = "Person: name (String), age (int), adult (boolean)";

        System.out.println("=== DRL Generator - Model Comparison Report ===");
        System.out.println();
        System.out.println("Requirement: " + requirement);
        System.out.println("Fact Types:  " + factTypes);
        System.out.println();
        System.out.println("Testing " + models.size() + " models...");
        System.out.println();
        System.out.println(String.format("%-42s | %-8s | %-12s | %-8s",
                "Model", "Status", "Time", "Valid"));
        System.out.println("=".repeat(80));

        List<TestResult> results = new ArrayList<>();

        for (String modelName : models) {
            TestResult result = testModel(modelName, requirement, factTypes);
            results.add(result);

            System.out.println(String.format("%-42s | %-8s | %-12s | %-8s",
                    modelName,
                    result.status,
                    result.timeMs > 0 ? result.timeMs + "ms" : "-",
                    result.valid ? "YES" : "NO"));

            if (result.error != null) {
                String shortError = result.error.length() > 70
                        ? result.error.substring(0, 70) + "..."
                        : result.error;
                System.out.println("  -> " + shortError);
            }
        }

        // Print summary
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("=== SUMMARY ===");
        System.out.println();

        long successCount = results.stream().filter(r -> r.valid).count();
        System.out.println("Success rate: " + successCount + "/" + results.size());
        System.out.println();

        // Find best model (fastest successful)
        results.stream()
                .filter(r -> r.valid)
                .min((a, b) -> Long.compare(a.timeMs, b.timeMs))
                .ifPresent(best -> {
                    System.out.println("RECOMMENDED FOR CPU: " + best.modelName);
                    System.out.println("  - Generation time: " + best.timeMs + "ms");
                    System.out.println();
                    System.out.println("Generated DRL:");
                    System.out.println(best.generatedDrl);
                });

        // List all successful models
        System.out.println();
        System.out.println("=== ALL WORKING MODELS (by speed) ===");
        results.stream()
                .filter(r -> r.valid)
                .sorted((a, b) -> Long.compare(a.timeMs, b.timeMs))
                .forEach(r -> System.out.println(String.format("  %s - %dms",
                        r.modelName, r.timeMs)));
    }

    private static TestResult testModel(String modelName, String requirement, String factTypes) {
        TestResult result = new TestResult();
        result.modelName = modelName;

        try {
            long start = System.currentTimeMillis();

            ChatModel model = ModelConfiguration.createModel(modelName);
            DRLGenerator generator = DRLGenerator.builder()
                    .chatModel(model)
                    .build();

            GenerationResult genResult = generator.generate(requirement, factTypes);

            result.timeMs = System.currentTimeMillis() - start;
            result.valid = genResult.validationPassed();
            result.generatedDrl = genResult.generatedDrl();
            result.status = result.valid ? "SUCCESS" : "INVALID";

            if (!result.valid) {
                result.error = genResult.validationMessage();
            }

        } catch (Exception e) {
            result.status = "ERROR";
            result.valid = false;
            result.error = e.getMessage();
        }

        return result;
    }

    static class TestResult {
        String modelName;
        String status;
        long timeMs;
        boolean valid;
        String generatedDrl;
        String error;
    }
}

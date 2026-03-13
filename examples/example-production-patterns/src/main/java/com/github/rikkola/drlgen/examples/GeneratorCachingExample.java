package com.github.rikkola.drlgen.examples;

import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generator Caching Example - Cache DRLGenerator instances for efficiency.
 *
 * <p>In production applications, you should cache generator instances because:
 * <ul>
 *   <li>Creating chat models has connection/initialization overhead</li>
 *   <li>DRLGenerator instances are thread-safe and reusable</li>
 *   <li>Caching improves response times for subsequent requests</li>
 * </ul>
 *
 * <p>This pattern is used in the drlgen-ui REST API.
 *
 * <p>Run with: {@code mvn compile exec:java}
 */
public class GeneratorCachingExample {

    /**
     * Thread-safe cache of generators by model name.
     * Uses ConcurrentHashMap for safe concurrent access.
     */
    private static final Map<String, DRLGenerator> generatorCache = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("=== DRL Generator - Caching Pattern Example ===\n");

        String requirement = "If person age >= 18, set adult to true";
        String factTypes = "Person: name (String), age (int), adult (boolean)";

        String modelName = "granite4";

        // First request - generator will be created
        System.out.println("First request (generator created)...");
        long start1 = System.currentTimeMillis();
        DRLGenerator generator1 = getOrCreateGenerator(modelName);
        GenerationResult result1 = generator1.generate(requirement, factTypes);
        long time1 = System.currentTimeMillis() - start1;

        System.out.println("Result: " + (result1.validationPassed() ? "SUCCESS" : "FAILED"));
        System.out.println("Total time (including generator creation): " + time1 + "ms\n");

        // Second request - generator retrieved from cache
        System.out.println("Second request (generator from cache)...");
        long start2 = System.currentTimeMillis();
        DRLGenerator generator2 = getOrCreateGenerator(modelName);
        GenerationResult result2 = generator2.generate(requirement, factTypes);
        long time2 = System.currentTimeMillis() - start2;

        System.out.println("Result: " + (result2.validationPassed() ? "SUCCESS" : "FAILED"));
        System.out.println("Total time (generator cached): " + time2 + "ms\n");

        // Verify same instance
        System.out.println("=== Cache Verification ===");
        System.out.println("Same generator instance: " + (generator1 == generator2));
        System.out.println("Cache size: " + generatorCache.size());
        System.out.println("Cached models: " + generatorCache.keySet());
    }

    /**
     * Gets an existing generator from cache or creates a new one.
     *
     * <p>This method is thread-safe due to ConcurrentHashMap.computeIfAbsent().
     *
     * @param modelName the model name/id
     * @return the cached or newly created generator
     */
    public static DRLGenerator getOrCreateGenerator(String modelName) {
        return generatorCache.computeIfAbsent(modelName, name -> {
            System.out.println("  Creating new generator for model: " + name);
            ChatModel chatModel = ModelConfiguration.createModel(name);
            return DRLGenerator.builder()
                    .chatModel(chatModel)
                    .build();
        });
    }

    /**
     * Clears the generator cache.
     * Call this if you need to force recreation of generators (e.g., config change).
     */
    public static void clearCache() {
        generatorCache.clear();
    }

    /**
     * Removes a specific generator from cache.
     *
     * @param modelName the model to remove
     */
    public static void removeFromCache(String modelName) {
        generatorCache.remove(modelName);
    }
}

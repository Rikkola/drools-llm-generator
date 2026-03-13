package com.github.rikkola.drlgen.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class for loading domain-specific instructions from file paths.
 * Provides optional caching for frequently accessed instruction files.
 */
public final class DomainInstructionsLoader {

    private static final Logger logger = LoggerFactory.getLogger(DomainInstructionsLoader.class);

    // Optional cache: path -> content (keyed by absolute path string)
    private static final Map<String, String> cache = new ConcurrentHashMap<>();

    private DomainInstructionsLoader() {
        // Utility class
    }

    /**
     * Loads domain instructions from a file path.
     * Returns empty string if path is null or file doesn't exist.
     *
     * @param filePath path to the domain instructions file
     * @return file content or empty string if not available
     */
    public static String load(Path filePath) {
        return load(filePath, false);
    }

    /**
     * Loads domain instructions with optional caching.
     *
     * @param filePath path to the domain instructions file
     * @param useCache if true, caches the content for subsequent calls
     * @return file content or empty string if not available
     */
    public static String load(Path filePath, boolean useCache) {
        if (filePath == null) {
            logger.debug("Domain instructions path is null, returning empty");
            return "";
        }

        Path absolutePath = filePath.toAbsolutePath().normalize();
        String pathKey = absolutePath.toString();

        if (useCache && cache.containsKey(pathKey)) {
            logger.debug("Returning cached domain instructions for: {}", pathKey);
            return cache.get(pathKey);
        }

        if (!Files.exists(absolutePath)) {
            logger.warn("Domain instructions file not found: {}", absolutePath);
            return "";
        }

        if (!Files.isRegularFile(absolutePath)) {
            logger.warn("Domain instructions path is not a regular file: {}", absolutePath);
            return "";
        }

        try {
            String content = Files.readString(absolutePath, StandardCharsets.UTF_8);
            logger.info("Loaded domain instructions from: {} ({} chars)", absolutePath, content.length());

            if (useCache) {
                cache.put(pathKey, content);
            }

            return content;
        } catch (IOException e) {
            logger.error("Failed to read domain instructions from {}: {}", absolutePath, e.getMessage());
            return "";
        }
    }

    /**
     * Clears the cache. Primarily used for testing.
     */
    public static void clearCache() {
        cache.clear();
    }
}

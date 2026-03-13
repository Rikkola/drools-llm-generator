package com.github.rikkola.drlgen.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * Utility class for loading the DRL reference guide from classpath resources.
 */
public final class DRLGuideLoader {

    private static final String GUIDE_RESOURCE_PATH = "drl-reference-guide.md";

    private static String cachedGuide;

    private DRLGuideLoader() {
        // Utility class
    }

    /**
     * Loads the DRL reference guide from the classpath.
     * The guide is cached after first load for performance.
     *
     * @return the DRL reference guide content
     * @throws IllegalStateException if the guide cannot be loaded
     */
    public static String loadGuide() {
        if (cachedGuide != null) {
            return cachedGuide;
        }

        try (InputStream is = DRLGuideLoader.class.getClassLoader()
                .getResourceAsStream(GUIDE_RESOURCE_PATH)) {
            if (is == null) {
                throw new IllegalStateException(
                        "DRL reference guide not found: " + GUIDE_RESOURCE_PATH);
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                cachedGuide = reader.lines().collect(Collectors.joining("\n"));
                return cachedGuide;
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load DRL reference guide", e);
        }
    }

    /**
     * Clears the cached guide. Primarily used for testing.
     */
    public static void clearCache() {
        cachedGuide = null;
    }
}

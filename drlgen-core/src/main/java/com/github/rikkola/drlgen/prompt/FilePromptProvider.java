package com.github.rikkola.drlgen.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * PromptProvider implementation that loads prompts from the filesystem.
 *
 * <p>Prompts are loaded from the following file paths:
 * <ul>
 *   <li>{@code {basePath}/system.txt} - System message</li>
 *   <li>{@code {basePath}/user.txt} - User message template</li>
 * </ul>
 *
 * <p>This provider is useful for:
 * <ul>
 *   <li>Development and testing with frequently changing prompts</li>
 *   <li>Production deployments where prompts are externalized from the JAR</li>
 *   <li>Multi-tenant scenarios with customer-specific prompts</li>
 * </ul>
 */
public class FilePromptProvider implements PromptProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FilePromptProvider.class);

    private static final String SYSTEM_FILE = "system.txt";
    private static final String USER_FILE = "user.txt";

    private final Path basePath;
    private final boolean cacheEnabled;
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Creates a provider that loads prompts from the specified directory with caching enabled.
     *
     * @param basePath the base directory containing prompt files
     */
    public FilePromptProvider(Path basePath) {
        this(basePath, true);
    }

    /**
     * Creates a provider that loads prompts from the specified directory.
     *
     * @param basePath the base directory containing prompt files
     * @param cacheEnabled whether to cache loaded prompts
     */
    public FilePromptProvider(Path basePath, boolean cacheEnabled) {
        this.basePath = basePath;
        this.cacheEnabled = cacheEnabled;
        LOG.debug("FilePromptProvider initialized with basePath: {}, cacheEnabled: {}",
                  basePath, cacheEnabled);
    }

    @Override
    public String getSystemMessage() {
        return loadPrompt(SYSTEM_FILE);
    }

    @Override
    public String getUserMessageTemplate() {
        return loadPrompt(USER_FILE);
    }

    private String loadPrompt(String filename) {
        if (cacheEnabled) {
            return cache.computeIfAbsent(filename, this::loadFromFile);
        }
        return loadFromFile(filename);
    }

    private String loadFromFile(String filename) {
        Path filePath = basePath.resolve(filename);

        if (!Files.exists(filePath)) {
            throw new IllegalStateException(
                "Prompt file not found: " + filePath.toAbsolutePath());
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            LOG.debug("Loaded prompt from file: {} ({} chars)", filePath, content.length());
            return content;
        } catch (IOException e) {
            throw new IllegalStateException(
                "Error reading prompt file: " + filePath.toAbsolutePath(), e);
        }
    }

    /**
     * Clears the prompt cache. Useful for hot-reloading scenarios.
     */
    public void clearCache() {
        cache.clear();
        LOG.debug("Prompt cache cleared");
    }

    /**
     * Returns the base path where prompts are loaded from.
     *
     * @return the base path
     */
    public Path getBasePath() {
        return basePath;
    }
}

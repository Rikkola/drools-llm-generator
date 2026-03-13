package com.github.rikkola.drlgen.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * PromptProvider implementation that loads prompts from the classpath.
 *
 * <p>Prompts are loaded from the following classpath locations:
 * <ul>
 *   <li>{@code prompts/{variant}/system.txt} - System message</li>
 *   <li>{@code prompts/{variant}/user.txt} - User message template</li>
 * </ul>
 *
 * <p>If variant-specific prompts are not found, falls back to the default variant.</p>
 */
public class ClasspathPromptProvider implements PromptProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathPromptProvider.class);

    private static final String DEFAULT_VARIANT = "default";
    private static final String PROMPTS_BASE = "prompts/";
    private static final String SYSTEM_FILE = "system.txt";
    private static final String USER_FILE = "user.txt";

    private final String variant;
    private final ConcurrentMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Creates a provider that loads prompts from the default variant.
     */
    public ClasspathPromptProvider() {
        this(DEFAULT_VARIANT);
    }

    /**
     * Creates a provider that loads prompts from a specific variant.
     *
     * <p>This allows for model-family specific prompts, e.g., "qwen", "llama", "granite".</p>
     *
     * @param variant the prompt variant to load
     */
    public ClasspathPromptProvider(String variant) {
        this.variant = variant != null ? variant : DEFAULT_VARIANT;
        LOG.debug("ClasspathPromptProvider initialized with variant: {}", this.variant);
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
        String cacheKey = variant + "/" + filename;
        return cache.computeIfAbsent(cacheKey, key -> {
            // Try variant-specific first
            String variantPath = PROMPTS_BASE + variant + "/" + filename;
            String content = loadFromClasspath(variantPath);

            if (content == null && !DEFAULT_VARIANT.equals(variant)) {
                // Fall back to default variant
                LOG.debug("Variant '{}' prompt not found, falling back to default", variant);
                String defaultPath = PROMPTS_BASE + DEFAULT_VARIANT + "/" + filename;
                content = loadFromClasspath(defaultPath);
            }

            if (content == null) {
                throw new IllegalStateException(
                    "Could not load prompt file: " + filename + " for variant: " + variant);
            }

            return content;
        });
    }

    private String loadFromClasspath(String path) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                LOG.trace("Resource not found: {}", path);
                return null;
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LOG.debug("Loaded prompt from classpath: {} ({} chars)", path, content.length());
            return content;
        } catch (IOException e) {
            LOG.warn("Error reading prompt file from classpath: {}", path, e);
            return null;
        }
    }

    /**
     * Clears the prompt cache. Useful for testing or hot-reloading scenarios.
     */
    public void clearCache() {
        cache.clear();
        LOG.debug("Prompt cache cleared");
    }
}

package com.github.rikkola.drlgen.guide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * GuideProvider implementation that loads the DRL guide from the classpath.
 *
 * <p>By default, loads from {@code drl-reference-guide.md} in the classpath root.
 * Supports custom resource paths and optional caching.</p>
 */
public class ClasspathGuideProvider implements GuideProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ClasspathGuideProvider.class);

    private static final String DEFAULT_GUIDE_PATH = "drl-reference-guide.md";

    private final String resourcePath;
    private final boolean cacheEnabled;
    private volatile String cachedGuide;

    /**
     * Creates a provider that loads the default guide from classpath with caching.
     */
    public ClasspathGuideProvider() {
        this(DEFAULT_GUIDE_PATH, true);
    }

    /**
     * Creates a provider that loads a guide from a specific classpath location.
     *
     * @param resourcePath the classpath resource path
     */
    public ClasspathGuideProvider(String resourcePath) {
        this(resourcePath, true);
    }

    /**
     * Creates a provider with full configuration options.
     *
     * @param resourcePath the classpath resource path
     * @param cacheEnabled whether to cache the loaded guide
     */
    public ClasspathGuideProvider(String resourcePath, boolean cacheEnabled) {
        this.resourcePath = resourcePath != null ? resourcePath : DEFAULT_GUIDE_PATH;
        this.cacheEnabled = cacheEnabled;
        LOG.debug("ClasspathGuideProvider initialized with resourcePath: {}, cacheEnabled: {}",
                  this.resourcePath, cacheEnabled);
    }

    @Override
    public String getGuide() {
        if (cacheEnabled && cachedGuide != null) {
            return cachedGuide;
        }

        String guide = loadFromClasspath();

        if (cacheEnabled) {
            cachedGuide = guide;
        }

        return guide;
    }

    private String loadFromClasspath() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException(
                    "DRL reference guide not found on classpath: " + resourcePath);
            }

            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            LOG.debug("Loaded guide from classpath: {} ({} chars)", resourcePath, content.length());
            return content;
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to load DRL reference guide from classpath: " + resourcePath, e);
        }
    }

    /**
     * Clears the cached guide. Useful for testing or hot-reloading scenarios.
     */
    public void clearCache() {
        cachedGuide = null;
        LOG.debug("Guide cache cleared");
    }

    /**
     * Returns the resource path where the guide is loaded from.
     *
     * @return the resource path
     */
    public String getResourcePath() {
        return resourcePath;
    }
}

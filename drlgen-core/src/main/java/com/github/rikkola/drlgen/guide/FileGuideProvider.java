package com.github.rikkola.drlgen.guide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GuideProvider implementation that loads the DRL guide from the filesystem.
 *
 * <p>This provider is useful for:
 * <ul>
 *   <li>Development and testing with frequently changing guides</li>
 *   <li>Production deployments where guides are externalized from the JAR</li>
 *   <li>Multi-tenant scenarios with customer-specific guides</li>
 * </ul>
 */
public class FileGuideProvider implements GuideProvider {

    private static final Logger LOG = LoggerFactory.getLogger(FileGuideProvider.class);

    private final Path filePath;
    private final boolean cacheEnabled;
    private volatile String cachedGuide;

    /**
     * Creates a provider that loads the guide from a file with caching enabled.
     *
     * @param filePath path to the guide file
     */
    public FileGuideProvider(Path filePath) {
        this(filePath, true);
    }

    /**
     * Creates a provider with full configuration options.
     *
     * @param filePath path to the guide file
     * @param cacheEnabled whether to cache the loaded guide
     */
    public FileGuideProvider(Path filePath, boolean cacheEnabled) {
        this.filePath = filePath;
        this.cacheEnabled = cacheEnabled;
        LOG.debug("FileGuideProvider initialized with filePath: {}, cacheEnabled: {}",
                  filePath, cacheEnabled);
    }

    @Override
    public String getGuide() {
        if (cacheEnabled && cachedGuide != null) {
            return cachedGuide;
        }

        String guide = loadFromFile();

        if (cacheEnabled) {
            cachedGuide = guide;
        }

        return guide;
    }

    private String loadFromFile() {
        if (!Files.exists(filePath)) {
            throw new IllegalStateException(
                "Guide file not found: " + filePath.toAbsolutePath());
        }

        try {
            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            LOG.debug("Loaded guide from file: {} ({} chars)", filePath, content.length());
            return content;
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to load guide from file: " + filePath.toAbsolutePath(), e);
        }
    }

    /**
     * Clears the cached guide. Useful for hot-reloading scenarios.
     */
    public void clearCache() {
        cachedGuide = null;
        LOG.debug("Guide cache cleared");
    }

    /**
     * Returns the file path where the guide is loaded from.
     *
     * @return the file path
     */
    public Path getFilePath() {
        return filePath;
    }
}

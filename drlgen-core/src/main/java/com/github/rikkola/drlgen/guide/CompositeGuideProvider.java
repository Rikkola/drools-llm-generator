package com.github.rikkola.drlgen.guide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * GuideProvider that combines a base guide with domain-specific instructions.
 *
 * <p>The composite guide is created by appending domain instructions to the base guide
 * with a clear section header. This allows for domain-specific customization while
 * maintaining the core DRL reference material.</p>
 *
 * <p>Example output structure:
 * <pre>
 * [Base guide content]
 *
 * ---
 *
 * ## Domain-Specific Instructions
 *
 * The following domain-specific rules and constraints MUST be followed...
 *
 * [Domain instructions content]
 * </pre>
 */
public class CompositeGuideProvider implements GuideProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeGuideProvider.class);

    private static final String DOMAIN_SECTION_HEADER = """

        ---

        ## Domain-Specific Instructions

        The following domain-specific rules and constraints MUST be followed in addition to the general DRL syntax rules above:

        """;

    private final GuideProvider baseProvider;
    private final Path domainInstructionsPath;
    private final boolean cacheEnabled;
    private volatile String cachedGuide;

    /**
     * Creates a composite provider with caching enabled.
     *
     * @param baseProvider base guide provider
     * @param domainInstructionsPath path to domain instructions (may be null)
     */
    public CompositeGuideProvider(GuideProvider baseProvider, Path domainInstructionsPath) {
        this(baseProvider, domainInstructionsPath, true);
    }

    /**
     * Creates a composite provider with full configuration options.
     *
     * @param baseProvider base guide provider
     * @param domainInstructionsPath path to domain instructions (may be null)
     * @param cacheEnabled whether to cache the merged guide
     */
    public CompositeGuideProvider(GuideProvider baseProvider, Path domainInstructionsPath, boolean cacheEnabled) {
        this.baseProvider = baseProvider;
        this.domainInstructionsPath = domainInstructionsPath;
        this.cacheEnabled = cacheEnabled;
        LOG.debug("CompositeGuideProvider initialized with domainPath: {}, cacheEnabled: {}",
                  domainInstructionsPath, cacheEnabled);
    }

    @Override
    public String getGuide() {
        if (cacheEnabled && cachedGuide != null) {
            return cachedGuide;
        }

        String mergedGuide = buildMergedGuide();

        if (cacheEnabled) {
            cachedGuide = mergedGuide;
        }

        return mergedGuide;
    }

    private String buildMergedGuide() {
        String baseGuide = baseProvider.getGuide();
        String domainInstructions = loadDomainInstructions();

        if (domainInstructions == null || domainInstructions.isBlank()) {
            LOG.debug("No domain instructions, returning base guide only");
            return baseGuide;
        }

        LOG.debug("Merging base guide ({} chars) with domain instructions ({} chars)",
                  baseGuide.length(), domainInstructions.length());

        return baseGuide + DOMAIN_SECTION_HEADER + domainInstructions.trim();
    }

    private String loadDomainInstructions() {
        if (domainInstructionsPath == null) {
            return null;
        }

        Path absolutePath = domainInstructionsPath.toAbsolutePath().normalize();

        if (!Files.exists(absolutePath)) {
            LOG.warn("Domain instructions file not found: {}", absolutePath);
            return null;
        }

        if (!Files.isRegularFile(absolutePath)) {
            LOG.warn("Domain instructions path is not a regular file: {}", absolutePath);
            return null;
        }

        try {
            String content = Files.readString(absolutePath, StandardCharsets.UTF_8);
            LOG.info("Loaded domain instructions from: {} ({} chars)", absolutePath, content.length());
            return content;
        } catch (IOException e) {
            LOG.error("Failed to read domain instructions from {}: {}", absolutePath, e.getMessage());
            return null;
        }
    }

    /**
     * Clears the cached merged guide. Useful for hot-reloading scenarios.
     */
    public void clearCache() {
        cachedGuide = null;
        LOG.debug("Composite guide cache cleared");
    }
}

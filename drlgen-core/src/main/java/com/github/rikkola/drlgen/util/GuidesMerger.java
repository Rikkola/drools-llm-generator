package com.github.rikkola.drlgen.util;

/**
 * Utility for merging DRL reference guide with domain-specific instructions.
 */
public final class GuidesMerger {

    private static final String DOMAIN_SECTION_HEADER = """

        ---

        ## Domain-Specific Instructions

        The following domain-specific rules and constraints MUST be followed in addition to the general DRL syntax rules above:

        """;

    private GuidesMerger() {
        // Utility class
    }

    /**
     * Merges the DRL reference guide with optional domain instructions.
     * Domain instructions are appended after the main guide with a clear section header.
     *
     * @param drlGuide           the base DRL reference guide
     * @param domainInstructions domain-specific instructions (may be null or empty)
     * @return merged guide content
     */
    public static String merge(String drlGuide, String domainInstructions) {
        if (drlGuide == null || drlGuide.isBlank()) {
            throw new IllegalArgumentException("DRL guide cannot be null or blank");
        }

        if (domainInstructions == null || domainInstructions.isBlank()) {
            return drlGuide;
        }

        return drlGuide + DOMAIN_SECTION_HEADER + domainInstructions.trim();
    }
}

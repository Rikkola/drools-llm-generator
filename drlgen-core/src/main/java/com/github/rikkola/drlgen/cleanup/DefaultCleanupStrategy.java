package com.github.rikkola.drlgen.cleanup;

import com.github.rikkola.drlgen.util.StringCleanupUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default cleanup strategy that handles common AI generation artifacts.
 *
 * <p>This strategy performs the following cleanup operations:
 * <ol>
 *   <li>Remove markdown code block markers (```drl, ```drools)</li>
 *   <li>Fix boolean setter names based on field naming conventions</li>
 *   <li>Fix logical operators in pattern constraints (or/and → ||/&&)</li>
 *   <li>Remove commas between patterns in when clause (common AI mistake)</li>
 * </ol>
 *
 * <p>Delegates to {@link StringCleanupUtils} for common cleanup operations.
 */
public class DefaultCleanupStrategy implements DRLCleanupStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCleanupStrategy.class);

    @Override
    public String cleanup(String drl) {
        if (drl == null) {
            return null;
        }

        LOG.debug("Cleaning up DRL ({} chars)", drl.length());

        // Use StringCleanupUtils for common cleanup operations
        String cleaned = StringCleanupUtils.cleanupDrl(drl);

        // Additional cleanup: Remove commas between patterns in when clause
        cleaned = removeCommasBetweenPatterns(cleaned);

        LOG.debug("Cleanup complete ({} chars after cleanup)", cleaned.length());

        return cleaned;
    }

    /**
     * Removes commas between patterns in the when clause.
     *
     * <p>AI models often incorrectly add commas between patterns:
     * <pre>
     * when
     *     $a : TypeA(),
     *     $b : TypeB()  // WRONG: comma after TypeA()
     * </pre>
     *
     * <p>This should be:
     * <pre>
     * when
     *     $a : TypeA()
     *     $b : TypeB()  // CORRECT: no comma between patterns
     * </pre>
     */
    private String removeCommasBetweenPatterns(String drl) {
        StringBuilder result = new StringBuilder();
        String[] lines = drl.split("\n");
        boolean inWhenBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Track when we enter/exit when blocks
            if (trimmed.equals("when") || trimmed.startsWith("when ")) {
                inWhenBlock = true;
            } else if (trimmed.equals("then") || trimmed.startsWith("then ")) {
                inWhenBlock = false;
            }

            // If we're in a when block and the line ends with ), followed by comma
            // and the next line looks like a pattern, remove the comma
            if (inWhenBlock && i < lines.length - 1) {
                String nextLine = lines[i + 1].trim();

                // Check if current line ends with ), or ),  (pattern followed by comma)
                // and next line starts a new pattern
                if (trimmed.matches(".*\\)\\s*,\\s*$") && isStartOfPattern(nextLine)) {
                    // Remove trailing comma (keep everything up to and including the closing paren)
                    line = line.replaceFirst("\\)\\s*,\\s*$", ")");
                    LOG.trace("Removed comma between patterns: {}", trimmed);
                }
            }

            result.append(line).append("\n");
        }

        // Remove trailing newline if original didn't have one
        if (!drl.endsWith("\n") && result.length() > 0) {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    /**
     * Checks if a line looks like the start of a pattern in a when clause.
     */
    private boolean isStartOfPattern(String trimmedLine) {
        // Matches patterns like:
        // $var : Type(
        // Type(
        // not Type(
        // not $var : Type(
        // exists Type(
        return trimmedLine.matches("^(not\\s+|exists\\s+)?(\\$\\w+\\s*:\\s*)?\\w+\\s*\\(.*");
    }
}

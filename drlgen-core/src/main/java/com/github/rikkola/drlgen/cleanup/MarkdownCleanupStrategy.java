package com.github.rikkola.drlgen.cleanup;

/**
 * Cleanup strategy that only removes markdown code block markers.
 *
 * <p>This is a lightweight strategy useful when the AI model produces clean DRL
 * but wraps it in markdown formatting.</p>
 */
public class MarkdownCleanupStrategy implements DRLCleanupStrategy {

    @Override
    public String cleanup(String drl) {
        if (drl == null) {
            return null;
        }

        return drl.replaceAll("```(?:drl|drools)?\\n?", "")
                  .replaceAll("```\\n?", "")
                  .trim();
    }
}

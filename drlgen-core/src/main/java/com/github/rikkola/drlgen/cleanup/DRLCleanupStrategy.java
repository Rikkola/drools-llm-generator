package com.github.rikkola.drlgen.cleanup;

import java.util.Arrays;
import java.util.List;

/**
 * Strategy interface for cleaning up AI-generated DRL code.
 *
 * <p>Different AI models may produce different artifacts that need cleanup,
 * such as markdown code blocks, incorrect operator usage, or wrong setter names.
 * This interface allows for customizable cleanup pipelines.</p>
 *
 * <p>Common cleanup tasks include:
 * <ul>
 *   <li>Removing markdown code block markers (```drl)</li>
 *   <li>Fixing boolean setter names (setXxx vs setIsXxx)</li>
 *   <li>Fixing logical operators in constraints (or/and vs ||/&&)</li>
 *   <li>Removing explanatory text or comments</li>
 * </ul>
 */
@FunctionalInterface
public interface DRLCleanupStrategy {

    /**
     * Cleans up the generated DRL code.
     *
     * @param drl the raw generated DRL code (may be null)
     * @return the cleaned DRL code, or null if input was null
     */
    String cleanup(String drl);

    /**
     * Creates a composite strategy that applies multiple strategies in order.
     *
     * @param strategies the strategies to apply
     * @return a composite strategy
     */
    static DRLCleanupStrategy composite(DRLCleanupStrategy... strategies) {
        return composite(Arrays.asList(strategies));
    }

    /**
     * Creates a composite strategy that applies multiple strategies in order.
     *
     * @param strategies the strategies to apply
     * @return a composite strategy
     */
    static DRLCleanupStrategy composite(List<DRLCleanupStrategy> strategies) {
        return new CompositeCleanupStrategy(strategies);
    }

    /**
     * Creates the default cleanup strategy that handles common AI generation artifacts.
     *
     * @return the default cleanup strategy
     */
    static DRLCleanupStrategy createDefault() {
        return new DefaultCleanupStrategy();
    }

    /**
     * Creates a no-op cleanup strategy that returns input unchanged.
     *
     * @return a no-op strategy
     */
    static DRLCleanupStrategy noOp() {
        return drl -> drl;
    }

    /**
     * Creates a strategy that only removes markdown code blocks.
     *
     * @return a markdown removal strategy
     */
    static DRLCleanupStrategy markdownOnly() {
        return new MarkdownCleanupStrategy();
    }
}

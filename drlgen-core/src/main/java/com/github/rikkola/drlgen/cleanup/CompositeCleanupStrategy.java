package com.github.rikkola.drlgen.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Cleanup strategy that chains multiple strategies together.
 *
 * <p>Strategies are applied in the order they are provided. Each strategy
 * receives the output of the previous strategy as input.</p>
 *
 * <p>Example usage:
 * <pre>
 * DRLCleanupStrategy composite = DRLCleanupStrategy.composite(
 *     new MarkdownCleanupStrategy(),
 *     customModelSpecificCleanup,
 *     new BooleanSetterFixStrategy()
 * );
 * </pre>
 */
public class CompositeCleanupStrategy implements DRLCleanupStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeCleanupStrategy.class);

    private final List<DRLCleanupStrategy> strategies;

    /**
     * Creates a composite strategy with the given list of strategies.
     *
     * @param strategies the strategies to apply in order
     */
    public CompositeCleanupStrategy(List<DRLCleanupStrategy> strategies) {
        this.strategies = new ArrayList<>(strategies);
        LOG.debug("CompositeCleanupStrategy initialized with {} strategies", this.strategies.size());
    }

    @Override
    public String cleanup(String drl) {
        if (drl == null) {
            return null;
        }

        String result = drl;

        for (int i = 0; i < strategies.size(); i++) {
            DRLCleanupStrategy strategy = strategies.get(i);
            LOG.trace("Applying cleanup strategy {} of {}: {}",
                      i + 1, strategies.size(), strategy.getClass().getSimpleName());
            result = strategy.cleanup(result);
        }

        return result;
    }

    /**
     * Returns an immutable copy of the strategies in this composite.
     *
     * @return the list of strategies
     */
    public List<DRLCleanupStrategy> getStrategies() {
        return List.copyOf(strategies);
    }
}

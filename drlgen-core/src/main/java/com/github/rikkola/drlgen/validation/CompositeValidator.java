package com.github.rikkola.drlgen.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Validator that combines multiple validators and aggregates their results.
 *
 * <p>All validators are run, and their messages are combined into a single result.
 * The combined result is valid only if all individual validators report success.</p>
 */
public class CompositeValidator implements DRLValidator {

    private static final Logger LOG = LoggerFactory.getLogger(CompositeValidator.class);

    private final List<DRLValidator> validators;

    /**
     * Creates a composite validator with the given validators.
     *
     * @param validators the validators to combine
     */
    public CompositeValidator(DRLValidator... validators) {
        this.validators = Arrays.asList(validators);
        LOG.debug("CompositeValidator initialized with {} validators", this.validators.size());
    }

    /**
     * Creates a composite validator with the given list of validators.
     *
     * @param validators the validators to combine
     */
    public CompositeValidator(List<DRLValidator> validators) {
        this.validators = new ArrayList<>(validators);
        LOG.debug("CompositeValidator initialized with {} validators", this.validators.size());
    }

    @Override
    public ValidationResult validate(String drlCode) {
        LOG.debug("Running {} validators", validators.size());

        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        for (int i = 0; i < validators.size(); i++) {
            DRLValidator validator = validators.get(i);
            LOG.trace("Running validator {} of {}: {}",
                      i + 1, validators.size(), validator.getClass().getSimpleName());

            ValidationResult result = validator.validate(drlCode);

            for (ValidationResult.ValidationMessage message : result.getMessages()) {
                resultBuilder.message(message);
            }
        }

        return resultBuilder.build();
    }

    /**
     * Returns an immutable copy of the validators in this composite.
     *
     * @return the list of validators
     */
    public List<DRLValidator> getValidators() {
        return List.copyOf(validators);
    }
}

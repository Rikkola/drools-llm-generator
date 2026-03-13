package com.github.rikkola.drlgen.validation;

/**
 * Interface for validating DRL code.
 *
 * <p>Implementations can provide different validation strategies, such as:
 * <ul>
 *   <li>Syntax validation using Drools verifier</li>
 *   <li>Semantic validation for rule correctness</li>
 *   <li>Custom domain-specific validation rules</li>
 *   <li>Style/linting validation</li>
 * </ul>
 *
 * <p>The default implementation uses the Drools verifier with fault finding
 * for precise error location.</p>
 */
public interface DRLValidator {

    /**
     * Validates the given DRL code.
     *
     * @param drlCode the DRL code to validate
     * @return the validation result
     */
    ValidationResult validate(String drlCode);

    /**
     * Creates the default validator using Drools verifier.
     *
     * @return the default validator
     */
    static DRLValidator createDefault() {
        return new DroolsValidator();
    }

    /**
     * Creates a no-op validator that always returns success.
     *
     * <p>Useful for testing or when validation is not required.</p>
     *
     * @return a no-op validator
     */
    static DRLValidator noOp() {
        return drlCode -> ValidationResult.success();
    }

    /**
     * Creates a composite validator that runs multiple validators.
     *
     * <p>All validators are run, and their results are combined.
     * The combined result is valid only if all individual results are valid.</p>
     *
     * @param validators the validators to combine
     * @return a composite validator
     */
    static DRLValidator composite(DRLValidator... validators) {
        return new CompositeValidator(validators);
    }
}

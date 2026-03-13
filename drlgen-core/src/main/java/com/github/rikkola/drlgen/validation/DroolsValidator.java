package com.github.rikkola.drlgen.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * DRL validator implementation using the Drools verifier.
 *
 * <p>This validator:
 * <ol>
 *   <li>Runs the Drools verifier for syntax checking</li>
 *   <li>Uses DrlFaultFinder for precise error location when errors are found</li>
 *   <li>Returns structured ValidationResult with all messages</li>
 * </ol>
 */
public class DroolsValidator implements DRLValidator {

    private static final Logger LOG = LoggerFactory.getLogger(DroolsValidator.class);

    private final DRLVerifier verifier;
    private final DrlFaultFinder faultFinder;

    /**
     * Creates a validator with default verifier and fault finder.
     */
    public DroolsValidator() {
        this(new DRLVerifier(), new DrlFaultFinder());
    }

    /**
     * Creates a validator with custom verifier and fault finder.
     *
     * @param verifier the verifier to use
     * @param faultFinder the fault finder to use
     */
    public DroolsValidator(DRLVerifier verifier, DrlFaultFinder faultFinder) {
        this.verifier = verifier;
        this.faultFinder = faultFinder;
    }

    @Override
    public ValidationResult validate(String drlCode) {
        LOG.debug("Validating DRL ({} chars)", drlCode != null ? drlCode.length() : 0);

        if (drlCode == null || drlCode.trim().isEmpty()) {
            LOG.warn("DRL validation failed: null or empty code");
            return ValidationResult.error("DRL code cannot be null or empty");
        }

        try {
            String verificationResult = verifier.verify(drlCode);
            LOG.debug("Verifier result: {}", verificationResult);

            // Parse the verification result string into structured messages
            List<ValidationResult.ValidationMessage> messages = parseVerificationResult(verificationResult);

            // If there are errors, try to get precise location
            boolean hasErrors = messages.stream()
                    .anyMatch(m -> m.severity() == ValidationResult.Severity.ERROR);

            if (hasErrors) {
                try {
                    DrlFaultFinder.FaultLocation faultLocation = faultFinder.findFaultyLine(drlCode);
                    if (faultLocation != null) {
                        LOG.debug("Fault found at line {}: {}",
                                  faultLocation.getLineNumber(), faultLocation.getErrorMessage());
                        // Add detailed fault information
                        messages.add(new ValidationResult.ValidationMessage(
                                ValidationResult.Severity.ERROR,
                                faultLocation.getErrorMessage(),
                                faultLocation.getLineNumber(),
                                faultLocation.getFaultyContent()
                        ));
                    }
                } catch (Exception e) {
                    LOG.debug("Fault finder failed: {}", e.getMessage());
                }
            }

            return ValidationResult.withMessages(messages);

        } catch (Exception e) {
            LOG.error("Validation failed with exception: {}", e.getMessage());

            // Try fault finder for exception cases
            try {
                DrlFaultFinder.FaultLocation faultLocation = faultFinder.findFaultyLine(drlCode);
                if (faultLocation != null) {
                    return ValidationResult.error(
                            faultLocation.getErrorMessage(),
                            faultLocation.getLineNumber(),
                            faultLocation.getFaultyContent()
                    );
                }
            } catch (Exception faultFinderException) {
                LOG.debug("Fault finder also failed: {}", faultFinderException.getMessage());
            }

            return ValidationResult.error("Validation failed: " + e.getMessage());
        }
    }

    private List<ValidationResult.ValidationMessage> parseVerificationResult(String result) {
        List<ValidationResult.ValidationMessage> messages = new ArrayList<>();

        if (result == null || result.equals("Code looks good")) {
            return messages;
        }

        String[] lines = result.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("ERROR:")) {
                messages.add(new ValidationResult.ValidationMessage(
                        ValidationResult.Severity.ERROR,
                        line.substring(6).trim(),
                        null,
                        null
                ));
            } else if (line.startsWith("WARNING:")) {
                messages.add(new ValidationResult.ValidationMessage(
                        ValidationResult.Severity.WARNING,
                        line.substring(8).trim(),
                        null,
                        null
                ));
            } else if (line.startsWith("NOTE:")) {
                messages.add(new ValidationResult.ValidationMessage(
                        ValidationResult.Severity.NOTE,
                        line.substring(5).trim(),
                        null,
                        null
                ));
            }
        }

        return messages;
    }
}

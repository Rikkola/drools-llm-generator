package com.github.rikkola.drlgen.model;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Captures the result of a DRL generation attempt.
 */
public record GenerationResult(
        String modelName,
        String generatedDrl,
        boolean validationPassed,
        String validationMessage,
        boolean executionPassed,
        String executionMessage,
        int rulesFired,
        List<Object> resultingFacts,
        Duration generationTime,
        Duration totalTime
) {
    /**
     * Returns true if DRL was successfully generated, validated, and executed.
     * Note: For generation-only validation (without execution), use validationPassed.
     */
    public boolean isSuccessful() {
        return validationPassed && executionPassed;
    }

    /**
     * Returns true if DRL was generated and validated (without requiring execution).
     */
    public boolean isValidated() {
        return validationPassed && generatedDrl != null && !generatedDrl.isBlank();
    }

    /**
     * Returns a concise summary of the generation result.
     */
    public String getSummary() {
        return String.format(
                "Model: %s | Valid: %s | Executed: %s | Rules Fired: %d | Gen Time: %dms",
                modelName, validationPassed, executionPassed, rulesFired, generationTime.toMillis()
        );
    }

    /**
     * Creates a failed result for generation errors.
     */
    public static GenerationResult failed(String modelName, String message, Duration totalTime) {
        return new GenerationResult(
                modelName,
                "",
                false,
                message,
                false,
                message,
                0,
                Collections.emptyList(),
                Duration.ZERO,
                totalTime
        );
    }

    /**
     * Creates a partial result where generation succeeded but validation/execution failed.
     */
    public static GenerationResult partial(String modelName, String drl, boolean validated,
                                           String message, Duration genTime, Duration totalTime) {
        return new GenerationResult(
                modelName,
                drl,
                validated,
                message,
                false,
                message,
                0,
                Collections.emptyList(),
                genTime,
                totalTime
        );
    }

    /**
     * Creates a validation-only success result (no execution performed).
     */
    public static GenerationResult validated(String modelName, String drl, String validationMessage,
                                             Duration genTime, Duration totalTime) {
        return new GenerationResult(
                modelName,
                drl,
                true,
                validationMessage,
                false,
                "Execution not performed",
                0,
                Collections.emptyList(),
                genTime,
                totalTime
        );
    }
}

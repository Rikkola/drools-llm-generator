package com.github.rikkola.drlgen.ui.dto;

public record GenerationResponse(
        boolean success,
        String generatedDrl,
        boolean validationPassed,
        String validationMessage,
        long generationTimeMs
) {
    public static GenerationResponse success(String drl, long timeMs) {
        return new GenerationResponse(true, drl, true, "Validation passed", timeMs);
    }

    public static GenerationResponse validationFailed(String drl, String message, long timeMs) {
        return new GenerationResponse(true, drl, false, message, timeMs);
    }

    public static GenerationResponse error(String message) {
        return new GenerationResponse(false, null, false, message, 0);
    }
}

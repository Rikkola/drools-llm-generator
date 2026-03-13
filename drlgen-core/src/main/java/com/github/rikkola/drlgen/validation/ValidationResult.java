package com.github.rikkola.drlgen.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents the result of DRL validation.
 *
 * <p>Contains structured information about validation status, errors,
 * warnings, and notes found during validation.</p>
 */
public final class ValidationResult {

    private final boolean valid;
    private final List<ValidationMessage> messages;

    private ValidationResult(boolean valid, List<ValidationMessage> messages) {
        this.valid = valid;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }

    /**
     * Returns true if the DRL is valid (no errors).
     *
     * @return true if valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Returns all validation messages.
     *
     * @return list of messages
     */
    public List<ValidationMessage> getMessages() {
        return messages;
    }

    /**
     * Returns only error messages.
     *
     * @return list of error messages
     */
    public List<ValidationMessage> getErrors() {
        return messages.stream()
                .filter(m -> m.severity() == Severity.ERROR)
                .toList();
    }

    /**
     * Returns only warning messages.
     *
     * @return list of warning messages
     */
    public List<ValidationMessage> getWarnings() {
        return messages.stream()
                .filter(m -> m.severity() == Severity.WARNING)
                .toList();
    }

    /**
     * Returns only note/info messages.
     *
     * @return list of note messages
     */
    public List<ValidationMessage> getNotes() {
        return messages.stream()
                .filter(m -> m.severity() == Severity.NOTE)
                .toList();
    }

    /**
     * Returns true if there are any error messages.
     *
     * @return true if errors exist
     */
    public boolean hasErrors() {
        return messages.stream().anyMatch(m -> m.severity() == Severity.ERROR);
    }

    /**
     * Returns true if there are any warning messages.
     *
     * @return true if warnings exist
     */
    public boolean hasWarnings() {
        return messages.stream().anyMatch(m -> m.severity() == Severity.WARNING);
    }

    /**
     * Returns a human-readable summary of the validation result.
     *
     * @return summary string
     */
    public String getSummary() {
        if (valid && messages.isEmpty()) {
            return "Code looks good";
        }

        StringBuilder sb = new StringBuilder();
        for (ValidationMessage msg : messages) {
            sb.append(msg.severity()).append(": ").append(msg.message());
            if (msg.lineNumber() != null) {
                sb.append(" (line ").append(msg.lineNumber()).append(")");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return "ValidationResult{valid=" + valid + ", messages=" + messages.size() + "}";
    }

    // ========== Factory Methods ==========

    /**
     * Creates a successful validation result.
     *
     * @return a valid result
     */
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    /**
     * Creates a validation result with the given messages.
     * The result is valid if there are no ERROR severity messages.
     *
     * @param messages the validation messages
     * @return the validation result
     */
    public static ValidationResult withMessages(List<ValidationMessage> messages) {
        boolean valid = messages.stream()
                .noneMatch(m -> m.severity() == Severity.ERROR);
        return new ValidationResult(valid, messages);
    }

    /**
     * Creates a failed validation result with a single error.
     *
     * @param errorMessage the error message
     * @return a failed result
     */
    public static ValidationResult error(String errorMessage) {
        return new ValidationResult(false, List.of(
                new ValidationMessage(Severity.ERROR, errorMessage, null, null)
        ));
    }

    /**
     * Creates a failed validation result with detailed error information.
     *
     * @param errorMessage the error message
     * @param lineNumber the line number where the error occurred
     * @param content the content at the error location
     * @return a failed result
     */
    public static ValidationResult error(String errorMessage, Integer lineNumber, String content) {
        return new ValidationResult(false, List.of(
                new ValidationMessage(Severity.ERROR, errorMessage, lineNumber, content)
        ));
    }

    // ========== Builder ==========

    /**
     * Creates a builder for constructing validation results.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for constructing ValidationResult instances.
     */
    public static class Builder {
        private final List<ValidationMessage> messages = new ArrayList<>();

        /**
         * Adds an error message.
         *
         * @param message the error message
         * @return this builder
         */
        public Builder error(String message) {
            messages.add(new ValidationMessage(Severity.ERROR, message, null, null));
            return this;
        }

        /**
         * Adds an error message with line number.
         *
         * @param message the error message
         * @param lineNumber the line number
         * @return this builder
         */
        public Builder error(String message, int lineNumber) {
            messages.add(new ValidationMessage(Severity.ERROR, message, lineNumber, null));
            return this;
        }

        /**
         * Adds a warning message.
         *
         * @param message the warning message
         * @return this builder
         */
        public Builder warning(String message) {
            messages.add(new ValidationMessage(Severity.WARNING, message, null, null));
            return this;
        }

        /**
         * Adds a note/info message.
         *
         * @param message the note message
         * @return this builder
         */
        public Builder note(String message) {
            messages.add(new ValidationMessage(Severity.NOTE, message, null, null));
            return this;
        }

        /**
         * Adds a validation message.
         *
         * @param message the message to add
         * @return this builder
         */
        public Builder message(ValidationMessage message) {
            messages.add(message);
            return this;
        }

        /**
         * Builds the validation result.
         *
         * @return the validation result
         */
        public ValidationResult build() {
            return ValidationResult.withMessages(messages);
        }
    }

    // ========== Nested Types ==========

    /**
     * Message severity levels.
     */
    public enum Severity {
        ERROR,
        WARNING,
        NOTE
    }

    /**
     * A single validation message.
     */
    public record ValidationMessage(
            Severity severity,
            String message,
            Integer lineNumber,
            String content
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(severity).append(": ").append(message);
            if (lineNumber != null) {
                sb.append(" (line ").append(lineNumber).append(")");
            }
            return sb.toString();
        }
    }
}

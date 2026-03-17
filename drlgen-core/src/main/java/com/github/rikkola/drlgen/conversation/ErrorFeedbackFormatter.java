package com.github.rikkola.drlgen.conversation;

import com.github.rikkola.drlgen.validation.ValidationResult;
import com.github.rikkola.drlgen.validation.ValidationResult.ValidationMessage;

/**
 * Formats validation errors and test execution failures for LLM feedback.
 *
 * <p>Creates structured, actionable error messages that help the model
 * understand what went wrong and how to fix it.</p>
 */
public final class ErrorFeedbackFormatter {

    private ErrorFeedbackFormatter() {
        // Utility class
    }

    /**
     * Formats validation errors for LLM feedback.
     *
     * @param result the validation result containing errors
     * @return formatted error string
     */
    public static String formatValidationErrors(ValidationResult result) {
        if (result.isValid()) {
            return "No errors found.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("VALIDATION ERRORS:\n\n");

        for (ValidationMessage error : result.getErrors()) {
            sb.append("- ");
            if (error.lineNumber() != null) {
                sb.append("[Line ").append(error.lineNumber()).append("] ");
            }
            sb.append(error.message());
            sb.append("\n");
        }

        // Also include warnings as they may be relevant
        if (result.hasWarnings()) {
            sb.append("\nWARNINGS:\n");
            for (ValidationMessage warning : result.getWarnings()) {
                sb.append("- ").append(warning.message()).append("\n");
            }
        }

        return sb.toString().trim();
    }

    /**
     * Formats a test execution failure for LLM feedback.
     *
     * @param testCaseName the name of the failed test case
     * @param expectedRules expected number of rules to fire
     * @param actualRules actual number of rules that fired
     * @return formatted error string
     */
    public static String formatTestFailure(String testCaseName, int expectedRules, int actualRules) {
        StringBuilder sb = new StringBuilder();
        sb.append("TEST EXECUTION FAILURE:\n\n");
        sb.append("Test Case: ").append(testCaseName).append("\n");
        sb.append("Expected: ").append(expectedRules).append(" rule(s) to fire\n");
        sb.append("Actual: ").append(actualRules).append(" rule(s) fired\n\n");

        if (actualRules == 0) {
            sb.append("DIAGNOSIS: No rules matched. Check your pattern conditions - ");
            sb.append("they may be too restrictive or checking for wrong values.\n");
        } else if (actualRules > expectedRules) {
            if (actualRules >= 100) {
                sb.append("DIAGNOSIS: Infinite loop detected! Your rules are firing repeatedly.\n");
                sb.append("FIX: Add a state guard to prevent re-firing. For example:\n");
                sb.append("  Instead of: $obj : Fact(field > 10)\n");
                sb.append("  Use: $obj : Fact(field > 10, status == \"PENDING\")\n");
                sb.append("Then in 'then': modify($obj) { setStatus(\"DONE\") }\n");
            } else {
                sb.append("DIAGNOSIS: Multiple rules are matching when only one should.\n");
                sb.append("FIX: Make rule conditions mutually exclusive or add state guards.\n");
            }
        } else {
            sb.append("DIAGNOSIS: Not enough rules fired. Some conditions may not be met.\n");
        }

        return sb.toString().trim();
    }

    /**
     * Formats a field value assertion failure for LLM feedback.
     *
     * @param testCaseName the name of the failed test case
     * @param fieldName the field that had wrong value
     * @param expectedValue expected value
     * @param actualValue actual value
     * @return formatted error string
     */
    public static String formatFieldAssertionFailure(String testCaseName, String fieldName,
                                                      Object expectedValue, Object actualValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("TEST ASSERTION FAILURE:\n\n");
        sb.append("Test Case: ").append(testCaseName).append("\n");
        sb.append("Field: ").append(fieldName).append("\n");
        sb.append("Expected: ").append(expectedValue).append("\n");
        sb.append("Actual: ").append(actualValue).append("\n\n");
        sb.append("DIAGNOSIS: The rule logic produced an incorrect value for this field.\n");
        sb.append("Check your rule conditions and the value being set in the modify() block.\n");

        return sb.toString().trim();
    }

    /**
     * Formats a compilation error for LLM feedback.
     *
     * @param errorMessage the compilation error message
     * @return formatted error string
     */
    public static String formatCompilationError(String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("DRL COMPILATION ERROR:\n\n");
        sb.append(errorMessage).append("\n\n");

        // Add common fixes based on error patterns
        if (errorMessage.contains("undefined") && errorMessage.contains("method")) {
            sb.append("LIKELY FIX: Use $variable.getMethod() instead of getMethod() inside modify().\n");
            sb.append("Example: modify($obj) { setTotal($obj.getSubtotal() * 1.1) }\n");
        } else if (errorMessage.contains("mismatched input")) {
            sb.append("LIKELY FIX: Check DRL syntax - missing 'end', incorrect operators, or invalid keywords.\n");
        }

        return sb.toString().trim();
    }
}

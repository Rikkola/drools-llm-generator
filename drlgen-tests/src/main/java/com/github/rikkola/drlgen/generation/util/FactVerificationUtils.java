package com.github.rikkola.drlgen.generation.util;

import com.github.rikkola.drlgen.generation.model.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for verifying expected facts against actual facts in working memory.
 */
public final class FactVerificationUtils {

    private static final Logger logger = LoggerFactory.getLogger(FactVerificationUtils.class);

    private FactVerificationUtils() {
        // Utility class
    }

    /**
     * Verifies that the resulting facts contain the expected field values (legacy format).
     * Returns null if verification passes, or an error message if it fails.
     */
    public static String verifyExpectedFields(List<Object> facts, Map<String, Object> expectedFields) {
        for (Map.Entry<String, Object> entry : expectedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object expectedValue = entry.getValue();

            boolean found = false;
            for (Object fact : facts) {
                try {
                    String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
                    java.lang.reflect.Method getter = fact.getClass().getMethod(getterName);
                    Object actualValue = getter.invoke(fact);

                    if (valuesMatch(expectedValue, actualValue)) {
                        found = true;
                        logger.debug("Field '{}' matched: expected={}, actual={}", fieldName, expectedValue, actualValue);
                        break;
                    }
                } catch (NoSuchMethodException e) {
                    try {
                        java.lang.reflect.Field field = fact.getClass().getDeclaredField(fieldName);
                        field.setAccessible(true);
                        Object actualValue = field.get(fact);
                        if (valuesMatch(expectedValue, actualValue)) {
                            found = true;
                            logger.debug("Field '{}' matched: expected={}, actual={}", fieldName, expectedValue, actualValue);
                            break;
                        }
                    } catch (Exception ignored) {
                        // Field not found on this fact, try next
                    }
                } catch (Exception e) {
                    logger.debug("Could not access field '{}' on {}: {}", fieldName, fact.getClass().getSimpleName(), e.getMessage());
                }
            }

            if (!found) {
                return String.format("Expected field '%s' to have value '%s' but no matching fact found", fieldName, expectedValue);
            }
        }
        return null;
    }

    /**
     * Verifies that the resulting facts contain the expected typed facts.
     * Returns null if verification passes, or an error message if it fails.
     */
    public static String verifyExpectedFacts(List<Object> facts, List<TestScenario.ExpectedFact> expectedFacts) {
        for (TestScenario.ExpectedFact expected : expectedFacts) {
            String typeName = expected.type();

            List<Object> factsOfType = facts.stream()
                    .filter(f -> f.getClass().getSimpleName().equals(typeName))
                    .collect(Collectors.toList());

            if (factsOfType.isEmpty()) {
                return String.format("Expected fact of type '%s' not found in working memory", typeName);
            }

            boolean found = false;
            StringBuilder mismatchDetails = new StringBuilder();

            for (Object fact : factsOfType) {
                String matchError = verifyFactFields(fact, expected.fields());
                if (matchError == null) {
                    found = true;
                    logger.debug("Found matching {} fact", typeName);
                    break;
                } else {
                    mismatchDetails.append("\n  - ").append(matchError);
                }
            }

            if (!found) {
                return String.format(
                        "Expected %s fact with fields %s not found. Found %d %s fact(s) but none matched:%s",
                        typeName, expected.fields(), factsOfType.size(), typeName, mismatchDetails);
            }
        }
        return null;
    }

    /**
     * Verifies all expected fields on a single fact.
     * Returns null if all fields match, or error description if not.
     */
    public static String verifyFactFields(Object fact, Map<String, Object> expectedFields) {
        for (Map.Entry<String, Object> entry : expectedFields.entrySet()) {
            String fieldName = entry.getKey();
            Object expectedValue = entry.getValue();

            try {
                Object actualValue = getFieldValue(fact, fieldName);
                if (!valuesMatch(expectedValue, actualValue)) {
                    return String.format("Field '%s': expected '%s' but was '%s'",
                            fieldName, expectedValue, actualValue);
                }
            } catch (Exception e) {
                return String.format("Field '%s' not accessible: %s", fieldName, e.getMessage());
            }
        }
        return null;
    }

    /**
     * Gets field value from a fact using getter or direct field access.
     */
    public static Object getFieldValue(Object fact, String fieldName) throws Exception {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        try {
            java.lang.reflect.Method getter = fact.getClass().getMethod(getterName);
            return getter.invoke(fact);
        } catch (NoSuchMethodException e) {
            java.lang.reflect.Field field = fact.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(fact);
        }
    }

    /**
     * Normalizes a string value to uppercase for enum comparison.
     * Handles null values safely.
     *
     * @param value the string value to normalize
     * @return normalized uppercase string, or null if input was null
     */
    public static String normalizeEnumValue(String value) {
        return value == null ? null : value.toUpperCase().trim();
    }

    /**
     * Compares expected and actual values, handling type conversions.
     * String values are normalized to UPPERCASE before comparison to handle
     * AI output variations (e.g., "approved" vs "APPROVED" vs "Approved").
     */
    public static boolean valuesMatch(Object expected, Object actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;

        // String comparison - normalize to uppercase for consistent enum handling
        if (expected instanceof String && actual instanceof String) {
            String normalizedExpected = normalizeEnumValue((String) expected);
            String normalizedActual = normalizeEnumValue((String) actual);
            return normalizedExpected.equals(normalizedActual);
        }

        // Number comparison (handle int/Integer/double etc.)
        if (expected instanceof Number && actual instanceof Number) {
            return ((Number) expected).doubleValue() == ((Number) actual).doubleValue();
        }

        // Boolean comparison
        if (expected instanceof Boolean && actual instanceof Boolean) {
            return expected.equals(actual);
        }

        // Try string comparison as fallback (also normalized)
        String expectedStr = normalizeEnumValue(String.valueOf(expected));
        String actualStr = normalizeEnumValue(String.valueOf(actual));
        return expectedStr.equals(actualStr);
    }

    /**
     * Validates that a value is within the allowed enum values.
     *
     * @param value the value to check
     * @param allowedValues list of allowed enum values (already uppercase)
     * @return true if value is valid (or allowedValues is null), false otherwise
     */
    public static boolean isValidEnumValue(String value, List<String> allowedValues) {
        if (allowedValues == null || allowedValues.isEmpty()) {
            return true; // Non-enum field, any value allowed
        }
        String normalized = normalizeEnumValue(value);
        return allowedValues.contains(normalized);
    }
}

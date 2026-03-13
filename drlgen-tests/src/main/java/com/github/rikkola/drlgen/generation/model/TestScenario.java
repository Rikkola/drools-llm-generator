package com.github.rikkola.drlgen.generation.model;

import java.util.List;
import java.util.Map;

/**
 * Represents a test scenario for DRL generation.
 * Contains the requirement, expected fact types, test data, and validation criteria.
 */
public record TestScenario(
        String name,
        String description,
        String requirement,
        List<FactTypeDefinition> expectedFactTypes,
        List<TestCase> testCases,
        String sourceFilename  // Original YAML filename (e.g., "adult-validation.yaml")
) {
    /**
     * Constructor without sourceFilename for backward compatibility.
     */
    public TestScenario(String name, String description, String requirement,
                        List<FactTypeDefinition> expectedFactTypes, List<TestCase> testCases) {
        this(name, description, requirement, expectedFactTypes, testCases, null);
    }

    /**
     * Definition of an expected fact type in the generated DRL.
     */
    public record FactTypeDefinition(
            String typeName,
            Map<String, String> fields  // fieldName -> fieldType
    ) {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(typeName).append(" {\n");
            fields.forEach((field, type) ->
                    sb.append("    ").append(field).append(": ").append(type).append("\n"));
            sb.append("}");
            return sb.toString();
        }
    }

    /**
     * Represents an expected fact with type-specific field verification.
     */
    public record ExpectedFact(
            String type,                      // Fact type name (e.g., "Order", "Alert")
            Map<String, Object> fields        // Expected field values
    ) {}

    /**
     * A single test case with input data and expected outcomes.
     * Supports both legacy expectedFieldValues and new type-aware expectedFacts.
     */
    public record TestCase(
            String name,
            String inputJson,
            Map<String, Object> expectedFieldValues,  // Legacy format (backward compat)
            List<ExpectedFact> expectedFacts,         // New: Type-specific expectations
            Integer expectedRulesFired                // null = don't check, 0 = no rules, N = exactly N rules
    ) {
        /**
         * Constructor with default expectedRulesFired = null (don't check) for backward compatibility.
         */
        public TestCase(String name, String inputJson, Map<String, Object> expectedFieldValues, List<ExpectedFact> expectedFacts) {
            this(name, inputJson, expectedFieldValues, expectedFacts, null);
        }

        /**
         * Returns true if this test case uses the new typed verification.
         */
        public boolean hasTypedExpectations() {
            return expectedFacts != null && !expectedFacts.isEmpty();
        }

        /**
         * Checks if the number of rules fired matches the expectation.
         * @param actualRulesFired the actual number of rules fired
         * @return null if OK, error message if validation failed
         */
        public String validateRulesFired(int actualRulesFired) {
            if (expectedRulesFired == null) {
                // No expectation set - any number is OK (including 0)
                return null;
            }
            if (expectedRulesFired == 0 && actualRulesFired > 0) {
                return "Expected no rules to fire, but " + actualRulesFired + " fired";
            }
            if (expectedRulesFired > 0 && actualRulesFired == 0) {
                return "Expected " + expectedRulesFired + " rules to fire, but none fired";
            }
            if (expectedRulesFired > 0 && actualRulesFired != expectedRulesFired) {
                return "Expected " + expectedRulesFired + " rules to fire, but " + actualRulesFired + " fired";
            }
            return null;
        }
    }

    /**
     * Generates a human-readable fact types description for the agent.
     */
    public String getFactTypesDescription() {
        StringBuilder sb = new StringBuilder();
        for (FactTypeDefinition ft : expectedFactTypes) {
            sb.append("- ").append(ft.typeName()).append(":\n");
            ft.fields().forEach((field, type) ->
                    sb.append("    ").append(field).append(": ").append(type).append("\n"));
        }
        return sb.toString();
    }

    /**
     * Gets a test scenario description for the AI agent.
     * Note: Test cases are intentionally NOT included to avoid showing expected outputs to the AI.
     */
    public String getTestScenarioDescription() {
        // Test cases are not shown to the AI - it should generate rules based on requirements only
        return "";
    }

    @Override
    public String toString() {
        return name;
    }
}

package com.github.rikkola.drlgen.generation.runner;

import java.util.List;
import java.util.Map;

/**
 * Detailed result of a scenario test run, suitable for JSON serialization.
 */
public record ScenarioResult(
        String scenarioName,
        String modelName,
        boolean success,
        long generationTimeMs,
        long totalTimeMs,
        int rulesFired,
        String errorMessage,
        List<TestCaseResult> testCases,
        List<String> factsInMemory
) {
    /**
     * Creates a successful scenario result.
     */
    public static ScenarioResult success(String scenarioName, String modelName,
                                          long generationTimeMs, long totalTimeMs,
                                          int rulesFired, List<TestCaseResult> testCases,
                                          List<String> factsInMemory) {
        return new ScenarioResult(scenarioName, modelName, true,
                generationTimeMs, totalTimeMs, rulesFired, null, testCases, factsInMemory);
    }

    /**
     * Creates a failed scenario result.
     */
    public static ScenarioResult failure(String scenarioName, String modelName,
                                          long generationTimeMs, long totalTimeMs,
                                          String errorMessage, List<TestCaseResult> testCases) {
        return new ScenarioResult(scenarioName, modelName, false,
                generationTimeMs, totalTimeMs, 0, errorMessage, testCases, List.of());
    }

    /**
     * Result of a single test case execution.
     */
    public record TestCaseResult(
            String name,
            boolean passed,
            int rulesFired,
            String errorMessage,
            Map<String, Object> expectedValues,
            Map<String, Object> actualValues
    ) {
        /**
         * Creates a successful test case result.
         */
        public static TestCaseResult success(String name, int rulesFired,
                                              Map<String, Object> expectedValues,
                                              Map<String, Object> actualValues) {
            return new TestCaseResult(name, true, rulesFired, null, expectedValues, actualValues);
        }

        /**
         * Creates a failed test case result.
         */
        public static TestCaseResult failure(String name, String errorMessage,
                                              Map<String, Object> expectedValues,
                                              Map<String, Object> actualValues) {
            return new TestCaseResult(name, false, 0, errorMessage, expectedValues, actualValues);
        }
    }
}

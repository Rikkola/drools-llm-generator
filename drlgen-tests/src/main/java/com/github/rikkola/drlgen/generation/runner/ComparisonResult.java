package com.github.rikkola.drlgen.generation.runner;

import java.time.Duration;
import java.util.List;

/**
 * Represents the result of a single generation test.
 */
public record ComparisonResult(
        String modelName,
        String scenarioName,
        String scenarioFilename,
        String format,  // "DRL"
        boolean success,
        int rulesFired,
        Duration generationTime,
        String errorMessage,
        String generatedDrl,
        List<ScenarioResult.TestCaseResult> testCaseResults,
        List<String> factsInMemory
) {
    public static ComparisonResult success(String modelName, String scenarioName, String format,
                                           int rulesFired, Duration generationTime) {
        return new ComparisonResult(modelName, scenarioName, null, format, true, rulesFired,
                generationTime, null, null, List.of(), List.of());
    }

    public static ComparisonResult success(String modelName, String scenarioName, String scenarioFilename,
                                           String format, int rulesFired, Duration generationTime,
                                           String generatedDrl, List<ScenarioResult.TestCaseResult> testCases,
                                           List<String> factsInMemory) {
        return new ComparisonResult(modelName, scenarioName, scenarioFilename, format, true, rulesFired,
                generationTime, null, generatedDrl, testCases, factsInMemory);
    }

    public static ComparisonResult failure(String modelName, String scenarioName, String format,
                                           Duration generationTime, String errorMessage) {
        return new ComparisonResult(modelName, scenarioName, null, format, false, 0,
                generationTime, errorMessage, null, List.of(), List.of());
    }

    public static ComparisonResult failure(String modelName, String scenarioName, String scenarioFilename,
                                           String format, Duration generationTime, String errorMessage,
                                           String generatedDrl, List<ScenarioResult.TestCaseResult> testCases) {
        return new ComparisonResult(modelName, scenarioName, scenarioFilename, format, false, 0,
                generationTime, errorMessage, generatedDrl, testCases, List.of());
    }

    public String getStatusString() {
        return success ? "PASS" : "FAIL";
    }

    /**
     * Converts this result to a ScenarioResult for JSON serialization.
     */
    public ScenarioResult toScenarioResult() {
        if (success) {
            return ScenarioResult.success(
                    scenarioName,
                    modelName,
                    generationTime != null ? generationTime.toMillis() : 0,
                    generationTime != null ? generationTime.toMillis() : 0,
                    rulesFired,
                    testCaseResults,
                    factsInMemory
            );
        } else {
            return ScenarioResult.failure(
                    scenarioName,
                    modelName,
                    generationTime != null ? generationTime.toMillis() : 0,
                    generationTime != null ? generationTime.toMillis() : 0,
                    errorMessage,
                    testCaseResults
            );
        }
    }
}

package com.github.rikkola.drlgen.generation.base;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.service.DRLGenerationService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for DRL generation tests providing common setup and utilities.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDRLGenerationTest {

    protected DRLGenerationService generationService;
    protected ChatModel defaultModel;

    @BeforeAll
    void setUp() {
        generationService = new DRLGenerationService();
        defaultModel = ModelConfiguration.createFromEnvironment();
    }

    /**
     * Generates DRL and asserts successful generation and execution.
     */
    protected GenerationResult generateAndAssertSuccess(TestScenario scenario) {
        return generateAndAssertSuccess(defaultModel, scenario);
    }

    /**
     * Generates DRL with a specific model and asserts successful generation and execution.
     */
    protected GenerationResult generateAndAssertSuccess(ChatModel model, TestScenario scenario) {
        GenerationResult result = generationService.generateAndTest(model, scenario);

        printResult(scenario, result);

        assertThat(result.validationPassed())
                .as("DRL validation should pass")
                .isTrue();
        assertThat(result.executionPassed())
                .as("DRL execution should pass")
                .isTrue();
        // Note: rulesFired can be 0 if all test cases have expectRulesToFire=false
        // The executionPassed check above already validates correct behavior

        return result;
    }

    /**
     * Generates DRL without assertions - for comparison tests where failures are informational.
     */
    protected GenerationResult generateWithoutAssertions(TestScenario scenario) {
        return generateWithoutAssertions(defaultModel, scenario);
    }

    /**
     * Generates DRL with a specific model without assertions.
     */
    protected GenerationResult generateWithoutAssertions(ChatModel model, TestScenario scenario) {
        GenerationResult result = generationService.generateAndTest(model, scenario);
        printResult(scenario, result);
        return result;
    }

    /**
     * Generates DRL with retry on failure (useful for non-deterministic models).
     */
    protected GenerationResult generateWithRetry(TestScenario scenario, int maxAttempts) {
        return generateWithRetry(defaultModel, scenario, maxAttempts);
    }

    /**
     * Generates DRL with a specific model with retry on failure.
     */
    protected GenerationResult generateWithRetry(ChatModel model, TestScenario scenario, int maxAttempts) {
        GenerationResult lastResult = null;
        for (int i = 0; i < maxAttempts; i++) {
            lastResult = generationService.generateAndTest(model, scenario);
            if (lastResult.isSuccessful()) {
                System.out.println("Succeeded on attempt " + (i + 1));
                return lastResult;
            }
            System.out.println("Attempt " + (i + 1) + " failed: " + lastResult.validationMessage());
        }
        return lastResult;
    }

    /**
     * Prints the generation result to console.
     */
    protected void printResult(TestScenario scenario, GenerationResult result) {
        System.out.println("=== Generation Result ===");
        System.out.println("Scenario: " + scenario.name());
        System.out.println(result.getSummary());
        if (!result.isSuccessful()) {
            System.out.println("Generated DRL:\n" + result.generatedDrl());
            System.out.println("Validation: " + result.validationMessage());
            System.out.println("Execution: " + result.executionMessage());
        } else {
            System.out.println("Generated DRL:\n" + result.generatedDrl());
        }
        System.out.println("=".repeat(50));
    }
}

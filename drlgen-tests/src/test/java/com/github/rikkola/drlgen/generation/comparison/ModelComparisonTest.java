package com.github.rikkola.drlgen.generation.comparison;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.generation.base.AbstractDRLGenerationTest;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Compares DRL generation capabilities across different AI models.
 * Models are loaded from models.yaml configuration.
 */
@DisplayName("Model Comparison Tests")
@Tag("comparison")
@Tag("slow")
class ModelComparisonTest extends AbstractDRLGenerationTest {

    private static final List<GenerationResult> comparisonResults = new ArrayList<>();

    static Stream<Arguments> modelScenarioCombinations() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        List<String> models = ModelConfiguration.getAvailableModels();

        return models.stream()
                .map(modelName -> Arguments.of(modelName, scenario));
    }

    static Stream<Arguments> allModelsAllScenarios() {
        List<Arguments> combinations = new ArrayList<>();
        List<String> models = ModelConfiguration.getAvailableModels();

        for (String modelName : models) {
            combinations.add(Arguments.of(modelName, TestScenarioProvider.getScenarioByName("Adult Validation")));
            combinations.add(Arguments.of(modelName, TestScenarioProvider.getScenarioByName("Order Discount - Basic")));
        }

        return combinations.stream();
    }

    static Stream<String> availableModels() {
        return ModelConfiguration.getAvailableModels().stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("availableModels")
    @DisplayName("Compare models on adult validation scenario")
    void compareModelsOnAdultValidation(String modelName) {
        ChatModel model = ModelConfiguration.createModel(modelName);
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");

        GenerationResult result = generateWithoutAssertions(model, scenario);
        comparisonResults.add(result);

        printDetailedResult(modelName, scenario, result);

        // Log but don't fail - comparison tests are informational
        if (!result.isSuccessful()) {
            System.out.println("FAILED: " + result.validationMessage());
        }
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("modelScenarioCombinations")
    @DisplayName("Comprehensive model comparison")
    void comprehensiveComparison(String modelName, TestScenario scenario) {
        ChatModel model = ModelConfiguration.createModel(modelName);
        GenerationResult result = generateWithoutAssertions(model, scenario);

        printDetailedResult(modelName, scenario, result);

        // Soft assertion - log failures but allow test to continue
        if (!result.isSuccessful()) {
            System.err.println("WARN: " + modelName + " failed on " + scenario.name());
        }
    }

    @ParameterizedTest(name = "{0} - {1}")
    @MethodSource("allModelsAllScenarios")
    @DisplayName("Full matrix comparison - all models x all scenarios")
    void fullMatrixComparison(String modelName, TestScenario scenario) {
        ChatModel model = ModelConfiguration.createModel(modelName);
        GenerationResult result = generateWithoutAssertions(model, scenario);

        printDetailedResult(modelName, scenario, result);
    }

    private void printDetailedResult(String modelName, TestScenario scenario, GenerationResult result) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("Model: " + modelName);
        System.out.println("Scenario: " + scenario.name());
        System.out.println("=".repeat(60));
        System.out.println("Success: " + result.isSuccessful());
        System.out.println("Validation Passed: " + result.validationPassed());
        System.out.println("Execution Passed: " + result.executionPassed());
        System.out.println("Rules Fired: " + result.rulesFired());
        System.out.println("Generation Time: " + result.generationTime().toMillis() + "ms");
        System.out.println("Total Time: " + result.totalTime().toMillis() + "ms");
        if (!result.generatedDrl().isEmpty()) {
            System.out.println("\nGenerated DRL:\n" + result.generatedDrl());
        }
        if (!result.isSuccessful()) {
            System.out.println("\nError Details:");
            System.out.println("Validation: " + result.validationMessage());
            System.out.println("Execution: " + result.executionMessage());
        }
        System.out.println("=".repeat(60) + "\n");
    }
}

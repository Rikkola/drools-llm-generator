package com.github.rikkola.drlgen.generation.scenarios;

import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.generation.base.AbstractDRLGenerationTest;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for direct DRL generation pipeline.
 * Pipeline: Requirement -> DRL (single stage)
 * Requires Ollama LLM to be running.
 */
@DisplayName("Direct DRL Generation Tests")
@Tag("integration")
class DRLGenerationTest extends AbstractDRLGenerationTest {

    @Test
    @DisplayName("Generate DRL for adult validation")
    void testAdultValidationDRL() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        GenerationResult result = generateAndAssertSuccess(scenario);

        // Verify DRL has proper structure
        assertThat(result.generatedDrl())
                .contains("rule")
                .contains("when")
                .contains("then")
                .contains("end");
    }

    @Test
    @DisplayName("Generate DRL for senior citizen validation")
    void testSeniorCitizenDRL() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Senior Citizen Discount");
        GenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedDrl())
                .containsIgnoringCase("65");
    }

    @Test
    @DisplayName("Generate DRL for order discount")
    void testOrderDiscountDRL() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Order Discount - Basic");
        GenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedDrl())
                .containsIgnoringCase("discount");
    }

    @Test
    @DisplayName("Generate DRL with retry on failure")
    void testDRLGenerationWithRetry() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        GenerationResult result = generateWithRetry(scenario, 3);

        assertThat(result.isSuccessful())
                .as("DRL generation should succeed within 3 attempts")
                .isTrue();
    }

    @Test
    @DisplayName("Verify DRL structure contains package declaration")
    void testDRLStructureContainsPackage() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        GenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedDrl())
                .contains("package")
                .contains("rule")
                .contains("when")
                .contains("then")
                .contains("end");
    }

    @Test
    @DisplayName("Verify generation timing is recorded")
    void testGenerationTiming() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        GenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generationTime().toMillis())
                .as("Generation time should be recorded")
                .isGreaterThan(0);

        assertThat(result.totalTime().toMillis())
                .as("Total time should be at least as long as generation time")
                .isGreaterThanOrEqualTo(result.generationTime().toMillis());
    }

    @Test
    @DisplayName("Generate DRL for email validation")
    void testEmailValidationDRL() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Email Validation");
        GenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.generatedDrl())
                .containsIgnoringCase("email");
    }

    @Test
    @DisplayName("Generate DRL for loan eligibility")
    void testLoanEligibilityDRL() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Loan Eligibility");
        GenerationResult result = generateAndAssertSuccess(scenario);

        // This is a multi-condition rule
        assertThat(result.generatedDrl())
                .containsIgnoringCase("income");
    }

    @Test
    @DisplayName("Verify rules fire during execution")
    void testRulesFire() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        GenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.rulesFired())
                .as("At least one rule should fire")
                .isGreaterThan(0);

        assertThat(result.executionPassed())
                .as("Execution should pass")
                .isTrue();
    }

    @Test
    @DisplayName("Verify validation passes for generated DRL")
    void testValidationPasses() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Adult Validation");
        GenerationResult result = generateAndAssertSuccess(scenario);

        assertThat(result.validationPassed())
                .as("DRL validation should pass")
                .isTrue();

        assertThat(result.validationMessage())
                .as("Validation message should not contain ERROR")
                .doesNotContain("ERROR:");
    }

    @Test
    @DisplayName("Generate DRL for age classification with all categories")
    void testAgeClassificationDRL() {
        TestScenario scenario = TestScenarioProvider.getScenarioByName("Age Classification");
        GenerationResult result = generateAndAssertSuccess(scenario);

        // Should have rules for CHILD, TEEN, ADULT, SENIOR
        assertThat(result.generatedDrl())
                .containsIgnoringCase("rule");
    }
}

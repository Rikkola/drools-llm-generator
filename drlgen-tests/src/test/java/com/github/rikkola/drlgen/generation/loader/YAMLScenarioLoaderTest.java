package com.github.rikkola.drlgen.generation.loader;

import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YAMLScenarioLoaderTest {

    private final YAMLScenarioLoader loader = new YAMLScenarioLoader();

    @Test
    void shouldLoadSingleScenario() {
        TestScenario scenario = loader.loadScenario("adult-validation.yaml");

        assertThat(scenario.name()).isEqualTo("Adult Validation");
        assertThat(scenario.description()).isEqualTo("Mark persons 18 or older as adults");
        assertThat(scenario.requirement()).contains("18 years or older");
        assertThat(scenario.expectedFactTypes()).hasSize(1);
        assertThat(scenario.expectedFactTypes().get(0).typeName()).isEqualTo("Person");
        assertThat(scenario.testCases()).hasSize(2);
    }

    @Test
    void shouldLoadAllScenarios() {
        List<TestScenario> scenarios = loader.loadAllScenarios();

        assertThat(scenarios).hasSizeGreaterThanOrEqualTo(17);
        assertThat(scenarios).extracting(TestScenario::name)
                .contains("Adult Validation", "Senior Citizen Discount", "Loan Eligibility",
                        "Insurance Risk Assessment", "Credit Card Approval");
    }

    @Test
    void shouldLoadScenariosViaProvider() {
        List<TestScenario> scenarios = TestScenarioProvider.getAllScenarios();

        assertThat(scenarios).hasSizeGreaterThanOrEqualTo(17);
    }

    @Test
    void shouldParseTestCaseInputAsJson() {
        TestScenario scenario = loader.loadScenario("adult-validation.yaml");

        String inputJson = scenario.testCases().get(0).inputJson();
        assertThat(inputJson).contains("\"_type\":\"Person\"");
        assertThat(inputJson).contains("\"name\":\"John\"");
        assertThat(inputJson).contains("\"age\":25");
    }

    @Test
    void shouldParseExpectedFields() {
        TestScenario scenario = loader.loadScenario("adult-validation.yaml");

        assertThat(scenario.testCases().get(0).expectedFieldValues())
                .containsEntry("adult", true);
        assertThat(scenario.testCases().get(1).expectedFieldValues())
                .containsEntry("adult", false);
    }

    @Test
    void shouldParseExpectedRulesFired() {
        TestScenario scenario = loader.loadScenario("adult-validation.yaml");

        // First test case expects 1 rule to fire
        assertThat(scenario.testCases().get(0).expectedRulesFired()).isEqualTo(1);
        // Second test case expects 0 rules to fire
        assertThat(scenario.testCases().get(1).expectedRulesFired()).isEqualTo(0);
    }

    @Test
    void shouldParseNullExpectedRulesFired() {
        // order-discount-customer-type.yaml has test cases without expectedRulesFired
        TestScenario scenario = loader.loadScenario("order-discount-customer-type.yaml");

        // All test cases should have null expectedRulesFired (not specified)
        for (TestScenario.TestCase tc : scenario.testCases()) {
            assertThat(tc.expectedRulesFired()).isNull();
        }
    }

    @Test
    void shouldMaintainBackwardCompatibilityWithLegacyFormat() {
        TestScenario scenario = loader.loadScenario("age-classification.yaml");

        assertThat(scenario.name()).isEqualTo("Age Classification");
        assertThat(scenario.testCases()).hasSize(4);

        // All test cases should use legacy format (not typed expectations)
        for (TestScenario.TestCase tc : scenario.testCases()) {
            assertThat(tc.hasTypedExpectations()).isFalse();
            assertThat(tc.expectedFieldValues()).isNotEmpty();
        }
    }

    // Domain-specific loading tests

    @Test
    void shouldListAvailableDomains() {
        List<String> domains = loader.listAvailableDomains();

        assertThat(domains).contains("insurance", "banking");
    }

    @Test
    void shouldLoadInsuranceDomainScenarios() {
        List<TestScenario> scenarios = loader.loadDomainScenarios("insurance");

        assertThat(scenarios).hasSizeGreaterThanOrEqualTo(3);
        assertThat(scenarios).extracting(TestScenario::name)
                .contains("Insurance Claim Auto-Approval", "Driver Risk Assessment", "Insurance Premium Discount");
    }

    @Test
    void shouldLoadBankingDomainScenarios() {
        List<TestScenario> scenarios = loader.loadDomainScenarios("banking");

        assertThat(scenarios).hasSizeGreaterThanOrEqualTo(3);
        assertThat(scenarios).extracting(TestScenario::name)
                .contains("Banking Loan Approval", "Transaction Monitoring", "Account Tier Assignment");
    }

    @Test
    void shouldLoadSingleDomainScenario() {
        TestScenario scenario = loader.loadDomainScenario("insurance", "claim-auto-approval.yaml");

        assertThat(scenario.name()).isEqualTo("Insurance Claim Auto-Approval");
        assertThat(scenario.expectedFactTypes()).hasSize(1);
        assertThat(scenario.expectedFactTypes().get(0).typeName()).isEqualTo("InsuranceClaim");
        assertThat(scenario.testCases()).hasSize(4);
    }

    @Test
    void shouldGetDomainInstructionsContent() {
        String content = loader.getDomainInstructionsContent("insurance");

        assertThat(content).isNotEmpty();
        assertThat(content).contains("Insurance Domain Instructions");
        assertThat(content).contains("Premium");
        assertThat(content).contains("Claim");
    }

    @Test
    void shouldReturnEmptyForNonExistentDomain() {
        String content = loader.getDomainInstructionsContent("nonexistent");

        assertThat(content).isEmpty();
    }

    @Test
    void shouldReturnEmptyScenariosForNonExistentDomain() {
        List<TestScenario> scenarios = loader.loadDomainScenarios("nonexistent");

        assertThat(scenarios).isEmpty();
    }
}

package com.github.rikkola.drlgen.generation.provider;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * JUnit 5 argument providers for parameterized tests.
 * Uses TestScenarioProvider to load scenarios from YAML files.
 */
public class TestScenarioArguments {

    public static Stream<Arguments> personAgeScenarios() {
        return Stream.of(
                Arguments.of(TestScenarioProvider.getScenarioByName("Adult Validation")),
                Arguments.of(TestScenarioProvider.getScenarioByName("Senior Citizen Discount"))
        );
    }

    public static Stream<Arguments> orderDiscountScenarios() {
        return Stream.of(
                Arguments.of(TestScenarioProvider.getScenarioByName("Order Discount - Basic")),
                Arguments.of(TestScenarioProvider.getScenarioByName("Order Discount - Tiered"))
        );
    }

    public static Stream<Arguments> allScenarios() {
        return TestScenarioProvider.getAllScenarios().stream()
                .map(Arguments::of);
    }
}

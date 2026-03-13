package com.github.rikkola.drlgen.generation.provider;

import com.github.rikkola.drlgen.generation.loader.YAMLScenarioLoader;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Provides test scenarios for DRL/YAML generation testing.
 * All scenarios are loaded from YAML files in src/main/resources/scenarios/.
 */
public class TestScenarioProvider {

    private static final Logger logger = LoggerFactory.getLogger(TestScenarioProvider.class);
    private static final YAMLScenarioLoader yamlLoader = new YAMLScenarioLoader();

    /**
     * Returns all test scenarios loaded from YAML files.
     *
     * @return list of all scenarios
     * @throws RuntimeException if no scenarios can be loaded
     */
    public static List<TestScenario> getAllScenarios() {
        List<TestScenario> scenarios = yamlLoader.loadAllScenarios();
        if (scenarios.isEmpty()) {
            throw new RuntimeException("No scenarios found in YAML files. " +
                    "Ensure scenario files exist in src/main/resources/scenarios/");
        }
        logger.info("Loaded {} scenarios from YAML files", scenarios.size());
        return scenarios;
    }

    /**
     * Get scenario by exact name.
     *
     * @param name exact scenario name
     * @return the scenario
     * @throws IllegalArgumentException if scenario not found
     */
    public static TestScenario getScenarioByName(String name) {
        return getAllScenarios().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + name));
    }
}

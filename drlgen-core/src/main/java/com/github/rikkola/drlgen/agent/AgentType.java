package com.github.rikkola.drlgen.agent;

/**
 * Enum representing the type of DRL generation agent to use.
 */
public enum AgentType {

    /**
     * Simple agent with self-contained detailed system prompt.
     * Uses 3 parameters: requirement, factTypes, testScenario.
     * No external DRL guide needed.
     */
    SIMPLE,

    /**
     * Guided agent with concise system prompt.
     * Uses 4 parameters: drlGuide, requirement, factTypes, exampleInput.
     * Achieved 86.2% success rate in tests.
     * This is the default agent type.
     */
    GUIDED
}

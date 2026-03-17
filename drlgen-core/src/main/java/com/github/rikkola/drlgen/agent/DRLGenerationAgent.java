package com.github.rikkola.drlgen.agent;

import dev.langchain4j.model.chat.ChatModel;

/**
 * Functional interface for DRL generation agents.
 * Use {@link AgentFactory} to create instances of specific agent types.
 *
 * @see AgentFactory
 * @see AgentType
 */
@FunctionalInterface
public interface DRLGenerationAgent {

    /**
     * Generates DRL code from the given inputs.
     *
     * @param drlGuide optional DRL reference guide (may be ignored by some implementations)
     * @param requirement the natural language requirement
     * @param factTypes description of fact types to declare
     * @param exampleInput example input/output scenarios
     * @return generated DRL code
     */
    String generateDRL(String drlGuide,
                       String requirement,
                       String factTypes,
                       String exampleInput);

    /**
     * Creates a DRLGenerationAgent using the default agent type (GUIDED).
     *
     * @param chatModel the chat model to use
     * @return a DRLGenerationAgent instance
     */
    static DRLGenerationAgent create(ChatModel chatModel) {
        return AgentFactory.create(AgentType.GUIDED, chatModel);
    }
}

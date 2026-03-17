package com.github.rikkola.drlgen.agent;

import dev.langchain4j.model.chat.ChatModel;

import java.util.function.Function;

/**
 * Factory for creating DRL generation agents based on the configured agent type.
 */
public final class AgentFactory {

    private AgentFactory() {
        // Utility class
    }

    /**
     * Creates a DRLGenerationAgent of the specified type.
     *
     * @param type the agent type to create
     * @param chatModel the chat model to use
     * @return a DRLGenerationAgent instance
     */
    public static DRLGenerationAgent create(AgentType type, ChatModel chatModel) {
        return switch (type) {
            case SIMPLE -> createSimpleAdapter(chatModel);
            case GUIDED -> createGuidedAdapter(chatModel);
        };
    }

    /**
     * Creates a factory function for the specified agent type.
     * This can be passed to DRLGenerationService.builder().agentFactory().
     *
     * @param type the agent type
     * @return a function that creates agents for the given type
     */
    public static Function<ChatModel, DRLGenerationAgent> forType(AgentType type) {
        return chatModel -> create(type, chatModel);
    }

    /**
     * Wraps SimpleDRLGenerationAgent to match the 4-parameter DRLGenerationAgent interface.
     * The drlGuide parameter is ignored since SimpleDRLGenerationAgent has a self-contained prompt.
     */
    private static DRLGenerationAgent createSimpleAdapter(ChatModel chatModel) {
        SimpleDRLGenerationAgent delegate = SimpleDRLGenerationAgent.create(chatModel);
        return (drlGuide, requirement, factTypes, exampleInput) ->
                delegate.generateDRL(requirement, factTypes, exampleInput);
    }

    /**
     * Wraps GuidedDRLGenerationAgent to match the DRLGenerationAgent interface.
     */
    private static DRLGenerationAgent createGuidedAdapter(ChatModel chatModel) {
        GuidedDRLGenerationAgent delegate = GuidedDRLGenerationAgent.create(chatModel);
        return delegate::generateDRL;
    }
}

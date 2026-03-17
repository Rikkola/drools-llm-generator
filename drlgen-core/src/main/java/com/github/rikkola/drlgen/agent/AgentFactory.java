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
     * <p>Note: For CONVERSATIONAL type, use {@link #createConversational(ChatModel)} instead,
     * as it has additional methods for multi-turn generation.</p>
     *
     * @param type the agent type to create
     * @param chatModel the chat model to use
     * @return a DRLGenerationAgent instance
     * @throws IllegalArgumentException if type is CONVERSATIONAL (use createConversational instead)
     */
    public static DRLGenerationAgent create(AgentType type, ChatModel chatModel) {
        return switch (type) {
            case SIMPLE -> createSimpleAdapter(chatModel);
            case GUIDED -> createGuidedAdapter(chatModel);
            case CONVERSATIONAL -> throw new IllegalArgumentException(
                    "Use createConversational() for CONVERSATIONAL type");
        };
    }

    /**
     * Creates a ConversationalDRLAgent with ChatMemory support.
     *
     * <p>This agent supports multi-turn generation with error feedback and self-correction.
     * Use {@link ConversationalDRLAgent#generateDRL} for initial generation and
     * {@link ConversationalDRLAgent#fixDRL} for subsequent corrections.</p>
     *
     * @param chatModel the chat model to use
     * @return a ConversationalDRLAgent instance
     */
    public static ConversationalDRLAgent createConversational(ChatModel chatModel) {
        return ConversationalDRLAgent.create(chatModel);
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

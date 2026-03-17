package com.github.rikkola.drlgen.agent;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Conversational DRL Generation Agent with ChatMemory support.
 *
 * <p>Uses LangChain4j's ChatMemory to maintain conversation history across calls,
 * enabling the model to learn from validation errors and fix its DRL.</p>
 *
 * <p>Typical flow:
 * <ol>
 *   <li>Call {@link #generateDRL} to create initial DRL</li>
 *   <li>Validate the DRL</li>
 *   <li>If errors, call {@link #fixDRL} with error feedback</li>
 *   <li>Repeat until valid or max turns reached</li>
 * </ol>
 * </p>
 */
public interface ConversationalDRLAgent {

    @SystemMessage("""
        You are a Drools DRL (Decision Rule Language) expert code generator.
        Your task is to generate syntactically correct and executable DRL code.

        Follow the DRL REFERENCE GUIDE provided carefully.
        Pay special attention to the anti-patterns section to avoid common mistakes.

        When asked to fix errors:
        - Analyze the error messages carefully
        - Review the relevant guide sections provided
        - Fix ALL issues in your regenerated DRL
        - Return only the complete, corrected DRL code
        """)
    @UserMessage("""
        DRL REFERENCE GUIDE:
        {{drlGuide}}

        ---

        Generate DRL code for the following business rule requirement:

        REQUIREMENT:
        {{requirement}}

        FACT TYPES (declare these in the DRL):
        {{factTypes}}

        TEST CASES (use these EXACT values in your rules):
        {{exampleInput}}
        """)
    String generateDRL(@V("drlGuide") String drlGuide,
                       @V("requirement") String requirement,
                       @V("factTypes") String factTypes,
                       @V("exampleInput") String exampleInput);

    @UserMessage("""
        Your previous DRL had the following errors:

        {{errors}}

        Relevant guide sections to fix these issues:
        {{guideSections}}

        Your previous DRL that needs fixing:
        ```drl
        {{previousDrl}}
        ```

        Please fix ALL the issues and regenerate the complete DRL code.
        Return only the corrected DRL, no explanations.
        """)
    String fixDRL(@V("errors") String errors,
                  @V("guideSections") String guideSections,
                  @V("previousDrl") String previousDrl);

    /**
     * Creates a ConversationalDRLAgent with ChatMemory enabled.
     *
     * <p>The memory window retains the last 10 messages, which is sufficient
     * for the typical flow of 1 generation + 2 fix attempts.</p>
     *
     * @param chatModel the chat model to use
     * @return a new ConversationalDRLAgent instance
     */
    static ConversationalDRLAgent create(ChatModel chatModel) {
        return AiServices.builder(ConversationalDRLAgent.class)
                .chatModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }
}

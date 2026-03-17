package com.github.rikkola.drlgen.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Guided DRL Generation Agent with comprehensive DRL reference guide.
 * Uses 4 parameters: drlGuide, requirement, factTypes, exampleInput.
 * The drlGuide contains detailed syntax rules and anti-patterns to avoid.
 */
public interface GuidedDRLGenerationAgent {

    @SystemMessage("""
        You are a Drools DRL (Decision Rule Language) expert code generator.
        Your task is to generate syntactically correct and executable DRL code based on business rules.

        Follow the DRL REFERENCE GUIDE provided in the user message carefully.
        Pay special attention to the anti-patterns section to avoid common mistakes.

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

    /**
     * Creates a GuidedDRLGenerationAgent instance with the specified chat model.
     */
    static GuidedDRLGenerationAgent create(ChatModel chatModel) {
        return AiServices.builder(GuidedDRLGenerationAgent.class)
                .chatModel(chatModel)
                .build();
    }
}

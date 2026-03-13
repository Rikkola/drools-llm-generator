package com.github.rikkola.drlgen.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI Agent interface for generating Drools DRL code from natural language constraints.
 * Uses LangChain4j AiServices pattern for declarative agent definition.
 */
public interface DRLGenerationAgent {

    @SystemMessage("""
        You are a Drools DRL (Decision Rule Language) expert code generator.
        Your task is to generate syntactically correct and executable DRL code based on business rules.

        You will receive a comprehensive DRL reference guide with syntax, patterns, and examples.
        Follow the guide precisely to generate valid, compilable DRL code.

        OUTPUT FORMAT:
        Return ONLY valid DRL code. No explanations, no markdown code blocks, no backticks.
        The output must be directly savable as a .drl file and compilable by Drools.
        """)
    @UserMessage("""
        {{drlGuide}}

        ---

        Generate DRL code for the following business rule requirement:

        REQUIREMENT:
        {{requirement}}

        FACT TYPES (declare these in the DRL):
        {{factTypes}}

        TEST CASES (use these EXACT values in your rules):
        {{exampleInput}}

        CRITICAL REMINDERS:
        1. Use && and || in constraints, NEVER 'and' or 'or'
        2. For field 'isXxx', setter is setIsXxx(), NOT setXxx()
        3. Use $variable.getField() prefix in modify expressions
        4. Use EXACT values from test cases (e.g., "MANUAL_REVIEW" not "ESCALATE")
        5. Use salience to control rule priority when rules should be mutually exclusive
        """)
    String generateDRL(@V("drlGuide") String drlGuide,
                       @V("requirement") String requirement,
                       @V("factTypes") String factTypes,
                       @V("exampleInput") String exampleInput);

    /**
     * Creates a DRLGenerationAgent instance with the specified chat model.
     */
    static DRLGenerationAgent create(ChatModel chatModel) {
        return AiServices.builder(DRLGenerationAgent.class)
                .chatModel(chatModel)
                .build();
    }
}

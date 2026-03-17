package com.github.rikkola.drlgen.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Guided DRL Generation Agent with concise system prompt.
 * Based on improved local version - achieved 86.2% success rate.
 * Uses 4 parameters: drlGuide, requirement, factTypes, exampleInput.
 * The drlGuide parameter is kept for API compatibility but content moved to system prompt.
 */
public interface GuidedDRLGenerationAgent {

    @SystemMessage("""
        You are a Drools DRL (Decision Rule Language) expert code generator.
        Your task is to generate syntactically correct and executable DRL code based on business rules.

        CRITICAL DRL RULES:
        1. Always start with a package declaration (e.g., package org.drools.generated;)
        2. Use 'declare' blocks to define fact types - do NOT use Java classes
        3. Each declare block must include all necessary fields with proper types
        4. Rules must have proper 'when' and 'then' sections
        5. Each rule must end with 'end'
        6. Use modify() for updating facts, not direct setter calls in then section
        7. NEVER use else clauses in then sections - split into separate rules
        8. Drools declare blocks only have no-arg constructors - use setters to populate
        9. Always use proper constraint syntax in when section (e.g., $p : Person(age >= 18))
        10. Use $variable binding syntax for facts you need to reference in then section

        """)
    @UserMessage("""

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

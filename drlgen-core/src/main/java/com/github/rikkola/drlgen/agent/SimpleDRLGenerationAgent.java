package com.github.rikkola.drlgen.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Simple DRL Generation Agent with self-contained system prompt.
 * Based on origin/main version - no external DRL guide required.
 * Uses 3 parameters: requirement, factTypes, testScenario.
 */
public interface SimpleDRLGenerationAgent {

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

        FIELD TYPES TO USE:
        - String for text
        - int or Integer for whole numbers
        - double or Double for decimals
        - boolean for true/false
        - java.util.Date for dates
        - java.math.BigDecimal for currency

        MODIFY SYNTAX:
        Use modify($variable) { setField(value) } to update facts.
        Example:
        modify($p) { setAdult(true) }

        OUTPUT FORMAT:
        Return ONLY valid DRL code. No explanations, no markdown code blocks, no backticks.
        The output must be directly savable as a .drl file and compilable by Drools.
        """)
    @UserMessage("""
        Generate DRL code for the following business rule requirement:

        {{requirement}}

        Fact types needed (declare these in the DRL):
        {{factTypes}}

        Test scenario that must work:
        {{testScenario}}
        """)
    String generateDRL(@V("requirement") String requirement,
                       @V("factTypes") String factTypes,
                       @V("testScenario") String testScenario);

    /**
     * Creates a SimpleDRLGenerationAgent instance with the specified chat model.
     */
    static SimpleDRLGenerationAgent create(ChatModel chatModel) {
        return AiServices.builder(SimpleDRLGenerationAgent.class)
                .chatModel(chatModel)
                .build();
    }
}

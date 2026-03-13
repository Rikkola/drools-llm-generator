package com.github.rikkola.drlgen.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Old-style AI Agent for testing - uses simpler prompt without reference guide.
 *
 * <p><strong>Deprecated:</strong> This agent uses a legacy prompt format that is less
 * effective than the current {@link DRLGenerationAgent}. It is retained for backward
 * compatibility with existing test scenarios that were tuned for this prompt style.</p>
 *
 * <p>For new development, use {@link DRLGenerationAgent} instead.</p>
 *
 * @deprecated Use {@link DRLGenerationAgent} for new development
 */
@Deprecated(since = "1.0.0", forRemoval = false)
public interface OldStyleDRLGenerationAgent extends DRLGenerationAgent {

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
        11. SETTER NAMES: For field 'xyz', setter is setXyz(). For field 'isXyz', setter is setIsXyz() NOT setXyz()

        FIELD TYPES TO USE:
        - String for text
        - int or Integer for whole numbers
        - double or Double for decimals
        - boolean for true/false
        - java.util.Date for dates
        - java.math.BigDecimal for currency

        ENUMERATION VALUES:
        When a field represents a status, decision, category, or type with a fixed set of values,
        use String type and the EXACT values from the requirements or test cases.

        CRITICAL: If the requirement mentions specific values like "APPROVED", "DENIED", "MANUAL_REVIEW",
        you MUST use those EXACT values in your rules. Do NOT invent alternative names like "ESCALATE"
        when "MANUAL_REVIEW" is specified.

        Example: If test expects decision="MANUAL_REVIEW", use:
          modify($app) { setDecision("MANUAL_REVIEW") }  // CORRECT
          modify($app) { setDecision("ESCALATE") }       // WRONG - invented value!

        Always extract the expected values from the test cases and use them exactly as written.

        MODIFY SYNTAX AND SETTER NAMING:
        Use modify($variable) { setFieldName(value) } to update facts.

        CRITICAL: The setter name is 'set' + EXACT field name (first letter capitalized).

        If field is 'adult' → setter is setAdult(value)
        If field is 'isVip' → setter is setIsVip(value)  ← NOT setVip()!
        If field is 'isActive' → setter is setIsActive(value)  ← NOT setActive()!

        Example with boolean field named 'isVip':
        modify($c) { setIsVip(true) }   // CORRECT
        modify($c) { setVip(true) }     // WRONG - will not compile!

        COMMON MISTAKE TO AVOID:
        When a field is named 'isVip', you MUST use setIsVip(), never setVip().
        Drools generates setters from the exact field name. Field 'isVip' → setIsVip().

        OUTPUT FORMAT:
        Return ONLY valid DRL code. No explanations, no markdown code blocks, no backticks.
        The output must be directly savable as a .drl file and compilable by Drools.
        """)
    @UserMessage("""
        Generate DRL code for the following business rule requirement:

        {{requirement}}

        Fact types needed (declare these in the DRL):
        {{factTypes}}

        Example input that the rules should handle:
        {{exampleInput}}

        REMINDERS:
        - For any field named 'isXxx', use setIsXxx() as the setter, not setXxx().
        - Use EXACT string values from the test cases (e.g., "MANUAL_REVIEW" not "ESCALATE").
        """)
    String generateDRLOldStyle(@V("requirement") String requirement,
                               @V("factTypes") String factTypes,
                               @V("exampleInput") String exampleInput);

    @Override
    default String generateDRL(String drlGuide, String requirement, String factTypes, String exampleInput) {
        // Ignore drlGuide and use old-style generation
        return generateDRLOldStyle(requirement, factTypes, exampleInput);
    }

    /**
     * Creates an OldStyleDRLGenerationAgent instance with the specified chat model.
     */
    static OldStyleDRLGenerationAgent create(ChatModel chatModel) {
        return AiServices.builder(OldStyleDRLGenerationAgent.class)
                .chatModel(chatModel)
                .build();
    }
}

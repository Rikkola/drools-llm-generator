package com.github.rikkola.drlgen.conversation;

import com.github.rikkola.drlgen.validation.ValidationResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds relevant sections from the DRL reference guide based on errors.
 *
 * <p>Maps common error patterns to the guide sections that explain how to fix them.</p>
 */
public final class GuideSectionFinder {

    private GuideSectionFinder() {
        // Utility class
    }

    // Guide sections keyed by error pattern
    private static final Map<String, String> ERROR_TO_GUIDE_SECTION = new LinkedHashMap<>();

    static {
        // Getter/setter issues
        ERROR_TO_GUIDE_SECTION.put("undefined.*method.*get", """
            ## Section 4: Using Getters in modify()

            CRITICAL: When referencing fields inside modify(), you MUST use the variable prefix.

            CORRECT:
            ```drl
            modify($budget) {
                setTotalRevenue($budget.getSales() + $budget.getServices())
            }
            ```

            WRONG (undefined method error):
            ```drl
            modify($budget) {
                setTotalRevenue(getSales() + getServices())  // Missing $budget prefix!
            }
            ```
            """);

        // Infinite loop issues
        ERROR_TO_GUIDE_SECTION.put("100 rule.*fired|infinite loop", """
            ## Section 9: Negative Check Causing Infinite Loop

            When using modify(), the rule condition MUST become false after modification.
            Use POSITIVE checks for the initial value, not negative checks.

            WRONG - causes infinite loop:
            ```drl
            rule "Set Expiring"
            when
                $lic : License(daysRemaining <= 30, state != "EXPIRED")
            then
                modify($lic) { setState("EXPIRING") }  // Still matches!
            end
            ```

            CORRECT - check for specific initial value:
            ```drl
            rule "Set Expiring"
            when
                $lic : License(daysRemaining <= 30, state == "ACTIVE")
            then
                modify($lic) { setState("EXPIRING") }  // No longer matches
            end
            ```
            """);

        // Multiple rules firing
        ERROR_TO_GUIDE_SECTION.put("Expected 1.*but [2-9]|multiple rule", """
            ## Section 7: Mutually Exclusive Rules

            When only ONE rule should fire, ensure conditions don't overlap.

            Use state guards to prevent re-firing:
            ```drl
            rule "Approve Premium"
            when
                $app : Application(decision == "PENDING", score > 90)
            then
                modify($app) { setDecision("APPROVED_PREMIUM") }
            end

            rule "Approve Standard"
            when
                $app : Application(decision == "PENDING", score > 70)
            then
                modify($app) { setDecision("APPROVED_STANDARD") }
            end
            ```
            """);

        // No rules firing
        ERROR_TO_GUIDE_SECTION.put("0 rule.*fired|none fired", """
            ## Pattern Matching Issues

            If no rules fire, check:
            1. Pattern conditions match the input data exactly
            2. Field names are spelled correctly
            3. Comparison values match (e.g., "PENDING" vs "pending")
            4. Numeric comparisons use correct operators (> vs >=)

            Check test input: If input has `state: "PENDING"`, your rule needs:
            ```drl
            $obj : Fact(state == "PENDING")
            ```
            """);

        // Syntax errors - or/and operators
        ERROR_TO_GUIDE_SECTION.put("mismatched input.*or|mismatched input.*and", """
            ## Section 9: Anti-Pattern - Using 'or'/'and' in Constraints

            CRITICAL: Use `&&` for AND and `||` for OR. Do NOT use `and` or `or`.

            CORRECT:
            ```drl
            Vehicle(mileage >= 50000 && mileage <= 100000)
            Machine(temperature > 80 || pressure > 100)
            ```

            WRONG (syntax error):
            ```drl
            Vehicle(mileage >= 50000 and mileage <= 100000)
            Machine(temperature > 80 or pressure > 100)
            ```
            """);

        // Boolean setter issues
        ERROR_TO_GUIDE_SECTION.put("setIs|setEligible|setValid", """
            ## Section 2: Setter Naming Convention

            The setter name is `set` + exact field name with first letter capitalized.

            | Field Name | Setter Name |
            |------------|-------------|
            | `isEligible` | `setIsEligible()` |
            | `valid` | `setValid()` |

            CRITICAL: For field `isEligible`, the setter is `setIsEligible()`, NOT `setEligible()`.
            """);

        // If/else in then section
        ERROR_TO_GUIDE_SECTION.put("if.*then|else", """
            ## DRL Rule: No if/else in then sections

            NEVER use if/else in the then section. Split into separate rules instead.

            WRONG:
            ```drl
            then
                if ($obj.getScore() > 90) {
                    modify($obj) { setGrade("A") }
                } else {
                    modify($obj) { setGrade("B") }
                }
            ```

            CORRECT - separate rules:
            ```drl
            rule "Grade A"
            when
                $obj : Student(score > 90, grade == null)
            then
                modify($obj) { setGrade("A") }
            end

            rule "Grade B"
            when
                $obj : Student(score <= 90, grade == null)
            then
                modify($obj) { setGrade("B") }
            end
            ```
            """);
    }

    /**
     * Finds relevant guide sections based on error messages.
     *
     * @param errorMessage the error message to analyze
     * @return relevant guide sections concatenated
     */
    public static String findRelevantSections(String errorMessage) {
        if (errorMessage == null || errorMessage.isEmpty()) {
            return "";
        }

        String lowerError = errorMessage.toLowerCase();
        List<String> sections = new ArrayList<>();

        for (Map.Entry<String, String> entry : ERROR_TO_GUIDE_SECTION.entrySet()) {
            if (lowerError.matches(".*" + entry.getKey().toLowerCase() + ".*")) {
                sections.add(entry.getValue());
            }
        }

        if (sections.isEmpty()) {
            return getDefaultSection();
        }

        return String.join("\n\n---\n\n", sections);
    }

    /**
     * Finds relevant guide sections based on validation result.
     *
     * @param result the validation result
     * @return relevant guide sections concatenated
     */
    public static String findRelevantSections(ValidationResult result) {
        if (result.isValid()) {
            return "";
        }

        StringBuilder allErrors = new StringBuilder();
        for (var error : result.getErrors()) {
            allErrors.append(error.message()).append("\n");
        }

        return findRelevantSections(allErrors.toString());
    }

    /**
     * Finds relevant guide sections based on test execution.
     *
     * @param expectedRules expected rules count
     * @param actualRules actual rules count
     * @return relevant guide sections
     */
    public static String findRelevantSections(int expectedRules, int actualRules) {
        if (actualRules == 0) {
            return findRelevantSections("0 rules fired");
        } else if (actualRules >= 100) {
            return findRelevantSections("100 rules fired infinite loop");
        } else if (actualRules > expectedRules) {
            return findRelevantSections("Expected 1 but " + actualRules);
        }
        return getDefaultSection();
    }

    private static String getDefaultSection() {
        return """
            ## General DRL Best Practices

            1. Always add state guards to prevent rules from re-firing
            2. Use $variable.getField() in modify() expressions
            3. Use && and || instead of 'and' and 'or'
            4. Make rule conditions mutually exclusive when needed
            5. Check exact string values match test input
            """;
    }
}

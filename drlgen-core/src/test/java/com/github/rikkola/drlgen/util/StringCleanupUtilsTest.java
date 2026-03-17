package com.github.rikkola.drlgen.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StringCleanupUtils}.
 */
@DisplayName("StringCleanupUtils")
class StringCleanupUtilsTest {

    @Nested
    @DisplayName("cleanupDrl")
    class CleanupDrlTests {

        @Test
        @DisplayName("should return null for null input")
        void nullInput() {
            assertThat(StringCleanupUtils.cleanupDrl(null)).isNull();
        }

        @Test
        @DisplayName("should remove DRL markdown code blocks")
        void removesDrlCodeBlocks() {
            String input = "```drl\npackage test;\nrule \"Test\" end\n```";
            String expected = "package test;\nrule \"Test\" end";

            assertThat(StringCleanupUtils.cleanupDrl(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should remove generic markdown code blocks")
        void removesGenericCodeBlocks() {
            String input = "```\npackage test;\n```";
            String expected = "package test;";

            assertThat(StringCleanupUtils.cleanupDrl(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should trim whitespace")
        void trimsWhitespace() {
            String input = "  \n  package test;  \n  ";
            String expected = "package test;";

            assertThat(StringCleanupUtils.cleanupDrl(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should extract DRL from markdown with leading text")
        void extractsFromMarkdownWithLeadingText() {
            String input = """
                    Here's the DRL code for the given business rule requirement:

                    ```drl
                    package org.drools.generated;

                    rule "Test"
                    when
                    then
                    end
                    ```
                    """;

            String result = StringCleanupUtils.cleanupDrl(input);

            assertThat(result).startsWith("package org.drools.generated;");
            assertThat(result).doesNotContain("Here's");
            assertThat(result).contains("rule \"Test\"");
        }

        @Test
        @DisplayName("should extract DRL from markdown with leading and trailing text")
        void extractsFromMarkdownWithLeadingAndTrailingText() {
            String input = """
                    Here's the generated DRL code based on the provided business rule requirement:

                    ```drl
                    package org.drools.generated;

                    declare Person
                        name : String
                        age : int
                    end

                    rule "Check Adult"
                    when
                        $p : Person(age >= 18)
                    then
                        modify($p) { setAdult(true) }
                    end
                    ```

                    Explanation:
                    1. The `package` declaration specifies the package name.
                    2. The `declare` block defines the Person fact type.
                    3. The rule checks if age >= 18.

                    This DRL code satisfies the given requirements.
                    """;

            String result = StringCleanupUtils.cleanupDrl(input);

            assertThat(result).startsWith("package org.drools.generated;");
            assertThat(result).endsWith("end");
            assertThat(result).doesNotContain("Here's");
            assertThat(result).doesNotContain("Explanation:");
            assertThat(result).doesNotContain("This DRL code satisfies");
            assertThat(result).contains("declare Person");
            assertThat(result).contains("rule \"Check Adult\"");
        }

        @Test
        @DisplayName("should extract DRL from markdown with drools language tag")
        void extractsFromMarkdownWithDroolsTag() {
            String input = """
                    Here is the code:

                    ```drools
                    package test;
                    rule "R1" end
                    ```

                    Hope this helps!
                    """;

            String result = StringCleanupUtils.cleanupDrl(input);

            assertThat(result).startsWith("package test;");
            assertThat(result).doesNotContain("Here is the code");
            assertThat(result).doesNotContain("Hope this helps");
        }

        @Test
        @DisplayName("should handle multiple code blocks by taking the first DRL block")
        void handlesMultipleCodeBlocks() {
            String input = """
                    Here's an example:

                    ```java
                    // This is Java, not DRL
                    public class Test {}
                    ```

                    And here's the DRL:

                    ```drl
                    package org.drools.generated;
                    rule "Test" end
                    ```

                    Done!
                    """;

            String result = StringCleanupUtils.cleanupDrl(input);

            // Should extract the DRL block, not the Java block
            assertThat(result).contains("package org.drools.generated;");
            assertThat(result).contains("rule \"Test\"");
            // Should not contain explanatory text
            assertThat(result).doesNotContain("Here's an example");
            assertThat(result).doesNotContain("Done!");
        }

        @Test
        @DisplayName("should handle plain code block without language specifier containing DRL")
        void handlesPlainCodeBlockWithDrl() {
            String input = """
                    The code:

                    ```
                    package org.drools.generated;
                    rule "Test" end
                    ```

                    That's it.
                    """;

            String result = StringCleanupUtils.cleanupDrl(input);

            assertThat(result).startsWith("package org.drools.generated;");
            assertThat(result).doesNotContain("The code:");
            assertThat(result).doesNotContain("That's it");
        }

        @Test
        @DisplayName("should return clean DRL when no markdown present")
        void handlesCleanDrlWithoutMarkdown() {
            String input = """
                    package org.drools.generated;

                    rule "Test"
                    when
                    then
                    end
                    """;

            String result = StringCleanupUtils.cleanupDrl(input);

            assertThat(result).startsWith("package org.drools.generated;");
            assertThat(result).contains("rule \"Test\"");
        }
    }

    @Nested
    @DisplayName("stripUnnecessaryImports")
    class StripUnnecessaryImportsTests {

        @Test
        @DisplayName("should return null for null input")
        void nullInput() {
            assertThat(StringCleanupUtils.stripUnnecessaryImports(null)).isNull();
        }

        @Test
        @DisplayName("should strip import java.util.List")
        void stripsUtilListImport() {
            String input = """
                    package org.drools.generated;

                    import java.util.List;

                    declare Person
                        name : String
                    end
                    """;

            String result = StringCleanupUtils.stripUnnecessaryImports(input);

            assertThat(result).doesNotContain("import java.util.List");
            assertThat(result).contains("package org.drools.generated;");
            assertThat(result).contains("declare Person");
        }

        @Test
        @DisplayName("should strip import java.util.* wildcard")
        void stripsWildcardImport() {
            String input = """
                    package org.drools.generated;

                    import java.util.*;

                    declare Order
                        total : double
                    end
                    """;

            String result = StringCleanupUtils.stripUnnecessaryImports(input);

            assertThat(result).doesNotContain("import java.util.*");
            assertThat(result).contains("declare Order");
        }

        @Test
        @DisplayName("should strip multiple imports")
        void stripsMultipleImports() {
            String input = """
                    package org.drools.generated;

                    import java.util.List;
                    import java.util.ArrayList;
                    import java.math.BigDecimal;
                    import java.lang.String;

                    declare Invoice
                        total : double
                    end
                    """;

            String result = StringCleanupUtils.stripUnnecessaryImports(input);

            assertThat(result).doesNotContain("import java.util.List");
            assertThat(result).doesNotContain("import java.util.ArrayList");
            assertThat(result).doesNotContain("import java.math.BigDecimal");
            assertThat(result).doesNotContain("import java.lang.String");
            assertThat(result).contains("declare Invoice");
        }

        @Test
        @DisplayName("should strip import java.util.Date")
        void stripsDateImport() {
            String input = """
                    package org.drools.generated;

                    import java.util.Date;

                    declare Event
                        name : String
                    end
                    """;

            String result = StringCleanupUtils.stripUnnecessaryImports(input);

            assertThat(result).doesNotContain("import java.util.Date");
            assertThat(result).contains("declare Event");
        }

        @Test
        @DisplayName("should strip import java.util.regex.Pattern")
        void stripsPatternImport() {
            String input = """
                    package org.drools.generated;

                    import java.util.regex.Pattern;

                    declare Email
                        address : String
                    end
                    """;

            String result = StringCleanupUtils.stripUnnecessaryImports(input);

            assertThat(result).doesNotContain("import java.util.regex.Pattern");
            assertThat(result).contains("declare Email");
        }

        @Test
        @DisplayName("should preserve DRL content when stripping imports")
        void preservesDrlContent() {
            String input = """
                    package org.drools.generated;

                    import java.util.List;

                    declare Person
                        name : String
                        age : int
                    end

                    rule "Check Age"
                    when
                        $p : Person(age >= 18)
                    then
                        modify($p) { setAdult(true) }
                    end
                    """;

            String result = StringCleanupUtils.stripUnnecessaryImports(input);

            assertThat(result).contains("package org.drools.generated;");
            assertThat(result).contains("declare Person");
            assertThat(result).contains("name : String");
            assertThat(result).contains("age : int");
            assertThat(result).contains("rule \"Check Age\"");
            assertThat(result).contains("$p : Person(age >= 18)");
            assertThat(result).contains("modify($p) { setAdult(true) }");
        }

        @Test
        @DisplayName("should handle DRL without imports")
        void handlesDrlWithoutImports() {
            String input = """
                    package org.drools.generated;

                    declare Person
                        name : String
                    end
                    """;

            String result = StringCleanupUtils.stripUnnecessaryImports(input);

            assertThat(result).isEqualTo(input);
        }
    }

    @Nested
    @DisplayName("fixBooleanSetters")
    class FixBooleanSettersTests {

        @Test
        @DisplayName("should return null for null input")
        void nullInput() {
            assertThat(StringCleanupUtils.fixBooleanSetters(null)).isNull();
        }

        @Test
        @DisplayName("should fix setVip() to setIsVip() for field isVip")
        void fixesIsVipSetter() {
            String input = """
                    declare Customer
                        isVip : boolean
                    end

                    rule "VIP"
                    when
                        $c : Customer()
                    then
                        modify($c) { setVip(true) }
                    end
                    """;

            String result = StringCleanupUtils.fixBooleanSetters(input);

            assertThat(result).contains("setIsVip(true)");
            assertThat(result).doesNotContain("setVip(true)");
        }

        @Test
        @DisplayName("should fix setActive() to setIsActive() for field isActive")
        void fixesIsActiveSetter() {
            String input = """
                    declare Account
                        isActive : boolean
                    end

                    rule "Activate"
                    then
                        modify($a) { setActive(true) }
                    end
                    """;

            String result = StringCleanupUtils.fixBooleanSetters(input);

            assertThat(result).contains("setIsActive(true)");
            assertThat(result).doesNotContain("setActive(true)");
        }

        @Test
        @DisplayName("should fix setIsValid() to setValid() for field valid")
        void fixesNonIsFieldSetter() {
            String input = """
                    declare Email
                        valid : boolean
                    end

                    rule "Validate"
                    then
                        modify($e) { setIsValid(true) }
                    end
                    """;

            String result = StringCleanupUtils.fixBooleanSetters(input);

            assertThat(result).contains("setValid(true)");
            assertThat(result).doesNotContain("setIsValid(true)");
        }

        @Test
        @DisplayName("should not change correct setters")
        void preservesCorrectSetters() {
            String input = """
                    declare Customer
                        isVip : boolean
                        valid : boolean
                    end

                    rule "Test"
                    then
                        modify($c) { setIsVip(true), setValid(false) }
                    end
                    """;

            String result = StringCleanupUtils.fixBooleanSetters(input);

            assertThat(result).contains("setIsVip(true)");
            assertThat(result).contains("setValid(false)");
        }

        @Test
        @DisplayName("should handle multiple is-prefixed fields")
        void handlesMultipleIsFields() {
            String input = """
                    declare Status
                        isEnabled : boolean
                        isVisible : boolean
                    end

                    rule "Enable"
                    then
                        modify($s) { setEnabled(true), setVisible(true) }
                    end
                    """;

            String result = StringCleanupUtils.fixBooleanSetters(input);

            assertThat(result).contains("setIsEnabled(true)");
            assertThat(result).contains("setIsVisible(true)");
        }

        @Test
        @DisplayName("should handle multiple non-is fields")
        void handlesMultipleNonIsFields() {
            String input = """
                    declare Form
                        submitted : boolean
                        approved : boolean
                    end

                    rule "Process"
                    then
                        modify($f) { setIsSubmitted(true), setIsApproved(false) }
                    end
                    """;

            String result = StringCleanupUtils.fixBooleanSetters(input);

            assertThat(result).contains("setSubmitted(true)");
            assertThat(result).contains("setApproved(false)");
        }
    }

    @Nested
    @DisplayName("fixPatternConstraintOperators")
    class FixPatternConstraintOperatorsTests {

        @Test
        @DisplayName("should return null for null input")
        void nullInput() {
            assertThat(StringCleanupUtils.fixPatternConstraintOperators(null)).isNull();
        }

        @Test
        @DisplayName("should replace 'or' with '||' in pattern constraints")
        void replacesOrWithLogicalOr() {
            String input = """
                    rule "High Risk"
                    when
                        $a : Applicant( age > 60 or smoker or bmi > 30 )
                    then
                    end
                    """;

            String result = StringCleanupUtils.fixPatternConstraintOperators(input);

            assertThat(result).contains("age > 60 || smoker || bmi > 30");
            assertThat(result).doesNotContain(" or ");
        }

        @Test
        @DisplayName("should replace 'and' with '&&' in pattern constraints")
        void replacesAndWithLogicalAnd() {
            String input = """
                    rule "Medium Risk"
                    when
                        $a : Applicant( age >= 40 and age <= 60 )
                    then
                    end
                    """;

            String result = StringCleanupUtils.fixPatternConstraintOperators(input);

            assertThat(result).contains("age >= 40 && age <= 60");
            assertThat(result).doesNotContain(" and ");
        }

        @Test
        @DisplayName("should handle mixed 'or' and 'and' operators")
        void handlesMixedOperators() {
            String input = """
                    rule "Complex"
                    when
                        $a : Applicant( (age >= 40 and age <= 60) or (bmi >= 25 and bmi <= 30) )
                    then
                    end
                    """;

            String result = StringCleanupUtils.fixPatternConstraintOperators(input);

            assertThat(result).contains("(age >= 40 && age <= 60) || (bmi >= 25 && bmi <= 30)");
        }

        @Test
        @DisplayName("should handle 'not' patterns")
        void handlesNotPatterns() {
            String input = """
                    rule "NoDiscount"
                    when
                        not Order( amount > 100 or premium )
                    then
                    end
                    """;

            String result = StringCleanupUtils.fixPatternConstraintOperators(input);

            assertThat(result).contains("amount > 100 || premium");
        }

        @Test
        @DisplayName("should preserve rule names containing 'or' or 'and'")
        void preservesRuleNames() {
            String input = """
                    rule "Order Processing"
                    when
                        $o : Order( status == "pending" )
                    then
                    end
                    """;

            String result = StringCleanupUtils.fixPatternConstraintOperators(input);

            assertThat(result).contains("rule \"Order Processing\"");
        }

        @Test
        @DisplayName("should not change lines without patterns")
        void preservesNonPatternLines() {
            String input = """
                    package org.drools.generated;

                    // This is a comment with or and

                    rule "Test"
                    when
                        $p : Person( age > 18 )
                    then
                        System.out.println("adult or child");
                    end
                    """;

            String result = StringCleanupUtils.fixPatternConstraintOperators(input);

            // Comment line should be preserved (it's not a pattern line)
            assertThat(result).contains("// This is a comment with or and");
            // String in then block should be preserved (it's not a pattern line)
            assertThat(result).contains("\"adult or child\"");
        }

        @Test
        @DisplayName("should handle pattern without binding")
        void handlesPatternWithoutBinding() {
            String input = """
                    rule "Simple"
                    when
                        Person( age > 18 or vip )
                    then
                    end
                    """;

            String result = StringCleanupUtils.fixPatternConstraintOperators(input);

            assertThat(result).contains("age > 18 || vip");
        }
    }

    @Nested
    @DisplayName("cleanupYaml")
    class CleanupYamlTests {

        @Test
        @DisplayName("should return null for null input")
        void nullInput() {
            assertThat(StringCleanupUtils.cleanupYaml(null)).isNull();
        }

        @Test
        @DisplayName("should remove YAML markdown code blocks")
        void removesYamlCodeBlocks() {
            String input = "```yaml\ntypes:\n  - name: Person\n```";
            String expected = "types:\n  - name: Person";

            assertThat(StringCleanupUtils.cleanupYaml(input)).isEqualTo(expected);
        }

        @Test
        @DisplayName("should remove YML markdown code blocks")
        void removesYmlCodeBlocks() {
            String input = "```yml\nrules:\n  - name: Test\n```";
            String expected = "rules:\n  - name: Test";

            assertThat(StringCleanupUtils.cleanupYaml(input)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Integration - cleanupDrl applies all fixes")
    class IntegrationTests {

        @Test
        @DisplayName("should apply all fixes: markdown, boolean setters, and operators")
        void appliesAllFixes() {
            String input = """
                    ```drl
                    package org.drools.generated;

                    declare Customer
                        isVip : boolean
                    end

                    rule "VIP Discount"
                    when
                        $c : Customer( isVip or totalPurchases > 1000 )
                    then
                        modify($c) { setVip(true) }
                    end
                    ```
                    """;

            String result = StringCleanupUtils.cleanupDrl(input);

            // Markdown removed
            assertThat(result).doesNotContain("```");
            // Boolean setter fixed
            assertThat(result).contains("setIsVip(true)");
            assertThat(result).doesNotContain("setVip(true)");
            // Operator fixed
            assertThat(result).contains("isVip || totalPurchases > 1000");
            assertThat(result).doesNotContain(" or ");
        }
    }
}

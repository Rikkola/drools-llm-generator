package com.github.rikkola.drlgen.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GuidesMerger")
class GuidesMergerTest {

    @Test
    @DisplayName("should return guide unchanged when domain instructions are null")
    void nullDomainInstructions() {
        String guide = "# DRL Guide\nSome content";
        String result = GuidesMerger.merge(guide, null);
        assertThat(result).isEqualTo(guide);
    }

    @Test
    @DisplayName("should return guide unchanged when domain instructions are empty")
    void emptyDomainInstructions() {
        String guide = "# DRL Guide\nSome content";
        String result = GuidesMerger.merge(guide, "");
        assertThat(result).isEqualTo(guide);
    }

    @Test
    @DisplayName("should return guide unchanged when domain instructions are blank")
    void blankDomainInstructions() {
        String guide = "# DRL Guide\nSome content";
        String result = GuidesMerger.merge(guide, "   \n  ");
        assertThat(result).isEqualTo(guide);
    }

    @Test
    @DisplayName("should merge guide with domain instructions")
    void mergesContent() {
        String guide = "# DRL Guide\nSome content";
        String domain = "# Domain Rules\n- Rule A\n- Rule B";

        String result = GuidesMerger.merge(guide, domain);

        assertThat(result).startsWith(guide);
        assertThat(result).contains("Domain-Specific Instructions");
        assertThat(result).contains("# Domain Rules");
        assertThat(result).contains("- Rule A");
        assertThat(result).contains("- Rule B");
    }

    @Test
    @DisplayName("should trim domain instructions")
    void trimsDomainInstructions() {
        String guide = "# DRL Guide";
        String domain = "  \n  Domain content with whitespace  \n  ";

        String result = GuidesMerger.merge(guide, domain);

        assertThat(result).contains("Domain content with whitespace");
        assertThat(result).doesNotContain("Domain content with whitespace  \n  ");
    }

    @Test
    @DisplayName("should throw for null guide")
    void nullGuideThrows() {
        assertThatThrownBy(() -> GuidesMerger.merge(null, "domain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DRL guide cannot be null");
    }

    @Test
    @DisplayName("should throw for blank guide")
    void blankGuideThrows() {
        assertThatThrownBy(() -> GuidesMerger.merge("  ", "domain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DRL guide cannot be null or blank");
    }

    @Test
    @DisplayName("should throw for empty guide")
    void emptyGuideThrows() {
        assertThatThrownBy(() -> GuidesMerger.merge("", "domain"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DRL guide cannot be null or blank");
    }

    @Test
    @DisplayName("should preserve multiline domain instructions")
    void preservesMultilineDomainInstructions() {
        String guide = "# DRL Guide";
        String domain = """
                # Insurance Domain Rules

                ## Premium Calculation
                - Base premium starts at $100
                - Add 10% for each risk factor

                ## Claim Processing
                - Auto-approve claims under $500
                - Require review for claims over $5000
                """;

        String result = GuidesMerger.merge(guide, domain);

        assertThat(result).contains("# Insurance Domain Rules");
        assertThat(result).contains("## Premium Calculation");
        assertThat(result).contains("## Claim Processing");
        assertThat(result).contains("Auto-approve claims under $500");
    }
}

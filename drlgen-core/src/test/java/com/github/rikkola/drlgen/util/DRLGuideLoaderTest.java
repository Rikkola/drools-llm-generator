package com.github.rikkola.drlgen.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DRLGuideLoader}.
 */
@DisplayName("DRLGuideLoader")
class DRLGuideLoaderTest {

    @AfterEach
    void clearCache() {
        DRLGuideLoader.clearCache();
    }

    @Test
    @DisplayName("should load guide from classpath")
    void loadsGuide() {
        String guide = DRLGuideLoader.loadGuide();

        assertThat(guide).isNotNull();
        assertThat(guide).isNotEmpty();
    }

    @Test
    @DisplayName("should contain expected sections")
    void containsExpectedSections() {
        String guide = DRLGuideLoader.loadGuide();

        assertThat(guide).contains("# DRL (Drools Rule Language) Reference Guide");
        assertThat(guide).contains("## 1. Basic Structure");
        assertThat(guide).contains("## 2. Type Declarations");
        assertThat(guide).contains("## 3. Pattern Matching");
        assertThat(guide).contains("## 4. The modify() Action");
        assertThat(guide).contains("## 5. Rule Salience and Priority");
        assertThat(guide).contains("## 9. Anti-Patterns and Fixes");
        assertThat(guide).contains("## 10. Complete Examples");
    }

    @Test
    @DisplayName("should contain critical syntax rules")
    void containsCriticalRules() {
        String guide = DRLGuideLoader.loadGuide();

        // Boolean operators
        assertThat(guide).contains("&&");
        assertThat(guide).contains("||");
        assertThat(guide).contains("Do NOT use `and` or `or`");

        // Setter naming
        assertThat(guide).contains("setIsEligible()");
        assertThat(guide).contains("NOT `setEligible()`");

        // Variable prefix in modify
        assertThat(guide).contains("$var.getField()");
    }

    @Test
    @DisplayName("should cache guide on subsequent calls")
    void cachesGuide() {
        String guide1 = DRLGuideLoader.loadGuide();
        String guide2 = DRLGuideLoader.loadGuide();

        // Same instance due to caching
        assertThat(guide1).isSameAs(guide2);
    }

    @Test
    @DisplayName("should return fresh guide after cache clear")
    void clearCacheWorks() {
        String guide1 = DRLGuideLoader.loadGuide();
        DRLGuideLoader.clearCache();
        String guide2 = DRLGuideLoader.loadGuide();

        // Same content but different instance
        assertThat(guide1).isEqualTo(guide2);
        assertThat(guide1).isNotSameAs(guide2);
    }
}

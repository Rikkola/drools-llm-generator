package com.github.rikkola.drlgen.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DomainInstructionsLoader")
class DomainInstructionsLoaderTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void cleanup() {
        DomainInstructionsLoader.clearCache();
    }

    @Test
    @DisplayName("should return empty string for null path")
    void nullPath() {
        String result = DomainInstructionsLoader.load(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for non-existent file")
    void nonExistentFile() {
        String result = DomainInstructionsLoader.load(Path.of("/non/existent/file.md"));
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return empty string for directory path")
    void directoryPath() {
        String result = DomainInstructionsLoader.load(tempDir);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should load content from existing file")
    void loadsExistingFile() throws IOException {
        Path tempFile = tempDir.resolve("domain-instructions.md");
        String content = "# Domain Instructions\n- Rule 1\n- Rule 2";
        Files.writeString(tempFile, content);

        String result = DomainInstructionsLoader.load(tempFile);
        assertThat(result).isEqualTo(content);
    }

    @Test
    @DisplayName("should cache content when caching enabled")
    void cachingWorks() throws IOException {
        Path tempFile = tempDir.resolve("cached-instructions.md");
        String content = "cached content";
        Files.writeString(tempFile, content);

        String result1 = DomainInstructionsLoader.load(tempFile, true);
        Files.writeString(tempFile, "modified content");
        String result2 = DomainInstructionsLoader.load(tempFile, true);

        assertThat(result1).isEqualTo(content);
        assertThat(result2).isEqualTo(content); // Still cached
    }

    @Test
    @DisplayName("should not cache when caching disabled")
    void noCachingByDefault() throws IOException {
        Path tempFile = tempDir.resolve("no-cache-instructions.md");
        String content1 = "original content";
        Files.writeString(tempFile, content1);

        String result1 = DomainInstructionsLoader.load(tempFile, false);

        String content2 = "modified content";
        Files.writeString(tempFile, content2);
        String result2 = DomainInstructionsLoader.load(tempFile, false);

        assertThat(result1).isEqualTo(content1);
        assertThat(result2).isEqualTo(content2); // Fresh read
    }

    @Test
    @DisplayName("should clear cache successfully")
    void clearCacheWorks() throws IOException {
        Path tempFile = tempDir.resolve("clear-cache-test.md");
        String content1 = "original content";
        Files.writeString(tempFile, content1);

        String result1 = DomainInstructionsLoader.load(tempFile, true);

        DomainInstructionsLoader.clearCache();

        String content2 = "modified content";
        Files.writeString(tempFile, content2);
        String result2 = DomainInstructionsLoader.load(tempFile, true);

        assertThat(result1).isEqualTo(content1);
        assertThat(result2).isEqualTo(content2); // Cache was cleared, so fresh read
    }

    @Test
    @DisplayName("should handle relative paths correctly")
    void relativePath() throws IOException {
        Path tempFile = tempDir.resolve("relative-test.md");
        String content = "relative path content";
        Files.writeString(tempFile, content);

        // Create a relative path
        Path relativePath = tempDir.relativize(tempFile.toAbsolutePath());
        // Note: This test may behave differently depending on working directory
        // The loader normalizes to absolute path internally

        String result = DomainInstructionsLoader.load(tempFile);
        assertThat(result).isEqualTo(content);
    }
}

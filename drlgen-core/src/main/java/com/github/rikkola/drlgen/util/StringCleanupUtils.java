package com.github.rikkola.drlgen.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for cleaning up generated content by removing markdown artifacts
 * and fixing common AI generation mistakes.
 */
public final class StringCleanupUtils {

    private static final Pattern DECLARE_BLOCK_PATTERN =
            Pattern.compile("declare\\s+(\\w+)\\s*\\n(.*?)\\nend", Pattern.DOTALL);
    // Matches boolean fields starting with 'is' (e.g., isVip, isActive)
    private static final Pattern IS_PREFIXED_FIELD_PATTERN =
            Pattern.compile("^\\s*(is[A-Z]\\w*)\\s*:\\s*boolean", Pattern.MULTILINE);
    // Matches boolean fields NOT starting with 'is' (e.g., valid, active, enabled)
    private static final Pattern NON_IS_BOOLEAN_FIELD_PATTERN =
            Pattern.compile("^\\s*([a-z]\\w*)\\s*:\\s*boolean", Pattern.MULTILINE);

    private StringCleanupUtils() {
        // Utility class
    }

    // Pattern to match a fact pattern line: optional binding, type name, opening paren
    // e.g., "$a : Applicant(" or "Person(" or "not Person("
    private static final Pattern FACT_PATTERN_LINE =
            Pattern.compile("^\\s*(?:not\\s+)?(?:\\$\\w+\\s*:\\s*)?\\w+\\s*\\(");

    // Pattern to extract content from markdown code blocks
    // Matches ```drl or ```drools blocks specifically
    private static final Pattern MARKDOWN_DRL_BLOCK_PATTERN =
            Pattern.compile("```(?:drl|drools)\\s*\\n(.*?)\\n?```", Pattern.DOTALL);
    // Matches plain ``` blocks (without language specifier)
    private static final Pattern MARKDOWN_PLAIN_BLOCK_PATTERN =
            Pattern.compile("```\\s*\\n(.*?)\\n?```", Pattern.DOTALL);

    /**
     * Cleans up generated DRL by extracting content from markdown code blocks
     * and applying various fixes.
     *
     * <p>If the input contains markdown code blocks (```drl, ```drools, or ```),
     * this method extracts only the content within the first suitable block,
     * removing any surrounding explanatory text.</p>
     */
    public static String cleanupDrl(String drl) {
        if (drl == null) return null;

        String cleaned = extractFromMarkdown(drl);
        cleaned = stripUnnecessaryImports(cleaned);
        cleaned = fixBooleanSetters(cleaned);
        cleaned = fixPatternConstraintOperators(cleaned);
        return cleaned;
    }

    /**
     * Removes unnecessary Java import statements from DRL.
     *
     * <p>DRL declare blocks don't need Java imports for basic types like
     * String, int, double, boolean. AI models often add unnecessary imports
     * like {@code import java.util.List;} which cause validation errors.</p>
     *
     * @param drl the DRL code
     * @return DRL with java.* imports removed
     */
    public static String stripUnnecessaryImports(String drl) {
        if (drl == null) return null;
        // Remove import statements for java.* packages (util, lang, math, etc.)
        return drl.replaceAll("import\\s+java\\..*?;\\s*\\n?", "");
    }

    /**
     * Extracts DRL content from markdown code blocks.
     *
     * <p>Priority order:
     * <ol>
     *   <li>```drl or ```drools blocks (preferred)</li>
     *   <li>Plain ``` blocks that contain DRL-like content</li>
     *   <li>Original content with markdown markers removed (fallback)</li>
     * </ol>
     */
    private static String extractFromMarkdown(String input) {
        // First, try to extract from ```drl or ```drools blocks
        Matcher drlMatcher = MARKDOWN_DRL_BLOCK_PATTERN.matcher(input);
        if (drlMatcher.find()) {
            return drlMatcher.group(1).trim();
        }

        // Next, try plain ``` blocks that look like they contain DRL
        Matcher genericMatcher = MARKDOWN_PLAIN_BLOCK_PATTERN.matcher(input);
        while (genericMatcher.find()) {
            String content = genericMatcher.group(1).trim();
            // Check if it looks like DRL (starts with package, declare, or rule)
            if (looksLikeDrl(content)) {
                return content;
            }
        }

        // Fallback: remove markdown markers but keep content
        // This handles cases where DRL isn't wrapped in code blocks
        return input.replaceAll("```(?:drl|drools)?\\n?", "")
                    .replaceAll("```\\n?", "")
                    .trim();
    }

    /**
     * Checks if content looks like DRL code.
     */
    private static boolean looksLikeDrl(String content) {
        String trimmed = content.trim();
        return trimmed.startsWith("package ") ||
               trimmed.startsWith("declare ") ||
               trimmed.startsWith("rule ") ||
               trimmed.startsWith("import ") ||
               trimmed.startsWith("global ");
    }

    /**
     * Fixes boolean setter names based on actual field names.
     *
     * Drools generates setters from the exact field name:
     * - Field 'isVip' requires setIsVip(), not setVip()
     * - Field 'valid' requires setValid(), not setIsValid()
     *
     * AI models often confuse these conventions.
     */
    public static String fixBooleanSetters(String drl) {
        if (drl == null) return null;

        String result = drl;

        // Fix 1: Field 'isXxx' but AI used 'setXxx()' - change to 'setIsXxx()'
        List<String> isFields = findBooleanFields(drl, IS_PREFIXED_FIELD_PATTERN);
        for (String fieldName : isFields) {
            // fieldName is like "isVip", fix "setVip(" to "setIsVip("
            String suffix = fieldName.substring(2); // "Vip" from "isVip"
            String wrongSetter = "set" + suffix + "(";
            String correctSetter = "set" + capitalize(fieldName) + "(";

            if (result.contains(wrongSetter) && !result.contains(correctSetter)) {
                result = result.replace(wrongSetter, correctSetter);
            }
        }

        // Fix 2: Field 'xxx' (not starting with 'is') but AI used 'setIsXxx()' - change to 'setXxx()'
        List<String> nonIsFields = findBooleanFields(drl, NON_IS_BOOLEAN_FIELD_PATTERN);
        for (String fieldName : nonIsFields) {
            // Skip fields that actually start with "is" (caught by first pattern)
            if (fieldName.startsWith("is") && fieldName.length() > 2 &&
                    Character.isUpperCase(fieldName.charAt(2))) {
                continue;
            }
            // fieldName is like "valid", fix "setIsValid(" to "setValid("
            String capitalizedName = capitalize(fieldName);
            String wrongSetter = "setIs" + capitalizedName + "(";
            String correctSetter = "set" + capitalizedName + "(";

            if (result.contains(wrongSetter)) {
                result = result.replace(wrongSetter, correctSetter);
            }
        }

        return result;
    }

    /**
     * Fixes logical operators in pattern constraints.
     *
     * AI models sometimes use 'or'/'and' keywords inside pattern constraints,
     * but DRL requires '||'/'&&' operators there.
     *
     * Valid DRL: Applicant( age > 60 || smoker || bmi > 30 )
     * Invalid:   Applicant( age > 60 or smoker or bmi > 30 )
     *
     * Note: 'or'/'and' ARE valid between patterns at the when level,
     * so we only fix them within pattern constraint blocks.
     */
    public static String fixPatternConstraintOperators(String drl) {
        if (drl == null) return null;

        StringBuilder result = new StringBuilder();
        String[] lines = drl.split("\n");

        for (String line : lines) {
            String fixedLine = line;

            // Check if this line contains a fact pattern
            if (isFactPatternLine(line)) {
                // Replace ' or ' with ' || ' and ' and ' with ' && '
                // Use word boundaries to avoid replacing parts of identifiers
                fixedLine = fixedLine.replaceAll("\\s+or\\s+", " || ");
                fixedLine = fixedLine.replaceAll("\\s+and\\s+", " && ");
            }

            result.append(fixedLine).append("\n");
        }

        // Remove trailing newline if original didn't have one
        if (!drl.endsWith("\n") && result.length() > 0) {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    /**
     * Checks if a line contains a fact pattern (e.g., "$a : Person(" or "not Order(").
     */
    private static boolean isFactPatternLine(String line) {
        return FACT_PATTERN_LINE.matcher(line).find();
    }

    /**
     * Finds boolean fields matching the given pattern in declare blocks.
     */
    private static List<String> findBooleanFields(String drl, Pattern fieldPattern) {
        List<String> fields = new ArrayList<>();

        Matcher declareMatcher = DECLARE_BLOCK_PATTERN.matcher(drl);
        while (declareMatcher.find()) {
            String blockContent = declareMatcher.group(2);
            Matcher fieldMatcher = fieldPattern.matcher(blockContent);
            while (fieldMatcher.find()) {
                fields.add(fieldMatcher.group(1));
            }
        }

        return fields;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Cleans up generated YAML by removing markdown code blocks.
     */
    public static String cleanupYaml(String yaml) {
        if (yaml == null) return null;
        return yaml.replaceAll("```(?:yaml|yml)?\\n?", "")
                   .replaceAll("```\\n?", "")
                   .trim();
    }

    /**
     * Cleans up generic text output by removing markdown code blocks.
     */
    public static String cleanupText(String text) {
        if (text == null) return null;
        return text.replaceAll("```(?:text|markdown)?\\n?", "")
                   .replaceAll("```\\n?", "")
                   .trim();
    }
}

package com.github.rikkola.drlgen.cleanup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default cleanup strategy that handles common AI generation artifacts.
 *
 * <p>This strategy performs the following cleanup operations:
 * <ol>
 *   <li>Remove markdown code block markers (```drl, ```drools)</li>
 *   <li>Fix boolean setter names based on field naming conventions</li>
 *   <li>Fix logical operators in pattern constraints (or/and → ||/&&)</li>
 *   <li>Remove commas between patterns in when clause (common AI mistake)</li>
 * </ol>
 */
public class DefaultCleanupStrategy implements DRLCleanupStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCleanupStrategy.class);

    private static final Pattern DECLARE_BLOCK_PATTERN =
            Pattern.compile("declare\\s+(\\w+)\\s*\\n(.*?)\\nend", Pattern.DOTALL);

    private static final Pattern IS_PREFIXED_FIELD_PATTERN =
            Pattern.compile("^\\s*(is[A-Z]\\w*)\\s*:\\s*boolean", Pattern.MULTILINE);

    private static final Pattern NON_IS_BOOLEAN_FIELD_PATTERN =
            Pattern.compile("^\\s*([a-z]\\w*)\\s*:\\s*boolean", Pattern.MULTILINE);

    private static final Pattern FACT_PATTERN_LINE =
            Pattern.compile("^\\s*(?:not\\s+)?(?:\\$\\w+\\s*:\\s*)?\\w+\\s*\\(");

    @Override
    public String cleanup(String drl) {
        if (drl == null) {
            return null;
        }

        LOG.debug("Cleaning up DRL ({} chars)", drl.length());

        String cleaned = drl;

        // Step 1: Remove markdown code blocks
        cleaned = removeMarkdownBlocks(cleaned);

        // Step 2: Fix boolean setters
        cleaned = fixBooleanSetters(cleaned);

        // Step 3: Fix pattern constraint operators
        cleaned = fixPatternConstraintOperators(cleaned);

        // Step 4: Remove commas between patterns in when clause
        cleaned = removeCommasBetweenPatterns(cleaned);

        LOG.debug("Cleanup complete ({} chars after cleanup)", cleaned.length());

        return cleaned;
    }

    private String removeMarkdownBlocks(String drl) {
        return drl.replaceAll("```(?:drl|drools)?\\n?", "")
                  .replaceAll("```\\n?", "")
                  .trim();
    }

    /**
     * Fixes boolean setter names based on actual field names.
     *
     * <p>Drools generates setters from the exact field name:
     * <ul>
     *   <li>Field 'isVip' requires setIsVip(), not setVip()</li>
     *   <li>Field 'valid' requires setValid(), not setIsValid()</li>
     * </ul>
     */
    private String fixBooleanSetters(String drl) {
        String result = drl;

        // Fix 1: Field 'isXxx' but AI used 'setXxx()' - change to 'setIsXxx()'
        List<String> isFields = findBooleanFields(drl, IS_PREFIXED_FIELD_PATTERN);
        for (String fieldName : isFields) {
            String suffix = fieldName.substring(2);
            String wrongSetter = "set" + suffix + "(";
            String correctSetter = "set" + capitalize(fieldName) + "(";

            if (result.contains(wrongSetter) && !result.contains(correctSetter)) {
                result = result.replace(wrongSetter, correctSetter);
                LOG.trace("Fixed setter: {} -> {}", wrongSetter, correctSetter);
            }
        }

        // Fix 2: Field 'xxx' (not starting with 'is') but AI used 'setIsXxx()'
        List<String> nonIsFields = findBooleanFields(drl, NON_IS_BOOLEAN_FIELD_PATTERN);
        for (String fieldName : nonIsFields) {
            if (fieldName.startsWith("is") && fieldName.length() > 2 &&
                    Character.isUpperCase(fieldName.charAt(2))) {
                continue;
            }

            String capitalizedName = capitalize(fieldName);
            String wrongSetter = "setIs" + capitalizedName + "(";
            String correctSetter = "set" + capitalizedName + "(";

            if (result.contains(wrongSetter)) {
                result = result.replace(wrongSetter, correctSetter);
                LOG.trace("Fixed setter: {} -> {}", wrongSetter, correctSetter);
            }
        }

        return result;
    }

    /**
     * Fixes logical operators in pattern constraints.
     *
     * <p>AI models sometimes use 'or'/'and' keywords inside pattern constraints,
     * but DRL requires '||'/'&&' operators there.</p>
     */
    private String fixPatternConstraintOperators(String drl) {
        StringBuilder result = new StringBuilder();
        String[] lines = drl.split("\n");

        for (String line : lines) {
            String fixedLine = line;

            if (isFactPatternLine(line)) {
                fixedLine = fixedLine.replaceAll("\\s+or\\s+", " || ");
                fixedLine = fixedLine.replaceAll("\\s+and\\s+", " && ");
            }

            result.append(fixedLine).append("\n");
        }

        if (!drl.endsWith("\n") && result.length() > 0) {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    private boolean isFactPatternLine(String line) {
        return FACT_PATTERN_LINE.matcher(line).find();
    }

    /**
     * Removes commas between patterns in the when clause.
     *
     * <p>AI models often incorrectly add commas between patterns:
     * <pre>
     * when
     *     $a : TypeA(),
     *     $b : TypeB()  // WRONG: comma after TypeA()
     * </pre>
     *
     * <p>This should be:
     * <pre>
     * when
     *     $a : TypeA()
     *     $b : TypeB()  // CORRECT: no comma between patterns
     * </pre>
     */
    private String removeCommasBetweenPatterns(String drl) {
        StringBuilder result = new StringBuilder();
        String[] lines = drl.split("\n");
        boolean inWhenBlock = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // Track when we enter/exit when blocks
            if (trimmed.equals("when") || trimmed.startsWith("when ")) {
                inWhenBlock = true;
            } else if (trimmed.equals("then") || trimmed.startsWith("then ")) {
                inWhenBlock = false;
            }

            // If we're in a when block and the line ends with ), followed by comma
            // and the next line looks like a pattern, remove the comma
            if (inWhenBlock && i < lines.length - 1) {
                String nextLine = lines[i + 1].trim();

                // Check if current line ends with ), or ),  (pattern followed by comma)
                // and next line starts a new pattern
                if (trimmed.matches(".*\\)\\s*,\\s*$") && isStartOfPattern(nextLine)) {
                    // Remove trailing comma (keep everything up to and including the closing paren)
                    line = line.replaceFirst("\\)\\s*,\\s*$", ")");
                    LOG.trace("Removed comma between patterns: {}", trimmed);
                }
            }

            result.append(line).append("\n");
        }

        // Remove trailing newline if original didn't have one
        if (!drl.endsWith("\n") && result.length() > 0) {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    /**
     * Checks if a line looks like the start of a pattern in a when clause.
     */
    private boolean isStartOfPattern(String trimmedLine) {
        // Matches patterns like:
        // $var : Type(
        // Type(
        // not Type(
        // not $var : Type(
        // exists Type(
        return trimmedLine.matches("^(not\\s+|exists\\s+)?(\\$\\w+\\s*:\\s*)?\\w+\\s*\\(.*");
    }

    private List<String> findBooleanFields(String drl, Pattern fieldPattern) {
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}

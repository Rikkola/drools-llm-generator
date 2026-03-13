package com.github.rikkola.drlgen.generation.runner;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Aggregates comparison results and generates reports.
 */
public class ComparisonReport {

    private final List<ComparisonResult> results = new ArrayList<>();
    private final Set<String> models = new LinkedHashSet<>();
    private final Set<String> scenarios = new LinkedHashSet<>();
    private final Set<String> formats = new LinkedHashSet<>();
    private final LocalDateTime startTime;

    public ComparisonReport() {
        this.startTime = LocalDateTime.now();
    }

    public void addResult(ComparisonResult result) {
        results.add(result);
        models.add(result.modelName());
        scenarios.add(result.scenarioName());
        formats.add(result.format());
    }

    /**
     * Gets all results.
     */
    public List<ComparisonResult> getResults() {
        return results;
    }

    /**
     * Gets a specific result by model, scenario, and format.
     */
    public ComparisonResult getResult(String modelName, String scenarioName, String format) {
        for (ComparisonResult result : results) {
            if (result.modelName().equals(modelName) &&
                    result.scenarioName().equals(scenarioName) &&
                    result.format().equals(format)) {
                return result;
            }
        }
        return null;
    }

    public void printConsoleReport() {
        String separator = "=".repeat(100);
        String thinSeparator = "-".repeat(100);

        System.out.println("\n" + separator);
        System.out.println(centerText("DROOLS RULE GENERATION COMPARISON REPORT", 100));
        System.out.println(separator);
        System.out.println("Date: " + startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        System.out.println("Models: " + String.join(", ", models));
        System.out.println("Scenarios: " + scenarios.size() + " total");
        System.out.println();

        // Print results matrix
        printResultsMatrix();

        // Print summary
        System.out.println("\nSUMMARY:");
        printSummary();
        System.out.println(separator);
    }

    private void printResultsMatrix() {
        // Calculate column widths dynamically based on formats
        int scenarioWidth = 30;
        int formatWidth = 7; // "ENGLISH" or shorter
        int modelColumnWidth = formats.size() * formatWidth;

        // Print header
        System.out.print(String.format("%-" + scenarioWidth + "s |", "Scenario"));
        for (String model : models) {
            String shortName = shortenModelName(model);
            System.out.print(String.format(" %-" + modelColumnWidth + "s |", shortName));
        }
        System.out.println();

        // Print sub-header with format names
        System.out.print(String.format("%-" + scenarioWidth + "s |", ""));
        for (int i = 0; i < models.size(); i++) {
            StringBuilder formatHeader = new StringBuilder();
            for (String format : formats) {
                formatHeader.append(String.format("%-7s", format.substring(0, Math.min(6, format.length()))));
            }
            System.out.print(" " + formatHeader + "|");
        }
        System.out.println();

        System.out.println("-".repeat(scenarioWidth + 2 + (modelColumnWidth + 3) * models.size()));

        // Print each scenario row
        for (String scenario : scenarios) {
            String shortScenario = scenario.length() > scenarioWidth - 1 ?
                    scenario.substring(0, scenarioWidth - 4) + "..." : scenario;
            System.out.print(String.format("%-" + scenarioWidth + "s |", shortScenario));

            for (String model : models) {
                StringBuilder statuses = new StringBuilder();
                for (String format : formats) {
                    String status = getResultStatus(model, scenario, format);
                    statuses.append(String.format("%-7s", status));
                }
                System.out.print(" " + statuses + "|");
            }
            System.out.println();
        }

        System.out.println("-".repeat(scenarioWidth + 2 + (modelColumnWidth + 3) * models.size()));

        // Print totals row
        System.out.print(String.format("%-" + scenarioWidth + "s |", "TOTALS"));
        for (String model : models) {
            StringBuilder totals = new StringBuilder();
            for (String format : formats) {
                int passes = countPasses(model, format);
                totals.append(String.format("%d/%-4d ", passes, scenarios.size()));
            }
            System.out.print(" " + totals + "|");
        }
        System.out.println();
    }

    private void printSummary() {
        int total = scenarios.size();

        // Find best for each format
        for (String format : formats) {
            String bestModel = null;
            int bestScore = -1;
            for (String model : models) {
                int score = countPasses(model, format);
                if (score > bestScore) {
                    bestScore = score;
                    bestModel = model;
                }
            }
            System.out.printf("- Best %s generator: %s (%d/%d = %.1f%%)%n",
                    format, bestModel, bestScore, total,
                    total > 0 ? (bestScore * 100.0 / total) : 0.0);
        }

        // Find best overall
        String bestOverallModel = null;
        int bestOverallScore = -1;
        for (String model : models) {
            int score = 0;
            for (String format : formats) {
                score += countPasses(model, format);
            }
            if (score > bestOverallScore) {
                bestOverallScore = score;
                bestOverallModel = model;
            }
        }

        int totalPossible = total * formats.size();
        System.out.printf("- Best overall: %s (%d/%d = %.1f%%)%n",
                bestOverallModel, bestOverallScore, totalPossible,
                totalPossible > 0 ? (bestOverallScore * 100.0 / totalPossible) : 0.0);
    }

    public void writeCsvReport(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            // Header
            writer.println("Model,Scenario,Format,Success,RulesFired,GenerationTimeMs,ErrorMessage");

            // Data rows
            for (ComparisonResult result : results) {
                writer.printf("%s,%s,%s,%s,%d,%d,%s%n",
                        escapeCSV(result.modelName()),
                        escapeCSV(result.scenarioName()),
                        result.format(),
                        result.success(),
                        result.rulesFired(),
                        result.generationTime().toMillis(),
                        escapeCSV(result.errorMessage() != null ? result.errorMessage() : ""));
            }

            System.out.println("\nCSV report written to: " + filename);
        } catch (IOException e) {
            System.err.println("Failed to write CSV report: " + e.getMessage());
        }
    }

    private String getResultStatus(String model, String scenario, String format) {
        for (ComparisonResult result : results) {
            if (result.modelName().equals(model) &&
                    result.scenarioName().equals(scenario) &&
                    result.format().equals(format)) {
                return result.getStatusString();
            }
        }
        return "N/A";
    }

    private int countPasses(String model, String format) {
        return (int) results.stream()
                .filter(r -> r.modelName().equals(model) && r.format().equals(format) && r.success())
                .count();
    }

    private String shortenModelName(String modelName) {
        // Shorten model names for display
        if (modelName.contains("granite-code:8b")) return "granite-8b";
        if (modelName.contains("granite-code:20b")) return "granite-20b";
        if (modelName.contains("granite3-moe")) return "granite-moe";
        if (modelName.contains("granite3.3")) return "granite-3.3";
        if (modelName.contains("qwen2.5-coder")) return "qwen-coder";
        if (modelName.contains("qwen3")) return "qwen3";
        if (modelName.contains("llama3")) return "llama3";
        if (modelName.contains("codellama")) return "codellama";
        return modelName.length() > 12 ? modelName.substring(0, 12) : modelName;
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        return " ".repeat(Math.max(0, padding)) + text;
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

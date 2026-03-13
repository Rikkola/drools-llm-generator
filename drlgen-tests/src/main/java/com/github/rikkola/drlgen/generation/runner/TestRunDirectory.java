package com.github.rikkola.drlgen.generation.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages the test run directory structure for storing artifacts.
 */
public class TestRunDirectory {

    private static final String SCENARIOS_RESOURCE_DIR = "scenarios";
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path runDirectory;
    private final Path scenariosDirectory;

    private TestRunDirectory(Path runDirectory) {
        this.runDirectory = runDirectory;
        this.scenariosDirectory = runDirectory.resolve("scenarios");
    }

    /**
     * Creates a new timestamped run directory.
     *
     * @param baseDir   Base directory for test runs (e.g., "test-runs")
     * @param modelName Model name to include in directory name
     * @return TestRunDirectory instance
     */
    public static TestRunDirectory create(Path baseDir, String modelName) throws IOException {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String safeName = sanitizeDirectoryName(modelName);
        String dirName = timestamp + "_" + safeName;

        Path runDir = baseDir.resolve(dirName);
        Files.createDirectories(runDir);
        Files.createDirectories(runDir.resolve("scenarios"));

        return new TestRunDirectory(runDir);
    }

    /**
     * Creates a subdirectory for a specific scenario.
     *
     * @param scenarioName Name of the scenario
     * @return Path to the scenario directory
     */
    public Path createScenarioDirectory(String scenarioName) throws IOException {
        String safeName = sanitizeDirectoryName(scenarioName);
        Path scenarioDir = scenariosDirectory.resolve(safeName);
        Files.createDirectories(scenarioDir);
        return scenarioDir;
    }

    /**
     * Copies the original YAML scenario file to the scenario directory.
     *
     * @param scenarioFilename Original YAML filename (e.g., "adult-validation.yaml")
     * @param scenarioDir      Target scenario directory
     */
    public void copyScenarioYaml(String scenarioFilename, Path scenarioDir) throws IOException {
        String resourcePath = SCENARIOS_RESOURCE_DIR + "/" + scenarioFilename;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                Files.copy(is, scenarioDir.resolve("scenario.yaml"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /**
     * Saves generated DRL to the scenario directory.
     *
     * @param drl         Generated DRL content
     * @param scenarioDir Target scenario directory
     */
    public void saveGeneratedDrl(String drl, Path scenarioDir) throws IOException {
        if (drl != null) {
            Files.writeString(scenarioDir.resolve("generated.drl"), drl);
        }
    }

    /**
     * Saves the scenario result as JSON.
     *
     * @param result      ScenarioResult to save
     * @param scenarioDir Target scenario directory
     */
    public void saveResultJson(ScenarioResult result, Path scenarioDir) throws IOException {
        String json = objectMapper.writeValueAsString(result);
        Files.writeString(scenarioDir.resolve("result.json"), json);
    }

    /**
     * Appends log content to the scenario log file.
     *
     * @param logContent  Log content to append
     * @param scenarioDir Target scenario directory
     */
    public void appendLog(String logContent, Path scenarioDir) throws IOException {
        Path logFile = scenarioDir.resolve("test-log.txt");
        if (Files.exists(logFile)) {
            String existing = Files.readString(logFile);
            Files.writeString(logFile, existing + logContent);
        } else {
            Files.writeString(logFile, logContent);
        }
    }

    /**
     * Gets the path to the run directory.
     */
    public Path getRunDirectory() {
        return runDirectory;
    }

    /**
     * Gets the path to the scenarios directory.
     */
    public Path getScenariosDirectory() {
        return scenariosDirectory;
    }

    /**
     * Writes the run summary file.
     *
     * @param summary Summary content
     */
    public void writeRunSummary(String summary) throws IOException {
        Files.writeString(runDirectory.resolve("run-summary.txt"), summary);
    }

    /**
     * Writes the CSV results file.
     *
     * @param csvContent CSV content
     */
    public void writeCsvResults(String csvContent) throws IOException {
        Files.writeString(runDirectory.resolve("results.csv"), csvContent);
    }

    /**
     * Sanitizes a name for use as a directory name.
     */
    private static String sanitizeDirectoryName(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}

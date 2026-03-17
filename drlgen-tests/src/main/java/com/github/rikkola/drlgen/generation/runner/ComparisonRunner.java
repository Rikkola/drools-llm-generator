package com.github.rikkola.drlgen.generation.runner;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.agent.AgentFactory;
import com.github.rikkola.drlgen.agent.AgentType;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.config.ModelDefinition;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.generation.loader.YAMLScenarioLoader;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.provider.TestScenarioProvider;
import com.github.rikkola.drlgen.generation.service.DRLGenerationService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;

/**
 * Main runner for comparing DRL rule generation across models.
 *
 * Usage:
 *   java ComparisonRunner [options]
 *
 * Options:
 *   --models <model1,model2,...>   Comma-separated list of models (default: all from models.yaml)
 *   --scenarios <name1,name2,...>  Filter scenarios by name substring
 *   --scenarios-dir <path>         Load scenarios from file system directory
 *   --instructions <path>          Domain instructions file to augment DRL guide
 *   --max-turns <n>                Max conversation turns for self-correction (default: 1, max: 5)
 *   --output <filename.csv>        CSV output file (default: comparison-results.csv)
 *   --output-dir <directory>       Output directory for test artifacts (default: test-runs)
 */
public class ComparisonRunner {

    public ComparisonRunner() {
    }

    /**
     * Creates a DRLGenerationService configured for the specified model's agent type.
     */
    private DRLGenerationService createServiceForModel(String modelName) {
        AgentType agentType = ModelConfiguration.getModelDefinition(modelName)
                .map(ModelDefinition::getAgentType)
                .orElse(AgentType.GUIDED);

        return DRLGenerationService.builder()
                .agentFactory(AgentFactory.forType(agentType))
                .build();
    }

    public static void main(String[] args) {
        ComparisonRunner runner = new ComparisonRunner();
        runner.run(args);
    }

    public void run(String[] args) {
        // Check for help flag first
        if (hasFlag(args, "-h") || hasFlag(args, "--help")) {
            printUsage();
            return;
        }

        // Parse arguments
        List<String> models = parseModels(args);
        Path scenariosDir = parseScenariosDir(args);
        Path instructionsPath = parseInstructionsPath(args);
        List<TestScenario> scenarios = parseScenarios(args, scenariosDir);
        String outputFile = parseOutput(args);
        Path outputDir = parseOutputDir(args);
        int maxTurns = parseMaxTurns(args);

        System.out.println("Starting Drools Rule Generation Comparison");
        System.out.println("Models: " + models.size());
        System.out.println("Scenarios: " + scenarios.size());
        if (maxTurns > 1) {
            System.out.println("Max turns: " + maxTurns + " (conversational mode)");
        }
        if (scenariosDir != null) {
            System.out.println("Scenarios directory: " + scenariosDir.toAbsolutePath());
        }
        if (instructionsPath != null) {
            System.out.println("Instructions file: " + instructionsPath.toAbsolutePath());
        }
        System.out.println("Output directory: " + outputDir.toAbsolutePath());
        System.out.println();

        ComparisonReport report = new ComparisonReport();

        int totalTests = models.size() * scenarios.size();
        int currentTest = 0;

        for (String modelName : models) {
            System.out.println("\n=== Testing model: " + modelName + " ===");

            // Get agent type for this model
            AgentType agentType = ModelConfiguration.getModelDefinition(modelName)
                    .map(ModelDefinition::getAgentType)
                    .orElse(AgentType.GUIDED);
            System.out.println("Agent type: " + agentType);

            // Create run directory for this model
            TestRunDirectory runDir = null;
            try {
                runDir = TestRunDirectory.create(outputDir, modelName);
                System.out.println("Artifacts directory: " + runDir.getRunDirectory());
            } catch (IOException e) {
                System.err.println("Failed to create output directory: " + e.getMessage());
            }

            ChatModel chatModel;
            DRLGenerationService drlService;
            try {
                chatModel = ModelConfiguration.createModel(modelName);
                drlService = createServiceForModel(modelName);
            } catch (Exception e) {
                System.err.println("Failed to load model " + modelName + ": " + e.getMessage());
                // Add failure results for all scenarios
                for (TestScenario scenario : scenarios) {
                    ComparisonResult result = ComparisonResult.failure(
                            modelName, scenario.name(), "DRL",
                            Duration.ZERO, "Model load failed: " + e.getMessage());
                    report.addResult(result);
                    saveResultArtifacts(runDir, scenario, result);
                }
                continue;
            }

            for (TestScenario scenario : scenarios) {
                currentTest++;
                System.out.printf("[%d/%d] %s - %s... ",
                        currentTest, totalTests, modelName, scenario.name());

                ComparisonResult result = testDRL(drlService, chatModel, modelName, scenario, instructionsPath, maxTurns);
                report.addResult(result);
                System.out.println(result.getStatusString() +
                        (result.success() ? " (" + result.rulesFired() + " rules)" : ""));

                // Save artifacts for this scenario
                saveResultArtifacts(runDir, scenario, result);
            }

            // Write run summary for this model
            if (runDir != null) {
                try {
                    writeRunSummary(runDir, modelName, scenarios, report);
                    writeCsvToRunDir(runDir, report);
                } catch (IOException e) {
                    System.err.println("Failed to write run summary: " + e.getMessage());
                }
            }
        }

        // Generate legacy reports (for backward compatibility)
        report.printConsoleReport();
        report.writeCsvReport(outputFile);
    }

    private ComparisonResult testDRL(DRLGenerationService drlService, ChatModel model, String modelName,
                                       TestScenario scenario, Path instructionsPath, int maxTurns) {
        List<ScenarioResult.TestCaseResult> testCaseResults = new ArrayList<>();
        List<String> factsInMemory = new ArrayList<>();

        try {
            GenerationResult result;
            if (maxTurns > 1) {
                // Use conversational generation with retries
                result = drlService.generateAndTestWithRetry(model, scenario, maxTurns, instructionsPath);
            } else {
                // Use single-turn generation
                result = drlService.generateAndTest(model, scenario, instructionsPath);
            }

            // Capture facts as strings
            if (result.resultingFacts() != null) {
                for (Object fact : result.resultingFacts()) {
                    factsInMemory.add(fact.toString());
                }
            }

            if (result.isSuccessful()) {
                // Create test case results for successful run
                for (TestScenario.TestCase tc : scenario.testCases()) {
                    testCaseResults.add(ScenarioResult.TestCaseResult.success(
                            tc.name(),
                            result.rulesFired() / scenario.testCases().size(), // Approximate per test case
                            tc.expectedFieldValues(),
                            Map.of() // Actual values would need to be tracked separately
                    ));
                }

                return ComparisonResult.success(
                        modelName,
                        scenario.name(),
                        scenario.sourceFilename(),
                        "DRL",
                        result.rulesFired(),
                        result.generationTime(),
                        result.generatedDrl(),
                        testCaseResults,
                        factsInMemory
                );
            } else {
                return ComparisonResult.failure(
                        modelName,
                        scenario.name(),
                        scenario.sourceFilename(),
                        "DRL",
                        result.generationTime(),
                        result.validationMessage(),
                        result.generatedDrl(),
                        testCaseResults
                );
            }
        } catch (Exception e) {
            return ComparisonResult.failure(
                    modelName,
                    scenario.name(),
                    scenario.sourceFilename(),
                    "DRL",
                    Duration.ZERO,
                    "Exception: " + e.getMessage(),
                    null,
                    testCaseResults
            );
        }
    }

    private void saveResultArtifacts(TestRunDirectory runDir, TestScenario scenario, ComparisonResult result) {
        if (runDir == null) return;

        try {
            Path scenarioDir = runDir.createScenarioDirectory(scenario.name());

            // Copy original YAML
            if (scenario.sourceFilename() != null) {
                runDir.copyScenarioYaml(scenario.sourceFilename(), scenarioDir);
            }

            // Save generated DRL
            if (result.generatedDrl() != null) {
                runDir.saveGeneratedDrl(result.generatedDrl(), scenarioDir);
            }

            // Save result JSON
            runDir.saveResultJson(result.toScenarioResult(), scenarioDir);

            // Save log content
            StringBuilder log = new StringBuilder();
            log.append("Scenario: ").append(scenario.name()).append("\n");
            log.append("Status: ").append(result.getStatusString()).append("\n");
            log.append("Rules Fired: ").append(result.rulesFired()).append("\n");
            log.append("Generation Time: ").append(result.generationTime().toMillis()).append("ms\n");
            if (result.errorMessage() != null) {
                log.append("Error: ").append(result.errorMessage()).append("\n");
            }
            runDir.appendLog(log.toString(), scenarioDir);

        } catch (IOException e) {
            System.err.println("Failed to save artifacts for " + scenario.name() + ": " + e.getMessage());
        }
    }

    private void writeRunSummary(TestRunDirectory runDir, String modelName,
                                  List<TestScenario> scenarios, ComparisonReport report) throws IOException {
        StringBuilder summary = new StringBuilder();
        summary.append("=== DRL Generation Test Run Summary ===\n\n");
        summary.append("Model: ").append(modelName).append("\n");
        summary.append("Scenarios: ").append(scenarios.size()).append("\n");

        int passed = 0;
        int failed = 0;
        for (TestScenario scenario : scenarios) {
            ComparisonResult result = report.getResult(modelName, scenario.name(), "DRL");
            if (result != null && result.success()) {
                passed++;
            } else {
                failed++;
            }
        }

        summary.append("Passed: ").append(passed).append("\n");
        summary.append("Failed: ").append(failed).append("\n");
        summary.append("Success Rate: ").append(String.format("%.1f%%", passed * 100.0 / scenarios.size())).append("\n\n");

        summary.append("=== Results ===\n");
        for (TestScenario scenario : scenarios) {
            ComparisonResult result = report.getResult(modelName, scenario.name(), "DRL");
            String status = (result != null && result.success()) ? "PASS" : "FAIL";
            summary.append(String.format("%-40s %s\n", scenario.name(), status));
        }

        runDir.writeRunSummary(summary.toString());
    }

    private void writeCsvToRunDir(TestRunDirectory runDir, ComparisonReport report) throws IOException {
        StringBuilder csv = new StringBuilder();
        csv.append("Model,Scenario,Format,Success,RulesFired,GenerationTimeMs,ErrorMessage\n");

        for (ComparisonResult result : report.getResults()) {
            csv.append(escapeCSV(result.modelName())).append(",");
            csv.append(escapeCSV(result.scenarioName())).append(",");
            csv.append(result.format()).append(",");
            csv.append(result.success()).append(",");
            csv.append(result.rulesFired()).append(",");
            csv.append(result.generationTime().toMillis()).append(",");
            csv.append(escapeCSV(result.errorMessage() != null ? result.errorMessage() : "")).append("\n");
        }

        runDir.writeCsvResults(csv.toString());
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private List<String> parseModels(String[] args) {
        String modelsArg = getArgValue(args, "--models");
        List<String> availableModels = ModelConfiguration.getAvailableModels();

        if (modelsArg == null) {
            // Default: all models from configuration
            return availableModels;
        }

        List<String> models = new ArrayList<>();
        for (String modelName : modelsArg.split(",")) {
            modelName = modelName.trim();
            // Check if exact match exists
            if (availableModels.contains(modelName)) {
                models.add(modelName);
            } else {
                // Try partial match
                String match = findPartialMatch(modelName, availableModels);
                if (match != null) {
                    System.out.println("Note: '" + modelName + "' matched to '" + match + "'");
                    models.add(match);
                } else {
                    System.err.println("Warning: Model '" + modelName + "' not found.");
                    System.err.println("  Available models: " + availableModels);
                }
            }
        }

        if (models.isEmpty()) {
            System.err.println("Error: No valid models specified. Use --help for usage.");
            System.exit(1);
        }
        return models;
    }

    private String findPartialMatch(String modelName, List<String> availableModels) {
        for (String available : availableModels) {
            if (available.contains(modelName) || modelName.contains(available)) {
                return available;
            }
        }
        return null;
    }

    private List<TestScenario> parseScenarios(String[] args, Path scenariosDir) {
        String scenariosArg = getArgValue(args, "--scenarios");

        // Load scenarios from file system directory or classpath
        List<TestScenario> allScenarios;
        if (scenariosDir != null) {
            YAMLScenarioLoader loader = new YAMLScenarioLoader();
            allScenarios = loader.loadScenariosFromFileSystem(scenariosDir);
            if (allScenarios.isEmpty()) {
                System.err.println("Warning: No scenarios found in directory: " + scenariosDir);
            }
        } else {
            allScenarios = TestScenarioProvider.getAllScenarios();
        }

        if (scenariosArg == null) {
            return allScenarios;
        }

        List<String> filters = Arrays.asList(scenariosArg.toLowerCase().split(","));
        List<TestScenario> filtered = new ArrayList<>();
        for (TestScenario scenario : allScenarios) {
            for (String filter : filters) {
                if (scenario.name().toLowerCase().contains(filter.trim())) {
                    filtered.add(scenario);
                    break;
                }
            }
        }
        return filtered.isEmpty() ? allScenarios : filtered;
    }

    private Path parseScenariosDir(String[] args) {
        String scenariosDir = getArgValue(args, "--scenarios-dir");
        if (scenariosDir == null) {
            return null;
        }
        Path path = Paths.get(scenariosDir);
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            System.err.println("Warning: Scenarios directory does not exist or is not a directory: " + scenariosDir);
            return null;
        }
        return path;
    }

    private Path parseInstructionsPath(String[] args) {
        String instructionsFile = getArgValue(args, "--instructions");
        if (instructionsFile == null) {
            return null;
        }
        Path path = Paths.get(instructionsFile);
        if (!Files.exists(path)) {
            System.err.println("Warning: Instructions file does not exist: " + instructionsFile);
            return null;
        }
        return path;
    }

    private String parseOutput(String[] args) {
        String output = getArgValue(args, "--output");
        return output != null ? output : "comparison-results.csv";
    }

    private Path parseOutputDir(String[] args) {
        String outputDir = getArgValue(args, "--output-dir");
        return Paths.get(outputDir != null ? outputDir : "test-runs");
    }

    private int parseMaxTurns(String[] args) {
        String maxTurnsArg = getArgValue(args, "--max-turns");
        if (maxTurnsArg == null) {
            return 1; // Default: single turn (no retries)
        }
        try {
            int maxTurns = Integer.parseInt(maxTurnsArg);
            if (maxTurns < 1) {
                System.err.println("Warning: --max-turns must be at least 1, using 1");
                return 1;
            }
            if (maxTurns > 5) {
                System.err.println("Warning: --max-turns capped at 5, using 5");
                return 5;
            }
            return maxTurns;
        } catch (NumberFormatException e) {
            System.err.println("Warning: Invalid --max-turns value '" + maxTurnsArg + "', using 1");
            return 1;
        }
    }

    private String getArgValue(String[] args, String argName) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(argName)) {
                return args[i + 1];
            }
        }
        return null;
    }

    private boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) {
                return true;
            }
        }
        return false;
    }

    private void printUsage() {
        System.out.println("DRL Generation Comparison Runner");
        System.out.println();
        System.out.println("Runs test scenarios against AI models to evaluate DRL rule generation.");
        System.out.println();
        System.out.println("Usage: java -jar drlgen-tests.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --models <list>        Comma-separated model names/ids (default: all from models.yaml)");
        System.out.println("  --scenarios <list>     Filter scenarios by name substring (default: all)");
        System.out.println("  --scenarios-dir <path> Load scenarios from filesystem directory");
        System.out.println("  --instructions <path>  Domain instructions file to augment DRL guide");
        System.out.println("  --max-turns <n>        Max conversation turns for self-correction (default: 1, max: 5)");
        System.out.println("                         Use 3 for initial generation + 2 retries");
        System.out.println("  --output <file>        CSV output filename (default: comparison-results.csv)");
        System.out.println("  --output-dir <dir>     Output directory for artifacts (default: test-runs)");
        System.out.println("  -h, --help             Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar drlgen-tests.jar");
        System.out.println("  java -jar drlgen-tests.jar --models qwen3-coder-next,granite4:small-h");
        System.out.println("  java -jar drlgen-tests.jar --scenarios adult,discount");
        System.out.println("  java -jar drlgen-tests.jar --scenarios-dir ./my-scenarios --output results.csv");
        System.out.println("  java -jar drlgen-tests.jar --models qwen2.5-coder --max-turns 3  # With retries");
        System.out.println();
        System.out.println("Prerequisites:");
        System.out.println("  - Java 17+");
        System.out.println("  - Ollama running locally (ollama serve)");
        System.out.println("  - Required models pulled (ollama pull <model-name>)");
    }
}

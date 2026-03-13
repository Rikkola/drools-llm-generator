package com.github.rikkola.drlgen.generation.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.model.TestScenario.ExpectedFact;
import com.github.rikkola.drlgen.generation.model.TestScenario.FactTypeDefinition;
import com.github.rikkola.drlgen.generation.model.TestScenario.TestCase;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads TestScenario objects from YAML files.
 * Supports both the default scenarios directory and domain-specific directories.
 */
public class YAMLScenarioLoader {

    private static final String DEFAULT_SCENARIOS_DIR = "scenarios";
    private static final String DOMAINS_DIR = "domains";
    private static final String DOMAIN_INSTRUCTIONS_FILE = "domain-instructions.md";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final Yaml yaml = new Yaml();

    /**
     * Load a single scenario from a YAML file in the default scenarios directory.
     *
     * @param filename the filename (e.g., "adult-validation.yaml")
     * @return the parsed TestScenario
     */
    public TestScenario loadScenario(String filename) {
        return loadScenarioFromDirectory(DEFAULT_SCENARIOS_DIR, filename);
    }

    /**
     * Load a single scenario from a domain's scenarios directory.
     *
     * @param domainName the domain name (e.g., "insurance", "banking")
     * @param filename   the filename (e.g., "claim-auto-approval.yaml")
     * @return the parsed TestScenario
     */
    public TestScenario loadDomainScenario(String domainName, String filename) {
        String directory = DOMAINS_DIR + "/" + domainName + "/scenarios";
        return loadScenarioFromDirectory(directory, filename);
    }

    /**
     * Load all scenarios from the default scenarios directory.
     *
     * @return list of all scenarios
     */
    public List<TestScenario> loadAllScenarios() {
        return loadAllScenarios(DEFAULT_SCENARIOS_DIR);
    }

    /**
     * Load all scenarios from a specified directory.
     *
     * @param directory the directory path relative to classpath
     * @return list of all scenarios
     */
    public List<TestScenario> loadAllScenarios(String directory) {
        List<TestScenario> scenarios = new ArrayList<>();
        try {
            URL dirUrl = getClass().getClassLoader().getResource(directory);
            if (dirUrl == null) {
                return scenarios;
            }

            URI uri = dirUrl.toURI();

            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    Path dirPath = fs.getPath(directory);
                    loadScenariosFromPath(dirPath, directory, scenarios);
                }
            } else {
                Path dirPath = Paths.get(uri);
                loadScenariosFromPath(dirPath, directory, scenarios);
            }
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Failed to load scenarios from directory: " + directory, e);
        }
        return scenarios;
    }

    /**
     * Load all scenarios for a specific domain.
     * Domain scenarios are located in domains/{domainName}/scenarios/
     *
     * @param domainName the domain name (e.g., "insurance", "banking")
     * @return list of scenarios for the domain
     */
    public List<TestScenario> loadDomainScenarios(String domainName) {
        String directory = DOMAINS_DIR + "/" + domainName + "/scenarios";
        return loadAllScenarios(directory);
    }

    /**
     * Get the path to the domain instructions file.
     * Returns null if the file doesn't exist.
     *
     * @param domainName the domain name (e.g., "insurance", "banking")
     * @return Path to the domain instructions file, or null if not found
     */
    public Path getDomainInstructionsPath(String domainName) {
        String resourcePath = DOMAINS_DIR + "/" + domainName + "/" + DOMAIN_INSTRUCTIONS_FILE;
        URL url = getClass().getClassLoader().getResource(resourcePath);
        if (url == null) {
            return null;
        }
        try {
            URI uri = url.toURI();
            if (uri.getScheme().equals("jar")) {
                // For JAR resources, we need to extract to temp file or return null
                // For simplicity, return null for JAR resources (use getDomainInstructionsContent instead)
                return null;
            }
            return Paths.get(uri);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Get the content of the domain instructions file.
     *
     * @param domainName the domain name (e.g., "insurance", "banking")
     * @return the content of the domain instructions, or empty string if not found
     */
    public String getDomainInstructionsContent(String domainName) {
        String resourcePath = DOMAINS_DIR + "/" + domainName + "/" + DOMAIN_INSTRUCTIONS_FILE;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                return "";
            }
            return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * List all available domain names.
     *
     * @return list of domain names that have scenarios
     */
    public List<String> listAvailableDomains() {
        List<String> domains = new ArrayList<>();
        try {
            URL dirUrl = getClass().getClassLoader().getResource(DOMAINS_DIR);
            if (dirUrl == null) {
                return domains;
            }

            URI uri = dirUrl.toURI();

            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    Path dirPath = fs.getPath(DOMAINS_DIR);
                    listDomainsFromPath(dirPath, domains);
                }
            } else {
                Path dirPath = Paths.get(uri);
                listDomainsFromPath(dirPath, domains);
            }
        } catch (IOException | URISyntaxException e) {
            // Return empty list on error
        }
        return domains;
    }

    private void listDomainsFromPath(Path dirPath, List<String> domains) throws IOException {
        if (!Files.exists(dirPath)) {
            return;
        }
        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .forEach(domains::add);
        }
    }

    private void loadScenariosFromPath(Path dirPath, String baseDirectory, List<TestScenario> scenarios) throws IOException {
        try (Stream<Path> paths = Files.list(dirPath)) {
            paths.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .sorted()
                    .forEach(path -> {
                        String filename = path.getFileName().toString();
                        scenarios.add(loadScenarioFromDirectory(baseDirectory, filename));
                    });
        }
    }

    /**
     * Load all scenarios from a file system directory path.
     * Unlike classpath-based loading, this reads directly from the file system.
     *
     * @param directory the file system path to the scenarios directory
     * @return list of all scenarios found in the directory
     */
    public List<TestScenario> loadScenariosFromFileSystem(Path directory) {
        List<TestScenario> scenarios = new ArrayList<>();
        if (directory == null || !Files.exists(directory) || !Files.isDirectory(directory)) {
            return scenarios;
        }

        try (Stream<Path> paths = Files.list(directory)) {
            paths.filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                    .sorted()
                    .forEach(path -> {
                        try {
                            scenarios.add(loadScenarioFromFile(path));
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to load scenario: " + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to list scenarios in directory: " + directory, e);
        }
        return scenarios;
    }

    /**
     * Load a single scenario from a file system path.
     *
     * @param filePath the file system path to the YAML scenario file
     * @return the parsed TestScenario
     */
    public TestScenario loadScenarioFromFile(Path filePath) {
        if (filePath == null || !Files.exists(filePath)) {
            throw new IllegalArgumentException("Scenario file not found: " + filePath);
        }

        try (InputStream is = Files.newInputStream(filePath)) {
            Map<String, Object> data = yaml.load(is);
            return parseScenario(data, filePath.getFileName().toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scenario: " + filePath, e);
        }
    }

    /**
     * Load a scenario from a specific directory.
     *
     * @param directory the directory path relative to classpath
     * @param filename  the filename (e.g., "adult-validation.yaml")
     * @return the parsed TestScenario
     */
    public TestScenario loadScenarioFromDirectory(String directory, String filename) {
        String path = directory + "/" + filename;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Scenario file not found: " + path);
            }
            Map<String, Object> data = yaml.load(is);
            return parseScenario(data, filename);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load scenario: " + path, e);
        }
    }

    @SuppressWarnings("unchecked")
    private TestScenario parseScenario(Map<String, Object> data, String filename) {
        String name = (String) data.get("name");
        String description = (String) data.get("description");
        String requirement = (String) data.get("requirement");

        List<FactTypeDefinition> factTypes = parseFactTypes((List<Map<String, Object>>) data.get("factTypes"));
        List<TestCase> testCases = parseTestCases((List<Map<String, Object>>) data.get("testCases"));

        return new TestScenario(name, description, requirement, factTypes, testCases, filename);
    }

    @SuppressWarnings("unchecked")
    private List<FactTypeDefinition> parseFactTypes(List<Map<String, Object>> factTypesData) {
        if (factTypesData == null) {
            return List.of();
        }
        List<FactTypeDefinition> result = new ArrayList<>();
        for (Map<String, Object> ft : factTypesData) {
            String typeName = (String) ft.get("name");
            Map<String, String> fields = new LinkedHashMap<>();
            Map<String, Object> fieldsData = (Map<String, Object>) ft.get("fields");
            if (fieldsData != null) {
                fieldsData.forEach((k, v) -> fields.put(k, String.valueOf(v)));
            }
            result.add(new FactTypeDefinition(typeName, fields));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<TestCase> parseTestCases(List<Map<String, Object>> testCasesData) {
        if (testCasesData == null) {
            return List.of();
        }
        List<TestCase> result = new ArrayList<>();
        for (Map<String, Object> tc : testCasesData) {
            String name = (String) tc.get("name");
            Object input = tc.get("input");
            String inputJson = convertToJson(input);

            // Parse legacy expectedFields (backward compatibility)
            Map<String, Object> expectedFields = (Map<String, Object>) tc.get("expectedFields");
            if (expectedFields == null) {
                expectedFields = Map.of();
            }

            // Parse new typed expectedFacts
            List<ExpectedFact> expectedFacts = parseExpectedFacts(
                    (List<Map<String, Object>>) tc.get("expectedFacts"));

            // Parse expectedRulesFired (null = don't check, 0 = no rules, N = exactly N rules)
            Integer expectedRulesFired = null;
            Object expectedRulesFiredObj = tc.get("expectedRulesFired");
            if (expectedRulesFiredObj != null) {
                expectedRulesFired = ((Number) expectedRulesFiredObj).intValue();
            }

            result.add(new TestCase(name, inputJson, expectedFields, expectedFacts, expectedRulesFired));
        }
        return result;
    }

    /**
     * Parses the expectedFacts list from YAML.
     */
    @SuppressWarnings("unchecked")
    private List<ExpectedFact> parseExpectedFacts(List<Map<String, Object>> expectedFactsData) {
        if (expectedFactsData == null) {
            return List.of();
        }

        List<ExpectedFact> result = new ArrayList<>();
        for (Map<String, Object> ef : expectedFactsData) {
            String type = (String) ef.get("type");
            Map<String, Object> fields = (Map<String, Object>) ef.get("fields");
            if (fields == null) {
                fields = Map.of();
            }
            result.add(new ExpectedFact(type, fields));
        }
        return result;
    }

    private String convertToJson(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert input to JSON", e);
        }
    }
}

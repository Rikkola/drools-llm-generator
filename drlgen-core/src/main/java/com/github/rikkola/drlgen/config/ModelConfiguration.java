package com.github.rikkola.drlgen.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Central configuration for AI models used in DRL generation.
 * Loads model definitions from models.yaml in the project root.
 */
public class ModelConfiguration {

    private static final String CONFIG_FILE = "models.yaml";
    private static final Pattern MODEL_NAME_PATTERN = Pattern.compile("modelName=([^,\\]]+)");
    private static final Map<String, ModelDefinition> MODEL_CACHE = new ConcurrentHashMap<>();
    private static volatile ModelsConfig cachedConfig;

    /**
     * Loads the models configuration from YAML file.
     * Searches in: current directory, parent directories, classpath.
     */
    public static ModelsConfig loadConfig() {
        if (cachedConfig != null) {
            return cachedConfig;
        }

        synchronized (ModelConfiguration.class) {
            if (cachedConfig != null) {
                return cachedConfig;
            }

            cachedConfig = loadConfigFromFile();

            // Populate model cache (using id as key, which defaults to name if not set)
            for (ModelDefinition model : cachedConfig.getModels()) {
                MODEL_CACHE.put(model.getId(), model);
            }

            return cachedConfig;
        }
    }

    private static ModelsConfig loadConfigFromFile() {
        // Try to find models.yaml in current directory or parent directories
        Path configPath = findConfigFile();

        if (configPath != null) {
            try (InputStream is = Files.newInputStream(configPath)) {
                return parseYaml(is);
            } catch (IOException e) {
                System.err.println("Failed to load " + configPath + ": " + e.getMessage());
            }
        }

        // Fallback to classpath
        try (InputStream is = ModelConfiguration.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (is != null) {
                return parseYaml(is);
            }
        } catch (IOException e) {
            System.err.println("Failed to load from classpath: " + e.getMessage());
        }

        // Return default config if no file found
        System.err.println("Warning: " + CONFIG_FILE + " not found, using defaults");
        return createDefaultConfig();
    }

    private static Path findConfigFile() {
        Path current = Paths.get("").toAbsolutePath();

        // Search up to 5 parent directories
        for (int i = 0; i < 5; i++) {
            Path configPath = current.resolve(CONFIG_FILE);
            if (Files.exists(configPath)) {
                return configPath;
            }
            Path parent = current.getParent();
            if (parent == null) {
                break;
            }
            current = parent;
        }

        return null;
    }

    private static ModelsConfig parseYaml(InputStream is) {
        LoaderOptions options = new LoaderOptions();
        Yaml yaml = new Yaml(new Constructor(ModelsConfig.class, options));
        return yaml.load(is);
    }

    private static ModelsConfig createDefaultConfig() {
        ModelsConfig config = new ModelsConfig();
        config.setDefaultModel("qwen3-coder-next");
        config.getModels().add(new ModelDefinition("qwen3-coder-next"));
        return config;
    }

    /**
     * Creates a ChatModel from system properties or environment variables.
     * Falls back to the default model from configuration.
     */
    public static ChatModel createFromEnvironment() {
        ModelsConfig config = loadConfig();

        String modelName = getPropertyOrEnv("test.ollama.model", "TEST_OLLAMA_MODEL", config.getDefaultModel());
        String baseUrl = getPropertyOrEnv("test.ollama.baseUrl", "TEST_OLLAMA_BASE_URL", config.getOllama().getBaseUrl());

        return createModel(modelName, baseUrl);
    }

    /**
     * Creates a ChatModel for the specified model name.
     */
    public static ChatModel createModel(String modelName) {
        ModelsConfig config = loadConfig();
        return createModel(modelName, config.getOllama().getBaseUrl());
    }

    /**
     * Creates a ChatModel for the given model id (or name for backward compatibility).
     * Uses the actual Ollama model name from the definition.
     */
    public static ChatModel createModel(String modelId, String baseUrl) {
        ModelsConfig config = loadConfig();
        ModelDefinition modelDef = MODEL_CACHE.getOrDefault(modelId, new ModelDefinition(modelId));

        // Use the actual Ollama model name (from 'name' field), not the id
        String actualModelName = modelDef.getName();

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(actualModelName)
                .timeout(Duration.ofMinutes(config.getOllama().getTimeoutMinutes()))
                .temperature(modelDef.getTemperature())
                .topP(modelDef.getTopP())
                .numPredict(modelDef.getNumPredict())
                .numCtx(modelDef.getNumCtx())
                .repeatPenalty(modelDef.getRepeatPenalty())
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * Gets the model definition by id.
     */
    public static Optional<ModelDefinition> getModelDefinition(String modelId) {
        loadConfig();
        return Optional.ofNullable(MODEL_CACHE.get(modelId));
    }

    /**
     * Returns all configured model ids (unique identifiers).
     */
    public static List<String> getAvailableModels() {
        return loadConfig().getModels().stream()
                .map(ModelDefinition::getId)
                .collect(Collectors.toList());
    }

    /**
     * Returns all available models as suppliers for comparison testing.
     * Keys are model ids (unique identifiers).
     */
    public static Map<String, Supplier<ChatModel>> getAllModels() {
        return loadConfig().getModels().stream()
                .collect(Collectors.toMap(
                        ModelDefinition::getId,
                        m -> () -> createModel(m.getId())
                ));
    }

    /**
     * Returns the default base URL for Ollama.
     */
    public static String getDefaultBaseUrl() {
        return loadConfig().getOllama().getBaseUrl();
    }

    /**
     * Gets a value from system property, environment variable, or default.
     */
    private static String getPropertyOrEnv(String propertyName, String envName, String defaultValue) {
        String value = System.getProperty(propertyName);
        if (value != null && !value.isBlank() && !value.startsWith("${")) {
            return value;
        }
        value = System.getenv(envName);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }

    /**
     * Extracts the model name from a ChatModel instance.
     * @param model The ChatModel to extract the name from
     * @return The model name or "unknown" if not found
     */
    public static String extractModelName(ChatModel model) {
        if (model == null) return "unknown";
        String modelString = model.toString();
        Matcher matcher = MODEL_NAME_PATTERN.matcher(modelString);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "unknown";
    }

    /**
     * Clears the cached configuration (useful for testing).
     */
    public static void clearCache() {
        synchronized (ModelConfiguration.class) {
            cachedConfig = null;
            MODEL_CACHE.clear();
        }
    }
}

package com.github.rikkola.drlgen.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Root configuration class for models.yaml.
 */
public class ModelsConfig {

    private String defaultModel = "qwen3-coder-next";
    private OllamaConfig ollama = new OllamaConfig();
    private List<ModelDefinition> models = new ArrayList<>();

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public OllamaConfig getOllama() {
        return ollama;
    }

    public void setOllama(OllamaConfig ollama) {
        this.ollama = ollama;
    }

    public List<ModelDefinition> getModels() {
        return models;
    }

    public void setModels(List<ModelDefinition> models) {
        this.models = models;
    }

    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private int timeoutMinutes = 5;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public int getTimeoutMinutes() {
            return timeoutMinutes;
        }

        public void setTimeoutMinutes(int timeoutMinutes) {
            this.timeoutMinutes = timeoutMinutes;
        }
    }
}

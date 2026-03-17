package com.github.rikkola.drlgen.config;

import com.github.rikkola.drlgen.agent.AgentType;

/**
 * Represents a model definition loaded from YAML configuration.
 *
 * The 'id' field is a unique identifier for this configuration (used for lookups).
 * The 'name' field is the actual Ollama model name (can be duplicated across configs).
 * If 'id' is not specified, it defaults to 'name' for backward compatibility.
 */
public class ModelDefinition {

    private String id;
    private String name;
    private AgentType agentType = AgentType.GUIDED;
    private double temperature = 0.1;
    private double topP = 0.9;
    private int numPredict = 2048;
    private int numCtx = 16384;
    private double repeatPenalty = 1.1;

    public ModelDefinition() {
    }

    public ModelDefinition(String name) {
        this.name = name;
        this.id = name;
    }

    /**
     * Gets the unique identifier for this model configuration.
     * Falls back to 'name' if 'id' is not set.
     */
    public String getId() {
        return id != null ? id : name;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the actual Ollama model name.
     */
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the agent type for DRL generation.
     * Defaults to GUIDED if not specified.
     */
    public AgentType getAgentType() {
        return agentType;
    }

    public void setAgentType(AgentType agentType) {
        this.agentType = agentType;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getTopP() {
        return topP;
    }

    public void setTopP(double topP) {
        this.topP = topP;
    }

    public int getNumPredict() {
        return numPredict;
    }

    public void setNumPredict(int numPredict) {
        this.numPredict = numPredict;
    }

    public int getNumCtx() {
        return numCtx;
    }

    public void setNumCtx(int numCtx) {
        this.numCtx = numCtx;
    }

    public double getRepeatPenalty() {
        return repeatPenalty;
    }

    public void setRepeatPenalty(double repeatPenalty) {
        this.repeatPenalty = repeatPenalty;
    }

    @Override
    public String toString() {
        return "ModelDefinition{id='" + getId() + "', name='" + name + "', agentType=" + agentType + ", temperature=" + temperature + "}";
    }
}

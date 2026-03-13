package com.github.rikkola.drlgen.execution;

import org.kie.api.runtime.KieContainer;

import java.util.Collections;
import java.util.List;

/**
 * Simplified facade for DRL execution with various input types.
 * This class delegates to specialized components for actual work.
 *
 * Can be used either through static convenience methods (using default instance)
 * or as an instance with injected dependencies for better testability.
 */
public class DRLPopulatorRunner {

    private static final DRLPopulatorRunner DEFAULT_INSTANCE = new DRLPopulatorRunner();

    private final DRLExecutor executor;
    private final FactBuilder factBuilder;
    private final DRLParser parser;

    /**
     * Creates a DRLPopulatorRunner with default dependencies.
     */
    public DRLPopulatorRunner() {
        this(new DRLExecutor(), new FactBuilder(), new DRLParser());
    }

    /**
     * Creates a DRLPopulatorRunner with custom dependencies for testing.
     */
    public DRLPopulatorRunner(DRLExecutor executor, FactBuilder factBuilder, DRLParser parser) {
        this.executor = executor;
        this.factBuilder = factBuilder;
        this.parser = parser;
    }

    // ========== Static convenience methods (delegate to DEFAULT_INSTANCE) ==========

    /**
     * Executes a DRL file that may contain declared types and data creation rules
     * @param drlContent The DRL content as a string
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRL(String drlContent) {
        return DEFAULT_INSTANCE.executeDRL(drlContent);
    }

    /**
     * Executes a DRL file that may contain declared types and data creation rules
     * @param drlContent The DRL content as a string
     * @param maxRuns Maximum number of rules to fire (0 for unlimited)
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRL(String drlContent, int maxRuns) {
        return DEFAULT_INSTANCE.executeDRL(drlContent, maxRuns);
    }

    /**
     * Executes a DRL file with external facts provided as JSON
     * @param drlContent The DRL content as a string
     * @param factsJson JSON string containing array of facts with type fields
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRLWithJsonFacts(String drlContent, String factsJson) {
        return DEFAULT_INSTANCE.executeDRLWithJsonFacts(drlContent, factsJson);
    }

    /**
     * Executes a DRL file with external facts provided as JSON
     * @param drlContent The DRL content as a string
     * @param factsJson JSON string containing array of facts with type fields
     * @param maxRuns Maximum number of rules to fire (0 for unlimited)
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRLWithJsonFacts(String drlContent, String factsJson, int maxRuns) {
        return DEFAULT_INSTANCE.executeDRLWithJsonFacts(drlContent, factsJson, maxRuns);
    }

    /**
     * Executes a DRL file with external facts
     * @param drlContent The DRL content as a string
     * @param facts External facts to insert into working memory
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRLWithFacts(String drlContent, List<Object> facts) {
        return DEFAULT_INSTANCE.executeDRLWithFacts(drlContent, facts);
    }

    /**
     * Executes a DRL file with external facts
     * @param drlContent The DRL content as a string
     * @param facts External facts to insert into working memory
     * @param maxRuns Maximum number of rules to fire (0 for unlimited)
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRLWithFacts(String drlContent, List<Object> facts, int maxRuns) {
        return DEFAULT_INSTANCE.executeDRLWithFacts(drlContent, facts, maxRuns);
    }

    /**
     * Executes a DRL file and populates it with JSON data
     * @param drlContent The DRL content as a string
     * @param jsonString JSON data to populate declared types
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRL(String drlContent, String jsonString) {
        return DEFAULT_INSTANCE.executeDRL(drlContent, jsonString);
    }

    /**
     * Executes a DRL file and populates it with JSON data
     * @param drlContent The DRL content as a string
     * @param jsonString JSON data to populate declared types
     * @param maxRuns Maximum number of rules to fire (0 for unlimited)
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRL(String drlContent, String jsonString, int maxRuns) {
        return DEFAULT_INSTANCE.executeDRL(drlContent, jsonString, maxRuns);
    }

    /**
     * Executes a DRL file and populates it with JSON data using specified package and type names
     * @param drlContent The DRL content as a string
     * @param jsonString JSON data to populate declared types
     * @param packageName Package name for the declared type
     * @param declaredTypeName Name of the declared type to populate
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRL(String drlContent, String jsonString, String packageName, String declaredTypeName) {
        return DEFAULT_INSTANCE.executeDRL(drlContent, jsonString, packageName, declaredTypeName);
    }

    /**
     * Executes a DRL file and populates it with JSON data using specified package and type names
     * @param drlContent The DRL content as a string
     * @param jsonString JSON data to populate declared types
     * @param packageName Package name for the declared type
     * @param declaredTypeName Name of the declared type to populate
     * @param maxRuns Maximum number of rules to fire (0 for unlimited)
     * @return DRLRunnerResult containing facts in working memory and fired rules count after rule execution
     */
    public static DRLRunnerResult runDRL(String drlContent, String jsonString, String packageName, String declaredTypeName, int maxRuns) {
        return DEFAULT_INSTANCE.executeDRL(drlContent, jsonString, packageName, declaredTypeName, maxRuns);
    }

    /**
     * Filter facts by type name (useful for declared types)
     * @param facts List of facts
     * @param typeName Name of the type to filter by
     * @return List of facts matching the type name
     */
    public static List<Object> filterFactsByType(List<Object> facts, String typeName) {
        return facts.stream()
                .filter(fact -> fact.getClass().getSimpleName().equals(typeName))
                .collect(java.util.stream.Collectors.toList());
    }

    // ========== Instance methods (for testability with injected dependencies) ==========

    /**
     * Executes a DRL file that may contain declared types and data creation rules
     */
    public DRLRunnerResult executeDRL(String drlContent) {
        return executor.execute(drlContent, Collections.emptyList(), 0);
    }

    /**
     * Executes a DRL file that may contain declared types and data creation rules
     */
    public DRLRunnerResult executeDRL(String drlContent, int maxRuns) {
        return executor.execute(drlContent, Collections.emptyList(), maxRuns);
    }

    /**
     * Executes a DRL file with external facts provided as JSON
     */
    public DRLRunnerResult executeDRLWithJsonFacts(String drlContent, String factsJson) {
        return executeDRLWithJsonFacts(drlContent, factsJson, 0);
    }

    /**
     * Executes a DRL file with external facts provided as JSON
     */
    public DRLRunnerResult executeDRLWithJsonFacts(String drlContent, String factsJson, int maxRuns) {
        try {
            KieContainer kieContainer = executor.buildKieContainer(drlContent);
            String packageName = parser.extractPackageName(drlContent);
            List<Object> facts = factBuilder.buildFromJsonArray(factsJson, kieContainer, packageName);
            return executor.executeWithContainer(kieContainer, facts, maxRuns);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute DRL with JSON facts: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a DRL file with external facts
     */
    public DRLRunnerResult executeDRLWithFacts(String drlContent, List<Object> facts) {
        return executor.execute(drlContent, facts, 0);
    }

    /**
     * Executes a DRL file with external facts
     */
    public DRLRunnerResult executeDRLWithFacts(String drlContent, List<Object> facts, int maxRuns) {
        return executor.execute(drlContent, facts, maxRuns);
    }

    /**
     * Executes a DRL file and populates it with JSON data
     */
    public DRLRunnerResult executeDRL(String drlContent, String jsonString) {
        return executeDRL(drlContent, jsonString, 0);
    }

    /**
     * Executes a DRL file and populates it with JSON data
     */
    public DRLRunnerResult executeDRL(String drlContent, String jsonString, int maxRuns) {
        try {
            KieContainer kieContainer = executor.buildKieContainer(drlContent);
            String packageName = parser.extractPackageName(drlContent);
            Object fact = factBuilder.buildFromJsonSingle(jsonString, kieContainer, packageName);
            List<Object> facts = fact != null ? List.of(fact) : Collections.emptyList();
            return executor.executeWithContainer(kieContainer, facts, maxRuns);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute DRL with JSON population: " + e.getMessage(), e);
        }
    }

    /**
     * Executes a DRL file and populates it with JSON data using specified package and type names
     */
    public DRLRunnerResult executeDRL(String drlContent, String jsonString, String packageName, String declaredTypeName) {
        return executeDRL(drlContent, jsonString, packageName, declaredTypeName, 0);
    }

    /**
     * Executes a DRL file and populates it with JSON data using specified package and type names
     */
    public DRLRunnerResult executeDRL(String drlContent, String jsonString, String packageName, String declaredTypeName, int maxRuns) {
        try {
            KieContainer kieContainer = executor.buildKieContainer(drlContent);
            Object fact = factBuilder.buildFromJsonWithExplicitType(jsonString, kieContainer, packageName, declaredTypeName);
            List<Object> facts = List.of(fact);
            return executor.executeWithContainer(kieContainer, facts, maxRuns);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute DRL with JSON population: " + e.getMessage(), e);
        }
    }
}

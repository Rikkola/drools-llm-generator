package com.github.rikkola.drlgen;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import com.github.rikkola.drlgen.cleanup.DRLCleanupStrategy;
import com.github.rikkola.drlgen.execution.DRLPopulatorRunner;
import com.github.rikkola.drlgen.execution.DRLRunnerResult;
import com.github.rikkola.drlgen.guide.GuideProvider;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.model.RuleDefinition;
import com.github.rikkola.drlgen.prompt.PromptProvider;
import com.github.rikkola.drlgen.validation.DRLValidator;
import com.github.rikkola.drlgen.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Main entry point for DRL (Drools Rule Language) generation using AI models.
 *
 * <p>DRLGenerator provides a fluent API for generating, validating, and executing
 * Drools rules from natural language requirements.</p>
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Create a generator with defaults
 * DRLGenerator generator = DRLGenerator.builder()
 *     .chatModel(myChatModel)
 *     .build();
 *
 * // Generate DRL from a requirement
 * GenerationResult result = generator.generate(ruleDefinition);
 *
 * if (result.isValidated()) {
 *     System.out.println(result.generatedDrl());
 * }
 * }</pre>
 *
 * <h2>Custom Configuration</h2>
 * <pre>{@code
 * DRLGenerator generator = DRLGenerator.builder()
 *     .chatModel(myChatModel)
 *     .promptProvider(PromptProvider.fromDirectory(Paths.get("prompts")))
 *     .guideProvider(GuideProvider.withDomainInstructions(
 *         GuideProvider.createDefault(),
 *         Paths.get("domain-instructions.md")))
 *     .cleanupStrategy(DRLCleanupStrategy.createDefault())
 *     .validator(DRLValidator.createDefault())
 *     .build();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>DRLGenerator is thread-safe and can be shared across multiple threads.
 * Each generation call is independent and does not share state.</p>
 */
public final class DRLGenerator {

    private static final Logger LOG = LoggerFactory.getLogger(DRLGenerator.class);

    private final ChatModel chatModel;
    private final PromptProvider promptProvider;
    private final GuideProvider guideProvider;
    private final DRLCleanupStrategy cleanupStrategy;
    private final DRLValidator validator;

    private DRLGenerator(Builder builder) {
        this.chatModel = Objects.requireNonNull(builder.chatModel, "chatModel is required");
        this.promptProvider = builder.promptProvider != null
                ? builder.promptProvider : PromptProvider.createDefault();
        this.guideProvider = builder.guideProvider != null
                ? builder.guideProvider : GuideProvider.createDefault();
        this.cleanupStrategy = builder.cleanupStrategy != null
                ? builder.cleanupStrategy : DRLCleanupStrategy.createDefault();
        this.validator = builder.validator != null
                ? builder.validator : DRLValidator.createDefault();

        LOG.debug("DRLGenerator initialized with model: {}", extractModelName());
    }

    /**
     * Creates a new builder for constructing DRLGenerator instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ========== Generation Methods ==========

    /**
     * Generates DRL from a rule definition.
     *
     * <p>The generated DRL is validated but not executed.</p>
     *
     * @param definition the rule definition containing requirement and fact types
     * @return generation result with the generated DRL and validation status
     */
    public GenerationResult generate(RuleDefinition definition) {
        return generate(definition.requirement(), definition.getFactTypesDescription(), "");
    }

    /**
     * Generates DRL from a rule definition with example input for the AI.
     *
     * @param definition the rule definition
     * @param exampleInput example input/output to guide the AI
     * @return generation result
     */
    public GenerationResult generate(RuleDefinition definition, String exampleInput) {
        return generate(definition.requirement(), definition.getFactTypesDescription(), exampleInput);
    }

    /**
     * Generates DRL from raw requirement and fact types description.
     *
     * @param requirement natural language requirement
     * @param factTypesDescription description of fact types to declare
     * @return generation result
     */
    public GenerationResult generate(String requirement, String factTypesDescription) {
        return generate(requirement, factTypesDescription, "");
    }

    /**
     * Core generation method with all parameters.
     *
     * @param requirement natural language requirement
     * @param factTypesDescription description of fact types to declare
     * @param exampleInput example input/output to guide the AI
     * @return generation result
     */
    public GenerationResult generate(String requirement, String factTypesDescription, String exampleInput) {
        String modelName = extractModelName();
        Instant start = Instant.now();

        LOG.info("Starting DRL generation using model '{}'", modelName);
        LOG.debug("Requirement:\n{}", requirement);
        LOG.debug("Fact Types:\n{}", factTypesDescription);

        // Generate DRL using prompts
        Instant genStart = Instant.now();
        String generatedDrl;
        try {
            generatedDrl = generateDrlFromPrompt(requirement, factTypesDescription, exampleInput);
            generatedDrl = cleanupStrategy.cleanup(generatedDrl);
            LOG.debug("Generated DRL:\n{}", generatedDrl);
        } catch (Exception e) {
            LOG.error("DRL generation failed: {}", e.getMessage());
            return GenerationResult.failed(modelName, "Generation failed: " + e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
        Duration genTime = Duration.between(genStart, Instant.now());
        LOG.info("DRL generation completed in {}ms", genTime.toMillis());

        // Validate DRL
        ValidationResult validationResult = validator.validate(generatedDrl);
        LOG.info("Validation result: {} (valid: {})", validationResult.getSummary(), validationResult.isValid());

        if (!validationResult.isValid()) {
            LOG.warn("DRL validation failed: {}", validationResult.getSummary());
            return GenerationResult.partial(modelName, generatedDrl, false,
                    validationResult.getSummary(), genTime, Duration.between(start, Instant.now()));
        }

        return GenerationResult.validated(modelName, generatedDrl, validationResult.getSummary(),
                genTime, Duration.between(start, Instant.now()));
    }

    // ========== Generation with Execution ==========

    /**
     * Generates DRL and executes it with the provided facts.
     *
     * @param definition the rule definition
     * @param factsJson JSON array of facts to execute against
     * @return generation result including execution results
     */
    public GenerationResult generateAndExecute(RuleDefinition definition, String factsJson) {
        GenerationResult genResult = generate(definition, factsJson);
        if (!genResult.validationPassed()) {
            return genResult;
        }
        return executeWithResult(genResult, factsJson);
    }

    /**
     * Generates DRL and executes it with the provided facts.
     *
     * @param requirement natural language requirement
     * @param factTypesDescription description of fact types
     * @param factsJson JSON array of facts to execute against
     * @return generation result including execution results
     */
    public GenerationResult generateAndExecute(String requirement, String factTypesDescription, String factsJson) {
        GenerationResult genResult = generate(requirement, factTypesDescription, factsJson);
        if (!genResult.validationPassed()) {
            return genResult;
        }
        return executeWithResult(genResult, factsJson);
    }

    // ========== Execution Methods ==========

    /**
     * Executes DRL code with the provided JSON facts.
     *
     * @param drlCode the DRL code to execute
     * @param factsJson JSON array of facts
     * @return execution result with fired rules and resulting facts
     */
    public DRLRunnerResult execute(String drlCode, String factsJson) {
        return execute(drlCode, factsJson, 100);
    }

    /**
     * Executes DRL code with the provided JSON facts and max rules limit.
     *
     * @param drlCode the DRL code to execute
     * @param factsJson JSON array of facts
     * @param maxRules maximum number of rules to fire
     * @return execution result
     */
    public DRLRunnerResult execute(String drlCode, String factsJson, int maxRules) {
        return DRLPopulatorRunner.runDRLWithJsonFacts(drlCode, factsJson, maxRules);
    }

    // ========== Validation Methods ==========

    /**
     * Validates DRL code.
     *
     * @param drlCode the DRL code to validate
     * @return validation result
     */
    public ValidationResult validate(String drlCode) {
        return validator.validate(drlCode);
    }

    // ========== Private Methods ==========

    private String generateDrlFromPrompt(String requirement, String factTypesDescription, String exampleInput) {
        String systemMessageText = promptProvider.getSystemMessage();
        String userTemplate = promptProvider.getUserMessageTemplate();
        String guide = guideProvider.getGuide();

        // Replace placeholders in user template
        String userMessageText = userTemplate
                .replace("{{drlGuide}}", guide)
                .replace("{{requirement}}", requirement)
                .replace("{{factTypes}}", factTypesDescription)
                .replace("{{exampleInput}}", exampleInput != null ? exampleInput : "");

        // Use the chat model with proper message types
        SystemMessage systemMessage = SystemMessage.from(systemMessageText);
        UserMessage userMessage = UserMessage.from(userMessageText);

        ChatResponse response = chatModel.chat(systemMessage, userMessage);
        AiMessage aiMessage = response.aiMessage();

        return aiMessage.text();
    }

    private GenerationResult executeWithResult(GenerationResult genResult, String factsJson) {
        String modelName = genResult.modelName();
        Instant start = Instant.now();

        try {
            DRLRunnerResult execResult = DRLPopulatorRunner.runDRLWithJsonFacts(
                    genResult.generatedDrl(), factsJson, 100);

            LOG.info("Execution completed: {} rules fired, {} facts",
                    execResult.firedRules(), execResult.objects().size());

            return new GenerationResult(
                    modelName,
                    genResult.generatedDrl(),
                    true,
                    genResult.validationMessage(),
                    true,
                    "Execution completed: " + execResult.firedRules() + " rules fired",
                    execResult.firedRules(),
                    execResult.objects(),
                    genResult.generationTime(),
                    genResult.totalTime().plus(Duration.between(start, Instant.now()))
            );
        } catch (Exception e) {
            LOG.error("Execution failed: {}", e.getMessage());
            return GenerationResult.partial(modelName, genResult.generatedDrl(), true,
                    "Execution failed: " + e.getMessage(),
                    genResult.generationTime(),
                    genResult.totalTime().plus(Duration.between(start, Instant.now())));
        }
    }

    private String extractModelName() {
        try {
            // Try to extract model name from ChatModel's toString
            String modelString = chatModel.toString();
            if (modelString.contains("modelName=")) {
                int start = modelString.indexOf("modelName=") + 10;
                int end = modelString.indexOf(",", start);
                if (end == -1) end = modelString.indexOf("]", start);
                if (end == -1) end = modelString.indexOf(")", start);
                if (end > start) {
                    return modelString.substring(start, end);
                }
            }
        } catch (Exception e) {
            LOG.debug("Could not extract model name: {}", e.getMessage());
        }
        return "unknown";
    }

    // ========== Builder ==========

    /**
     * Builder for constructing DRLGenerator instances.
     */
    public static final class Builder {
        private ChatModel chatModel;
        private PromptProvider promptProvider;
        private GuideProvider guideProvider;
        private DRLCleanupStrategy cleanupStrategy;
        private DRLValidator validator;

        private Builder() {}

        /**
         * Sets the chat model to use for generation.
         *
         * <p>This is required. The chat model can be any LangChain4j-compatible
         * model (Ollama, OpenAI, Anthropic, etc.).</p>
         *
         * @param chatModel the chat model
         * @return this builder
         */
        public Builder chatModel(ChatModel chatModel) {
            this.chatModel = chatModel;
            return this;
        }

        /**
         * Sets the prompt provider.
         *
         * <p>If not set, defaults to loading prompts from classpath.</p>
         *
         * @param promptProvider the prompt provider
         * @return this builder
         */
        public Builder promptProvider(PromptProvider promptProvider) {
            this.promptProvider = promptProvider;
            return this;
        }

        /**
         * Sets the guide provider.
         *
         * <p>If not set, defaults to loading the guide from classpath.</p>
         *
         * @param guideProvider the guide provider
         * @return this builder
         */
        public Builder guideProvider(GuideProvider guideProvider) {
            this.guideProvider = guideProvider;
            return this;
        }

        /**
         * Sets the guide provider with domain instructions from a file.
         *
         * <p>This is a convenience method that wraps the default guide provider
         * with domain-specific instructions.</p>
         *
         * @param domainInstructionsPath path to domain instructions file
         * @return this builder
         */
        public Builder domainInstructions(Path domainInstructionsPath) {
            this.guideProvider = GuideProvider.withDomainInstructions(
                    GuideProvider.createDefault(), domainInstructionsPath);
            return this;
        }

        /**
         * Sets the DRL cleanup strategy.
         *
         * <p>If not set, defaults to the standard cleanup strategy.</p>
         *
         * @param cleanupStrategy the cleanup strategy
         * @return this builder
         */
        public Builder cleanupStrategy(DRLCleanupStrategy cleanupStrategy) {
            this.cleanupStrategy = cleanupStrategy;
            return this;
        }

        /**
         * Sets the DRL validator.
         *
         * <p>If not set, defaults to the Drools-based validator.</p>
         *
         * @param validator the validator
         * @return this builder
         */
        public Builder validator(DRLValidator validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Disables validation.
         *
         * <p>Useful when validation is handled externally or not needed.</p>
         *
         * @return this builder
         */
        public Builder noValidation() {
            this.validator = DRLValidator.noOp();
            return this;
        }

        /**
         * Disables cleanup.
         *
         * <p>Useful when the model produces clean output.</p>
         *
         * @return this builder
         */
        public Builder noCleanup() {
            this.cleanupStrategy = DRLCleanupStrategy.noOp();
            return this;
        }

        /**
         * Builds the DRLGenerator instance.
         *
         * @return the configured DRLGenerator
         * @throws NullPointerException if chatModel is not set
         */
        public DRLGenerator build() {
            return new DRLGenerator(this);
        }
    }
}

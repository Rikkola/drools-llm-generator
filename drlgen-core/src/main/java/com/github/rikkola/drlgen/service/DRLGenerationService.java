package com.github.rikkola.drlgen.service;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.agent.ConversationalDRLAgent;
import com.github.rikkola.drlgen.agent.DRLGenerationAgent;
import com.github.rikkola.drlgen.cleanup.DRLCleanupStrategy;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.conversation.ErrorFeedbackFormatter;
import com.github.rikkola.drlgen.conversation.GuideSectionFinder;
import com.github.rikkola.drlgen.execution.DRLPopulatorRunner;
import com.github.rikkola.drlgen.execution.DRLRunnerResult;
import com.github.rikkola.drlgen.guide.GuideProvider;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.model.RuleDefinition;
import com.github.rikkola.drlgen.validation.DRLValidator;
import com.github.rikkola.drlgen.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Core service for DRL generation and validation.
 *
 * <p>This service provides generation, validation, and optional execution capabilities.
 * For test scenario execution with assertions, use TestDRLGenerationService in the tests module.</p>
 *
 * <p>For new code, consider using {@link com.github.rikkola.drlgen.DRLGenerator} instead,
 * which provides a cleaner builder-based API.</p>
 *
 * @see com.github.rikkola.drlgen.DRLGenerator
 */
public class DRLGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(DRLGenerationService.class);

    private final DRLValidator validator;
    private final DRLCleanupStrategy cleanupStrategy;
    private final GuideProvider guideProvider;
    private final Function<ChatModel, DRLGenerationAgent> agentFactory;

    // ========== Constructors ==========

    /**
     * Default constructor using default components.
     */
    public DRLGenerationService() {
        this(DRLValidator.createDefault(),
             DRLCleanupStrategy.createDefault(),
             GuideProvider.createDefault(),
             DRLGenerationAgent::create);
    }

    /**
     * Constructor with all configurable components.
     *
     * @param validator the DRL validator
     * @param cleanupStrategy the cleanup strategy
     * @param guideProvider the guide provider
     * @param agentFactory factory for creating generation agents
     */
    public DRLGenerationService(DRLValidator validator,
                                 DRLCleanupStrategy cleanupStrategy,
                                 GuideProvider guideProvider,
                                 Function<ChatModel, DRLGenerationAgent> agentFactory) {
        this.validator = validator;
        this.cleanupStrategy = cleanupStrategy;
        this.guideProvider = guideProvider;
        this.agentFactory = agentFactory;
        logger.debug("DRLGenerationService initialized with custom components");
    }

    /**
     * Creates a builder for constructing DRLGenerationService instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // ========== Generation Methods ==========

    /**
     * Generates DRL from a rule definition using the specified model.
     * The generated DRL is validated but not executed.
     *
     * @param model The AI model to use for generation
     * @param definition The rule definition containing requirement and fact types
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generate(ChatModel model, RuleDefinition definition) {
        return generate(model, definition.requirement(), definition.getFactTypesDescription(), "");
    }

    /**
     * Generates DRL from a rule definition with an example scenario for the AI.
     *
     * @param model The AI model to use for generation
     * @param definition The rule definition containing requirement and fact types
     * @param exampleInput An example input/output scenario to guide the AI
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generate(ChatModel model, RuleDefinition definition, String exampleInput) {
        return generate(model, definition.requirement(), definition.getFactTypesDescription(), exampleInput);
    }

    /**
     * Generates DRL from raw requirement and fact types description.
     * Convenience method for simple use cases.
     *
     * @param model The AI model to use for generation
     * @param requirement The natural language requirement
     * @param factTypesDescription Description of fact types to declare
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generate(ChatModel model, String requirement, String factTypesDescription) {
        return generate(model, requirement, factTypesDescription, "");
    }

    /**
     * Core generation method with all parameters (without domain instructions).
     */
    public GenerationResult generate(ChatModel model, String requirement,
                                     String factTypesDescription, String exampleInput) {
        return generate(model, requirement, factTypesDescription, exampleInput, null);
    }

    /**
     * Generates DRL from a rule definition with domain-specific instructions.
     *
     * @param model                   The AI model to use for generation
     * @param definition              The rule definition containing requirement and fact types
     * @param domainInstructionsPath  Path to domain-specific instructions file (may be null)
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generate(ChatModel model, RuleDefinition definition,
                                     Path domainInstructionsPath) {
        return generate(model, definition.requirement(), definition.getFactTypesDescription(),
                "", domainInstructionsPath);
    }

    /**
     * Generates DRL from a rule definition with an example scenario and domain-specific instructions.
     *
     * @param model                   The AI model to use for generation
     * @param definition              The rule definition containing requirement and fact types
     * @param exampleInput            An example input/output scenario to guide the AI
     * @param domainInstructionsPath  Path to domain-specific instructions file (may be null)
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generate(ChatModel model, RuleDefinition definition,
                                     String exampleInput, Path domainInstructionsPath) {
        return generate(model, definition.requirement(), definition.getFactTypesDescription(),
                exampleInput, domainInstructionsPath);
    }

    /**
     * Core generation method with all parameters including domain instructions.
     *
     * @param model                   The AI model to use for generation
     * @param requirement             The natural language requirement
     * @param factTypesDescription    Description of fact types to declare
     * @param exampleInput            Example input/output scenario
     * @param domainInstructionsPath  Path to domain-specific instructions file (may be null)
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generate(ChatModel model, String requirement,
                                     String factTypesDescription, String exampleInput,
                                     Path domainInstructionsPath) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting DRL generation using model '{}'", modelName);
        logger.debug("Requirement:\n{}", requirement);
        logger.debug("Fact Types:\n{}", factTypesDescription);

        if (domainInstructionsPath != null) {
            logger.info("Using domain instructions from: {}", domainInstructionsPath);
        }

        // Generate DRL
        DRLGenerationAgent agent = agentFactory.apply(model);
        Instant genStart = Instant.now();
        String generatedDrl;
        try {
            // Get guide (with domain instructions if path provided)
            String guide = getGuideWithDomainInstructions(domainInstructionsPath);

            generatedDrl = agent.generateDRL(guide, requirement, factTypesDescription, exampleInput);
            generatedDrl = cleanupStrategy.cleanup(generatedDrl);
            logger.debug("Generated DRL:\n{}", generatedDrl);
        } catch (Exception e) {
            logger.error("DRL generation failed: {}", e.getMessage());
            return GenerationResult.failed(modelName, "Generation failed: " + e.getMessage(),
                    Duration.between(start, Instant.now()));
        }
        Duration genTime = Duration.between(genStart, Instant.now());
        logger.info("DRL generation completed in {}ms", genTime.toMillis());

        // Validate DRL
        ValidationResult validationResult = validator.validate(generatedDrl);
        logger.info("Validation result: {} (valid: {})", validationResult.getSummary(), validationResult.isValid());

        if (!validationResult.isValid()) {
            logger.warn("DRL validation failed: {}", validationResult.getSummary());
            return GenerationResult.partial(modelName, generatedDrl, false,
                    validationResult.getSummary(), genTime, Duration.between(start, Instant.now()));
        }

        // Return validation-only success
        return GenerationResult.validated(modelName, generatedDrl, validationResult.getSummary(),
                genTime, Duration.between(start, Instant.now()));
    }

    // ========== Conversational Generation with Retry ==========

    /**
     * Generates DRL using a conversational agent with retry capability.
     *
     * <p>This method uses ChatMemory to maintain conversation history, allowing the model
     * to learn from validation errors and fix its DRL across multiple turns.</p>
     *
     * @param model       The AI model to use for generation
     * @param definition  The rule definition containing requirement and fact types
     * @param exampleInput An example input/output scenario to guide the AI
     * @param maxTurns    Maximum turns (1 = single generation, 2+ includes retries)
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generateWithRetry(ChatModel model, RuleDefinition definition,
                                               String exampleInput, int maxTurns) {
        return generateWithRetry(model, definition, exampleInput, maxTurns, null);
    }

    /**
     * Generates DRL using a conversational agent with retry capability and domain instructions.
     *
     * <p>This method uses ChatMemory to maintain conversation history, allowing the model
     * to learn from validation errors and fix its DRL across multiple turns.</p>
     *
     * @param model                   The AI model to use for generation
     * @param definition              The rule definition containing requirement and fact types
     * @param exampleInput            An example input/output scenario to guide the AI
     * @param maxTurns                Maximum turns (1 = single generation, 2+ includes retries)
     * @param domainInstructionsPath  Path to domain-specific instructions file (may be null)
     * @return GenerationResult with the generated DRL and validation status
     */
    public GenerationResult generateWithRetry(ChatModel model, RuleDefinition definition,
                                               String exampleInput, int maxTurns,
                                               Path domainInstructionsPath) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting conversational DRL generation using model '{}' with max {} turns",
                modelName, maxTurns);

        // Create conversational agent with ChatMemory
        ConversationalDRLAgent agent = ConversationalDRLAgent.create(model);
        String guide = getGuideWithDomainInstructions(domainInstructionsPath);

        // Turn 1: Initial generation
        String drl;
        try {
            drl = agent.generateDRL(guide, definition.requirement(),
                    definition.getFactTypesDescription(), exampleInput);
            drl = cleanupStrategy.cleanup(drl);
            logger.debug("Turn 1 - Generated DRL:\n{}", drl);
        } catch (Exception e) {
            logger.error("Initial DRL generation failed: {}", e.getMessage());
            return GenerationResult.failed(modelName, "Generation failed: " + e.getMessage(),
                    Duration.between(start, Instant.now()));
        }

        // Validate and retry loop
        for (int turn = 1; turn <= maxTurns; turn++) {
            ValidationResult validationResult = validator.validate(drl);
            Duration currentTime = Duration.between(start, Instant.now());

            if (validationResult.isValid()) {
                String message = String.format("Valid after %d turn(s): %s",
                        turn, validationResult.getSummary());
                logger.info("DRL valid after {} turn(s)", turn);
                return GenerationResult.validated(modelName, drl, message, currentTime, currentTime);
            }

            // If we've reached max turns, return partial result
            if (turn == maxTurns) {
                String message = String.format("Failed after %d turn(s): %s",
                        turn, validationResult.getSummary());
                logger.warn("DRL validation failed after {} turn(s): {}",
                        turn, validationResult.getSummary());
                return GenerationResult.partial(modelName, drl, false, message, currentTime, currentTime);
            }

            // Prepare error feedback for next turn
            String errors = ErrorFeedbackFormatter.formatValidationErrors(validationResult);
            String guideSections = GuideSectionFinder.findRelevantSections(validationResult);

            logger.info("Turn {} - Validation failed, requesting fix. Errors: {}",
                    turn, validationResult.getSummary());
            logger.debug("Error feedback:\n{}", errors);
            logger.debug("Relevant guide sections:\n{}", guideSections);

            // Turn N+1: Fix based on errors
            try {
                drl = agent.fixDRL(errors, guideSections, drl);
                drl = cleanupStrategy.cleanup(drl);
                logger.debug("Turn {} - Fixed DRL:\n{}", turn + 1, drl);
            } catch (Exception e) {
                logger.error("DRL fix attempt failed: {}", e.getMessage());
                return GenerationResult.failed(modelName,
                        String.format("Fix attempt failed at turn %d: %s", turn + 1, e.getMessage()),
                        Duration.between(start, Instant.now()));
            }
        }

        // Should not reach here, but return failure if somehow we do
        return GenerationResult.failed(modelName, "Unexpected end of retry loop",
                Duration.between(start, Instant.now()));
    }

    // ========== Execution Methods ==========

    /**
     * Generates DRL and executes it with the provided facts to verify it works.
     *
     * @param model The AI model to use for generation
     * @param definition The rule definition
     * @param factsJson JSON array of facts to execute against
     * @return GenerationResult including execution results
     */
    public GenerationResult generateAndExecute(ChatModel model, RuleDefinition definition, String factsJson) {
        return generateAndExecute(model, definition, factsJson, null);
    }

    /**
     * Generates DRL and executes it with the provided facts and domain-specific instructions.
     *
     * @param model                   The AI model to use for generation
     * @param definition              The rule definition
     * @param factsJson               JSON array of facts to execute against
     * @param domainInstructionsPath  Path to domain-specific instructions file (may be null)
     * @return GenerationResult including execution results
     */
    public GenerationResult generateAndExecute(ChatModel model, RuleDefinition definition,
                                                String factsJson, Path domainInstructionsPath) {
        // First generate and validate
        GenerationResult genResult = generate(model, definition, factsJson, domainInstructionsPath);
        if (!genResult.validationPassed()) {
            return genResult;
        }

        // Then execute
        return executeWithResult(genResult, factsJson);
    }

    /**
     * Executes DRL code with the provided JSON facts.
     *
     * @param drlCode The DRL code to execute
     * @param factsJson JSON array of facts
     * @return DRLRunnerResult with execution details
     */
    public DRLRunnerResult execute(String drlCode, String factsJson) {
        return DRLPopulatorRunner.runDRLWithJsonFacts(drlCode, factsJson, 100);
    }

    /**
     * Validates existing DRL code.
     *
     * @param drlCode The DRL code to validate
     * @return Validation result message
     */
    public String validate(String drlCode) {
        ValidationResult result = validator.validate(drlCode);
        return result.getSummary();
    }

    /**
     * Validates existing DRL code and returns structured result.
     *
     * @param drlCode The DRL code to validate
     * @return Structured validation result
     */
    public ValidationResult validateStructured(String drlCode) {
        return validator.validate(drlCode);
    }

    // ========== Private Methods ==========

    private String getGuideWithDomainInstructions(Path domainInstructionsPath) {
        if (domainInstructionsPath == null) {
            return guideProvider.getGuide();
        }

        // Create a composite guide provider with domain instructions
        GuideProvider compositeProvider = GuideProvider.withDomainInstructions(
                guideProvider, domainInstructionsPath);
        return compositeProvider.getGuide();
    }

    private GenerationResult executeWithResult(GenerationResult genResult, String factsJson) {
        String modelName = genResult.modelName();
        Instant start = Instant.now();

        try {
            DRLRunnerResult execResult = DRLPopulatorRunner.runDRLWithJsonFacts(
                    genResult.generatedDrl(), factsJson, 100);

            logger.info("Execution completed: {} rules fired, {} facts",
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
            logger.error("Execution failed: {}", e.getMessage());
            return GenerationResult.partial(modelName, genResult.generatedDrl(), true,
                    "Execution failed: " + e.getMessage(),
                    genResult.generationTime(),
                    genResult.totalTime().plus(Duration.between(start, Instant.now())));
        }
    }

    // ========== Builder ==========

    /**
     * Builder for constructing DRLGenerationService instances.
     */
    public static final class Builder {
        private DRLValidator validator;
        private DRLCleanupStrategy cleanupStrategy;
        private GuideProvider guideProvider;
        private Function<ChatModel, DRLGenerationAgent> agentFactory;

        private Builder() {}

        /**
         * Sets the DRL validator.
         */
        public Builder validator(DRLValidator validator) {
            this.validator = validator;
            return this;
        }

        /**
         * Sets the cleanup strategy.
         */
        public Builder cleanupStrategy(DRLCleanupStrategy cleanupStrategy) {
            this.cleanupStrategy = cleanupStrategy;
            return this;
        }

        /**
         * Sets the guide provider.
         */
        public Builder guideProvider(GuideProvider guideProvider) {
            this.guideProvider = guideProvider;
            return this;
        }

        /**
         * Sets the agent factory.
         */
        public Builder agentFactory(Function<ChatModel, DRLGenerationAgent> agentFactory) {
            this.agentFactory = agentFactory;
            return this;
        }

        /**
         * Builds the DRLGenerationService instance.
         */
        public DRLGenerationService build() {
            return new DRLGenerationService(
                    validator != null ? validator : DRLValidator.createDefault(),
                    cleanupStrategy != null ? cleanupStrategy : DRLCleanupStrategy.createDefault(),
                    guideProvider != null ? guideProvider : GuideProvider.createDefault(),
                    agentFactory != null ? agentFactory : DRLGenerationAgent::create
            );
        }
    }
}

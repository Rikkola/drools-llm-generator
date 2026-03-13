package com.github.rikkola.drlgen.generation.service;

import dev.langchain4j.model.chat.ChatModel;
import com.github.rikkola.drlgen.agent.DRLGenerationAgent;
import com.github.rikkola.drlgen.agent.OldStyleDRLGenerationAgent;
import com.github.rikkola.drlgen.cleanup.DRLCleanupStrategy;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.execution.DRLPopulatorRunner;
import com.github.rikkola.drlgen.execution.DRLRunnerResult;
import com.github.rikkola.drlgen.guide.GuideProvider;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.validation.DRLValidator;
import com.github.rikkola.drlgen.validation.ValidationResult;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.util.FactVerificationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Test-oriented DRL generation service that orchestrates generation, validation,
 * and test execution workflow.
 *
 * <p>This service is designed for testing scenarios where generated DRL is validated
 * against test cases with expected outcomes.</p>
 *
 * <p><strong>Note:</strong> For production use without test execution, prefer using
 * {@link com.github.rikkola.drlgen.DRLGenerator} or
 * {@link com.github.rikkola.drlgen.service.DRLGenerationService} from drlgen-core.</p>
 *
 * <p>This test-specific service uses {@link OldStyleDRLGenerationAgent} by default
 * for backward compatibility with existing test scenarios.</p>
 *
 * @see com.github.rikkola.drlgen.DRLGenerator
 * @see com.github.rikkola.drlgen.service.DRLGenerationService
 */
public class DRLGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(DRLGenerationService.class);

    private final DRLValidator validator;
    private final DRLCleanupStrategy cleanupStrategy;
    private final GuideProvider guideProvider;
    private final Function<ChatModel, DRLGenerationAgent> agentFactory;

    /**
     * Default constructor using default components.
     */
    public DRLGenerationService() {
        this(DRLValidator.createDefault(),
             DRLCleanupStrategy.createDefault(),
             GuideProvider.createDefault(),
             OldStyleDRLGenerationAgent::create);  // Using old-style prompt for testing
    }

    /**
     * Constructor with custom validation service for backward compatibility.
     *
     * @deprecated Use the builder or constructor with DRLValidator instead
     */
    @Deprecated
    public DRLGenerationService(com.github.rikkola.drlgen.service.DRLValidationService validationService) {
        this(wrapValidationService(validationService),
             DRLCleanupStrategy.createDefault(),
             GuideProvider.createDefault(),
             DRLGenerationAgent::create);
    }

    /**
     * Constructor with custom validation service and agent factory for backward compatibility.
     *
     * @deprecated Use the builder or constructor with interfaces instead
     */
    @Deprecated
    public DRLGenerationService(com.github.rikkola.drlgen.service.DRLValidationService validationService,
                                 Function<ChatModel, DRLGenerationAgent> agentFactory) {
        this(wrapValidationService(validationService),
             DRLCleanupStrategy.createDefault(),
             GuideProvider.createDefault(),
             agentFactory);
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

    /**
     * Generates DRL from a requirement and validates it (no test execution).
     * Suitable for web UI where only generation and validation is needed.
     */
    public GenerationResult generateAndValidate(ChatModel model, String requirement, String factTypesDescription) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting DRL generation for requirement using model '{}'", modelName);

        // Generate DRL
        DRLGenerationAgent agent = agentFactory.apply(model);
        Instant genStart = Instant.now();
        String generatedDrl;
        try {
            String guide = guideProvider.getGuide();
            generatedDrl = agent.generateDRL(guide, requirement, factTypesDescription, "");
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

        // Return success (validation only, no execution)
        return new GenerationResult(
                modelName,
                generatedDrl,
                true,
                validationResult.getSummary(),
                true,
                "Validation passed (no test execution)",
                0,
                java.util.List.of(),
                genTime,
                Duration.between(start, Instant.now())
        );
    }

    /**
     * Generates DRL from a scenario using the specified model and validates execution.
     */
    public GenerationResult generateAndTest(ChatModel model, TestScenario scenario) {
        return generateAndTest(model, scenario, null);
    }

    /**
     * Generates DRL from a scenario with optional domain instructions.
     *
     * @param model            The AI model to use for generation
     * @param scenario         The test scenario
     * @param instructionsPath Optional path to domain-specific instructions file
     * @return GenerationResult with test execution results
     */
    public GenerationResult generateAndTest(ChatModel model, TestScenario scenario, Path instructionsPath) {
        String modelName = ModelConfiguration.extractModelName(model);
        Instant start = Instant.now();

        logger.info("Starting DRL generation for scenario '{}' using model '{}'", scenario.name(), modelName);
        if (instructionsPath != null) {
            logger.info("Using domain instructions from: {}", instructionsPath);
        }
        logger.debug("--- Scenario Details ---");
        logger.debug("Description: {}", scenario.description());
        logger.debug("Requirement:\n{}", scenario.requirement());
        logger.debug("Fact Types:\n{}", scenario.getFactTypesDescription());
        logger.debug("Test Cases: {}", scenario.testCases().size());
        for (TestScenario.TestCase tc : scenario.testCases()) {
            logger.debug("  - {}: input={}", tc.name(), tc.inputJson());
        }

        // Generate DRL
        DRLGenerationAgent agent = agentFactory.apply(model);
        Instant genStart = Instant.now();
        String generatedDrl;
        try {
            // Get guide (with domain instructions if path provided)
            String guide = getGuideWithDomainInstructions(instructionsPath);

            generatedDrl = agent.generateDRL(
                    guide,
                    scenario.requirement(),
                    scenario.getFactTypesDescription(),
                    scenario.getTestScenarioDescription()
            );
            // Clean up DRL if it contains markdown code blocks
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

        // Execute DRL with all test cases
        int totalRulesFired = 0;
        List<Object> allResultingFacts = new ArrayList<>();
        int testCasesPassed = 0;

        for (TestScenario.TestCase testCase : scenario.testCases()) {
            logger.info("Executing test case '{}' with input: {}", testCase.name(), testCase.inputJson());
            try {
                DRLRunnerResult result = DRLPopulatorRunner.runDRLWithJsonFacts(
                        generatedDrl, testCase.inputJson(), 100);

                logger.info("Test case '{}': {} rules fired, {} facts in working memory",
                        testCase.name(), result.firedRules(), result.objects().size());

                // Verify rules fired count if specified
                String rulesFiredError = testCase.validateRulesFired(result.firedRules());
                if (rulesFiredError != null) {
                    logger.error("Test case '{}' failed: {}", testCase.name(), rulesFiredError);
                    return GenerationResult.partial(modelName, generatedDrl, true,
                            "Test case '" + testCase.name() + "' failed: " + rulesFiredError,
                            genTime, Duration.between(start, Instant.now()));
                }

                // Verify expected values (support both legacy and new formats)
                if (testCase.hasTypedExpectations()) {
                    // New: Type-aware verification
                    String factError = FactVerificationUtils.verifyExpectedFacts(result.objects(), testCase.expectedFacts());
                    if (factError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), factError);
                        return GenerationResult.partial(modelName, generatedDrl, true,
                                "Test case '" + testCase.name() + "' failed: " + factError,
                                genTime, Duration.between(start, Instant.now()));
                    }
                } else if (testCase.expectedFieldValues() != null && !testCase.expectedFieldValues().isEmpty()) {
                    // Legacy: Field-based verification (backward compatibility)
                    String fieldError = FactVerificationUtils.verifyExpectedFields(result.objects(), testCase.expectedFieldValues());
                    if (fieldError != null) {
                        logger.error("Test case '{}' failed: {}", testCase.name(), fieldError);
                        return GenerationResult.partial(modelName, generatedDrl, true,
                                "Test case '" + testCase.name() + "' failed: " + fieldError,
                                genTime, Duration.between(start, Instant.now()));
                    }
                }

                totalRulesFired += result.firedRules();
                allResultingFacts.addAll(result.objects());
                testCasesPassed++;
            } catch (Throwable e) {
                logger.error("Test case '{}' failed: {}", testCase.name(), e.getMessage());
                return GenerationResult.partial(modelName, generatedDrl, true,
                        "Test case '" + testCase.name() + "' failed: " + e.getMessage(),
                        genTime, Duration.between(start, Instant.now()));
            }
        }

        if (testCasesPassed == 0) {
            return GenerationResult.failed(modelName, "No test cases to execute", Duration.between(start, Instant.now()));
        }

        logger.info("All {} test cases passed. Total rules fired: {}", testCasesPassed, totalRulesFired);

        return new GenerationResult(
                modelName,
                generatedDrl,
                true,
                validationResult.getSummary(),
                true,
                "All " + testCasesPassed + " test cases passed",
                totalRulesFired,
                allResultingFacts,
                genTime,
                Duration.between(start, Instant.now())
        );
    }

    private String getGuideWithDomainInstructions(Path domainInstructionsPath) {
        if (domainInstructionsPath == null) {
            return guideProvider.getGuide();
        }

        // Create a composite guide provider with domain instructions
        GuideProvider compositeProvider = GuideProvider.withDomainInstructions(
                guideProvider, domainInstructionsPath);
        return compositeProvider.getGuide();
    }

    /**
     * Wraps a legacy DRLValidationService as a DRLValidator for backward compatibility.
     */
    private static DRLValidator wrapValidationService(com.github.rikkola.drlgen.service.DRLValidationService validationService) {
        return drlCode -> {
            try {
                String result = validationService.validateDRLStructure(drlCode);
                if (result.contains("ERROR:")) {
                    return ValidationResult.error(result);
                }
                return ValidationResult.success();
            } catch (Exception e) {
                return ValidationResult.error(e.getMessage());
            }
        };
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

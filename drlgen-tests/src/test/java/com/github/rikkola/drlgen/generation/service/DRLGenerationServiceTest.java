package com.github.rikkola.drlgen.generation.service;

import com.github.rikkola.drlgen.agent.DRLGenerationAgent;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.service.DRLValidationService;
import com.github.rikkola.drlgen.generation.model.TestScenario;
import com.github.rikkola.drlgen.generation.model.TestScenario.ExpectedFact;
import com.github.rikkola.drlgen.generation.model.TestScenario.FactTypeDefinition;
import com.github.rikkola.drlgen.generation.model.TestScenario.TestCase;
import dev.langchain4j.model.chat.ChatModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DRLGenerationService using mocked agents.
 * These tests run fast without requiring a real LLM.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DRLGenerationService Unit Tests")
class DRLGenerationServiceTest {

    // Valid DRL that compiles and executes correctly
    private static final String VALID_ADULT_DRL = """
            package org.drools.generated;

            declare Person
                name : String
                age : int
                adult : boolean
            end

            rule "Check Adult"
            when
                $p : Person(age >= 18, adult == false)
            then
                modify($p) { setAdult(true) }
            end
            """;

    // DRL with syntax error
    private static final String INVALID_DRL = """
            package org.drools.generated;

            declare Person
                name : String
            end

            rule "Bad Rule"
            when
                $p : Person(undefinedField == "test")
            then
            end
            """;

    // DRL that compiles but doesn't match input facts
    private static final String NON_MATCHING_DRL = """
            package org.drools.generated;

            declare Person
                name : String
                age : int
                adult : boolean
            end

            rule "Check Adult"
            when
                $p : Person(age >= 100)
            then
                modify($p) { setAdult(true) }
            end
            """;

    @Mock
    private ChatModel mockModel;

    @Mock
    private DRLGenerationAgent mockAgent;

    private DRLGenerationService service;

    @BeforeEach
    void setUp() {
        // Create service with mock agent factory
        service = new DRLGenerationService(
                new DRLValidationService(),
                model -> mockAgent  // Factory returns our mock regardless of model
        );
    }

    private TestScenario createAdultValidationScenario() {
        return new TestScenario(
                "Adult Validation",
                "Validates if a person is an adult based on age",
                "If a person's age is 18 or older, mark them as adult",
                List.of(new FactTypeDefinition("Person", Map.of(
                        "name", "String",
                        "age", "int",
                        "adult", "boolean"
                ))),
                List.of(new TestCase(
                        "Adult person",
                        "[{\"_type\":\"Person\", \"name\":\"John\", \"age\":25, \"adult\":false}]",
                        Map.of(),
                        List.of(new ExpectedFact("Person", Map.of("adult", true)))
                ))
        );
    }

    @Nested
    @DisplayName("Successful Generation Tests")
    class SuccessfulGenerationTests {

        @Test
        @DisplayName("Should generate and execute valid DRL successfully")
        void shouldGenerateValidDRL() {
            // Given
            when(mockAgent.generateDRL(any(), any(), any(), any())).thenReturn(VALID_ADULT_DRL);
            TestScenario scenario = createAdultValidationScenario();

            // When
            GenerationResult result = service.generateAndTest(mockModel, scenario);

            // Then
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.validationPassed()).isTrue();
            assertThat(result.executionPassed()).isTrue();
            assertThat(result.rulesFired()).isGreaterThan(0);
            assertThat(result.generatedDrl()).contains("rule");
        }

        @Test
        @DisplayName("Should record generation timing")
        void shouldRecordTiming() {
            // Given
            when(mockAgent.generateDRL(any(), any(), any(), any())).thenReturn(VALID_ADULT_DRL);
            TestScenario scenario = createAdultValidationScenario();

            // When
            GenerationResult result = service.generateAndTest(mockModel, scenario);

            // Then
            assertThat(result.generationTime()).isNotNull();
            assertThat(result.totalTime()).isNotNull();
            assertThat(result.totalTime().toMillis())
                    .isGreaterThanOrEqualTo(result.generationTime().toMillis());
        }
    }

    @Nested
    @DisplayName("Validation Failure Tests")
    class ValidationFailureTests {

        @Test
        @DisplayName("Should detect invalid DRL syntax")
        void shouldDetectInvalidDRL() {
            // Given
            when(mockAgent.generateDRL(any(), any(), any(), any())).thenReturn(INVALID_DRL);
            TestScenario scenario = createAdultValidationScenario();

            // When
            GenerationResult result = service.generateAndTest(mockModel, scenario);

            // Then
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.validationPassed()).isFalse();
            assertThat(result.validationMessage()).containsIgnoringCase("error");
        }

        @Test
        @DisplayName("Should handle empty DRL")
        void shouldHandleEmptyDRL() {
            // Given
            when(mockAgent.generateDRL(any(), any(), any(), any())).thenReturn("");
            TestScenario scenario = createAdultValidationScenario();

            // When
            GenerationResult result = service.generateAndTest(mockModel, scenario);

            // Then
            assertThat(result.isSuccessful()).isFalse();
        }
    }

    @Nested
    @DisplayName("Execution Failure Tests")
    class ExecutionFailureTests {

        @Test
        @DisplayName("Should detect when no rules fire")
        void shouldDetectNoRulesFired() {
            // Given - DRL that compiles but conditions don't match
            when(mockAgent.generateDRL(any(), any(), any(), any())).thenReturn(NON_MATCHING_DRL);
            // Create scenario that explicitly expects 1 rule to fire
            TestScenario scenario = new TestScenario(
                    "Adult Validation",
                    "Validates if a person is an adult based on age",
                    "If a person's age is 18 or older, mark them as adult",
                    List.of(new FactTypeDefinition("Person", Map.of(
                            "name", "String",
                            "age", "int",
                            "adult", "boolean"
                    ))),
                    List.of(new TestCase(
                            "Adult person",
                            "[{\"_type\":\"Person\", \"name\":\"John\", \"age\":25, \"adult\":false}]",
                            Map.of(),
                            List.of(new ExpectedFact("Person", Map.of("adult", true))),
                            1  // expectedRulesFired = 1
                    ))
            );

            // When
            GenerationResult result = service.generateAndTest(mockModel, scenario);

            // Then
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.validationPassed()).isTrue();  // DRL is valid
            assertThat(result.executionPassed()).isFalse();  // But execution failed
            assertThat(result.executionMessage()).contains("rules to fire, but none fired");
        }
    }

    @Nested
    @DisplayName("Agent Exception Tests")
    class AgentExceptionTests {

        @Test
        @DisplayName("Should handle agent exception gracefully")
        void shouldHandleAgentException() {
            // Given
            when(mockAgent.generateDRL(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("LLM connection failed"));
            TestScenario scenario = createAdultValidationScenario();

            // When
            GenerationResult result = service.generateAndTest(mockModel, scenario);

            // Then
            assertThat(result.isSuccessful()).isFalse();
            assertThat(result.validationMessage()).contains("Generation failed");
        }
    }

    @Nested
    @DisplayName("Multiple Test Cases")
    class MultipleTestCasesTests {

        @Test
        @DisplayName("Should execute multiple test cases")
        void shouldExecuteMultipleTestCases() {
            // Given
            when(mockAgent.generateDRL(any(), any(), any(), any())).thenReturn(VALID_ADULT_DRL);

            TestScenario scenario = new TestScenario(
                    "Multi-case Test",
                    "Test with multiple cases",
                    "If a person's age is 18 or older, mark them as adult",
                    List.of(new FactTypeDefinition("Person", Map.of(
                            "name", "String",
                            "age", "int",
                            "adult", "boolean"
                    ))),
                    List.of(
                            new TestCase("Adult 1",
                                    "[{\"_type\":\"Person\", \"name\":\"John\", \"age\":25, \"adult\":false}]",
                                    Map.of(), List.of(new ExpectedFact("Person", Map.of("adult", true)))),
                            new TestCase("Adult 2",
                                    "[{\"_type\":\"Person\", \"name\":\"Jane\", \"age\":30, \"adult\":false}]",
                                    Map.of(), List.of(new ExpectedFact("Person", Map.of("adult", true))))
                    )
            );

            // When
            GenerationResult result = service.generateAndTest(mockModel, scenario);

            // Then
            assertThat(result.isSuccessful()).isTrue();
            assertThat(result.rulesFired()).isEqualTo(2);  // One rule per test case
            assertThat(result.executionMessage()).contains("2 test cases passed");
        }
    }
}

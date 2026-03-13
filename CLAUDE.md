# drools-drl-generation-tests

This module provides a comprehensive testing framework for AI-generated Drools rules in both DRL and YAML formats.

## Purpose

- Test that AI models (primarily local Ollama models) can generate valid DRL and YAML rules
- Compare different models on identical scenarios
- Compare DRL vs YAML generation approaches
- Validate generated rules through compilation and execution
- Provide a regression test suite for rule generation capabilities

## Module Structure

```
drools-drl-generation-tests/
├── src/main/java/org/drools/generation/
│   ├── agent/
│   │   ├── DRLGenerationAgent.java       # LangChain4j agent for DRL
│   │   └── YAMLRuleGenerationAgent.java  # LangChain4j agent for YAML
│   ├── config/
│   │   └── ModelConfiguration.java       # Model provider abstraction
│   ├── model/
│   │   ├── TestScenario.java            # Test scenario data model
│   │   └── GenerationResult.java        # Result of DRL generation
│   └── service/
│       ├── DRLGenerationService.java    # Orchestrates DRL generation
│       ├── YAMLRuleGenerationService.java # Orchestrates YAML generation
│       └── YAMLToDRLConverter.java      # Converts YAML to DRL for execution
└── src/test/java/org/drools/generation/
    ├── base/
    │   ├── AbstractDRLGenerationTest.java   # Base class for DRL tests
    │   └── AbstractYAMLGenerationTest.java  # Base class for YAML tests
    ├── scenarios/
    │   ├── PersonAgeValidationTest.java     # Person adult/minor rules (DRL)
    │   ├── OrderDiscountRulesTest.java      # Order discount calculations (DRL)
    │   └── YAMLRuleGenerationTest.java      # YAML rule generation tests
    ├── comparison/
    │   ├── ModelComparisonTest.java         # Compare models side-by-side
    │   └── FormatComparisonTest.java        # Compare DRL vs YAML formats
    └── provider/
        └── TestScenarioProvider.java        # JUnit 5 argument providers
```

## Running Tests

### Default (uses environment/system property model, excludes slow tests)
```bash
mvn test -pl drools-drl-generation-tests
```

### With specific model via system property
```bash
mvn test -pl drools-drl-generation-tests -Dtest.ollama.model=qwen3-coder-next
mvn test -pl drools-drl-generation-tests -Dtest.ollama.model=granite4
mvn test -pl drools-drl-generation-tests -Dtest.ollama.model=llama4
```

### Model comparison tests (slow)
```bash
mvn test -pl drools-drl-generation-tests -Pmodel-comparison
```

### Run all tests including slow ones
```bash
mvn test -pl drools-drl-generation-tests -Pall-tests
```

## Environment Variables

- `TEST_OLLAMA_MODEL` - Ollama model name (default: qwen3-coder-next)
- `TEST_OLLAMA_BASE_URL` - Ollama server URL (default: http://localhost:11434)

## Adding New Test Scenarios

1. Add scenario definition to `TestScenarioProvider`:
```java
public static TestScenario createMyScenario() {
    return new TestScenario(
        "Scenario Name",
        "Description",
        "Natural language requirement...",
        List.of(new FactTypeDefinition("TypeName", Map.of(
            "field1", "String",
            "field2", "int"
        ))),
        List.of(new TestCase("Test Name",
            "{\"_type\":\"TypeName\", \"field1\":\"value\", \"field2\":42}",
            1,  // expected rules fired
            Map.of("field1", "expectedValue")))
    );
}
```

2. Create test class extending `AbstractDRLGenerationTest`:
```java
@DisplayName("My Scenario Tests")
class MyScenarioTest extends AbstractDRLGenerationTest {
    @Test
    void testMyScenario() {
        TestScenario scenario = TestScenarioProvider.createMyScenario();
        GenerationResult result = generateAndAssertSuccess(scenario);
    }
}
```

## Supported Models

### Current Models (Feb 2026)

| Model Type | Model Name | Parameters | Context | Temperature | DRL Success |
|------------|------------|------------|---------|-------------|-------------|
| QWEN3_CODER_NEXT | qwen3-coder-next | 79.7B | 262K | 0.1 | 100% |
| GRANITE4_SMALL_H | granite4:small-h | 32.2B | 1M | 0.1 | 100% |
| QWEN25_CODER_14B | qwen2.5-coder:14b-instruct-q4_K_M | 14.8B | 32K | 0.1 | 100% |
| QWEN3 | qwen3 | 8.2B | 40K | 0.0 | 88% |
| LLAMA4 | llama4 | 108.6B | 10.5M | 0.1 | 41% (use ENGLISH) |
| GRANITE4 | granite4 | 3.4B | 131K | 0.1 | 18% (use ENGLISH) |

### Legacy Models (Deprecated)

| Model Type | Model Name | Default Temperature |
|------------|------------|---------------------|
| GRANITE_CODE_8B | granite-code:8b | 0.1 |
| GRANITE_CODE_20B | granite-code:20b | 0.05 |
| GRANITE3_MOE | granite3-moe:3b | 0.1 |
| GRANITE_33_8B | granite3.3:8b | 0.1 |
| QWEN_CODER_14B | qwen2.5-coder:14b-instruct-q4_K_M | 0.1 |
| QWEN3_14B | qwen3:14b | 0.0 |
| LLAMA3_8B | llama3.2:8b | 0.1 |
| CODELLAMA_13B | codellama:13b | 0.1 |

## Test Categories

- **Default tests**: Quick scenarios, single model (no tag)
- `@Tag("slow")`: Tests with retries or multiple iterations
- `@Tag("comparison")`: Multi-model comparison tests

## YAML Rule Format

The module supports generating rules in YAML format, which is then converted to DRL for execution:

```yaml
types:
  - name: Person
    fields:
      name: String
      age: int
      adult: boolean

rules:
  - name: "Check Adult"
    condition:
      given: Person
      as: $person
      having:
        - age >= 18
    action:
      modify:
        target: $person
        set:
          adult: true
```

### Running YAML Tests

```bash
# Run YAML generation tests
mvn test -pl drools-drl-generation-tests -Dtest=YAMLRuleGenerationTest

# Run DRL vs YAML comparison (requires comparison tag)
mvn test -pl drools-drl-generation-tests -Pall-tests -Dtest=FormatComparisonTest
```

## Dependencies

- `drools-builder-core` - DRL execution and validation
- `langchain4j` / `langchain4j-ollama` - AI model integration
- `snakeyaml` - YAML parsing
- `junit-jupiter` - Testing framework
- `assertj-core` - Fluent assertions

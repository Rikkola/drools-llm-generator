# drlgen - AI-Powered DRL Generation Framework

A comprehensive framework for AI-generated Drools rules using local LLMs (Ollama).

## Project Structure

```
drlgen/
├── drlgen-core/                    # Core library (reusable)
│   └── src/main/java/com/github/rikkola/drlgen/
│       ├── agent/
│       │   └── DRLGenerationAgent.java       # LangChain4j AI agent
│       ├── cleanup/
│       │   ├── DRLCleanupStrategy.java       # Cleanup interface
│       │   └── DefaultCleanupStrategy.java   # Post-processing fixes
│       ├── config/
│       │   └── ModelConfiguration.java       # Model provider abstraction
│       ├── service/
│       │   └── DRLGenerationService.java     # Main generation service
│       ├── validation/
│       │   ├── DRLValidator.java             # Validation interface
│       │   └── DroolsValidator.java          # Drools compilation check
│       └── DRLGenerator.java                 # Fluent API entry point
│
├── drlgen-tests/                   # Test framework
│   ├── src/main/java/com/github/rikkola/drlgen/
│   │   ├── generation/
│   │   │   ├── loader/YAMLScenarioLoader.java    # Load YAML scenarios
│   │   │   ├── model/TestScenario.java           # Scenario data model
│   │   │   ├── provider/TestScenarioProvider.java
│   │   │   └── runner/ComparisonRunner.java      # Batch test runner
│   │   └── service/
│   │       └── DRLExecutionService.java          # Execute DRL rules
│   ├── src/main/resources/scenarios/             # YAML test scenarios
│   └── src/test/java/com/github/rikkola/drlgen/
│       ├── generation/
│       │   ├── base/AbstractDRLGenerationTest.java
│       │   ├── comparison/ModelComparisonTest.java
│       │   └── scenarios/DRLGenerationTest.java
│       └── validation/
│           └── DRLVerifierTest.java
│
├── drlgen-ui/                      # Quarkus web UI
├── examples/                       # Example projects
└── models.yaml                     # Model configurations
```

## Running Tests

### Unit tests (no Ollama required)
```bash
mvn test -pl drlgen-tests
```

### Integration tests (requires Ollama)
```bash
mvn test -pl drlgen-tests -Pintegration
mvn test -pl drlgen-tests -Pintegration -Dtest.ollama.model=qwen3-coder-next
mvn test -pl drlgen-tests -Pintegration -Dtest.ollama.model=granite4:small-h
```

### Model comparison tests
```bash
mvn test -pl drlgen-tests -Pmodel-comparison
```

### Run all tests
```bash
mvn test -pl drlgen-tests -Pall-tests
```

## Running the Comparison Runner

```bash
# Build and run as JAR
mvn package -pl drlgen-tests -DskipTests
java -jar drlgen-tests/target/drlgen-tests-1.0.0-SNAPSHOT.jar --help

# Run specific models
java -jar drlgen-tests/target/drlgen-tests-1.0.0-SNAPSHOT.jar --models qwen3-coder-next,granite4:small-h
```

## Environment Variables

- `TEST_OLLAMA_MODEL` - Ollama model name (default: qwen3-coder-next)
- `TEST_OLLAMA_BASE_URL` - Ollama server URL (default: http://localhost:11434)

## Adding New Test Scenarios

Create a YAML file in `drlgen-tests/src/main/resources/scenarios/`:

```yaml
name: My Scenario
description: Brief description

requirement: |
  Natural language description of the rule logic...

factTypes:
  - name: MyFact
    fields:
      field1: String
      field2: int

testCases:
  - name: Test Case 1
    input:
      - _type: MyFact
        field1: "value"
        field2: 42
    expectedRulesFired: 1
    expectedFields:
      field1: "expected"
```

The scenario will be automatically loaded by `YAMLScenarioLoader`.

## Supported Models

| Model | Parameters | DRL Success | Notes |
|-------|------------|-------------|-------|
| qwen3-coder-next | 79.7B | 100% | Recommended |
| granite4:small-h | 32.2B | 100% | Good alternative |
| qwen2.5-coder:14b-instruct-q4_K_M | 14.8B | 100% | Smaller option |
| qwen3 | 8.2B | 88% | Fast, good for simple rules |
| granite4 | 3.4B | 18% | Use Plain English approach |

## Test Profiles

| Profile | Description |
|---------|-------------|
| (default) | Unit tests only, no LLM required |
| `integration` | LLM integration tests |
| `model-comparison` | Multi-model comparison |
| `all-tests` | All tests including slow |

## Dependencies

- **Drools** - Rule engine (compilation and execution)
- **LangChain4j** - AI model integration
- **Quarkus** - Web UI framework (drlgen-ui only)
- **JUnit 5 / AssertJ** - Testing

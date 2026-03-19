# DRL Generation Tests

A comprehensive testing framework for AI-generated Drools rules. This project validates that local AI models (primarily Ollama) can generate valid DRL (Drools Rule Language) from natural language requirements.

## Overview

This framework:
- Tests AI models' ability to generate valid DRL rules from business requirements
- Compares different models (Qwen, Granite, DeepSeek, etc.) on identical scenarios
- Supports multiple generation approaches: Direct DRL, YAML-to-DRL, and Plain English
- Validates generated rules through compilation and execution
- Provides a web UI for interactive rule generation

## Project Structure

```
drools-drl-generation-tests/
├── drlgen-core/           # Reusable DRL generation library
├── drlgen-tests/          # Test framework with scenarios and comparison tools
├── drlgen-ui/             # Quarkus web UI for interactive generation
├── models.yaml            # Model configurations (temperatures, context sizes, etc.)
└── pom.xml                # Parent Maven POM
```

### Modules

| Module | Description |
|--------|-------------|
| `drlgen-core` | Reusable library: DRL generation agent, validation, execution |
| `drlgen-tests` | Test scenarios, comparison runners, integration tests |
| `drlgen-ui` | Quarkus REST API and web UI for interactive DRL generation |

## Prerequisites

- Java 17+
- Maven 3.8+
- [Ollama](https://ollama.ai/) running locally with desired models
- Drools (uses snapshot version `999-SNAPSHOT`)

## Quick Start

### 1. Run unit tests (no Ollama required)

```bash
mvn test -pl drlgen-tests
```

### 2. Run integration tests (requires Ollama)

```bash
# Start Ollama with a model
ollama run qwen3-coder-next

# Run integration tests
mvn test -pl drlgen-tests -Pintegration

# Run with a specific model
mvn test -pl drlgen-tests -Pintegration -Dtest.ollama.model=granite4:small-h
```

### 3. Start the Web UI

```bash
cd drlgen-ui
mvn quarkus:dev
```

Access the UI at http://localhost:8080

## Configuration

### models.yaml

Configure AI models in the project root `models.yaml`:

```yaml
defaultModel: qwen3-coder-next

ollama:
  baseUrl: http://localhost:11434
  timeoutMinutes: 5

models:
  - name: qwen3-coder-next
    temperature: 0.1
    topP: 0.9
    numPredict: 2048
    numCtx: 16384
    repeatPenalty: 1.1

  # Multiple configs for same model with different parameters
  - id: granite4-temp0
    name: granite4:small-h
    temperature: 0.0

  # Specify agent type per model
  - id: qwen2.5-coder-simple
    name: qwen2.5-coder:14b-instruct-q4_K_M
    agentType: SIMPLE
```

### Agent Types

Models can use different agent types, configured via `agentType` in models.yaml:

| Agent Type | Description | Parameters | Best For |
|------------|-------------|------------|----------|
| `GUIDED` (default) | Concise system prompt with external DRL reference guide | drlGuide, requirement, factTypes, exampleInput | Most models |
| `SIMPLE` | Self-contained detailed system prompt, no external guide | requirement, factTypes, testScenario | Models that perform better with all instructions inline |

**When to use SIMPLE:**
- Model struggles with long context (guide + requirement)
- Model performs better with self-contained prompts
- Testing showed qwen2.5-coder achieves 93%+ with SIMPLE vs 69% with GUIDED

**Example configuration:**
```yaml
models:
  - id: qwen2.5-coder-simple
    name: qwen2.5-coder:14b-instruct-q4_K_M
    agentType: SIMPLE    # Use self-contained prompt

  - name: qwen3-coder-next
    # agentType defaults to GUIDED
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `TEST_OLLAMA_MODEL` | Ollama model name/id | `qwen3-coder-next` |
| `TEST_OLLAMA_BASE_URL` | Ollama server URL | `http://localhost:11434` |

### System Properties

```bash
-Dtest.ollama.model=granite4:small-h
-Dtest.ollama.baseUrl=http://localhost:11434
```

## Test Scenarios

Test scenarios are defined in YAML files under `drlgen-tests/src/main/resources/scenarios/`:

```yaml
name: Order Discount - Basic
description: Apply 10% discount for orders over $100

requirement: |
  Create a rule that:
  1. Checks if Order total is greater than 100.0
  2. If yes, sets the 'discountPercent' field to 10
  3. Sets the 'discountApplied' field to true

factTypes:
  - name: Order
    fields:
      orderId: String
      total: double
      discountPercent: int
      discountApplied: boolean

testCases:
  - name: Qualifying Order
    input:
      - _type: Order
        orderId: ORD001
        total: 150.0
        discountPercent: 0
        discountApplied: false
    expectedRulesFired: 1
    expectedFields:
      discountApplied: true
      discountPercent: 10
```

### Available Scenarios

| Category | Scenarios |
|----------|-----------|
| **Validation** | adult-validation, age-classification, age-validation-simple, email-validation, password-strength |
| **Discounts** | basic-discount, bulk-order-discount, order-discount-customer-type, tiered-discount |
| **Financial** | credit-card-approval, insurance-premium, insurance-risk-assessment, loan-approval-complex, loan-eligibility, tax-bracket, tax-calculation-chain |
| **Orders** | cart-validation, inventory-alert, order-fraud-detection, shipping-cost, shipping-method-priority |
| **Misc** | grade-calculation, loyalty-points, missing-document-alert, priority-assignment, senior-citizen, simple-calculation, status-transition, subscription-renewal |

## Generation Approaches

### 1. Direct DRL Generation

AI generates DRL syntax directly from requirements.

```java
DRLGenerationService service = new DRLGenerationService();
ChatModel model = ModelConfiguration.createModel("qwen3-coder-next");
GenerationResult result = service.generateAndValidate(model, requirement, factTypes);
```

### 2. YAML-to-DRL Generation

AI generates rules in YAML format, then converts to DRL.

```java
YAMLRuleGenerationService service = new YAMLRuleGenerationService();
YAMLGenerationResult result = service.generateAndValidate(model, requirement, factTypes);
```

### 3. Plain English Generation

AI describes rules in structured English, then converts to DRL.

```java
PlainEnglishGenerationService service = new PlainEnglishGenerationService();
PlainEnglishGenerationResult result = service.generateAndValidate(model, requirement, factTypes);
```

## Supported Models

| Model | ID | Parameters | Best For |
|-------|-----|------------|----------|
| Qwen 3 Coder Next | `qwen3-coder-next` | 79.7B | DRL generation (100% success) |
| Granite 4 Small-H | `granite4:small-h` | 32.2B | DRL generation (100% success) |
| Qwen 2.5 Coder 14B | `qwen2.5-coder:14b-instruct-q4_K_M` | 14.8B | DRL generation (100% success) |
| Qwen 3 | `qwen3` | 8.2B | DRL generation (88% success) |
| DeepSeek Coder V2 | `deepseek-coder-v2:16b` | 16B | Code generation |
| Phi 4 | `phi4` | 14B | General purpose |
| CodeLlama 13B | `codellama:13b` | 13B | Code generation |
| Granite 4 | `granite4` | 3.4B | Plain English approach |

## Web UI API

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/generate/models` | List available models |
| POST | `/api/generate/drl` | Generate DRL from requirements |

### Example Request

```bash
curl -X POST http://localhost:8080/api/generate/drl \
  -H "Content-Type: application/json" \
  -d '{
    "modelName": "qwen3-coder-next",
    "requirement": "If person age >= 18, set adult to true",
    "factTypes": [
      {
        "name": "Person",
        "fields": {
          "name": "String",
          "age": "int",
          "adult": "boolean"
        }
      }
    ]
  }'
```

## Running Tests

### Unit Tests (Fast, No LLM Required)

```bash
# Run unit tests only (~13 seconds)
mvn test -pl drlgen-tests
```

### Integration Tests (Requires Ollama)

```bash
# Run LLM integration tests
mvn test -pl drlgen-tests -Pintegration

# Run specific integration test class
mvn test -pl drlgen-tests -Pintegration -Dtest=DRLGenerationTest

# With specific model
mvn test -pl drlgen-tests -Pintegration -Dtest.ollama.model=granite4:small-h
```

### Model Comparison

```bash
# Compare all configured models
mvn test -pl drlgen-tests -Pmodel-comparison -Dtest=ModelComparisonTest

# Compare DRL vs YAML formats
mvn test -pl drlgen-tests -Pall-tests -Dtest=FormatComparisonTest
```

### Test Profiles

| Profile | Description | Speed |
|---------|-------------|-------|
| (default) | Unit tests only (no LLM required) | ~13 seconds |
| `integration` | LLM integration tests (requires Ollama) | Minutes |
| `model-comparison` | Multi-model comparison tests | Very slow |
| `all-tests` | All tests including slow/integration | Slow |

```bash
# Fast unit tests (default)
mvn test -pl drlgen-tests

# Integration tests (requires Ollama running)
mvn test -pl drlgen-tests -Pintegration

# All tests
mvn test -pl drlgen-tests -Pall-tests
```

## Running the Comparison Runner

The `ComparisonRunner` is a standalone tool that runs all test scenarios against multiple models and generates a CSV report. This is the main entry point for batch testing.

### Running as Standalone JAR (Recommended for Distribution)

Build and run as a self-contained executable JAR with all dependencies included:

```bash
# Build the executable JAR
mvn package -pl drlgen-tests -DskipTests

# Show help
java -jar drlgen-tests/target/drlgen-tests-0.1.0.jar --help

# Run all scenarios with all models
java -jar drlgen-tests/target/drlgen-tests-0.1.0.jar

# Run specific models
java -jar drlgen-tests/target/drlgen-tests-0.1.0.jar --models qwen3-coder-next,granite4:small-h

# Filter scenarios and output to custom file
java -jar drlgen-tests/target/drlgen-tests-0.1.0.jar --scenarios discount --output discount-results.csv

# Use custom scenarios directory with domain instructions
java -jar drlgen-tests/target/drlgen-tests-0.1.0.jar \
    --scenarios-dir ./my-scenarios \
    --instructions ./domain-guide.md \
    --output-dir ./my-results
```

**Prerequisites for JAR usage:**
- Java 17+ installed
- Ollama running locally (`ollama serve`)
- Required models pulled (`ollama pull qwen3-coder-next`)

### Using the Shell Script

```bash
cd drlgen-tests

# Run all models on all scenarios (DRL + YAML formats)
./run-comparison.sh

# Test specific models
./run-comparison.sh --models qwen3-coder-next,granite4:small-h

# Filter scenarios by name
./run-comparison.sh --scenarios adult,discount

# Output to custom CSV file
./run-comparison.sh --output my-results.csv

# Test only specific formats
./run-comparison.sh --drl-only
./run-comparison.sh --yaml-only
./run-comparison.sh --english-only

# Test multiple formats
./run-comparison.sh --formats DRL,YAML,ENGLISH
```

### Using Maven Directly

```bash
# Run with default settings (all models, all scenarios)
mvn -pl drlgen-tests exec:java \
    -Dexec.mainClass="com.github.rikkola.drlgen.generation.runner.ComparisonRunner"

# With arguments
mvn -pl drlgen-tests exec:java \
    -Dexec.mainClass="com.github.rikkola.drlgen.generation.runner.ComparisonRunner" \
    -Dexec.args="--models qwen3-coder-next --formats DRL,YAML --output results.csv"
```

### Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `--models <list>` | Comma-separated model names/ids | All from models.yaml |
| `--scenarios <list>` | Filter scenarios by name substring | All scenarios |
| `--output <file>` | CSV output filename | `comparison-results.csv` |
| `--formats <list>` | Formats to test: DRL, YAML, ENGLISH | DRL, YAML |
| `--max-turns <n>` | Retry count (2+ enables retry with guard instruction on loop/multi-fire failures) | 1 |
| `--drl-only` | Only test DRL generation | - |
| `--yaml-only` | Only test YAML generation | - |
| `--english-only` | Only test Plain English pipeline | - |

### Output

The runner produces:
1. **Console output** - Real-time progress with pass/fail status
2. **CSV report** - Detailed results with model, scenario, format, success status, rules fired, and timing

Example console output:
```
Starting Drools Rule Generation Comparison
Models: 3
Scenarios: 29
Formats: DRL, YAML

=== Testing model: qwen3-coder-next ===
[1/174] qwen3-coder-next - Adult Validation (DRL)... PASS (1 rules)
[2/174] qwen3-coder-next - Adult Validation (YAML)... PASS (1 rules)
...
```

## Adding New Scenarios

1. Create a YAML file in `drlgen-tests/src/main/resources/scenarios/`:

```yaml
name: My New Scenario
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

2. The scenario will be automatically picked up by tests using `YAMLScenarioLoader`.

## Dependencies

- **Drools** - Rule engine (compilation and execution)
- **LangChain4j** - AI model integration
- **Quarkus** - Web UI framework
- **SnakeYAML** - YAML parsing
- **JUnit 5** - Testing framework
- **AssertJ** - Fluent assertions

## License

This project is for testing and evaluation purposes.

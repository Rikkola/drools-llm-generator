# Standalone JAR Example

This example demonstrates how to use the `drlgen-tests` JAR file as a standalone tool to run DRL generation tests against AI models - without needing Maven or the source code.

## What This Example Shows

- Running the comparison tool with a pre-built JAR
- Creating custom scenario YAML files
- Specifying which AI models to test
- Generating results and reports

## Prerequisites

1. **Java 17+** installed and in PATH
2. **Ollama** running locally (`ollama serve`)
3. At least one model pulled:
   ```bash
   ollama pull qwen3-coder-next
   ```

## Project Structure

```
example-standalone-jar/
├── README.md              # This file
├── run-example.sh         # Linux/Mac runner script
├── run-example.bat        # Windows runner script
├── scenarios/             # Custom test scenarios
│   ├── temperature-alert.yaml
│   └── product-pricing.yaml
└── results/               # Generated after running (output)
```

## Quick Start

### Linux/Mac

```bash
./run-example.sh
```

### Windows

```batch
run-example.bat
```

## Manual Usage

### 1. Build the JAR (if not already built)

```bash
# From the project root
mvn package -pl drlgen-tests -DskipTests
```

This creates: `drlgen-tests/target/drlgen-tests-1.0.0-SNAPSHOT.jar`

### 2. Show Help

```bash
java -jar path/to/drlgen-tests-1.0.0-SNAPSHOT.jar --help
```

### 3. Run with Custom Scenarios

```bash
java -jar path/to/drlgen-tests-1.0.0-SNAPSHOT.jar \
    --scenarios-dir ./scenarios \
    --models qwen3-coder-next \
    --output-dir ./results
```

### 4. Run Multiple Models

```bash
java -jar path/to/drlgen-tests-1.0.0-SNAPSHOT.jar \
    --scenarios-dir ./scenarios \
    --models qwen3-coder-next,granite4:small-h,qwen3 \
    --output results.csv
```

## Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `--models <list>` | Comma-separated model names | All from models.yaml |
| `--scenarios <list>` | Filter scenarios by name | All scenarios |
| `--scenarios-dir <path>` | Load scenarios from directory | Built-in scenarios |
| `--instructions <path>` | Domain instructions file | None |
| `--output <file>` | CSV output filename | comparison-results.csv |
| `--output-dir <dir>` | Output directory for artifacts | test-runs |
| `-h, --help` | Show help message | - |

## Creating Custom Scenarios

Scenarios are defined in YAML format. Here's a simple example:

```yaml
name: Adult Validation
description: Check if a person is an adult based on age

requirement: |
  If the person's age is 18 or greater, set the 'adult' field to true.
  Otherwise, set the 'adult' field to false.

factTypes:
  - name: Person
    fields:
      name: String
      age: int
      adult: boolean

testCases:
  - name: Adult person
    input:
      - _type: Person
        name: John
        age: 25
        adult: false
    expectedRulesFired: 1
    expectedFields:
      adult: true

  - name: Minor person
    input:
      - _type: Person
        name: Jane
        age: 15
        adult: false
    expectedRulesFired: 1
    expectedFields:
      adult: false
```

### Scenario YAML Structure

| Field | Description |
|-------|-------------|
| `name` | Scenario name (displayed in reports) |
| `description` | Brief description of what the scenario tests |
| `requirement` | Natural language description of the rules to generate |
| `factTypes` | List of fact types with their fields |
| `testCases` | Test cases to validate the generated rules |

### Test Case Structure

| Field | Description |
|-------|-------------|
| `name` | Test case name |
| `input` | List of fact objects to insert (use `_type` for class name) |
| `expectedRulesFired` | Number of rules expected to fire |
| `expectedFields` | Expected field values after rules execute |

## Output Structure

After running, the output directory contains:

```
results/
└── 2026-03-12_10-00-00_qwen3-coder-next/
    ├── run-summary.txt           # Overall results summary
    ├── results.csv               # CSV with all results
    ├── temperature-alert/
    │   ├── original.yaml         # Copy of input scenario
    │   ├── generated.drl         # AI-generated DRL rules
    │   ├── result.json           # Detailed execution results
    │   └── log.txt               # Execution log
    └── product-pricing/
        ├── original.yaml
        ├── generated.drl
        ├── result.json
        └── log.txt
```

## Example Output

```
Starting Drools Rule Generation Comparison
Models: 1
Scenarios: 2
Scenarios directory: /path/to/scenarios
Output directory: /path/to/results

=== Testing model: qwen3-coder-next ===
Artifacts directory: results/2026-03-12_10-00-00_qwen3-coder-next
[1/2] qwen3-coder-next - Temperature Alert... PASS (3 rules)
[2/2] qwen3-coder-next - Product Pricing Rules... PASS (4 rules)

=== Comparison Report ===
Model                  Scenarios  Passed  Failed  Success Rate
qwen3-coder-next             2       2       0       100.0%
```

## Troubleshooting

### "Ollama is not running"

Start Ollama:
```bash
ollama serve
```

### "Model not found"

Pull the required model:
```bash
ollama pull qwen3-coder-next
```

List available models:
```bash
ollama list
```

### "No scenarios found"

Ensure your scenario files:
- Have `.yaml` or `.yml` extension
- Are in the directory specified by `--scenarios-dir`
- Follow the correct YAML structure

## Next Steps

- See `scenarios/` for more complex scenario examples
- Check `../../drlgen-tests/src/main/resources/scenarios/` for built-in scenarios
- Read the main [README](../../README.md) for full documentation

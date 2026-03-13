# DRL Generator Examples

This directory contains standalone example projects demonstrating how to use the `drlgen-core` library for AI-powered DRL generation.

## Prerequisites

1. **Java 17+** installed
2. **Maven 3.8+** installed
3. **Ollama** running locally with a model (e.g., `granite4`)
4. **drlgen-core** installed in local Maven repository

### Installing drlgen-core

From the project root:
```bash
cd ..
mvn install -pl drlgen-core
```

### Installing Ollama Model

```bash
ollama pull granite4
ollama serve  # Start Ollama server
```

## Example Projects

| Project | Description | Complexity |
|---------|-------------|------------|
| [example-hello-world](example-hello-world/) | Minimal working example | Beginner |
| [example-generate-and-execute](example-generate-and-execute/) | Generate DRL and execute with test facts | Beginner |
| [example-standalone-jar](example-standalone-jar/) | Run tests using pre-built JAR (no Maven needed) | Beginner |
| [example-domain-instructions](example-domain-instructions/) | Add domain-specific knowledge | Intermediate |
| [example-custom-prompts](example-custom-prompts/) | Override default AI prompts | Intermediate |
| [example-custom-validator](example-custom-validator/) | Implement custom validation rules | Intermediate |
| [example-production-patterns](example-production-patterns/) | Caching, error handling, multi-model | Advanced |

## Running an Example

Each example can be run independently:

```bash
cd example-hello-world
mvn compile exec:java
```

### Standalone JAR Example (No Maven Required)

The `example-standalone-jar` shows how to run tests using a pre-built JAR:

```bash
cd example-standalone-jar
./run-example.sh          # Linux/Mac
run-example.bat           # Windows
```

Or run the JAR directly:
```bash
java -jar ../drlgen-tests/target/drlgen-tests-1.0.0-SNAPSHOT.jar \
    --scenarios-dir ./scenarios \
    --models qwen3-coder-next
```

## Quick Start

Start with `example-hello-world` to understand the basics:

```java
// Create AI model
ChatModel model = ModelConfiguration.createModel("granite4");

// Create DRL generator with defaults
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .build();

// Generate DRL from natural language
GenerationResult result = generator.generate(
    "If person age >= 18, set adult to true",
    "Person: name (String), age (int), adult (boolean)"
);

// Print generated rules
System.out.println(result.generatedDrl());
```

## Configuration

### Using Different Models

Set via environment variable:
```bash
export TEST_OLLAMA_MODEL=granite4:small-h
mvn exec:java
```

Or configure in code:
```java
ChatModel model = ModelConfiguration.createModel("granite4:small-h");
```

### Available Models

See `models.yaml` in the project root for all configured models.

| Model | Parameters | Success Rate |
|-------|------------|--------------|
| qwen3-coder-next | 79.7B | 100% |
| granite4:small-h | 32.2B | 100% |
| qwen2.5-coder:14b | 14.8B | 100% |
| qwen3 | 8.2B | 88% |
| granite4 | 3.4B | 18% |

## Troubleshooting

### "Connection refused" errors
Ensure Ollama is running:
```bash
ollama serve
```

### "Model not found" errors
Pull the required model:
```bash
ollama pull granite4
```

### Generation timeout
Increase timeout in code or use a faster model:
```java
ChatModel model = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("granite4")
    .timeout(Duration.ofMinutes(10))
    .build();
```

## Further Reading

- [LIBRARY_MANUAL.md](../LIBRARY_MANUAL.md) - Complete API documentation
- [README.md](../README.md) - Project overview

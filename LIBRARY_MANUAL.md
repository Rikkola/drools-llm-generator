# DRL Generation Library Manual

A comprehensive guide to using the `drlgen-core` library for AI-powered Drools Rule Language (DRL) generation.

## Table of Contents

1. [Overview](#overview)
2. [Getting Started](#getting-started)
3. [Core API](#core-api)
4. [Configuration](#configuration)
5. [Extension Points](#extension-points)
6. [Advanced Usage](#advanced-usage)
7. [REST API](#rest-api)
8. [Migration Guide](#migration-guide)
9. [Troubleshooting](#troubleshooting)

---

## Overview

The `drlgen-core` library provides a clean, extensible API for generating Drools rules from natural language requirements using AI models. The library is:

- **Provider-agnostic**: Works with any LangChain4j-compatible `ChatModel` (Ollama, OpenAI, Anthropic, etc.)
- **Extensible**: All components are interfaces with pluggable implementations
- **Thread-safe**: `DRLGenerator` instances can be shared across threads
- **Configurable**: Prompts, guides, cleanup strategies, and validators can all be customized

### Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│ PUBLIC API                                                       │
│                                                                  │
│  DRLGenerator.builder()                                         │
│      .chatModel(anyLangChain4jModel)     // Provider-agnostic   │
│      .promptProvider(prompts)            // Externalized        │
│      .guideProvider(guides)              // Customizable        │
│      .cleanupStrategy(strategy)          // Configurable        │
│      .validator(validator)               // Pluggable           │
│      .build()                                                   │
│                                                                  │
│  GenerationResult result = generator.generate(requirement);     │
└─────────────────────────────────────────────────────────────────┘
            │
            │ uses
            ▼
┌─────────────────────────────────────────────────────────────────┐
│ CORE INTERFACES                                                  │
│                                                                  │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐               │
│  │PromptProvider│ │GuideProvider │ │DRLValidator │               │
│  └─────────────┘ └─────────────┘ └─────────────┘               │
│  ┌─────────────────┐                                            │
│  │DRLCleanupStrategy│                                            │
│  └─────────────────┘                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## Getting Started

### Maven Dependency

Add `drlgen-core` to your project:

```xml
<dependency>
    <groupId>com.github.rikkola</groupId>
    <artifactId>drlgen-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### Minimal Example

```java
import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.model.GenerationResult;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;

// Create a chat model (Ollama example)
ChatModel model = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("qwen3-coder-next")
    .temperature(0.1)
    .build();

// Create the DRL generator
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .build();

// Define your requirement and fact types
String requirement = """
    Create a rule that checks if a person is an adult.
    If age is 18 or greater, set the 'adult' field to true.
    """;

String factTypes = """
    - Person: name (String), age (int), adult (boolean)
    """;

// Generate DRL
GenerationResult result = generator.generate(requirement, factTypes);

if (result.validationPassed()) {
    System.out.println("Generated DRL:\n" + result.generatedDrl());
} else {
    System.out.println("Validation failed: " + result.validationMessage());
}
```

### Using RuleDefinition

For structured input, use `RuleDefinition`:

```java
import com.github.rikkola.drlgen.model.RuleDefinition;

RuleDefinition definition = RuleDefinition.builder()
    .requirement("If person age >= 18, set adult to true")
    .factType("Person", Map.of(
        "name", "String",
        "age", "int",
        "adult", "boolean"
    ))
    .build();

GenerationResult result = generator.generate(definition);
```

---

## Core API

### DRLGenerator

The main entry point for DRL generation. Create instances using the builder:

```java
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(chatModel)                    // Required
    .promptProvider(promptProvider)          // Optional, defaults to classpath
    .guideProvider(guideProvider)            // Optional, defaults to classpath
    .cleanupStrategy(cleanupStrategy)        // Optional, defaults to markdown cleanup
    .validator(validator)                    // Optional, defaults to Drools validator
    .build();
```

#### Generation Methods

| Method | Description |
|--------|-------------|
| `generate(RuleDefinition)` | Generate from a rule definition |
| `generate(RuleDefinition, String exampleInput)` | Generate with example input |
| `generate(String requirement, String factTypes)` | Generate from raw strings |
| `generate(String requirement, String factTypes, String exampleInput)` | Full parameters |
| `generateAndExecute(RuleDefinition, String factsJson)` | Generate and execute with facts |
| `execute(String drlCode, String factsJson)` | Execute existing DRL |
| `validate(String drlCode)` | Validate existing DRL |

#### Example: Generate and Execute

```java
String factsJson = """
    [{"_type": "Person", "name": "John", "age": 25, "adult": false}]
    """;

GenerationResult result = generator.generateAndExecute(definition, factsJson);

if (result.executionPassed()) {
    System.out.println("Rules fired: " + result.rulesFired());
    System.out.println("Resulting facts: " + result.resultingFacts());
}
```

### GenerationResult

Contains the complete result of a generation operation:

```java
public record GenerationResult(
    String modelName,           // Name of the model used
    String generatedDrl,        // The generated DRL code
    boolean validationPassed,   // Whether DRL syntax is valid
    String validationMessage,   // Validation details
    boolean executionPassed,    // Whether execution succeeded (if run)
    String executionMessage,    // Execution details
    int rulesFired,             // Number of rules that fired
    List<Object> resultingFacts,// Facts after execution
    Duration generationTime,    // Time spent generating
    Duration totalTime          // Total operation time
) { }
```

#### Static Factory Methods

```java
// Create a failed result
GenerationResult.failed(modelName, errorMessage, duration);

// Create a partial result (validation failed)
GenerationResult.partial(modelName, drl, validationPassed, message, genTime, totalTime);

// Create a validated result (no execution)
GenerationResult.validated(modelName, drl, validationMessage, genTime, totalTime);
```

---

## Configuration

### Using models.yaml

Configure models in `models.yaml` at your project root:

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

  - id: openai-gpt4
    provider: openai
    name: gpt-4
    temperature: 0.1
```

Load models using `ModelConfiguration`:

```java
ChatModel model = ModelConfiguration.createModel("qwen3-coder-next");
```

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `TEST_OLLAMA_MODEL` | Default Ollama model | `qwen3-coder-next` |
| `TEST_OLLAMA_BASE_URL` | Ollama server URL | `http://localhost:11434` |

---

## Extension Points

### PromptProvider

Controls the system and user prompts sent to the AI model.

```java
public interface PromptProvider {
    String getSystemMessage();
    String getUserMessageTemplate();
}
```

#### Built-in Implementations

| Implementation | Description |
|----------------|-------------|
| `ClasspathPromptProvider` | Loads from `prompts/default/system.txt` and `user.txt` |
| `FilePromptProvider` | Loads from a specified directory |

#### Creating Custom Prompts

**Option 1: Override classpath resources**

Create `prompts/default/system.txt` and `prompts/default/user.txt` in your classpath.

**Option 2: Use FilePromptProvider**

```java
Path promptsDir = Paths.get("/path/to/my/prompts");
PromptProvider provider = PromptProvider.fromDirectory(promptsDir);

DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .promptProvider(provider)
    .build();
```

**Option 3: Implement custom provider**

```java
PromptProvider customProvider = new PromptProvider() {
    @Override
    public String getSystemMessage() {
        return "You are a Drools DRL expert...";
    }

    @Override
    public String getUserMessageTemplate() {
        return """
            Generate DRL for: {{requirement}}
            Fact types: {{factTypes}}
            """;
    }
};
```

#### User Message Template Placeholders

| Placeholder | Description |
|-------------|-------------|
| `{{drlGuide}}` | The DRL reference guide content |
| `{{requirement}}` | The natural language requirement |
| `{{factTypes}}` | Description of fact types |
| `{{exampleInput}}` | Example input/output (optional) |

### GuideProvider

Provides the DRL reference guide that helps the AI generate correct syntax.

```java
public interface GuideProvider {
    String getGuide();
}
```

#### Built-in Implementations

| Implementation | Description |
|----------------|-------------|
| `ClasspathGuideProvider` | Loads `drl-reference-guide.md` from classpath |
| `FileGuideProvider` | Loads guide from a specified file |
| `CompositeGuideProvider` | Combines base guide with domain instructions |

#### Adding Domain-Specific Instructions

```java
// Method 1: Using builder convenience method
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .domainInstructions(Paths.get("insurance-domain.md"))
    .build();

// Method 2: Using GuideProvider directly
GuideProvider baseGuide = GuideProvider.createDefault();
GuideProvider withDomain = GuideProvider.withDomainInstructions(
    baseGuide,
    Paths.get("insurance-domain.md")
);

DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .guideProvider(withDomain)
    .build();
```

#### Example Domain Instructions File

```markdown
# Insurance Domain Instructions

## Key Concepts
- Policies have coverage levels: BASIC, STANDARD, PREMIUM
- Risk factors are calculated based on age, health history, and lifestyle
- Premium calculations must account for base rate × risk factor

## Important Rules
- Never approve coverage for applicants under 18
- High-risk applicants (risk factor > 2.0) require manual review
- Always round premium to 2 decimal places

## Fact Types
- Policy: policyId, coverageLevel, basePremium, finalPremium
- Applicant: name, age, healthScore, riskFactor, approved
```

### DRLCleanupStrategy

Cleans AI-generated DRL to remove artifacts like markdown code blocks.

```java
@FunctionalInterface
public interface DRLCleanupStrategy {
    String cleanup(String drl);
}
```

#### Built-in Implementations

| Implementation | Description |
|----------------|-------------|
| `DefaultCleanupStrategy` | Removes markdown blocks, normalizes whitespace |
| `MarkdownCleanupStrategy` | Removes only markdown code fences |
| `CompositeCleanupStrategy` | Chains multiple strategies |

#### Creating Custom Cleanup

```java
// Simple lambda
DRLCleanupStrategy removeComments = drl ->
    drl.replaceAll("//.*?\n", "\n");

// Composite strategy
DRLCleanupStrategy composite = DRLCleanupStrategy.composite(
    DRLCleanupStrategy.createDefault(),
    removeComments
);

DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .cleanupStrategy(composite)
    .build();
```

### DRLValidator

Validates generated DRL syntax and structure.

```java
public interface DRLValidator {
    ValidationResult validate(String drlCode);
}
```

#### Built-in Implementations

| Implementation | Description |
|----------------|-------------|
| `DroolsValidator` | Uses Drools engine to validate syntax |
| `CompositeValidator` | Runs multiple validators, aggregates results |
| No-op validator | `DRLValidator.noOp()` - always returns success |

#### ValidationResult

```java
public final class ValidationResult {
    boolean isValid();                    // Overall validity
    List<ValidationMessage> getMessages(); // All messages
    List<ValidationMessage> getErrors();   // Only errors
    List<ValidationMessage> getWarnings(); // Only warnings
    String getSummary();                  // Human-readable summary
}

public record ValidationMessage(
    Severity severity,    // ERROR, WARNING, NOTE
    String message,       // The message text
    Integer lineNumber,   // Line number (may be null)
    String content        // Related code content (may be null)
) { }
```

#### Creating Custom Validators

```java
// Custom business rule validator
DRLValidator businessValidator = drlCode -> {
    if (!drlCode.contains("audit")) {
        return ValidationResult.builder()
            .message(ValidationResult.ValidationMessage.warning(
                "Consider adding audit logging to rules"))
            .build();
    }
    return ValidationResult.success();
};

// Combine with default validator
DRLValidator combined = new CompositeValidator(
    DRLValidator.createDefault(),
    businessValidator
);
```

---

## Advanced Usage

### Using Different AI Providers

#### Ollama (Local)

```java
ChatModel ollama = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("qwen3-coder-next")
    .temperature(0.1)
    .timeout(Duration.ofMinutes(5))
    .build();
```

#### OpenAI

```java
ChatModel openai = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4")
    .temperature(0.1)
    .build();
```

#### Anthropic

```java
ChatModel anthropic = AnthropicChatModel.builder()
    .apiKey(System.getenv("ANTHROPIC_API_KEY"))
    .modelName("claude-3-opus-20240229")
    .temperature(0.1)
    .build();
```

### Caching Generators

For applications handling multiple requests, cache generators by model:

```java
private final Map<String, DRLGenerator> cache = new ConcurrentHashMap<>();

public DRLGenerator getGenerator(String modelName) {
    return cache.computeIfAbsent(modelName, name -> {
        ChatModel model = ModelConfiguration.createModel(name);
        return DRLGenerator.builder()
            .chatModel(model)
            .build();
    });
}
```

### Executing Generated DRL

```java
// Execute with JSON facts
DRLRunnerResult result = generator.execute(drlCode, """
    [
        {"_type": "Order", "orderId": "ORD001", "total": 150.0, "discountApplied": false}
    ]
    """);

System.out.println("Rules fired: " + result.firedRules());
for (Object fact : result.objects()) {
    System.out.println("Fact: " + fact);
}
```

### Disabling Components

```java
// Disable validation (for testing)
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .noValidation()
    .build();

// Disable cleanup (if model produces clean output)
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .noCleanup()
    .build();
```

---

## REST API

The `drlgen-ui` module provides a REST API built with Quarkus.

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/generate/models` | List available models |
| POST | `/api/generate/drl` | Generate DRL from requirements |

### Generate DRL Request

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

### Response

```json
{
  "success": true,
  "generatedDrl": "package rules;\n\ndeclare Person...",
  "validationMessage": "Validation passed",
  "elapsedMs": 2345
}
```

### Running the UI

```bash
cd drlgen-ui
mvn quarkus:dev
```

Access at http://localhost:8080

---

## API Overview

### Recommended: DRLGenerator

The `DRLGenerator` class provides a clean builder-based API:

```java
ChatModel model = ModelConfiguration.createModel("qwen3-coder-next");
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .build();
GenerationResult result = generator.generate(requirement, factTypes);
```

### Alternative: DRLGenerationService

For more control, use `DRLGenerationService` directly:

```java
DRLGenerationService service = DRLGenerationService.builder()
    .validator(DRLValidator.createDefault())
    .cleanupStrategy(DRLCleanupStrategy.createDefault())
    .guideProvider(GuideProvider.createDefault())
    .agentFactory(DRLGenerationAgent::create)
    .build();
ChatModel model = ModelConfiguration.createModel("qwen3-coder-next");
GenerationResult result = service.generate(model, requirement, factTypes);
```

### Key Features

| Aspect | Description |
|--------|-------------|
| Entry point | `DRLGenerator` (simple) or `DRLGenerationService` (configurable) |
| Model handling | Configured once in builder |
| Customization | Builder with pluggable interfaces (`DRLValidator`, `DRLCleanupStrategy`, etc.) |
| Thread safety | Thread-safe by design |

---

## Troubleshooting

### Generation Timeout

If generation times out, increase the model timeout:

```java
ChatModel model = OllamaChatModel.builder()
    .baseUrl("http://localhost:11434")
    .modelName("qwen3-coder-next")
    .timeout(Duration.ofMinutes(10))  // Increase timeout
    .build();
```

### Invalid DRL Syntax

Common causes:
1. **Markdown in output**: Ensure cleanup strategy is enabled
2. **Wrong model**: Some models struggle with DRL syntax (see model compatibility table)
3. **Missing guide**: Ensure the DRL reference guide is available

Debug by examining the raw output:

```java
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .noCleanup()  // See raw output
    .noValidation()
    .build();

GenerationResult result = generator.generate(requirement, factTypes);
System.out.println("Raw output:\n" + result.generatedDrl());
```

### Model Not Found

Ensure Ollama is running and the model is pulled:

```bash
# Check Ollama is running
curl http://localhost:11434/api/tags

# Pull a model
ollama pull qwen3-coder-next
```

### ClassNotFoundException for ChatModel

Ensure you have the correct LangChain4j dependency:

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-ollama</artifactId>
    <version>0.36.2</version>
</dependency>
```

---

## Appendix: Model Compatibility

| Model | Parameters | DRL Success Rate | Notes |
|-------|------------|------------------|-------|
| qwen3-coder-next | 79.7B | 100% | Best for DRL |
| granite4:small-h | 32.2B | 100% | Mamba-2 hybrid |
| qwen2.5-coder:14b | 14.8B | 100% | Most efficient |
| qwen3 | 8.2B | 88% | Good balance |
| granite4 | 3.4B | 18% | Use Plain English pipeline |

For models with low DRL success rates, consider using the YAML or Plain English generation pipelines available in `drlgen-tests`.

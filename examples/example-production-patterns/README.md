# Production Patterns Example

Advanced patterns for using the DRL Generator in production applications.

## What This Example Contains

Three example classes demonstrating production-ready patterns:

1. **GeneratorCachingExample** - Cache generators per model for efficiency
2. **ErrorHandlingExample** - Robust error handling and recovery
3. **MultiModelComparisonExample** - Compare outputs from multiple models

## Prerequisites

- Ollama running locally with multiple models
- `drlgen-core` installed in local Maven repository

## Running

```bash
# Run caching example (default)
mvn compile exec:java

# Run specific example
mvn compile exec:java@caching
mvn compile exec:java@error-handling
mvn compile exec:java@multi-model
```

## Pattern 1: Generator Caching

Why cache generators:
- Creating models has overhead (connection setup, config parsing)
- DRLGenerator instances are thread-safe
- Same generator can handle multiple requests

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

## Pattern 2: Error Handling

Production error handling should:
- Catch specific exception types
- Provide meaningful error messages
- Support retry logic for transient failures
- Log appropriately for debugging

```java
try {
    GenerationResult result = generator.generate(requirement, factTypes);

    if (!result.validationPassed()) {
        // Handle validation failure
        log.warn("Validation failed: {}", result.validationMessage());
        return handleValidationFailure(result);
    }

    return result;

} catch (Exception e) {
    log.error("Generation failed", e);
    return GenerationResult.failed("unknown", e.getMessage(), Duration.ZERO);
}
```

## Pattern 3: Multi-Model Comparison

Compare outputs from different models to:
- Find the best model for your use case
- Implement fallback strategies
- A/B test different configurations

```java
List<String> models = List.of("granite4", "granite4:small-h", "qwen3");

for (String modelName : models) {
    GenerationResult result = getGenerator(modelName).generate(requirement, factTypes);
    System.out.printf("%s: %s (%dms)%n",
        modelName,
        result.validationPassed() ? "SUCCESS" : "FAILED",
        result.generationTime().toMillis());
}
```

## Production Checklist

- [ ] Cache generators per model
- [ ] Implement proper error handling
- [ ] Add logging at appropriate levels
- [ ] Set reasonable timeouts
- [ ] Consider retry logic for transient failures
- [ ] Monitor generation times and success rates
- [ ] Validate generated DRL before execution
- [ ] Test with your actual models and requirements

## Code Files

| File | Description |
|------|-------------|
| `GeneratorCachingExample.java` | Thread-safe generator caching |
| `ErrorHandlingExample.java` | Robust error handling patterns |
| `MultiModelComparisonExample.java` | Compare multiple AI models |

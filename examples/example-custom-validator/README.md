# Custom Validator Example

Demonstrates how to implement custom validation rules beyond the default Drools syntax checking.

## What This Example Does

1. Creates a custom validator that checks for business rules
2. Combines it with the default Drools validator
3. Shows validation results with custom warnings and errors
4. Demonstrates the CompositeValidator pattern

## When to Use Custom Validators

Use custom validators when you need to:
- **Enforce coding standards** - Check for required comments, naming patterns
- **Security checks** - Detect potentially dangerous patterns
- **Business rules** - Ensure generated rules follow company policies
- **Quality gates** - Add warnings for best practice violations

## Prerequisites

- Ollama running locally with `granite4` model
- `drlgen-core` installed in local Maven repository

## Running

```bash
mvn compile exec:java
```

## Custom Validators in This Example

### 1. Comment Validator
Warns if rules don't have comments:
```java
DRLValidator commentValidator = drlCode -> {
    if (!drlCode.contains("//") && !drlCode.contains("/*")) {
        return ValidationResult.builder()
            .message(ValidationMessage.warning("Rules should have comments"))
            .build();
    }
    return ValidationResult.success();
};
```

### 2. Naming Convention Validator
Checks for proper rule naming:
```java
DRLValidator namingValidator = drlCode -> {
    if (drlCode.contains("rule \"Rule1\"") || drlCode.contains("rule \"Test\"")) {
        return ValidationResult.builder()
            .message(ValidationMessage.error("Use descriptive rule names"))
            .build();
    }
    return ValidationResult.success();
};
```

### 3. Security Validator
Detects potentially dangerous patterns:
```java
DRLValidator securityValidator = drlCode -> {
    if (drlCode.contains("System.exit") || drlCode.contains("Runtime.exec")) {
        return ValidationResult.builder()
            .message(ValidationMessage.error("Security: Dangerous method call detected"))
            .build();
    }
    return ValidationResult.success();
};
```

## Code Explanation

```java
// Create custom validators
DRLValidator customValidator = createCustomValidator();

// Combine with default Drools validator
DRLValidator compositeValidator = new CompositeValidator(
    DRLValidator.createDefault(),  // Syntax validation
    customValidator                 // Business rules
);

// Build generator with composite validator
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .validator(compositeValidator)
    .build();
```

## ValidationResult Structure

```java
ValidationResult result = validator.validate(drlCode);

// Check overall validity
boolean isValid = result.isValid();

// Get all messages by severity
List<ValidationMessage> errors = result.getErrors();
List<ValidationMessage> warnings = result.getWarnings();
List<ValidationMessage> notes = result.getNotes();

// Get human-readable summary
String summary = result.getSummary();
```

## Next Steps

- [example-production-patterns](../example-production-patterns/) - Complete production setup

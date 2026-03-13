# Domain Instructions Example

Demonstrates how to add domain-specific knowledge to improve DRL generation quality.

## What This Example Does

1. Loads domain-specific instructions from a markdown file
2. Combines them with the default DRL reference guide
3. Generates rules that follow domain conventions
4. Shows how domain knowledge improves rule quality

## Why Use Domain Instructions?

Domain instructions help the AI model understand:
- **Business terminology** - What does "premium coverage" mean?
- **Calculation rules** - How are risk factors computed?
- **Constraints** - What values are valid?
- **Patterns** - How should related rules interact?

Without domain instructions, the AI might generate technically valid DRL that doesn't follow your business conventions.

## Prerequisites

- Ollama running locally with `qwen2.5-coder:14b-instruct-q4_K_M` model (9GB)
- `drlgen-core` installed in local Maven repository

> **Note:** This example uses a larger model because complex multi-rule domain scenarios
> require more reasoning capability. Simpler examples use `granite4` (2.1GB).

## Running

```bash
mvn compile exec:java
```

## Project Structure

```
example-domain-instructions/
├── pom.xml
├── README.md
└── src/main/resources/
    └── insurance-rules.md    # Domain-specific instructions
```

## Domain Instructions File

The `insurance-rules.md` file contains domain knowledge:

```markdown
# Insurance Domain Instructions

## Key Concepts
- Policies have coverage levels: BASIC, STANDARD, PREMIUM
- Risk factors are calculated based on age, health score, and lifestyle

## Calculation Rules
- Base premium is determined by coverage level
- Final premium = base premium * risk factor
- Risk factor range: 0.5 (low risk) to 3.0 (high risk)

## Important Constraints
- Never approve coverage for applicants under 18
- High-risk applicants (factor > 2.0) require manual review
```

## Code Explanation

```java
// Load domain instructions from file
Path domainInstructions = Paths.get("src/main/resources/insurance-rules.md");

// Create generator with domain instructions
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .domainInstructions(domainInstructions)  // Add domain knowledge
    .build();
```

## Next Steps

- [example-custom-prompts](../example-custom-prompts/) - Customize AI prompts entirely
- [example-custom-validator](../example-custom-validator/) - Add custom validation rules

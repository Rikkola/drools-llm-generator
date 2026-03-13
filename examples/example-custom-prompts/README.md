# Custom Prompts Example

Demonstrates how to completely override the default AI prompts with custom ones.

## What This Example Does

1. Loads custom system and user prompts from files
2. Uses these instead of the default classpath prompts
3. Generates DRL using the custom instructions
4. Shows how prompt customization affects output

## When to Use Custom Prompts

Use custom prompts when you need to:
- **Change the AI's persona** - Make it focus on specific aspects
- **Modify output format** - Request different code style or comments
- **Add company-specific guidelines** - Enforce coding standards
- **Optimize for specific models** - Different models respond differently to prompts

## Prerequisites

- Ollama running locally with `granite4` model
- `drlgen-core` installed in local Maven repository

## Running

```bash
mvn compile exec:java
```

## Project Structure

```
example-custom-prompts/
├── pom.xml
├── README.md
└── src/main/resources/prompts/
    ├── system.txt    # Custom system prompt
    └── user.txt      # Custom user message template
```

## Custom Prompt Files

### system.txt
The system prompt defines the AI's behavior:
```
You are a Drools DRL code generator specialized in financial services.
Generate clean, well-documented DRL code following these rules:
- Always add comments explaining rule logic
- Use descriptive rule names
- Group related rules together
...
```

### user.txt
The user template has placeholders for runtime values:
```
{{drlGuide}}

Generate DRL for this requirement:
{{requirement}}

Available fact types:
{{factTypes}}

Test scenarios:
{{exampleInput}}
```

## Code Explanation

```java
// Load custom prompts from a directory
Path promptsDir = Paths.get("src/main/resources/prompts");
PromptProvider customPrompts = PromptProvider.fromDirectory(promptsDir);

// Create generator with custom prompts
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .promptProvider(customPrompts)  // Use custom prompts
    .build();
```

## Next Steps

- [example-custom-validator](../example-custom-validator/) - Add custom validation rules
- [example-production-patterns](../example-production-patterns/) - Production-ready patterns

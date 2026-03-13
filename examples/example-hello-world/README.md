# Hello World Example

The simplest possible example of using the DRL Generator library.

## What This Example Does

1. Creates a chat model using the default Ollama configuration
2. Builds a DRL generator with default settings
3. Generates a simple "adult validation" rule from natural language
4. Prints the generated DRL code

## Prerequisites

- Ollama running locally with `granite4` model
- `drlgen-core` installed in local Maven repository

## Running

```bash
mvn compile exec:java
```

## Expected Output

```
=== DRL Generator - Hello World Example ===

Requirement:
If person age >= 18, set adult to true

Generating DRL...

=== Generated DRL ===
package rules;

declare Person
    name : String
    age : int
    adult : boolean
end

rule "Check Adult"
when
    $person : Person(age >= 18)
then
    modify($person) {
        setAdult(true)
    }
end

=== Validation: PASSED ===
```

## Code Explanation

```java
// 1. Create the AI model (connects to Ollama)
ChatModel model = ModelConfiguration.createModel("granite4");

// 2. Build the DRL generator with all defaults
DRLGenerator generator = DRLGenerator.builder()
    .chatModel(model)
    .build();

// 3. Generate DRL from natural language
GenerationResult result = generator.generate(requirement, factTypes);

// 4. Check result and print
if (result.validationPassed()) {
    System.out.println(result.generatedDrl());
}
```

## Next Steps

- [example-generate-and-execute](../example-generate-and-execute/) - Run generated rules with test data
- [example-domain-instructions](../example-domain-instructions/) - Add domain-specific knowledge

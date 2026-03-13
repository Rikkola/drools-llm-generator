# Generate and Execute Example

Demonstrates generating DRL rules and executing them with test facts to verify correctness.

## What This Example Does

1. Generates DRL rules for an order discount scenario
2. Executes the generated rules with sample order data
3. Verifies that the correct discounts are applied
4. Shows the resulting facts after rule execution

## Prerequisites

- Ollama running locally with `granite4` model
- `drlgen-core` installed in local Maven repository

## Running

```bash
mvn compile exec:java
```

## Expected Output

```
=== DRL Generator - Generate and Execute Example ===

Requirement:
Apply 10% discount if order total > 100

Test Facts:
[{"_type":"Order","orderId":"ORD001","total":150.0,"discountPercent":0}]

Generating and executing DRL...

=== Generated DRL ===
package rules;

declare Order
    orderId : String
    total : double
    discountPercent : int
end

rule "Apply Discount"
when
    $order : Order(total > 100.0)
then
    modify($order) {
        setDiscountPercent(10)
    }
end

=== Execution Results ===
Validation: PASSED
Execution: PASSED
Rules fired: 1
Total time: 2345ms

=== Resulting Facts ===
Order{orderId='ORD001', total=150.0, discountPercent=10}
```

## Code Explanation

```java
// Generate AND execute in one call
GenerationResult result = generator.generateAndExecute(
    requirement,
    factTypes,
    factsJson  // JSON array of facts to insert
);

// Check execution results
if (result.executionPassed()) {
    System.out.println("Rules fired: " + result.rulesFired());
    for (Object fact : result.resultingFacts()) {
        System.out.println(fact);
    }
}
```

## Next Steps

- [example-domain-instructions](../example-domain-instructions/) - Improve generation with domain knowledge
- [example-production-patterns](../example-production-patterns/) - Handle errors in production

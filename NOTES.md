# Development Notes - March 17, 2026

## Summary

Implemented **conversational DRL generation** using LangChain4j's ChatMemory feature to enable LLM self-correction through multi-turn conversations.

## Problem Statement

Models like qwen2.5-coder were failing ~50% of test scenarios due to logic errors (infinite loops, missing state guards, wrong operators). Single-turn generation couldn't recover from these errors.

## Solution: LangChain4j ChatMemory

Used LangChain4j's built-in `MessageWindowChatMemory` to maintain conversation history across API calls:

```java
ConversationalDRLAgent agent = AiServices.builder(ConversationalDRLAgent.class)
    .chatModel(model)
    .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
    .build();
```

### Flow
```
Turn 1: agent.generateDRL(guide, requirement, facts, examples)
           ↓
        Validate & Test → Error found
           ↓
Turn 2: agent.fixDRL(errorFeedback, guideSections, previousDrl)
           ↓
        Validate & Test → Still error
           ↓
Turn 3: agent.fixDRL(errorFeedback, guideSections, previousDrl)
           ↓
        Success or max turns reached
```

## Files Created

### drlgen-core/src/main/java/com/github/rikkola/drlgen/agent/
- **ConversationalDRLAgent.java** - Agent interface with ChatMemory for multi-turn generation
  - `generateDRL()` - Initial generation with full context
  - `fixDRL()` - Fix based on error feedback (ChatMemory remembers previous turns)

### drlgen-core/src/main/java/com/github/rikkola/drlgen/conversation/
- **ErrorFeedbackFormatter.java** - Formats validation/execution errors for LLM
  - `formatValidationErrors()` - Syntax errors with line numbers
  - `formatTestFailure()` - Test execution failures with diagnosis
  - `formatCompilationError()` - Compilation errors with likely fixes

- **GuideSectionFinder.java** - Maps error patterns to relevant DRL guide sections
  - Handles: getter issues, infinite loops, multiple rules firing, syntax errors
  - Returns targeted guide excerpts to help model fix specific issues

## Files Modified

### drlgen-core
- **AgentType.java** - Added `CONVERSATIONAL` enum value
- **AgentFactory.java** - Added `createConversational(ChatModel)` method
- **DRLGenerationService.java** - Added `generateWithRetry(model, definition, exampleInput, maxTurns)`

### drlgen-tests
- **DRLGenerationService.java** - Added `generateAndTestWithRetry(model, scenario, maxTurns, instructionsPath)`
- **ComparisonRunner.java** - Added `--max-turns <n>` CLI option (default: 1, max: 5)

## Usage

```bash
# Single turn (default, same as before)
java -jar drlgen-tests.jar --models qwen2.5-coder

# With retries (3 turns = initial + 2 retries)
java -jar drlgen-tests.jar --models qwen2.5-coder --max-turns 3
```

## Test Results

Testing qwen2.5-coder on 3 previously failing scenarios:

| Scenario | Single Turn | With --max-turns 3 |
|----------|-------------|-------------------|
| Credit Card Approval | FAIL | FAIL |
| Insurance Risk Assessment | FAIL → PASS* | PASS (Turn 1) |
| Priority Assignment | FAIL → PASS* | PASS (Turn 1) |
| **Overall** | ~33% | **66.7%** |

*Note: Insurance and Priority passed on Turn 1 because ConversationalDRLAgent includes the DRL guide in its system prompt (same fix we applied to GuidedDRLGenerationAgent).

### Credit Card Approval Failure Analysis

The model couldn't fix this even with 3 turns because:
- Test input has `cardType: null`
- Model kept generating `cardType == "NONE"` (string check)
- Error feedback said "check exact input values" but model didn't understand null vs "NONE"

This shows a limitation: retries don't help when the model doesn't understand the error.

## Key Findings

1. **Guide inclusion is critical** - Adding the DRL guide to the prompt improved pass rates significantly
2. **Retries have diminishing returns** - If model doesn't understand error, more turns don't help
3. **Error feedback quality matters** - Specific, actionable feedback works better than generic messages
4. **ChatMemory simplifies implementation** - No need to manually manage conversation state

## Next Steps

1. Improve error feedback for null vs string placeholder issues
2. Consider adding test input JSON to error feedback so model can see exact values
3. Test with more models to see which benefit most from conversational approach
4. Consider tool-based approach where model can query test inputs

## Related Files

- `DRL_COMPARISON_QWEN3_VS_QWEN25.md` - Detailed comparison of generated DRLs
- `COMPARISON_REPORT.md` - Previous test comparison report

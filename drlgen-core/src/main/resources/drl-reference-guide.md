# DRL (Drools Rule Language) Reference Guide

This guide provides the syntax and patterns for generating correct, compilable DRL code.

## 1. Basic Structure

Every DRL file follows this structure:

```drl
package org.drools.generated;

declare FactType
    fieldName : Type
end

rule "Rule Name"
when
    // Pattern matching conditions
then
    // Actions to execute
end
```

### Package Declaration
Always start with a package declaration:
```drl
package org.drools.generated;
```

### Declare Blocks
Define fact types using `declare` blocks. Do NOT use Java class imports.

```drl
declare Employee
    employeeId : String
    yearsOfService : int
    veteran : boolean
end
```

### Rule Structure
Every rule has:
- A unique name in quotes
- A `when` section with pattern conditions
- A `then` section with actions
- Ends with `end`

```drl
rule "Mark Veteran Employee"
when
    $e : Employee(yearsOfService >= 10, veteran == false)
then
    modify($e) { setVeteran(true) }
end
```

## 2. Type Declarations

### Supported Field Types
```drl
declare Example
    textField : String
    wholeNumber : int
    decimalNumber : double
    flag : boolean
    dateField : java.util.Date
    moneyField : java.math.BigDecimal
end
```

### Constructor Limitation
Declared types have ONLY a no-arg constructor. You MUST use setters to populate fields:

```drl
// In then section, create and populate:
Example ex = new Example();
ex.setTextField("value");
ex.setWholeNumber(42);
insert(ex);
```

### Setter Naming Convention
The setter name is `set` + exact field name with first letter capitalized.

| Field Name | Setter Name |
|------------|-------------|
| `name` | `setName()` |
| `count` | `setCount()` |
| `isEligible` | `setIsEligible()` |
| `isComplete` | `setIsComplete()` |
| `hasWarranty` | `setHasWarranty()` |

CRITICAL: For field `isEligible`, the setter is `setIsEligible()`, NOT `setEligible()`.

## 3. Pattern Matching

### Variable Binding
Bind facts to variables using `$variableName : TypeName(constraints)`:

```drl
when
    $emp : Employee(yearsOfService >= 5)
    $project : Project(leadId == $emp.employeeId)
then
    // Use $emp and $project here
end
```

### Multiple Patterns in When Clause

CRITICAL: When you have multiple patterns in a `when` clause, separate them with NEWLINES only. Do NOT use commas between patterns.

CORRECT (patterns separated by newlines):
```drl
when
    $applicant : Applicant(age >= 18)
    $risk : RiskAssessment(applicantName == $applicant.name)
then
    // Actions here
end
```

WRONG (comma between patterns - causes syntax error):
```drl
when
    $applicant : Applicant(age >= 18),
    $risk : RiskAssessment(applicantName == $applicant.name)
then
    // This will NOT compile!
end
```

**Remember:**
- Commas INSIDE a pattern separate constraints: `Person(age >= 18, name != null)`
- Commas BETWEEN patterns are INVALID - use newlines instead

### Constraint Operators

| Operator | Meaning | Example |
|----------|---------|---------|
| `==` | Equals | `state == "OPEN"` |
| `!=` | Not equals | `label != null` |
| `>` | Greater than | `count > 10` |
| `<` | Less than | `weight < 50` |
| `>=` | Greater or equal | `rating >= 4` |
| `<=` | Less or equal | `quantity <= 100` |

### Boolean Operators in Constraints

CRITICAL: Use `&&` for AND and `||` for OR. Do NOT use `and` or `or`.

CORRECT:
```drl
Vehicle(mileage >= 50000 && mileage <= 100000)
Task(priority == "HIGH" || priority == "URGENT")
Machine(temperature > 80 || pressure > 100 || vibration > 50)
```

WRONG (will cause syntax errors):
```drl
Vehicle(mileage >= 50000 and mileage <= 100000)    // WRONG: 'and' not allowed
Machine(temperature > 80 or pressure > 100)        // WRONG: 'or' not allowed
```

### Multiple Constraints
Separate constraints with commas (implicit AND):

```drl
Vehicle(
    mileage >= 50000,
    state == "ACTIVE",
    value > 10000
)
```

### Numeric Expressions in Constraints
```drl
Invoice(totalAmount > quantity * unitPrice)
Budget(requestedAmount < 3 * availableFunds)
```

### Boolean Field Matching
```drl
Member(isPremium == true)
Equipment(isOperational == true)
Item(inWarehouse == false)
```

## 4. The modify() Action

### Basic Syntax
Use `modify()` to update facts and trigger re-evaluation:

```drl
rule "Mark Veteran"
when
    $e : Employee(yearsOfService >= 10, veteran == false)
then
    modify($e) { setVeteran(true) }
end
```

### Multiple Field Updates
Separate multiple setters with commas:

```drl
modify($ticket) {
    setState("RESOLVED"),
    setPriority(0),
    setClosedDate(new java.util.Date())
}
```

### Using Getters in modify()

CRITICAL: When referencing other fields inside modify(), you MUST use the variable prefix.

CORRECT:
```drl
modify($budget) {
    setTotalRevenue($budget.getSales() + $budget.getServices())
}
```

WRONG (undefined method error):
```drl
modify($budget) {
    setTotalRevenue(getSales() + getServices())  // Missing $budget prefix!
}
```

### Alternative: Bind Values in When Clause
You can bind field values in the when clause to avoid getter issues:

```drl
when
    $b : Budget($sales : sales, $services : services)
then
    modify($b) { setTotalRevenue($sales + $services) }
end
```

## 5. Rule Salience and Priority

### Syntax
Use `salience` to control rule firing order. Higher values fire first.

```drl
rule "High Priority"
salience 100
when
    // conditions
then
    // actions
end

rule "Normal Priority"
salience 50
when
    // conditions
then
    // actions
end

rule "Low Priority (Default)"
salience 10
when
    // conditions
then
    // actions
end
```

### Use Cases

**Priority-based selection** - When multiple rules could match, fire the most specific first:

```drl
rule "Fragile Delivery"
salience 100
when
    $d : Delivery(deliveryMethod == "PENDING", isFragile == true)
then
    modify($d) { setDeliveryMethod("CAREFUL_HANDLING") }
end

rule "Urgent Delivery"
salience 90
when
    $d : Delivery(deliveryMethod == "PENDING", isUrgent == true)
then
    modify($d) { setDeliveryMethod("EXPRESS") }
end

rule "Normal Delivery"
salience 10
when
    $d : Delivery(deliveryMethod == "PENDING")
then
    modify($d) { setDeliveryMethod("STANDARD") }
end
```

## 6. Chained/Sequential Rules

When rules depend on results from previous rules, check that prior calculations exist.

### Pattern: Budget Calculation Chain

```drl
// Rule 1: Calculate total revenue (no dependencies)
rule "Calculate Total Revenue"
salience 50
when
    $b : ProjectBudget(totalRevenue == 0)
then
    modify($b) {
        setTotalRevenue($b.getSales() + $b.getGrants() + $b.getInvestments())
    }
end

// Rule 2: Calculate total costs (depends on Rule 1)
rule "Calculate Total Costs"
salience 40
when
    $b : ProjectBudget(totalRevenue > 0, totalCosts == 0)
then
    modify($b) {
        setTotalCosts($b.getLabor() * 1000 + $b.getMaterials())
    }
end

// Rule 3: Calculate profit (depends on Rules 1 and 2)
rule "Calculate Profit"
salience 30
when
    $b : ProjectBudget(totalRevenue > 0, totalCosts > 0, profit == 0)
then
    modify($b) {
        setProfit($b.getTotalRevenue() - $b.getTotalCosts())
    }
end

// Rule 4: Determine profit category (depends on Rule 3)
// NOTE: Check for the INITIAL value from input (e.g., "UNSET" or "PENDING"), not null
rule "Low Profit Category"
salience 20
when
    $b : ProjectBudget(profit > 0, profit <= 10000, category == "UNSET" || category == null)
then
    modify($b) { setCategory("LOW") }
end

rule "High Profit Category"
salience 20
when
    $b : ProjectBudget(profit > 50000, category == "UNSET" || category == null)
then
    modify($b) { setCategory("HIGH") }
end

// Rule 5: Calculate bonus pool (depends on Rule 4)
rule "Calculate Bonus - Low Category"
salience 10
when
    $b : ProjectBudget(category == "LOW", bonusPool == 0)
then
    modify($b) { setBonusPool($b.getProfit() * 0.05) }
end

rule "Calculate Bonus - High Category"
salience 10
when
    $b : ProjectBudget(category == "HIGH", bonusPool == 0)
then
    modify($b) { setBonusPool($b.getProfit() * 0.15) }
end
```

### Key Points
1. Use salience to order rules (higher = earlier)
2. Check preconditions: `totalRevenue > 0` before using it
3. Check that field hasn't been set: `totalCosts == 0`
4. Always use `$variable.getField()` in modify expressions

## 7. Mutually Exclusive Rules

When only ONE rule should fire for a given fact, ensure conditions don't overlap.

### Pattern: Rating Classification

```drl
rule "Rating Excellent"
when
    $c : Candidate(interviewScore >= 90, rating == null)
then
    modify($c) { setRating("EXCELLENT") }
end

rule "Rating Good"
when
    $c : Candidate(interviewScore >= 75, interviewScore < 90, rating == null)
then
    modify($c) { setRating("GOOD") }
end

rule "Rating Average"
when
    $c : Candidate(interviewScore >= 60, interviewScore < 75, rating == null)
then
    modify($c) { setRating("AVERAGE") }
end

rule "Rating Poor"
when
    $c : Candidate(interviewScore < 60, rating == null)
then
    modify($c) { setRating("POOR") }
end
```

### Key Points
1. Use non-overlapping ranges with explicit bounds
2. Check `rating == null` to prevent re-firing
3. Include both upper and lower bounds where applicable

### Pattern: State-Based Processing

```drl
rule "Approve Permit"
when
    $p : Permit(state == "SUBMITTED", inspectionsPassed > 3)
then
    modify($p) { setState("APPROVED") }
end

rule "Reject Permit"
when
    $p : Permit(state == "SUBMITTED", inspectionsPassed < 1)
then
    modify($p) { setState("REJECTED") }
end

rule "Further Review"
when
    $p : Permit(state == "SUBMITTED")  // Catches remaining cases
then
    modify($p) { setState("UNDER_REVIEW") }
end
```

Use salience to ensure specific rules fire before catch-all rules:
```drl
rule "Approve" salience 100 ...
rule "Reject" salience 100 ...
rule "Further Review" salience 10 ...  // Lower priority, fires last
```

## 8. Common Patterns

### Tiered Rebate

```drl
rule "Gold Member Rebate 20%"
salience 100
when
    $b : Booking(memberTier == "GOLD", totalNights >= 7, rebate == 0)
then
    modify($b) { setRebate(0.20) }
end

rule "Silver Member Rebate 10%"
salience 90
when
    $b : Booking(memberTier == "SILVER", totalNights >= 3, rebate == 0)
then
    modify($b) { setRebate(0.10) }
end

rule "Basic Rebate 5%"
salience 10
when
    $b : Booking(totalNights >= 2, rebate == 0)
then
    modify($b) { setRebate(0.05) }
end
```

### Multi-Condition Approval

```drl
rule "Approve Contract"
when
    $v : VendorContract(
        vendorRating > 4,
        contractYears >= 2,
        orderVolume < 3 * capacity,
        approval == "PENDING"
    )
then
    modify($v) { setApproval("APPROVED") }
end
```

### Validation with Reason

```drl
rule "Invalid Phone"
when
    $ph : PhoneNumber(number != null, number not matches "\\d{10}", valid == null)
then
    modify($ph) {
        setValid(false),
        setFormat("INVALID_FORMAT")
    }
end

rule "Valid Phone"
when
    $ph : PhoneNumber(number != null, number matches "\\d{10}", valid == null)
then
    modify($ph) { setValid(true) }
end
```

### State Transition

```drl
rule "Submit Report"
when
    $r : Report(stage == "DRAFT", hasContent == true)
then
    modify($r) { setStage("SUBMITTED") }
end

rule "Finalize Report"
when
    $r : Report(stage == "SUBMITTED", reviewed == true)
then
    modify($r) { setStage("FINALIZED") }
end
```

## 9. Anti-Patterns and Fixes

### WRONG: Using 'or'/'and' in Constraints

```drl
// WRONG - syntax error
Vehicle(mileage > 100000 or accidentCount > 2)
Machine(temp >= 50 and temp <= 80)
```

```drl
// CORRECT - use || and &&
Vehicle(mileage > 100000 || accidentCount > 2)
Machine(temp >= 50 && temp <= 80)
```

### WRONG: Comma Between Patterns in When Clause

```drl
// WRONG - comma between patterns causes syntax error
when
    $app : Applicant(age >= 18),
    $risk : RiskAssessment(applicantName == $app.name)
```

```drl
// CORRECT - patterns separated by newlines only
when
    $app : Applicant(age >= 18)
    $risk : RiskAssessment(applicantName == $app.name)
```

Remember: Commas separate constraints INSIDE a pattern, NOT between patterns.

### WRONG: Incorrect Setter for 'is' Fields

```drl
// WRONG - field is 'isEligible', setter should be setIsEligible
modify($m) { setEligible(true) }

// CORRECT
modify($m) { setIsEligible(true) }
```

### WRONG: Missing Variable Prefix in Getters

```drl
// WRONG - getSales() is undefined
modify($b) { setTotal(getSales() + getServices()) }

// CORRECT - use $b prefix
modify($b) { setTotal($b.getSales() + $b.getServices()) }
```

### WRONG: Overlapping Rule Conditions

```drl
// WRONG - old vehicle matches both Critical and Good
rule "Critical Condition"
when $v : Vehicle(mileage > 100000 || accidentCount > 2) ...

rule "Good Condition"
when $v : Vehicle(mileage < 50000) ...  // Old car with low mileage matches BOTH!
```

```drl
// CORRECT - explicit exclusion
rule "Critical Condition"
when $v : Vehicle((mileage > 100000 || accidentCount > 2 || maintenanceScore < 3), condition == null) ...

rule "Good Condition"
when $v : Vehicle(mileage < 50000, accidentCount == 0, maintenanceScore >= 4, condition == null) ...
```

### WRONG: Inventing Enumeration Values

```drl
// WRONG - test expects "UNDER_REVIEW" but you used "NEEDS_REVIEW"
modify($p) { setState("NEEDS_REVIEW") }

// CORRECT - use EXACT value from requirements
modify($p) { setState("UNDER_REVIEW") }
```

### WRONG: Using null Check When Input Has String Placeholder

When test input uses a STRING value as a placeholder (like "UNSET", "unknown", "NONE"), check for that EXACT string, not null.

```drl
// Test input: category: "UNSET"

// WRONG - "UNSET" is a string, not null!
$b : Budget(category == null)

// CORRECT - match the actual placeholder string from input
$b : Budget(category == "UNSET")
```

Similarly for other placeholder values:
```drl
// Input has condition: "unknown"
// CORRECT: condition == "unknown"

// Input has state: "PENDING"
// CORRECT: state == "PENDING"
```

**Rule:** Look at the test input JSON. If a field has a string value, check for that exact string.

### WRONG: Negative Check Causing Infinite Loop

When using modify(), the rule condition MUST become false after modification. Use POSITIVE checks for the initial value, not negative checks.

```drl
// Test input: state: "ACTIVE", expected output: state: "EXPIRING"

// WRONG - causes infinite loop!
// After setting "EXPIRING", condition (state != "EXPIRED") is STILL true!
rule "Set Expiring"
when
    $lic : License(daysRemaining <= 30, state != "EXPIRED")
then
    modify($lic) { setState("EXPIRING") }  // Fires repeatedly!
end

// CORRECT - check for the specific initial value
rule "Set Expiring"
when
    $lic : License(daysRemaining <= 30, state == "ACTIVE")
then
    modify($lic) { setState("EXPIRING") }  // Fires once, then state != "ACTIVE"
end
```

**Rule:** Always use POSITIVE equality checks (`state == "ACTIVE"`) for the initial state, not negative checks (`state != "EXPIRED"`). This ensures the rule stops matching after modification.

## 10. Complete Examples

### Example 1: Simple Veteran Status

```drl
package org.drools.generated;

declare Employee
    employeeId : String
    yearsOfService : int
    veteran : boolean
end

rule "Set Veteran True"
when
    $e : Employee(yearsOfService >= 10, veteran == false)
then
    modify($e) { setVeteran(true) }
end

rule "Set Veteran False"
when
    $e : Employee(yearsOfService < 10, veteran == true)
then
    modify($e) { setVeteran(false) }
end
```

### Example 2: Membership Tier with Salience

```drl
package org.drools.generated;

declare MembershipRequest
    annualSpend : double
    loyaltyPoints : int
    accountAge : int
    yearsActive : double
    decision : String
    tier : String
end

rule "Approve Platinum Tier"
salience 100
when
    $req : MembershipRequest(
        annualSpend > 10000,
        loyaltyPoints > 5000,
        yearsActive < 0.5,
        decision == "PENDING"
    )
then
    modify($req) {
        setDecision("APPROVED"),
        setTier("PLATINUM")
    }
end

rule "Approve Gold Tier"
salience 90
when
    $req : MembershipRequest(
        annualSpend > 5000,
        loyaltyPoints > 2000,
        decision == "PENDING"
    )
then
    modify($req) {
        setDecision("APPROVED"),
        setTier("GOLD")
    }
end

rule "Reject - Low Engagement"
salience 80
when
    $req : MembershipRequest(
        loyaltyPoints < 500,
        decision == "PENDING"
    )
then
    modify($req) {
        setDecision("REJECTED"),
        setTier("NONE")
    }
end

rule "Waitlist"
salience 10
when
    $req : MembershipRequest(decision == "PENDING")
then
    modify($req) {
        setDecision("WAITLISTED"),
        setTier("NONE")
    }
end
```

### Example 3: Vehicle Condition Assessment with Exclusive Rules

```drl
package org.drools.generated;

declare Vehicle
    vin : String
    mileage : int
    accidentCount : int
    maintenanceScore : double
    condition : String
end

rule "Critical Condition"
salience 100
when
    $v : Vehicle(
        (mileage > 150000 || accidentCount > 3 || maintenanceScore < 2),
        condition == null
    )
then
    modify($v) { setCondition("CRITICAL") }
end

rule "Fair Condition"
salience 50
when
    $v : Vehicle(
        ((mileage >= 75000 && mileage <= 150000) || (maintenanceScore >= 2 && maintenanceScore <= 3)),
        accidentCount <= 1,
        condition == null
    )
then
    modify($v) { setCondition("FAIR") }
end

rule "Excellent Condition"
salience 10
when
    $v : Vehicle(
        mileage < 75000,
        accidentCount == 0,
        maintenanceScore > 4,
        condition == null
    )
then
    modify($v) { setCondition("EXCELLENT") }
end
```

## Summary: Critical Rules

1. **Always use `&&` and `||`** - never `and` or `or` in constraints
2. **No commas between patterns** - patterns in `when` are separated by newlines, NOT commas
3. **Setter = set + FieldName** - field `isEligible` → `setIsEligible()`
4. **Prefix getters** - use `$var.getField()` in modify expressions
5. **Use salience** - control rule priority (higher fires first)
6. **Non-overlapping conditions** - prevent multiple rules firing
7. **Match input placeholders** - if input has `state: "PENDING"`, check `state == "PENDING"` (not null)
8. **Use exact values** - match test case expectations exactly
9. **Positive initial checks** - use `state == "ACTIVE"` not `state != "DONE"` to prevent infinite loops
10. **Check test input** - look at actual JSON values, check for those exact strings

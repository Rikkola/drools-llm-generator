# qwen2.5-coder Failure Analysis Report

**Model:** qwen2.5-coder:14b-instruct-q4_K_M
**Date:** 2026-03-17
**Result:** 23/29 (79.3%) ✅

## Improvement Summary

| Metric | Before Import Fix | After Import Fix |
|--------|------------------|------------------|
| Pass Rate | 41.4% (12/29) | **79.3% (23/29)** |
| Import-related failures | 15 | **0** |
| Logic-related failures | 2 | 6 |

The import stripping fix (`stripUnnecessaryImports()`) resolved all 15 import-related failures.

---

## Remaining Failures (6 scenarios)

These failures are due to **logic errors** in the generated rules, not import issues.

| # | Scenario | Error Type |
|---|----------|------------|
| 7 | Credit Card Approval | Wrong number of rules fired |
| 11 | Insurance Risk Assessment | Wrong number of rules fired |
| 20 | Priority Assignment | Wrong number of rules fired |
| 22 | Shipping Cost | ERROR: [ERR 102] Line 10:7 mismatched input ' |
| 24 | Simple Calculation | DRL compilation error |
| 28 | Tax Calculation Chain | DRL compilation error |

---

## Detailed Failure Analysis

### 7. Credit Card Approval

**Execution Error:** `Premium Card Approval: Expected 1 rules to fire, but 2 fired`

**Generated DRL:**

```drl
package org.drools.generated;

declare CreditCardApplication
    annualIncome: double
    creditScore: int
    existingDebt: double
    debtToIncomeRatio: double
    decision: String
    cardType: String
end

rule \"Initialize Application\"
when
    $app : CreditCardApplication(decision == null, cardType == null)
then
    modify($app) {
        setDecision(\"PENDING\"),
        setCardType(\"NONE\")
    }
end

rule \"Approve with Premium Card\"
when
    $app : CreditCardApplication(annualIncome > 50000, creditScore > 700, debtToIncomeRatio < 0.4)
then
    modify($app) {
        setDecision(\"APPROVED\"),
        setCardType(\"PREMIUM\")
    }
end

rule \"Approve with Standard Card\"
when
    $app : CreditCardApplication(annualIncome > 30000, creditScore > 600)
then
    modify($app) {
        setDecision(\"APPROVED\"),
        setCardType(\"STANDARD\")
    }
end

rule \"Deny Application\"
when
    $app : CreditCardApplication(creditScore < 500)
then
    modify($app) {
        setDecision(\"DENIED\"),
        setCardType(\"NONE\")
    }
end

rule \"Escalate for Manual Review\"
when
    $app : CreditCardApplication(decision == \"PENDING\")
then
    modify($app) {
        setDecision(\"MANUAL_REVIEW\"),
        setCardType(\"NONE\")
    }
end

```

**Analysis:**
- The DRL compiled successfully but the wrong number of rules fired
- This indicates a logic error in the rule conditions
- Rules may be too broad or too narrow in their conditions

---

### 11. Insurance Risk Assessment

**Execution Error:** `Medium Risk - Age Factor: Expected 1 rules to fire, but 100 fired`

**Generated DRL:**

```drl
package org.drools.generated;

declare Applicant
    age: int
    smoker: boolean
    bmi: double
    riskLevel: String
end

rule \"Initialize Applicant Risk Level\"
when
    $applicant : Applicant(riskLevel == null || riskLevel.equals(\"UNKNOWN\"))
then
    modify($applicant) {
        setRiskLevel(\"UNKNOWN\")
    }
end

rule \"High Risk Assessment\"
when
    $applicant : Applicant(age > 60 || smoker == true || bmi > 30)
then
    modify($applicant) {
        setRiskLevel(\"HIGH\")
    }
end

rule \"Medium Risk Assessment\"
when
    $applicant : Applicant(age >= 40 && age <= 60 || (bmi >= 25 && bmi <= 30))
then
    modify($applicant) {
        setRiskLevel(\"MEDIUM\")
    }
end

rule \"Low Risk Assessment\"
when
    $applicant : Applicant(age < 40 && smoker == false && bmi < 25)
then
    modify($applicant) {
        setRiskLevel(\"LOW\")
    }
end

```

**Analysis:**
- The DRL compiled successfully but the wrong number of rules fired
- This indicates a logic error in the rule conditions

---

### 20. Priority Assignment

**Execution Error:** `High Severity: Expected 1 rules to fire, but 100 fired`

**Analysis:**
- The DRL compiled successfully but the wrong number of rules fired
- This indicates a logic error in the rule conditions

---

### 22. Shipping Cost

**Validation Error:** `ERROR: [ERR 102] Line 10:7 mismatched input '`

**Analysis:**

---

### 24. Simple Calculation

**Execution Error:** `Calculate Tax: Failed to execute DRL with JSON facts: DRL compilation errors: [Message [id=1, kieBase=defaultKieBase, level=ERROR, path=file0.drl, line=10, column=0`

**Analysis:**
- The DRL failed to compile due to syntax errors
- Check for invalid method calls or undefined references

---

### 28. Tax Calculation Chain

**Execution Error:** `Low Tax Bracket: Failed to execute DRL with JSON facts: DRL compilation errors: [Message [id=1, kieBase=defaultKieBase, level=ERROR, path=file0.drl, line=17, column=0`

**Generated DRL:**

```drl
package org.drools.generated;

declare TaxReturn
    taxpayerId: String
    salary: double
    bonuses: double
    investments: double
    dependents: int
    expenses: double
    grossIncome: double
    deductions: double
    netIncome: double
    taxBracket: String
    taxOwed: double
end

rule \"Calculate Gross Income\"
when
    $taxReturn : TaxReturn()
then
    modify($taxReturn) {
        setGrossIncome(getSalary() + getBonuses() + getInvestments())
    }
end

rule \"Calculate Deductions\"
when
    $taxReturn : TaxReturn()
then
    modify($taxReturn) {
        setDeductions((getDependents() * 2000) + getExpenses())
    }
end

rule \"Calculate Net Income\"
when
    $taxReturn : TaxReturn()
then
    modify($taxReturn) {
        setNetIncome(getGrossIncome() - getDeductions())
    }
end

rule \"Determine Tax Bracket LOW\"
when
    $taxReturn : TaxReturn(netIncome < 40000)
then
    modify($taxReturn) {
        setTaxBracket(\"LOW\")
    }
end

rule \"Determine Tax Bracket MEDIUM\"
when
    $taxReturn : TaxReturn(netIncome >= 40000 && netIncome <= 100000)
then
    modify($taxReturn) {
        setTaxBracket(\"MEDIUM\")
    }
end

rule \"Determine Tax Bracket HIGH\"
when
    $taxReturn : TaxReturn(netIncome > 100000)
then
    modify($taxReturn) {
        setTaxBracket(\"HIGH\")
    }
end

rule \"Calculate Tax Owed LOW\"
when
    $taxReturn : TaxReturn(taxBracket == \"LOW\")
then
    modify($taxReturn) {
        setTaxOwed(getNetIncome() * 0.10)
    }
end

rule \"Calculate Tax Owed MEDIUM\"
when
    $taxReturn : TaxReturn(taxBracket == \"MEDIUM\")
then
    modify($taxReturn) {
        setTaxOwed(getNetIncome() * 0.22)
    }
end

rule \"Calculate Tax Owed HIGH\"
when
    $taxReturn : TaxReturn(taxBracket == \"HIGH\")
then
    modify($taxReturn) {
        setTaxOwed(getNetIncome() * 0.32)
    }
end

```

**Analysis:**
- The DRL failed to compile due to syntax errors
- Check for invalid method calls or undefined references

---

## Model Comparison (After All Fixes)

| Model | Pass Rate | Notes |
|-------|-----------|-------|
| qwen3-coder-next | **100%** (29/29) | Best performer |
| granite4:small-h | **79.3%** (23/29) | After markdown extraction fix |
| qwen2.5-coder | **79.3%** (23/29) | After import stripping fix |

## Fixes Applied

### 1. Markdown Extraction Fix
- **Problem:** Models wrap DRL in markdown with explanatory text
- **Solution:** Extract content from within ```drl blocks, ignoring surrounding text
- **Impact:** granite4:small-h improved from 0% to 79.3%

### 2. Import Stripping Fix
- **Problem:** qwen2.5-coder adds unnecessary `import java.*` statements
- **Solution:** Strip all `import java.*` lines from generated DRL
- **Impact:** qwen2.5-coder improved from 41.4% to 79.3%


# Insurance Domain Instructions

These instructions help the AI generate DRL rules that follow insurance industry conventions.

## Key Concepts

### Coverage Levels
- **BASIC**: Minimal coverage, lowest premiums
- **STANDARD**: Balanced coverage for most customers
- **PREMIUM**: Comprehensive coverage, highest premiums

### Risk Assessment
- Risk factors are calculated based on: age, health score, lifestyle factors
- Risk factor scale: 0.5 (very low risk) to 3.0 (very high risk)
- Average risk factor is 1.0

### Applicant Classification
- **MINOR**: Age < 18 (cannot be approved)
- **YOUNG_ADULT**: Age 18-25 (higher risk)
- **ADULT**: Age 26-64 (standard risk)
- **SENIOR**: Age 65+ (elevated risk)

## Calculation Rules

### Premium Calculation
```
finalPremium = basePremium * riskFactor
```

Where basePremium depends on coverage level:
- BASIC: $100/month
- STANDARD: $200/month
- PREMIUM: $400/month

### Risk Factor Calculation
```
riskFactor = baseRiskByAge + healthAdjustment + lifestyleAdjustment
```

Typical adjustments:
- Smoking: +0.5
- Regular exercise: -0.2
- Pre-existing conditions: +0.3 to +1.0

## Important Business Constraints

1. **Age Restriction**: Never approve coverage for applicants under 18
2. **Manual Review**: High-risk applicants (factor > 2.0) require manual review
3. **Maximum Premium**: Final premium cannot exceed $2000/month
4. **Rounding**: Always round premiums to 2 decimal places
5. **Health Score**: Must be between 0 and 100

## Rule Naming Conventions

Use descriptive names following this pattern:
- `Classify [Entity] by [Criteria]` - for classification rules
- `Calculate [Field] for [Entity]` - for calculation rules
- `Validate [Constraint]` - for validation rules
- `Flag [Condition]` - for flagging/alerting rules

Examples:
- "Classify Applicant by Age"
- "Calculate Risk Factor"
- "Validate Minimum Age"
- "Flag High Risk Applicant"

## Fact Type Guidelines

### Applicant
Should include: name, age, healthScore, smoker (boolean), exercisesRegularly (boolean)

### Policy
Should include: policyId, coverageLevel, basePremium, riskFactor, finalPremium, approved (boolean), requiresReview (boolean)

### RiskAssessment
Should include: applicantId, riskFactor, riskCategory (LOW, MEDIUM, HIGH, VERY_HIGH)

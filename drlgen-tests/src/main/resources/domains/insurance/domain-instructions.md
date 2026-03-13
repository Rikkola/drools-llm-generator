# Insurance Domain Instructions

This document provides domain-specific rules and constraints for generating DRL rules in the insurance domain.

## Business Context

Insurance rules must handle policy management, claim processing, premium calculations, and risk assessment.

## Domain-Specific Terminology

| Term | Definition |
|------|------------|
| Premium | The amount paid for insurance coverage |
| Claim | A request for payment under the policy terms |
| Deductible | Amount the policyholder pays before insurance covers the rest |
| Coverage | The scope of protection provided by the policy |
| Underwriting | Process of evaluating risk to determine premium |

## Required Field Naming Conventions

- Policy identifiers: `policyNumber`, `policyId`
- Claim identifiers: `claimNumber`, `claimId`
- Status fields: Use values like `PENDING`, `APPROVED`, `REJECTED`, `UNDER_REVIEW`
- Amount fields: Use `double` type for monetary values

## Business Rules Patterns

### Claim Processing Rules

1. **Auto-approve small claims**: Claims under $500 with no prior claims in 12 months should be auto-approved
2. **Flag high-value claims**: Claims over $10,000 require manual review
3. **Fraud detection**: Multiple claims in 30 days should trigger investigation

### Premium Calculation Rules

1. **Base premium**: Start with base rate based on coverage type
2. **Risk factors**: Apply multipliers for:
   - Age (higher for young/elderly drivers)
   - Location (higher for high-risk areas)
   - Claims history (higher for frequent claimers)
3. **Discounts**: Apply for:
   - Multi-policy bundles (10-15%)
   - Safe driver (5-10%)
   - Long-term customer (5%)

### Policy Status Transitions

Valid state transitions:
- `DRAFT` → `PENDING_APPROVAL`
- `PENDING_APPROVAL` → `ACTIVE` | `REJECTED`
- `ACTIVE` → `SUSPENDED` | `CANCELLED` | `EXPIRED`
- `SUSPENDED` → `ACTIVE` | `CANCELLED`

## Validation Requirements

1. Policy numbers must match pattern: `POL-[A-Z]{2}-[0-9]{6}`
2. Claim amounts must be positive and not exceed coverage limit
3. Dates must be valid (claim date cannot be before policy start date)

## Example Rule Patterns

### Claim Auto-Approval
```
IF claim.amount < 500
   AND policy.claimsInLast12Months == 0
   AND claim.status == "PENDING"
THEN set claim.status = "APPROVED"
```

### Risk Category Assignment
```
IF driver.age < 25 OR driver.age > 70
   OR driver.accidentsInLast3Years > 0
THEN set policy.riskCategory = "HIGH"
ELSE set policy.riskCategory = "STANDARD"
```

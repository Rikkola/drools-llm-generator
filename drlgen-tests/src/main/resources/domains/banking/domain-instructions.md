# Banking Domain Instructions

This document provides domain-specific rules and constraints for generating DRL rules in the banking domain.

## Business Context

Banking rules handle account management, loan processing, transaction validation, fraud detection, and regulatory compliance.

## Domain-Specific Terminology

| Term | Definition |
|------|------------|
| KYC | Know Your Customer - identity verification process |
| AML | Anti-Money Laundering - fraud prevention regulations |
| Credit Score | Numerical rating of creditworthiness (300-850) |
| DTI | Debt-to-Income ratio - monthly debt payments / gross monthly income |
| APR | Annual Percentage Rate - yearly interest rate |
| Principal | Original loan amount |

## Required Field Naming Conventions

- Account identifiers: `accountNumber`, `accountId`
- Transaction identifiers: `transactionId`, `referenceNumber`
- Status fields: Use values like `PENDING`, `APPROVED`, `REJECTED`, `FLAGGED`, `COMPLETED`
- Amount fields: Use `double` type for monetary values
- Score fields: Use `int` type for credit scores

## Business Rules Patterns

### Loan Approval Rules

1. **Credit score thresholds**:
   - Excellent (750+): Auto-approve up to $50,000
   - Good (700-749): Auto-approve up to $25,000
   - Fair (650-699): Requires manual review
   - Poor (<650): Auto-reject

2. **DTI requirements**:
   - DTI must be below 0.43 (43%) for approval
   - DTI below 0.36 (36%) qualifies for better rates

3. **Employment verification**:
   - Minimum 2 years at current employer for auto-approval
   - Self-employed requires 3 years of tax returns

### Transaction Monitoring Rules

1. **Large transaction alerts**: Flag transactions over $10,000
2. **Velocity checks**: More than 5 transactions in 1 hour triggers review
3. **Geographic anomalies**: Transaction from new country requires verification
4. **Round amount patterns**: Multiple round-number transactions may indicate structuring

### Account Status Transitions

Valid state transitions:
- `PENDING_VERIFICATION` → `ACTIVE` | `SUSPENDED`
- `ACTIVE` → `FROZEN` | `CLOSED`
- `FROZEN` → `ACTIVE` | `CLOSED`
- `SUSPENDED` → `ACTIVE` | `CLOSED`

## Validation Requirements

1. Account numbers must be exactly 10 digits
2. Credit scores must be between 300 and 850
3. Loan amounts must be positive
4. DTI ratios must be between 0 and 1

## Example Rule Patterns

### Loan Auto-Approval
```
IF applicant.creditScore >= 750
   AND applicant.dtiRatio < 0.36
   AND loan.amount <= 50000
   AND application.status == "PENDING"
THEN set application.status = "APPROVED"
     set application.interestRate = 5.5
```

### Suspicious Transaction Detection
```
IF transaction.amount > 10000
   AND transaction.type == "WITHDRAWAL"
   AND account.averageBalance < transaction.amount * 2
THEN set transaction.status = "FLAGGED"
     set transaction.reviewReason = "LARGE_WITHDRAWAL"
```

### Account Overdraft Prevention
```
IF withdrawal.amount > account.availableBalance
   AND account.overdraftProtection == false
THEN set withdrawal.status = "REJECTED"
     set withdrawal.rejectionReason = "INSUFFICIENT_FUNDS"
```

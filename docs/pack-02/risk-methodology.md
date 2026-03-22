# Risk Methodology

This document refines the risk-assessment method used for EduSecure so the register can be presented in a more report-ready form.

## 1. Chosen basis

EduSecure will use a simplified **NIST SP 800-30-style** approach because it is structured, recognisable, and suitable for a study project.

The workflow is:

1. identify assets
2. identify vulnerabilities
3. identify threat events
4. estimate likelihood
5. estimate impact
6. prioritise treatment
7. justify mitigations
8. record residual limitations

## 2. Assessment criteria

## Likelihood scale

| Level | Meaning |
|---|---|
| Low | unlikely in the intended study-project environment, or requires strong preconditions |
| Medium | plausible under realistic misuse or weak control conditions |
| High | directly supported by the case-study incidents or likely if the control is absent |

## Impact scale

| Level | Meaning |
|---|---|
| Low | limited academic, operational, or privacy harm |
| Medium | meaningful service or data harm affecting one role or process |
| High | serious privacy, integrity, or trust damage affecting sensitive records or core platform functions |

## Priority rule

Priority is assigned qualitatively using the combination of likelihood and impact.

| Likelihood | Impact | Priority |
|---|---|---|
| High | High | Critical |
| High | Medium | High |
| Medium | High | High |
| Medium | Medium | Medium |
| Low | High | Medium |
| Low | Medium | Low |
| Low | Low | Low |

## 3. EduSecure-specific threat context

The case study already indicates several serious threat conditions:
- plaintext passwords
- no HTTPS
- token interception over public Wi-Fi
- SQL injection exposure
- assignment tampering
- missing logging and verification for sensitive actions

Because these are explicitly stated in the brief, risks connected to them should not be rated unrealistically low.

## 4. Propositional-logic format

To strengthen the formal-reasoning requirement, each major mitigation should be expressed in three parts:

1. **symbol definition**
2. **formal statement**
3. **plain-English interpretation**

## Example template

- `P`: passwords stored in plaintext
- `D`: database compromise occurs
- `C`: attacker learns reusable user credentials
- `B`: passwords stored with bcrypt

Formal statements:
- `(D ∧ P) -> C`
- `(D ∧ B) -> ¬C_plaintext`

Plain-English interpretation:
- if the database is compromised and passwords are stored in plaintext, credential disclosure follows directly
- if the database is compromised but passwords are stored using bcrypt, the attacker does not obtain plaintext credentials directly, so the risk is materially reduced

## 5. Scope note on formal reasoning

The assignment asks for a **brief justification using propositional logic**, not a full formal-verification exercise. Therefore, the logic should be:
- correct
- concise
- clearly tied to the actual mitigation
- understandable to a marker without extra assumptions

## 6. Residual-risk principle

Even after mitigation, residual risk remains. EduSecure documentation should explicitly acknowledge this, for example:
- `bcrypt` reduces direct credential disclosure but does not stop phishing
- `TLS` reduces interception risk but not endpoint compromise
- digital signatures support authorship proof but depend on proper private-key protection

## 7. Reporting standard for the refined register

Each major risk entry should therefore contain:
- asset
- vulnerability
- threat event
- likelihood
- impact
- priority
- selected controls
- formal logic statement
- plain-English reasoning
- residual risk note

This structure will make the risk section more academically robust and easier to defend in the report.


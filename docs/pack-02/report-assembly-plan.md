# Report Assembly Plan

This document translates the assignment brief into a practical report-building structure.

## 1. Report objective

The report should demonstrate that EduSecure is a well-justified cryptography case study, not merely a software prototype.

## 2. Recommended report structure

## Section 1. Introduction and case-study framing
Include:
- brief explanation of EduSecure and the education-platform scenario
- summary of the security incidents from the brief
- statement of report aims

## Section 2. Role of cryptography in EduSecure
Include:
- confidentiality, integrity, authenticity, and limited availability discussion
- symmetric vs asymmetric approaches
- hashes, MACs, digital signatures
- benefits, limitations, and practical usage

## Section 3. Risk assessment
Include:
- chosen standard: simplified NIST SP 800-30-style assessment
- asset identification
- threat and vulnerability discussion
- prioritised risk register
- propositional logic justifications for mitigation entries

## Section 4. Secure system design
Include:
- use case diagram
- deployment diagram: insecure vs secure
- class diagram
- key sequence diagrams: insecure vs secure flows
- explanation of trust boundaries and RBAC

## Section 5. Cryptographic controls and algorithm selection
Include:
- algorithm comparison table
- chosen controls and justification
- why non-selected options were not chosen as primary implementation controls

## Section 6. Implementation plan and considerations
Include:
- planned Java/Spring libraries
- password hashing approach
- AES-GCM usage
- RSA signature workflow
- HMAC usage
- random number generation
- IV/nonce handling
- key distribution and storage assumptions

## Section 7. CIA evaluation
Include:
- control-by-control CIA analysis
- residual risks
- limitations of the study-project scope

## Section 8. Technical artefact summary
Include:
- what was implemented
- how to run or demonstrate it
- screenshots or sample outputs if useful
- how the artefact supports the report claims

## Section 9. Conclusion
Include:
- recap of the chosen security approach
- summary of residual limitations
- concise final evaluation

## 3. Word-count guidance

For a target of approximately 2,500 words, a practical distribution could be:

| Section | Suggested emphasis |
|---|---|
| Introduction | brief |
| Role of cryptography | medium |
| Risk assessment | high |
| Secure system design | high |
| Cryptographic control selection | high |
| Implementation considerations | medium |
| CIA evaluation | medium |
| Artefact summary + conclusion | brief-medium |

## 4. Title page checklist

The brief requires:
- full name
- student number
- module code
- assignment title

## 5. Citation and evidence plan

The report should use:
- scholarly and official sources
- consistent citation style
- citations for algorithm properties, standards, and best-practice claims

Recommended source types:
- NIST publications
- official Spring / Java security documentation
- recognised cryptography texts or peer-reviewed papers

## 6. Artefact evidence checklist

The code artefact should provide evidence for at least three cryptographic capabilities. The report should therefore capture:
- code screenshots where helpful
- sample request/response or console output
- signature verification evidence
- encryption/decryption evidence
- audit/integrity evidence

Current evidence already available in the repository:
- `backend/src/test/java/edusecure/edusecure/EduSecureApplicationTests.java`
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`

These currently provide runnable evidence for:
- public health endpoint verification
- registration with bcrypt-backed password handling
- login and JWT issuance
- authenticated `/api/auth/me` access

## 7. Presentation risks to avoid

- describing implementation ideas without linking them to the brief
- claiming JWT or generic login alone is sufficient cryptography coverage
- focusing too much on web-app features instead of cryptographic reasoning
- presenting diagrams without explaining what security point they prove
- failing to discuss limitations and residual risk

## 8. Final submission checklist

Before submission, confirm:
- the report includes the required title page details
- the report cites sources consistently
- the artefact is readable and well commented
- the report and artefact tell the same security story
- screenshots are included if they improve clarity
- the implementation demonstrates at least three required cryptographic techniques


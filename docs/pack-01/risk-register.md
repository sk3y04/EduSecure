# Initial Risk Register

This initial register uses a simple **NIST SP 800-30 style** structure: asset, vulnerability, threat event, likelihood, impact, priority, and proposed mitigation.

Likelihood and impact values are qualitative at this stage and may be refined later.

## Risk scale

- **Likelihood:** Low / Medium / High
- **Impact:** Low / Medium / High
- **Priority:** Low / Medium / High / Critical

## Risk entries

| ID | Asset | Vulnerability | Threat event | Likelihood | Impact | Priority | Proposed mitigation | Logic-style justification |
|---|---|---|---|---|---|---|---|---|
| R1 | User credentials | Passwords stored in plaintext | database exposure reveals reusable user passwords | High | High | Critical | Store passwords with `bcrypt`; never store plaintext or reversible passwords | `bcryptUsed ∧ plaintextNotStored -> lower(credentialDisclosureRisk)` |
| R2 | Session tokens | Unencrypted transport / insecure Wi-Fi exposure | attacker intercepts token during login or API use | High | High | Critical | Document and enforce `HTTPS/TLS`; use token expiry; avoid sending sensitive data in token claims | `TLS ∧ shortTokenLifetime -> lower(tokenInterceptionRisk)` |
| R3 | Exam results and grade data | unsafe query handling / weak input handling | attacker exploits injection to read or alter grade data | Medium | High | High | Use Spring Data JPA, validation, and parameterised persistence; restrict data exposure by role | `validatedInput ∧ parameterizedAccess -> lower(sqlInjectionRisk)` |
| R4 | Assignment submission files | no integrity/authorship protection | submitted file is tampered with before lecturer review | High | High | Critical | Hash submission and verify digital signature using student public key | `hashVerified ∧ signatureVerified -> lower(submissionTamperRisk)` |
| R5 | Grade updates | no integrity verification in transit or processing | grade values are changed in transit or through unauthorised modification | Medium | High | High | TLS for transport; server-side role checks; hash/HMAC-backed audit records for changes | `TLS ∧ roleChecks ∧ auditIntegrity -> lower(gradeTamperRisk)` |
| R6 | Sensitive actions log | no auditability or tamper evidence | grade changes occur without trustworthy traceability | High | High | Critical | Create audit records for create/update/delete of grades and other sensitive actions; protect integrity of records with chained hash/HMAC approach | `auditRecorded ∧ auditIntegrity -> lower(nonRepudiationGap)` |
| R7 | Student identity and authorship claims | no digital proof of authorship | student denies authorship or another actor submits on their behalf | Medium | High | High | Use digital signatures for submission authorship verification | `privateKeyControl ∧ signatureVerification -> stronger(authorshipProof)` |
| R8 | Personal records | weak role separation | students or unauthorised users access restricted records | Medium | High | High | Enforce role-based access control for Student, Lecturer, Admin; minimise returned fields in DTOs | `rbacEnforced ∧ leastPrivilege -> lower(unauthorisedAccessRisk)` |
| R9 | Cryptographic keys | poor key handling or hardcoded secrets | compromise of signing or token secrets undermines controls | Medium | High | High | Use env-based secrets in development; separate signing material; document key-rotation assumptions | `secretExternalized ∧ keySeparation -> lower(keyExposureRisk)` |
| R10 | Overall artefact quality | over-complex architecture not justified by brief | implementation becomes inconsistent, weakly documented, or unfinished | High | Medium | High | Keep scope small; prioritise traceability, UML, risk alignment, and only required features | `boundedScope ∧ documentedChoices -> higher(deliverableCompleteness)` |

## Notes on risk-treatment boundaries

### Cryptography helps directly with:

- password protection at rest
- confidentiality of files/messages
- integrity checking
- digital proof of authorship
- tamper-evident auditing

### Cryptography does not directly solve:

- SQL injection by itself
- poor access-control design by itself
- weak software quality or uncontrolled scope by itself

These must be addressed with secure coding, validation, and disciplined design.

## Priority focus for the first implementation phases

The highest-value risks to address first are:

1. `R1` plaintext password storage
2. `R2` token interception / lack of transport security
3. `R4` assignment tampering
4. `R6` missing trustworthy audit trail

These risks also map most clearly to the assignment's cryptographic objectives.


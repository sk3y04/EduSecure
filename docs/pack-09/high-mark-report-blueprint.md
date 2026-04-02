# High-Mark Report Blueprint

This document is a **final writing blueprint** for the EduSecure report.

Its purpose is to help the final submission score as highly as possible by ensuring that:
- each section answers the assignment brief directly
- each claim is backed by repository evidence
- the strongest implemented features are emphasized
- the report stays technically honest and avoids overclaiming

Use this document together with:
- `docs/assignment_brief.md`
- `docs/pack-09/report-section-to-evidence-map.md`
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`
- `docs/pack-09/report-claims-audit-note.md`
- `docs/pack-09/final-doc-alignment-summary.md`
- `docs/pack-09/final-submission-checklist.md`
- `docs/pack-09/final-report-draft-sections-1-to-5.md`
- `docs/pack-09/final-report-draft-sections-6-to-9.md`

## 1. High-level writing strategy

### Core principle
The report should read as a **cryptography case study supported by implementation evidence**, not as a generic software-project write-up.

### Strongest current EduSecure themes
These are the themes that should carry the report:
- plaintext password mitigation using `bcrypt`
- stronger authentication through optional TOTP-based MFA
- submission integrity/authorship evidence using `SHA-256` and ECC-based signing/verification logic
- submission confidentiality-at-rest using `AES-GCM`
- grade integrity and accountability using verified-submission-only grading plus HMAC-backed audit evidence
- a separate AES-GCM demo that proves symmetric encryption independently of the business flow

### Best evidence order for major claims
For important statements, cite in this order where possible:
1. implementation-status/evidence note
2. concrete backend class or endpoint
3. integration test proving the behavior
4. UML/design artefact as supporting explanation

### Word-count emphasis for a 2,500-word report
Recommended approximate weighting:
- Introduction: 150–200
- Role of cryptography: 350–450
- Risk assessment: 350–450
- Secure system design: 400–500
- Cryptographic control selection: 400–500
- Implementation considerations: 250–350
- CIA evaluation: 250–300
- Artefact summary: 200–250
- Conclusion: 100–150

## 2. Section-by-section blueprint

## Section 1. Introduction and case-study framing

### Purpose
Open the report by making it clear that EduSecure is a response to the exact incidents in the assignment brief.

### What to write
- Introduce EduSecure as an online education platform handling credentials, submissions, grades, and sensitive academic records.
- Summarize the brief’s main failures:
  - plaintext passwords
  - unencrypted communication
  - assignment tampering
  - grade alteration
  - lack of logging/verifiability
- State the report aim clearly:
  - analyze the risks
  - justify cryptographic controls
  - show how the implemented artefact demonstrates them

### Main evidence to cite
- `docs/assignment_brief.md`
- `docs/pack-01/assignment-traceability.md`

### Outstanding-band differentiation
Do not write a generic intro about “cybersecurity is important.”
Instead, make the opening immediately incident-driven and asset-driven.

### Caution
Do not spend too much word count here.

## Section 2. Role of cryptography in EduSecure

### Purpose
Answer the brief’s “role of cryptography” requirement with direct relevance to the case study.

### What to write
Structure this section by control type and security goal:

#### A. Password hashing
- explain why plaintext storage is unacceptable
- explain why password hashing is different from general hashing
- position `bcrypt` as the correct control for stored password verifiers

#### B. Symmetric encryption
- explain the role of symmetric encryption in confidentiality
- explain that TLS 1.3 (via Certbot/Let's Encrypt) handles secure transmission in deployment — this is the correct, complete solution
- explain why `AES-GCM` matters because it adds authenticated encryption
- connect `AES-GCM` to the two real business roles in EduSecure: MFA secret protection at rest and submission content encryption at rest

#### C. Asymmetric cryptography and signatures
- explain RSA/ECC as asymmetric approaches
- explain why signatures matter more than public-key encryption in this case study
- connect signatures to authorship and tamper-evidence for submissions

#### D. Hashes and MACs
- explain the difference between a digest and authenticated integrity
- connect `SHA-256` to submission digests
- connect `HMAC-SHA-256` to audit integrity protection

#### E. MFA as applied cryptography
- explain that TOTP strengthens authentication integrity
- clarify that MFA complements password hashing rather than replacing it

### Main evidence to cite
- `docs/pack-02/crypto-decision-matrix.md`
- `docs/pack-02/cia-evaluation.md`
- `docs/pack-03/mfa-cryptography-implementation.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`

### Strong repo examples to mention
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`
- `backend/src/main/java/edusecure/edusecure/service/crypto/AesRsaCryptoService.java`
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`

### Outstanding-band differentiation
Explicitly compare **what each primitive is for** and **what it is not for**.
Example distinctions to make:
- `bcrypt` is for password hashing, not file integrity
- `SHA-256` alone supports digesting, not authenticated sender proof
- `HMAC` is stronger than plain hashing for internal audit integrity
- JWT supports stateless sessions, but is not encryption

### Caution
Do not blur:
- JWT and encryption
- TLS (transport) and AES-GCM (application-layer at-rest encryption)
- digest and authorship proof

## Section 3. Risk assessment

### Purpose
Show a structured, recognised assessment rather than an informal list of issues.

### What to write
Use a simplified NIST SP 800-30-style approach and explain the steps briefly:
1. identify assets
2. identify vulnerabilities
3. identify threat events
4. estimate likelihood
5. estimate impact
6. prioritize treatment
7. justify mitigations
8. acknowledge residual risk

Then cover the highest-value assets:
- user credentials
- session/authentication state
- submissions
- grades
- audit records

Then present the main risks such as:
- credential disclosure from plaintext storage
- token/session interception over insecure transport
- assignment tampering
- weak authorship proof
- untraceable grade changes
- excessive scope/complexity as an availability risk

### Main evidence to cite
- `docs/pack-02/risk-methodology.md`
- `docs/pack-02/risk-register-refined.md`
- `docs/pack-01/risk-register.md`

### Formal-logic requirement
For each major mitigation, include a concise proposition set.
Use the model already established in your docs.

Example format:
- `P`: passwords stored in plaintext
- `D`: database compromise occurs
- `C`: attacker learns reusable credentials
- `(D ∧ P) -> C`
- `B`: passwords stored using bcrypt
- `(D ∧ B) -> ¬C_plaintext`

### Outstanding-band differentiation
For each major risk, add a one-line residual-risk statement.
That makes the section sound analytical rather than simplistic.

### Caution
Do not score explicit brief incidents unrealistically low.

## Section 4. Secure system design

### Purpose
Show how the insecure case is transformed into a more secure design.

### What to write
Present this section as a design story with selective UML support.

Recommended flow:
1. insecure baseline from the brief
2. secure auth design
3. secure submission design
4. secure grade-integrity design
5. TLS deployment via Certbot/Let's Encrypt

### Diagrams to emphasize
Use only the diagrams that help marks directly:
- `docs/pack-02/uml/sequence-login-secure.puml`
- `docs/pack-02/uml/sequence-submission-secure.puml`
- `docs/pack-04/uml/class-diagram-submission-addendum.puml`
- `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- one insecure/secure deployment comparison from `docs/pack-02/uml/`
- ~~`docs/pack-05/uml/sequence-aes-secure-transmission-demo.puml`~~ *(superseded — AES demo removed; TLS via Certbot/Let's Encrypt handles transmission)*

### Main evidence to cite
- `docs/pack-09/final-doc-alignment-summary.md`
- `docs/pack-09/uml-refresh-assessment.md`
- `docs/pack-06/submission-content-protection-and-retrieval.md`

### Key design points to explain
#### Auth
- `bcrypt`-protected password verification
- JWT only after successful authentication
- `HttpOnly` cookie transport for browser-facing session use
- MFA branch before authenticated session establishment

#### Submission
- backend digest generation
- backend signature generation/verification in the current simulated model
- encrypted-at-rest content storage
- metadata/content endpoint separation
- audit on content retrieval

#### Grade integrity
- only verified submissions may be graded
- create/update actions always write audit entries
- students can view only their own grades

#### TLS deployment
- enforced via Certbot/Let's Encrypt on deployment
- protects all client-server traffic in transit
- satisfies the "secure file/message transmission" artefact requirement through real infrastructure rather than a standalone demo

### Outstanding-band differentiation
When discussing each diagram, say what security property it proves.
Do not just insert diagrams without interpretation.

### Safe wording
> The UML artefacts represent the security design and main interaction logic of EduSecure, while implementation-specific refinements are corroborated by the implementation evidence notes and automated tests.

### Caution
Do not say the UML exactly mirrors every field/endpoint in the final code.

## Section 5. Cryptographic controls and algorithm selection

### Purpose
This is one of the most mark-dense sections. It should show comparison, selection, and justification.

### What to write
Use a compare-and-select structure.

#### A. Password storage
- compare `bcrypt` against plain `SHA-256`
- justify `bcrypt` because it is designed for password hashing and directly addresses plaintext storage risk

#### B. Symmetric encryption
- compare AES generally, then justify `AES-GCM`
- explain why authenticated encryption is better suited than insecure or easier-to-misuse alternatives

#### C. Asymmetric signing choice
- compare RSA vs ECC
- explain why ECC was chosen for the implemented artefact and compare it against RSA clearly
- explain that the implemented submission-signature flow now uses a stable configured demo ECC keypair for repeatable evidence, while still remaining a study-project simulation
- still acknowledge RSA as a viable comparison point even though ECC is the implemented choice

#### D. Hashing and MACs
- explain the separate roles of `SHA-256` and `HMAC-SHA-256`
- position `SHA-256` as digest evidence
- position `HMAC` as internal authenticated integrity for audit logs

#### E. TLS
- explain it as the secure transport design control
- clarify that repository evidence is design-oriented rather than deployment-proven

### Main evidence to cite
- `docs/pack-02/crypto-decision-matrix.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`
- `docs/pack-09/report-claims-audit-note.md`

### Outstanding-band differentiation
Include not only “why chosen” but “why not chosen as primary implementation control.”
That academic contrast reads strongly.

### Caution
Keep claim wording bounded:
- do not say the ECC flow is enterprise PKI
- do not conflate TLS (infrastructure) with application-layer AES-GCM encryption
- TLS is deployment-proven via Certbot/Let's Encrypt and can be stated as implemented

## Section 6. Implementation plan and considerations

### Purpose
Demonstrate practical cryptographic engineering judgement.

### What to write
Cover these topics directly:

#### Libraries/frameworks
- Spring Boot
- Spring Security
- Validation
- JPA
- Liquibase
- Java crypto APIs

#### Randomness
- explain `SecureRandom`
- connect it to nonces, keys, and secure secret generation

#### Nonce/IV handling
- explain why GCM nonce reuse is catastrophic
- explain that fresh nonces are generated for encryption operations

#### Key handling assumptions
Differentiate the key types:
- JWT secret
- MFA secret-encryption key
- audit HMAC secret
- submission storage master/wrapping material

#### Secure error handling
- no secret leakage in API responses
- validation vs auth failures handled cleanly

#### Persistence and schema delivery
- Liquibase for versioned schema delivery
- PostgreSQL as intended runtime database
- focused real-PostgreSQL smoke verification

### Main evidence to cite
- `docs/pack-02/implementation-plan-and-considerations.md`
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`
- `docs/pack-09/postgresql-setup-and-security.md`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

### Outstanding-band differentiation
Be explicit about trade-offs:
- why a bounded implementation is more reliable than overengineering
- why externalized secrets matter even if demo defaults exist
- why a focused PostgreSQL smoke test is valuable even if the fast suite still uses H2

### Caution
Do not present development default secrets as production-safe.
Say they are externalizable and that secure deployment would require environment-managed values.

## Section 7. CIA evaluation

### Purpose
Evaluate the selected controls critically instead of treating CIA as a checklist.

### What to write
A strong structure is control-by-control:
- what is protected
- how it is protected
- which CIA property is mainly improved
- what limitation remains

### Recommended control set to evaluate
- `bcrypt`
- TOTP-based MFA
- `TLS 1.3` in the design narrative
- `AES-GCM` demo
- submission AES-at-rest protection
- `SHA-256` digests
- ECC-based signature workflow
- `HMAC-SHA-256` audit integrity
- RBAC and audited sensitive actions

### Main evidence to cite
- `docs/pack-02/cia-evaluation.md`
- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `docs/pack-07/grade-phase-status-and-evidence.md`
- `docs/pack-08/aes-demo-phase-status-and-evidence.md`

### Outstanding-band differentiation
State clearly that:
- **Integrity** is the strongest and most central property in the implemented artefact
- **Confidentiality** is materially improved through password/MFA secret protection and AES use
- **Availability** is adequate but intentionally bounded rather than enterprise-grade

### Caution
Do not try to pretend that availability is fully solved through cryptography alone.

## Section 8. Technical artefact summary

### Purpose
Show that the code artefact implements the cryptographic discussion in a concrete, testable way.

### What to write
Present the artefact as a set of evidenced capability slices.

#### Slice 1: auth and MFA hardening
- registration/login
- `bcrypt`
- cookie-backed auth session
- TOTP MFA with recovery codes

#### Slice 2: submission integrity and confidentiality
- assignment creation
- submission digest/signature workflow
- verification-state persistence
- AES-GCM encrypted-at-rest storage
- metadata/content split
- audited content retrieval

#### Slice 3: grade integrity
- create/update grade
- verified-submission-only rule
- own-grade student retrieval
- HMAC-backed audit trail

#### Slice 4: TLS secure transmission
- all client-server traffic encrypted via TLS 1.3 (Certbot/Let's Encrypt)
- satisfies "secure file/message transmission" artefact requirement
- no standalone demo endpoint needed — real infrastructure deployment is the evidence

#### Slice 5: delivery evidence
- Liquibase schema
- PostgreSQL smoke verification
- frontend MVP as presentation support

### Main evidence to cite
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `docs/pack-07/grade-phase-status-and-evidence.md`
- `docs/pack-09/appendix-cicd-and-deployment-plan.md`
- `frontend/README.md`

### Best tests to mention explicitly
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

### Suggested screenshot/output picks
Choose only a few strong, readable items:
1. login success or MFA challenge/result
2. submission response showing `hashDigest`, `digitalSignature`, `verificationStatus`
3. content retrieval response from `/api/submissions/{submissionId}/content`
4. grade create/update response
5. browser showing HTTPS padlock / Certbot certificate proof
6. one short test result summary or IDE/terminal proof that tests pass

### Outstanding-band differentiation
The artefact section should emphasize that the project contains **multiple implemented cryptographic control types** and not just a single isolated demo.

### Caution
Do not let the artefact section become a full API manual.
Keep it evidence-focused.

## Section 9. Conclusion

### Purpose
End with a sober, technically mature evaluation.

### What to write
- summarize the main cryptographic improvements
- state that EduSecure now addresses the core brief incidents more effectively
- acknowledge that the artefact is bounded and educational rather than production-complete
- state the main remaining limitations:
  - simulated signing model
  - no full audit-review UI/API
  - bounded frontend scope
  - design-level rather than deployment-proven TLS narrative
  - focused PostgreSQL verification rather than full operational maturity

### Main evidence to cite
- `docs/pack-09/final-cryptography-claims-matrix.md`
- `docs/pack-09/report-claims-audit-note.md`

### Outstanding-band differentiation
A strong conclusion sounds confident **and** constrained.
That balance reads more professionally than exaggerated certainty.

## 3. Recommended evidence appendix structure

If you include appendix/support material, keep it concise.

### Appendix A. Endpoint/test evidence
- selected request/response examples
- selected screenshots
- short explanation of what each proves

### Appendix B. UML/design support
- a small number of exported diagrams
- one sentence per diagram explaining its relevance

### Appendix C. Implementation/deployment support
- Liquibase/PostgreSQL note
- optional frontend MVP note
- optional CI/CD/deployment ideas framed as support/future direction only

## 4. Safe wording patterns for top-band quality

### Strong safe patterns
- "EduSecure implements..."
- "The repository evidences..."
- "Within the study-project scope..."
- "The implemented backend demonstrates..."
- "This control materially reduces the risk of..."
- "Residual risk remains because..."

### Avoid these weak/unsafe patterns
- "fully secure"
- "unhackable"
- "production-ready" unless separately proven
- "complete non-repudiation" for the simulated signing model
- "database encrypted" unless you mean a specifically evidenced application-layer secret or content protection path

## 5. Final marker-facing differentiators

If the report is written well, these are the things most likely to make it stand out:
- clear incident-to-control mapping from the brief
- formal but readable risk-methodology and logic treatment
- correct differentiation between hash, HMAC, signature, symmetric encryption, and session tokens
- strong integrity narrative across submissions, grades, and audit records
- honest discussion of residual limitations
- evidence from tests, not just design claims
- disciplined scope control

## 6. Final self-review questions before submission

- Does each major section answer a specific brief requirement directly?
- Does every strong implementation claim point to real code/test/document evidence?
- Are the report’s strongest pages about cryptography and risk reasoning, not generic web-app features?
- Are the diagrams explained rather than merely inserted?
- Are the simulated or bounded parts described honestly?
- Does the artefact section prove at least three techniques clearly? (It should now prove more than that.)
- Does the conclusion sound academically mature rather than exaggerated?

## 7. Best practical writing order

To write efficiently, draft the report in this order:
1. Section 5: cryptographic controls and selection
2. Section 3: risk assessment
3. Section 4: secure system design
4. Section 6: implementation considerations
5. Section 7: CIA evaluation
6. Section 8: artefact summary
7. Section 2: role of cryptography
8. Section 1: introduction
9. Section 9: conclusion

That order usually produces a tighter and less repetitive report because the central reasoning is written first.


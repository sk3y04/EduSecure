# Step-by-Step Report Writing Plan for Sections 1 to 9

This note turns the existing reporting guidance into a practical writing sequence for the final EduSecure report.

Use it together with:
- `docs/07-reporting-presentation/high-mark-report-blueprint.md`
- `docs/07-reporting-presentation/report-section-to-evidence-map.md`
- `docs/07-reporting-presentation/final-report-skeleton.md`
- `docs/07-reporting-presentation/final-report-draft-sections-1-to-5.md`
- `docs/07-reporting-presentation/final-report-draft-sections-6-to-9.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`
- `docs/07-reporting-presentation/report-draft/06-report-core-vs-appendix-evidence-budget.md`
- `docs/07-reporting-presentation/report-draft/17-space-chat-e2ee-cross-section-draft.md`

## 1. How to use this plan

Work in the **recommended drafting order**, not the final report order.

Recommended drafting order:
1. Section 5 — Cryptographic controls and selection justification
2. Section 3 — Risk assessment
3. Section 4 — Secure system design
4. Section 6 — Implementation plan and considerations
5. Section 7 — CIA evaluation
6. Section 8 — Technical artefact summary
7. Section 2 — Role of cryptography in EduSecure
8. Section 1 — Introduction
9. Section 9 — Conclusion

Why this order works:
- Section 5 defines the main control choices and technical comparisons
- Section 3 explains why those controls are needed
- Section 4 shows how they fit together in the system design
- Sections 6 to 8 convert the design into implementation and evidence
- Sections 1, 2, and 9 are easier to write once the core reasoning is stable

## 2. Global drafting rules

Before writing each section, confirm:
- the claim is supported by a repository file, test, or documented design artefact
- the wording does not overclaim production maturity
- cryptography is explained correctly and not confused with authentication tokens or general security features
- each section includes at least one honest residual limitation or scope boundary where appropriate

While drafting, prefer this evidence order:
1. implementation or evidence note
2. concrete backend class, endpoint, or configuration file
3. test or manual-review evidence
4. UML or planning artefact as supporting explanation

Important cross-section reminder:
- treat the implemented space-chat E2EE feature as a major cryptographic slice, not as a footnote; weave it through Sections 2, 4, 5, 6, 7, and 8 rather than trying to invent a separate final report section outside the assignment structure

## 2.1 Visual-evidence rule for all sections

Because the report must stay within the word limit while still showing diagrams, tables, code artefacts, and frontend evidence, each section should reserve explicit placeholders for visuals during drafting.

Use this rule:
- add the placeholder immediately after the paragraph that introduces the claim
- keep only the strongest visual in the main body unless the section is design-heavy or evidence-heavy
- move secondary screenshots, extra diagrams, and detailed capture material into the appendix
- make sure every visual has a short interpretation sentence in the surrounding prose

For ready-to-paste placeholder blocks, use:
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`

## 3. Section-by-section writing plan

## Section 1. Introduction

### Goal
Frame EduSecure as a security-focused education-platform case study and explain the report aim clearly.

### Evidence to pull
- `docs/00-overview/assignment_brief.md`
- `docs/01-governance-risk-traceability/assignment-traceability.md`
- `docs/07-reporting-presentation/report-ready-section-1-and-8-scope-paragraphs.md`

### Claims to make
- the brief describes serious security failures
- the implemented artefact focuses on the most security-sensitive academic workflows
- the report analyses risks, control selection, design, and implementation evidence

### Suggested paragraph order
1. introduce the EduSecure case-study context
2. summarise the insecure baseline from the brief
3. state the report scope and honest feature boundary
4. end with a one-sentence roadmap of the remaining sections

### Figure or table slots
- no figure required unless you want a very small context diagram reference

### Done criteria
- the section is short and specific
- it does not read like a generic cybersecurity introduction
- it does not imply the whole LMS has been implemented

## Section 2. Role of Cryptography in EduSecure

### Goal
Explain why multiple different cryptographic techniques are needed and what each one does in the case study.

### Evidence to pull
- `docs/02-architecture-crypto/crypto-decision-matrix.md`
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/03-features/authentication/mfa-cryptography-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`

### Claims to make
- cryptography supports confidentiality, integrity, authenticity, and stronger authentication assurance
- different primitives serve different purposes
- the project uses more than one cryptographic control type in a coherent way
- browser-side encrypted space chat adds a distinct communication-confidentiality slice based on `ECDH`, `HKDF-SHA-256`, and `AES-GCM`

### Suggested paragraph order
1. explain why the platform assets require cryptographic protection
2. discuss password hashing and MFA
3. discuss symmetric encryption and transport protection
4. discuss encrypted space-chat key agreement, room-key wrapping, and ciphertext messaging
5. discuss signatures and asymmetric cryptography
6. discuss hashes and HMAC
7. end with a short synthesis paragraph

### Figure or table slots
- optional small comparison table for primitives and their purposes

### Done criteria
- `bcrypt`, `SHA-256`, `HMAC`, `AES-GCM`, signatures, TLS, JWT, and MFA are clearly differentiated
- no paragraph confuses encryption with hashing or tokens with encryption

## Section 3. Risk Assessment

### Goal
Present a structured and academically credible risk method, then discuss the highest-value risks.

### Evidence to pull
- `docs/01-governance-risk-traceability/risk-methodology.md`
- `docs/01-governance-risk-traceability/risk-register-refined.md`
- `docs/01-governance-risk-traceability/risk-register.md`
- `docs/01-governance-risk-traceability/cvss-risk-register.md`
- `docs/07-reporting-presentation/report-draft/01-methodology-report-ready-paragraph.md`
- `docs/07-reporting-presentation/report-draft/02-methodology-comparison-table.md`

### Claims to make
- the main assessment method is simplified NIST SP 800-30-style risk analysis
- OWASP provides application-security framing
- CVSS supports prioritisation, not full risk replacement
- the major risks concern credentials, sessions, submissions, grades, and audit evidence

### Suggested paragraph order
1. introduce the methodology and why it was chosen
2. insert or adapt the methodology paragraph
3. include the comparison table if useful
4. identify key assets
5. discuss main vulnerabilities and threat events
6. write three to five prioritised risk discussions
7. include propositional logic justification for major mitigations
8. finish with residual risk observations

### Figure or table slots
- methodology comparison table
- compact risk table or summarised refined register excerpt

### Done criteria
- the method is explicit and recognised
- risks are prioritised rather than listed randomly
- each major risk includes mitigation reasoning and residual risk

## Section 4. Secure System Design

### Goal
Show how the insecure baseline is transformed into a more secure architecture and workflow design.

### Evidence to pull
- `docs/02-architecture-crypto/current-state-data-flow-diagram.md`
- `docs/02-architecture-crypto/uml-documentation-plan.md`
- selected secure UML files from `docs/02-architecture-crypto/uml/`
- `docs/02-architecture-crypto/uml/academic-workflows/sequence-space-chat-e2ee-implemented.puml`
- `docs/02-architecture-crypto/space-chat-e2ee-design.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/02-architecture-crypto/uml-refresh-assessment.md`
- `docs/07-reporting-presentation/final-doc-alignment-summary.md`

### Claims to make
- the secure design addresses the brief's incidents directly
- trust boundaries, role boundaries, and sensitive data flows are better controlled
- the most important design themes are auth hardening, submission protection, grade integrity, and secure transmission design
- encrypted space chat is a distinct workflow where plaintext exposure is reduced by moving message encryption and decryption to the browser

### Suggested paragraph order
1. describe the insecure baseline briefly
2. explain the secure authentication design
3. explain the secure submission workflow
4. explain the grade-integrity design
5. explain the encrypted space-chat design and its browser trust boundary
6. explain the intended TLS transport design
7. interpret each referenced figure in terms of the security property it demonstrates

### Figure or table slots
- secure login sequence
- secure submission sequence
- grade-integrity sequence
- secure space-chat E2EE sequence
- one deployment or trust-boundary diagram

### Done criteria
- every included diagram is interpreted, not just inserted
- auth, submissions, grades, encrypted chat, and transport are all linked back to the brief's threat conditions

## Section 5. Cryptographic Controls and Selection Justification

### Goal
Make the strongest technical argument in the report by comparing alternatives and justifying the chosen controls.

### Evidence to pull
- `docs/02-architecture-crypto/crypto-decision-matrix.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- relevant implementation classes for auth, submission protection, and audit

### Claims to make
- chosen controls fit the case-study risks better than simpler or less suitable alternatives
- the report understands why some alternatives were not chosen as the primary implementation control
- the selected control set is appropriate for a bounded study-project implementation
- the chat E2EE stack is justified separately because it combines browser key agreement, recipient-specific wrapping, and message encryption for a different asset class than submissions or MFA secrets

### Suggested paragraph order
1. password storage: `bcrypt` versus unsuitable fast hashes
2. symmetric encryption: AES and why `AES-GCM` is appropriate
3. encrypted chat: `ECDH P-256`, `HKDF-SHA-256`, and per-space `AES-GCM` room keys versus weaker or less suitable messaging choices
4. signatures: RSA versus ECC and why ECC is used in the artefact
5. digest and MAC roles: `SHA-256` versus `HMAC-SHA-256`
6. transport security: TLS 1.3 in the deployment design
7. close with a short synthesis of why the final combination is coherent

### Figure or table slots
- algorithm comparison table
- one short control-to-risk mapping table if needed

### Done criteria
- every chosen control has a clear justification
- at least one rejected or non-primary alternative is discussed per major control category
- wording stays honest about the simulated or bounded parts of the signing model

## Section 6. Implementation Plan and Considerations

### Goal
Show practical engineering judgement about how the cryptographic controls are actually implemented.

### Evidence to pull
- `docs/02-architecture-crypto/implementation-plan-and-considerations.md`
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`
- `docs/06-operations/postgresql-setup-and-security.md`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `frontend/src/services/chatCrypto.ts`
- `frontend/src/services/chatKeyStore.ts`

### Claims to make
- the stack and libraries are appropriate for the project scope
- randomness, nonce handling, and separated secrets are treated carefully
- schema delivery and deployment assumptions are controlled more safely than the insecure baseline
- browser-held encrypted-chat keys and wrapped room-key metadata introduce additional but defensible implementation trade-offs around client storage and device lifecycle

### Suggested paragraph order
1. technology stack and framework choices
2. randomness and nonce handling
3. browser key storage and encrypted-chat lifecycle considerations
4. key handling assumptions and separation by purpose
5. secure error handling and validation boundaries
6. schema delivery and database verification
7. short trade-off paragraph on bounded implementation choices

### Figure or table slots
- optional small table of secret types and their purposes

### Done criteria
- the section sounds like engineering judgement, not feature listing
- it explicitly states that source-controlled defaults are not production-safe
- it explains why the chosen scope is reliable and defensible

## Section 7. CIA Evaluation

### Goal
Critically evaluate the implemented controls by their effect on confidentiality, integrity, and availability.

### Evidence to pull
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/04-evidence-testing/submissions-audit/submission-content-protection-and-retrieval.md`
- `docs/04-evidence-testing/grades/grade-phase-status-and-evidence.md`
- `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`

### Claims to make
- integrity is the strongest and most central property in the artefact
- confidentiality is materially improved through password, secret, and content protection
- availability is supported only in a bounded and honest sense
- encrypted space chat materially strengthens confidentiality for collaboration content, but not metadata confidentiality or zero-trust delivery guarantees

### Suggested paragraph order
1. confidentiality analysis
2. integrity analysis
3. availability analysis
4. residual limitations and remaining risks
5. conclude with the strongest overall CIA interpretation

### Figure or table slots
- optional control-by-CIA summary table

### Done criteria
- the section is analytical rather than checklist-based
- at least one limitation is stated for each major property or control grouping

## Section 8. Technical Artefact Summary

### Goal
Show exactly what the repository implements and how the artefact supports the report's cryptographic claims.

### Evidence to pull
- `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md`
- `docs/04-evidence-testing/submissions-audit/submission-phase-status-and-evidence.md`
- `docs/04-evidence-testing/submissions-audit/submission-content-protection-and-retrieval.md`
- `docs/04-evidence-testing/grades/grade-phase-status-and-evidence.md`
- `docs/05-security-review/security-review-evidence-log.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `frontend/README.md`
- key backend integration test classes
- `docs/07-reporting-presentation/report-ready-section-1-and-8-scope-paragraphs.md`

### Claims to make
- the artefact demonstrates multiple implemented cryptographic techniques
- the repository provides evidence through tests, documented flows, and supporting frontend material
- the implemented scope is strong but still bounded compared with the full platform brief
- encrypted space chat is one of the clearest examples of a multi-step implemented cryptographic workflow, not just a documented future design

### Suggested paragraph order
1. auth and MFA evidence
2. submission integrity and confidentiality evidence
3. grade-integrity and audit evidence
4. encrypted space-chat E2EE evidence
5. transport/deployment evidence with careful wording
6. test and execution evidence
7. close with an honest scope statement

### Figure or table slots
- one small evidence summary table
- one encrypted-chat screenshot or one concise encrypted-chat sequence figure if it replaces prose effectively
- selected screenshot or test-output references if used in the final report

### Done criteria
- the section proves what is implemented instead of repeating design intentions
- it distinguishes implemented, partial, and not-yet-implemented features clearly

## Section 9. Conclusion

### Goal
End with a technically mature, evidence-based evaluation.

### Evidence to pull
- strongest claims from Sections 3 to 8
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`

### Claims to make
- EduSecure materially improves the insecure baseline in the brief
- the strongest implemented gains are in password protection, authentication assurance, submission integrity/confidentiality, and audit-backed grade accountability
- the project remains educational and bounded, not enterprise-complete

### Suggested paragraph order
1. recap the central problem and the control strategy
2. summarise the strongest implemented improvements
3. state the most important residual limitations honestly
4. end with a clear final evaluation sentence

### Figure or table slots
- no figure needed

### Done criteria
- the conclusion is confident but restrained
- it sounds final and evaluative, not like a new section of analysis

## 4. Recommended immediate next actions

1. draft `Section 5` first using the comparison-and-selection structure
2. draft `Section 3` next and reuse the methodology paragraph and table from this `report-draft` directory
3. create a short heading skeleton for Sections 1 to 9 inside your working report document
4. keep a running list of every claim that still needs a citation, screenshot, test output, or code reference
5. only merge text into the final report draft after each section has passed an honesty check for scope and evidence

## 5. Fast self-review checklist before merging into the final draft

- Does the section answer a specific brief requirement directly?
- Does each strong claim point to evidence in the repository?
- Are cryptographic terms used correctly?
- Are diagrams interpreted rather than merely inserted?
- Is residual risk or scope limitation acknowledged where needed?
- Is the writing specific to EduSecure rather than generic security theory?


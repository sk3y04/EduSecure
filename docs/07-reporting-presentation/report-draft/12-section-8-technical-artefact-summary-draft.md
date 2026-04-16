# Section 8 Draft — Technical Artefact Summary

This file is a **report-ready working draft** for Section 8.

Use it together with:
- `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md`
- `docs/04-evidence-testing/submissions-audit/submission-phase-status-and-evidence.md`
- `docs/04-evidence-testing/submissions-audit/submission-content-protection-and-retrieval.md`
- `docs/04-evidence-testing/grades/grade-phase-status-and-evidence.md`
- `docs/05-security-review/security-review-evidence-log.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/07-reporting-presentation/report-ready-section-1-and-8-scope-paragraphs.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`
- `docs/07-reporting-presentation/report-draft/06-report-core-vs-appendix-evidence-budget.md`

## How to use this draft

- keep the section evidence-led rather than turning it into an API manual
- keep only the **implementation evidence summary table** and the strongest screenshots in the main body unless more space remains
- use this section to show what the repository demonstrably implements, not everything it might later support
- keep browser, DFD, and extra screenshot material appendix-first under word pressure
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-6-to-9.md`

## 8. Technical Artefact Summary

The EduSecure technical artefact should be presented as a secure academic-workflow core rather than as a complete implementation of every wider platform feature in the assignment scenario. The repository provides strong evidence for authentication hardening, staff-managed academic spaces, assignment visibility, secure submission handling, grade integrity, audit protection, and now browser-side encrypted space chat. By contrast, broader LMS-style capabilities such as live video teaching, formal exam administration, and a full audit-review interface remain outside the current implemented scope. This boundary is important because a technically honest artefact summary is stronger than an overstated claim of complete platform delivery.

[TABLE PLACEHOLDER — Table X. Implemented cryptographic evidence summary
Source: `docs/04-evidence-testing/testing-support/final-implementation-evidence-map.md` and related evidence notes
Purpose: summarise the strongest implemented slices, the main security role of each, and the best evidence anchors for the report
Placement: after the opening artefact-summary paragraph
Priority: core
Status: not inserted yet]

The summary table should compress the evidence story rather than replace it. In the final report, use the surrounding paragraphs to interpret which implemented slices matter most and why they exceed the brief’s minimum technical expectation.

## 8.1 Implemented authentication and MFA evidence

The first major implemented slice is authentication hardening. EduSecure supports registration, login, logout, authenticated identity lookup, MFA setup, MFA enablement, MFA verification, and MFA disablement under a cookie-authenticated browser session model. Within this slice, the strongest technical claims are that passwords are protected with `bcrypt`, MFA-enabled users receive a staged login flow rather than an immediate authenticated session, recovery codes are one-time-use and hashed, and unsafe browser requests now require the CSRF token pair under the current backend/browser contract.

This is a strong artefact slice because it combines several control types rather than demonstrating a single isolated login feature. The backend evidence map and auth/MFA integration tests show not only that authentication works, but that the repository now distinguishes password protection, second-factor verification, cookie transport, and configuration validation more carefully than the insecure baseline in the brief.

[FRONTEND SCREENSHOT PLACEHOLDER — Figure X. Authentication or MFA screen
Source: `frontend/src/pages/Login/index.vue` or `frontend/src/pages/MfaChallenge/index.vue`
Purpose: show the visible user-facing authentication flow that corresponds to the backend security design and evidence
Placement: after the auth/MFA evidence paragraph
Priority: core
Status: capture needed]

## 8.2 Implemented submission integrity and confidentiality evidence

The second major implemented slice is secure submission handling. The repository now evidences `SHA-256` digest generation, digital-signature creation and verification logic, stored verification state, and AES-GCM protection of submission content at rest. The most important implementation boundary is that the standard submission metadata endpoint exposes integrity/authorship evidence such as digest, signature, algorithm, and verification status, while plaintext content retrieval is moved to a separate controlled endpoint that applies authorisation checks and creates a dedicated audit event on success.

This is one of the strongest parts of the artefact because it shows multiple controls working together in a realistic academic workflow. Students can upload bounded TXT/PDF submissions, lecturers can review integrity metadata, unrelated users are denied access, ciphertext storage is kept separate from the metadata response, and successful plaintext retrieval becomes an auditable event rather than an automatic side effect of viewing the submission record.

[FRONTEND SCREENSHOT PLACEHOLDER — Figure X. Submission detail evidence screen
Source: `frontend/src/pages/SubmissionDetail/index.vue`
Purpose: show visible submission evidence such as digest, verification status, content-access flow, or grading-related context
Placement: after the submission evidence paragraph
Priority: core
Status: capture needed]

[FIGURE PLACEHOLDER — Optional Figure X. Current-state level-1 DFD
Source: `docs/02-architecture-crypto/uml/current-state/dfd-level-1-current-state.puml`
Purpose: provide one implementation-facing view of how submission data, ciphertext, grades, and audit records move through the current system
Placement: after the artefact-to-architecture bridge sentence if one extra overview figure is needed
Priority: optional
Status: export needed only if it simplifies the evidence story]

## 8.3 Implemented grade-integrity evidence

The third major implemented slice is grade integrity. EduSecure supports grade creation, update, and restricted retrieval under an owner-scoped lecturer model with admin override. Only verified submissions may be graded, students can retrieve only their own grade view while assignment visibility remains valid through current space membership, and unrelated lecturers or students are denied inappropriate grade access. Grade-sensitive actions are also tied into the audit path, which strengthens the accountability narrative beyond simple access control.

This slice is especially important because it links directly back to one of the most serious incidents in the brief: altered or weakly controlled academic outcomes. The evidence notes and grade integration tests show that the platform treats grades as integrity-sensitive records rather than as ordinary mutable values, which gives the artefact a stronger academic-trust story.

## 8.4 Implemented encrypted space-chat evidence

A particularly distinctive implemented slice is EduSecure’s end-to-end encrypted space chat. The repository now evidences browser-side `ECDH P-256` key generation, public-key registration, per-space room-key publication, recipient-specific room-key wrapping with `ECDH + HKDF-SHA-256 + AES-GCM`, ciphertext message storage in MongoDB, local decryption in the Vue chat UI, and rekey enforcement when membership changes. This is valuable in Section 8 because it proves a multi-step communication-security workflow rather than only at-rest protection of backend-managed assets.

This feature is also academically strong because its evidence spans several layers of the repository at once. The frontend cryptography helpers show Web Crypto use directly, the browser key-store layer shows local private-key and room-key handling, the Vue chat panel proves the user-facing encrypted workflow, the backend key-publication and message services show bounded server responsibilities, and the schema changes prove that the wrapped-key metadata model is actually persisted rather than only proposed in design notes. The strongest write-up here is to show that the feature is genuinely implemented while still stating the honest limits around browser trust, metadata visibility, and legacy plaintext coexistence.

[FRONTEND SCREENSHOT PLACEHOLDER — Figure X. Encrypted space-chat UI evidence
Source: `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
Purpose: show visible encrypted-chat states such as key setup, rekey-required state, encrypted message send, or decrypt-on-read behavior
Placement: after the encrypted-chat evidence paragraph
Priority: core if chat is one of the chosen artefact screenshots
Status: capture needed]

[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Optional Figure X. Browser cryptography helper evidence for encrypted chat
Source: `frontend/src/services/chatCrypto.ts`
Purpose: support the claim that the E2EE workflow is implemented with Web Crypto rather than only documented in design notes
Placement: appendix first or after the encrypted-chat evidence paragraph if one compact code capture replaces prose effectively
Priority: appendix
Status: capture only if needed]

## 8.5 Secure transmission and scope boundary

Secure transmission is part of the artefact story only in a bounded way. EduSecure documents TLS 1.3 via Certbot/Let’s Encrypt as the intended deployment-side control for protecting credentials and session traffic in transit, but this should remain a design/deployment claim unless direct HTTPS evidence is included in the final submission. The repository’s strongest implemented symmetric-encryption evidence instead comes from AES-GCM use in two real business roles: MFA-secret protection at rest and submission-content protection at rest.

This distinction is important because it keeps the artefact summary technically honest. The report should not present the repository as if it still used a separate standalone AES transport demo or as if local tests alone prove production HTTPS enforcement.

## 8.6 Test and execution evidence

The technical artefact is strengthened by the fact that its main claims are supported by evidence notes, integration tests, service-level tests, and a focused PostgreSQL/Testcontainers verification path rather than by design documentation alone. The evidence map shows strong proof anchors for authentication, MFA, submission protection, grade integrity, audit integrity, and Liquibase-backed PostgreSQL schema delivery. This matters because it demonstrates that the repository contains not only security ideas, but repeatable proof that the major control paths behave as described.

A concise terminal or code-artefact capture can strengthen this section if used carefully, especially when it replaces a longer explanation of test or database-verification evidence. However, long raw test output or multiple similar screenshots should be moved to the appendix first.

[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Figure X. Security test or PostgreSQL verification evidence
Source: targeted backend test run output or a concise capture from `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`
Purpose: show that the artefact’s strongest claims are backed by concrete execution or verification evidence
Placement: after the test and execution evidence paragraph
Priority: core
Status: capture needed]

[FRONTEND SCREENSHOT PLACEHOLDER — Optional Figure X. Browser cookie or CSRF evidence
Source: manual browser capture aligned to `CSRF-01` / `AUTH-15`
Purpose: support the browser-security posture if the final artefact summary includes one hostile-origin or cookie-behaviour example
Placement: appendix first; use only if it strengthens the evidence narrative without duplicating Section 4 or appendix material
Priority: appendix
Status: capture only if needed]

## Mini-conclusion

Overall, the EduSecure artefact exceeds the brief’s minimum technical expectation because it demonstrates multiple cryptographic technique areas working together in one coherent system rather than as disconnected demonstrations. Authentication, submission protection, grade integrity, encrypted space chat, and audit accountability are all backed by repository evidence, while broader non-core platform features remain honestly outside the implemented scope. This makes Section 8 strongest when it proves what is implemented, distinguishes that from what is only partial or future work, and keeps the evidence aligned tightly to the actual repository state.

## Safe wording reminders for this section

Prefer wording such as:
- "The implemented artefact demonstrates..."
- "The repository evidences..."
- "Within the study-project scope..."
- "The implemented slice is strongest in..."
- "Encrypted space chat is implemented as a browser-side ciphertext workflow..."
- "This bounded scope keeps the evidence aligned to the actual repository..."

Avoid wording such as:
- "the whole platform is implemented"
- "production-ready"
- "the repository proves deployed HTTPS" unless separate deployment evidence is included
- "encrypted chat is only a design proposal"
- "complete LMS feature coverage"
- "full public audit investigation tooling"
- "enterprise PKI"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. the optional DFD placeholder
2. the optional browser cookie/CSRF screenshot placeholder
3. repeated frontend-description detail once the core screenshots are chosen
4. repeated endpoint-like examples after the evidence table is inserted

Keep until the end:
- the implementation evidence summary table placeholder
- one auth/MFA evidence paragraph
- one submission evidence paragraph
- one grade-integrity evidence paragraph
- one encrypted-chat evidence paragraph
- one concise test/evidence paragraph
- the scope boundary that EduSecure is a secure coursework-management core rather than a full LMS


# Final Cryptography Claims Matrix

This document is a **report-ready claims matrix** for EduSecure.

Its purpose is to help the final report state cryptographic claims that are:
- supported by repository evidence
- technically accurate
- explicit about scope
- careful not to overclaim beyond what the implementation proves

Use this document together with:
- `final-implementation-evidence-map.md`
- `report-claims-audit-note.md`
- `report-section-to-evidence-map.md`
- `docs/pack-06/submission-content-protection-and-retrieval.md`

## 1. How to use this matrix

For each control below, the matrix records:
- what the control does in EduSecure
- what repository evidence supports it
- wording that is safe to use in the report
- wording that should be avoided

This is intentionally stricter than a normal feature list.

The goal is not only to show what exists, but to prevent the report from making claims the repository does not actually prove.

## 2. Final claims matrix

| Control / mechanism | Implemented scope in EduSecure | Main security role | Key evidence | Safe report claim | Do not overclaim |
|---|---|---|---|---|---|
| `bcrypt` password hashing | User passwords are stored as password hashes rather than plaintext, with validation supporting stronger password input quality | Confidentiality of stored password verifiers; stronger authentication baseline | `docs/pack-03/mfa-cryptography-implementation.md`; `docs/pack-03/implementation-status-and-evidence.md`; `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java` | "EduSecure protects stored passwords using bcrypt rather than plaintext storage." | Do not say bcrypt prevents phishing, password reuse, or all account takeover. |
| TOTP-based MFA | Optional MFA login branch using authenticator-app codes and recovery codes | Authentication integrity; stronger login assurance | `docs/pack-03/mfa-cryptography-implementation.md`; `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java` | "EduSecure supplements password authentication with optional TOTP-based MFA and recovery codes." | Do not claim phishing-resistant MFA or WebAuthn/passkey-level assurance. |
| AES-GCM protection of MFA secrets at rest | Stored TOTP secrets are encrypted under a dedicated application-managed key and separate nonce | Confidentiality and ciphertext integrity for recoverable MFA secrets | `docs/pack-03/mfa-cryptography-implementation.md`; `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java`; MFA tests/docs | "Recoverable MFA secrets are protected at rest using AES-GCM within the application model." | Do not generalise this into whole-database encryption. |
| `SHA-256` digest generation | Submission plaintext is hashed to create stable digest evidence before encryption-at-rest | Integrity evidence | `docs/pack-04/signature-hash-workflow.md`; `docs/pack-06/submission-phase-status-and-evidence.md`; `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java` | "EduSecure computes SHA-256 digests over submitted plaintext content to support integrity checking." | Do not claim a digest alone proves authorship. |
| `ECC + SHA-256` digital-signature workflow | Submission digests are signed and immediately verified using the project’s simulated signing model, now backed by a stable configured demo ECC keypair | Integrity and authorship / proof-of-authorship narrative | `docs/pack-04/signature-hash-workflow.md`; `docs/pack-06/submission-phase-status-and-evidence.md`; `SubmissionFlowIntegrationTests.java`; `backend/src/test/java/edusecure/edusecure/service/crypto/AesRsaCryptoServiceTests.java` | "EduSecure implements an ECC-based signature workflow to demonstrate submission-authorship and tamper-evidence logic using a stable configured demo signing keypair within the study-project model." | Do not claim full enterprise PKI or real student-controlled private-key infrastructure. |
| `HMAC-SHA-256` audit integrity | Audit entries are written with non-empty HMAC-backed integrity values and simple chaining support | Authenticated integrity and accountability for internal audit records | `docs/pack-04/audit-integrity-and-evidence-plan.md`; `docs/pack-06/submission-phase-status-and-evidence.md`; `docs/pack-07/grade-phase-status-and-evidence.md`; submission/grade tests | "EduSecure uses HMAC-backed integrity values to make internal audit records tamper-evident within the study-project scope." | Do not describe this as a full SIEM or enterprise audit platform. |
| `TLS 1.3` via Certbot/Let's Encrypt | All client-server traffic is encrypted in transit using TLS 1.3 enforced at the infrastructure level via Certbot and Let's Encrypt | Transport confidentiality and integrity; directly addresses the brief's Wi-Fi token-interception incident | Deployment configuration; Certbot certificate provisioning | "EduSecure enforces TLS 1.3 for all client-server communication via Certbot/Let's Encrypt, protecting credentials and session tokens in transit." | Do not conflate infrastructure TLS with application-layer AES encryption of individual payloads. |
| AES-GCM protection of submission content at rest | Student-uploaded UTF-8 text submission content is encrypted before durable storage, while metadata stays available through a separate endpoint split | Confidentiality-at-rest for a real EduSecure business asset | `docs/pack-06/submission-content-protection-and-retrieval.md`; `SubmissionFlowIntegrationTests.java`; `SubmissionContentEncryptionServiceTests.java`; `SubmissionKeyProtectionServiceTests.java`; `FileSystemSubmissionContentStoreTests.java` | "EduSecure applies AES-GCM to protect stored uploaded text submission content at rest while preserving a separate digest/signature workflow for integrity and authorship." | Do not claim enterprise KMS/HSM maturity, general binary document handling, or end-to-end encrypted submission transport. |
| Metadata/content endpoint split for submissions | Submission metadata and plaintext retrieval are separated; successful plaintext retrieval is audited | Confidentiality minimisation and accountability | `docs/pack-06/submission-content-protection-and-retrieval.md`; `SubmissionFlowIntegrationTests.java` | "EduSecure separates submission metadata review from plaintext content retrieval and records successful content access as a distinct audited action." | Do not claim a full audit-review UI or exposed audit investigation API if it is not implemented. |
| Verified-submission-only grading | Grades can only be created for verified submissions and grade changes are audited | Integrity and accountability in grade handling | `docs/pack-07/grade-phase-status-and-evidence.md`; `GradeFlowIntegrationTests.java` | "EduSecure restricts grading to verified submissions and audits grade-sensitive actions." | Do not claim broader academic workflow features such as moderation, appeals, or complex gradebook lifecycle management. |
| Liquibase-backed schema delivery + PostgreSQL smoke verification | Versioned schema delivery is implemented and checked against a real PostgreSQL container in a focused test | Availability/maintainability support and evidence reliability | `docs/pack-09/final-implementation-evidence-map.md`; `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`; `docs/pack-09/postgresql-setup-and-security.md` | "EduSecure verifies its Liquibase schema baseline against a real PostgreSQL instance in a focused automated smoke test." | Do not say all tests run on PostgreSQL or that operations are fully production-ready. |
| JWT-backed auth sessions | JWT is used for stateless API session handling after authentication completes | Session/authentication support | `docs/pack-03/api-auth-contract.md`; auth tests | "EduSecure uses JWT-based stateless API sessions after successful authentication." | Do not describe JWT itself as encryption or as a primary confidentiality control. |

## 3. Strong report-level summary claims

The following summary statements are currently defensible when supported by the matrix above:

- EduSecure implements multiple cryptographic control types rather than a single mechanism.
- The implemented artefact now evidences password hashing, TOTP-based MFA, hashing, digital signatures, HMAC-backed audit integrity, and AES-GCM.
- AES-GCM appears in two real business roles:
  - protection of MFA secrets at rest
  - confidentiality-at-rest for stored submission content
- Secure transmission is handled by TLS 1.3 enforced via Certbot/Let's Encrypt at the infrastructure level, satisfying the "secure file/message transmission" artefact requirement through real deployment rather than a standalone demo endpoint.
- Integrity is the strongest and most central property in the current artefact, but confidentiality is also materially improved through MFA-secret protection and encrypted submission storage.

## 4. Claims that must stay carefully bounded

Even after the latest implementation work, the following should still be described cautiously:

- enterprise-grade key management
- production deployment maturity
- full database encryption
- full API-wide PostgreSQL-backed integration coverage
- public audit investigation tooling
- end-to-end encrypted client transport beyond the secure design narrative

## 5. Best report usage pattern

A strong report pattern is:
1. use the design docs to explain *why* a control was selected
2. use this matrix to phrase the claim safely
3. use the evidence map and tests to prove the claim
4. use the claims-audit note to avoid overstating anything beyond repository evidence

## 6. Cross-reference set for final review

Before finalising the report, use this document together with:
- `docs/pack-09/report-claims-audit-note.md`
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/report-section-to-evidence-map.md`
- `docs/pack-09/final-submission-checklist.md`


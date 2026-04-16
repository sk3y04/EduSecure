# Section 6 Draft — Implementation Plan and Considerations

This file is a **report-ready working draft** for Section 6.

Use it together with:
- `docs/02-architecture-crypto/implementation-plan-and-considerations.md`
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`
- `docs/06-operations/postgresql-setup-and-security.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `frontend/src/services/chatCrypto.ts`
- `frontend/src/services/chatKeyStore.ts`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`
- `docs/07-reporting-presentation/report-draft/06-report-core-vs-appendix-evidence-budget.md`

## How to use this draft

- keep the section engineering-focused rather than repeating the design story from Section 4
- keep only the **key/secret separation table** in the main body unless space remains
- treat code/config screenshots as appendix-first unless one clearly replaces prose
- use this section to explain implementation trade-offs, not to claim production maturity
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-6-to-9.md`

## 6. Implementation Plan and Considerations

## 6.1 Technology stack

EduSecure is implemented as a Spring Boot backend with a small frontend layer and a PostgreSQL-oriented runtime configuration. This is an appropriate study-project stack because it supports REST APIs, validation, role-based security, persistence, and the standard Java cryptography architecture without requiring a large identity or security platform. In `backend/build.gradle`, the repository includes the core dependencies needed for this design: Spring Web MVC, Spring Security, Validation, Spring Data JPA, Liquibase, PostgreSQL, JSON Web Token support, and Testcontainers for focused PostgreSQL verification.

This stack is a defensible engineering choice because it keeps the implementation explainable while still supporting the report’s main cryptographic themes. Spring Security provides password hashing and request protection foundations; JPA and validation support safer data access patterns; Liquibase improves schema discipline; and standard Java cryptography APIs support digest, HMAC, encryption, key generation, and digital-signature workflows without introducing unnecessary cryptographic libraries.

## 6.2 Randomness and nonce handling

A strong implementation section should show that cryptographic controls are not only selected correctly, but also used safely. In EduSecure, secure randomness matters for key generation, MFA secret handling, recovery-code generation, and encryption operations. The implementation guidance therefore centres on `SecureRandom` rather than ad hoc entropy sources, because weak randomness can undermine otherwise strong algorithms.

Nonce and IV handling are especially important for `AES-GCM`. The implementation plan explicitly treats fresh nonces as mandatory for each encryption operation and recognises that nonce reuse with the same key can catastrophically weaken both confidentiality and integrity. This is a useful engineering point to include because it shows understanding beyond naming an algorithm: safe use depends on operational details as well as algorithm choice. The same point now matters even more because the encrypted space-chat implementation uses browser-generated 12-byte random nonces for both room-key wrapping and message encryption through the Web Crypto API, so the report can point to a second concrete place where nonce discipline is part of the implemented engineering story rather than only a theoretical warning.

## 6.3 Browser key storage and encrypted-chat lifecycle

The encrypted-chat slice introduces a different implementation trade-off from the backend-managed controls used for submissions, grades, and MFA. In this feature, the user’s private `ECDH P-256` key remains on the client device in IndexedDB-backed browser storage, decrypted room keys are cached locally, and only public-key plus wrapped-room-key metadata is stored server-side. This is a defensible engineering decision because it keeps the backend blind to routine chat plaintext and prevents the server from holding the private key material needed for room-key recovery.

At the same time, the report should explain the lifecycle cost honestly. Browser support must be checked before setup, the current model is intentionally single-device-first rather than seamless multi-device sync, loss of local key material can make prior encrypted chat unreadable on a new device, and legacy plaintext messages may still coexist during rollout. These are not flaws to hide; they are exactly the kind of bounded implementation trade-offs that make the feature credible within a coursework artefact.

## 6.4 Key handling assumptions

A central implementation consideration in EduSecure is that different secrets serve different purposes and should not be treated as interchangeable. The repository configuration already reflects this separation through distinct settings for the JWT signing secret, the MFA secret-encryption key, the audit HMAC secret, the signing-key locations, and the submission-storage master key. This separation matters because compromise of one key should not automatically collapse every other security control in the system.

[TABLE PLACEHOLDER — Table X. Key and secret separation in EduSecure
Source: writer-created summary using `backend/src/main/resources/application.properties`, `backend/src/main/resources/application-prod.properties`, and implementation notes
Purpose: distinguish JWT signing secret, MFA secret-encryption key, audit HMAC secret, signing-key material, and submission-storage keying material by role and handling expectation
Placement: after the key-handling paragraph
Priority: core
Status: not inserted yet]

The report should also state the deployment boundary clearly. `application.properties` contains convenient fallback values for local study use, but these should be described as development/demo defaults rather than production-safe secrets. Safer wording is that production-grade deployment must externalise secrets through environment configuration or a secret-management mechanism. This keeps the report honest while still acknowledging that the repository is structured for secret separation rather than hardcoded single-key reuse. In the encrypted-chat model, the same separation principle appears in a different form: the browser-held private chat key, wrapped room keys, and backend-held public metadata are intentionally split so that no single storage location contains everything needed to recover plaintext content.

## 6.5 Error handling and secure coding boundaries

Implementation quality in EduSecure depends not only on cryptography but also on how failures are handled around it. Request validation, role checks, and authentication failures should remain distinct from internal server errors, and API responses should avoid leaking stack traces, key material, plaintext secrets, or cryptographic internals. This boundary matters because even a well-chosen control can be weakened if the surrounding application exposes sensitive data through logs, errors, or incomplete access checks.

This section is also a good place to state what cryptography does **not** solve by itself. SQL injection, unsafe access-control design, and browser misuse are not prevented merely by adding encryption or signatures. EduSecure therefore combines cryptographic controls with DTO validation, Spring Data JPA, RBAC, and audit-backed sensitive actions. That engineering combination is stronger than a report that implies cryptography alone secures the whole system.

## 6.6 Schema delivery and database verification

The database and schema-delivery story is one of the strongest practical engineering improvements in the repository. EduSecure is configured to use PostgreSQL as its primary runtime database, and Liquibase now defines the main schema baseline rather than leaving schema evolution to implicit runtime mutation. This is academically valuable because it shows versioned, reviewable database delivery rather than relying on `ddl-auto=update` style mutation.

The report should also explain the current evidence boundary carefully. The default fast test path still relies mainly on H2 for speed, but the repository now includes a dedicated PostgreSQL/Testcontainers smoke test in `LiquibasePostgresIntegrationTests.java` that verifies Liquibase changesets against a real PostgreSQL container and performs a minimal repository round-trip. That is a meaningful improvement in evidence quality, but it should not be overstated as full production hardening or full-suite PostgreSQL coverage. This database-discipline story also matters for the encrypted-chat slice because the room-key metadata and participant wrapping state are persisted relationally through explicit schema changes rather than being left as ad hoc document-store fields.

[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Optional Figure X. Concise configuration or PostgreSQL verification evidence
Source: one small capture from `backend/src/main/resources/application-prod.properties`, `backend/src/main/resources/application.properties`, or `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`
Purpose: support one implementation-quality statement about secure configuration or real-PostgreSQL schema verification without turning the section into a configuration dump
Placement: appendix first; only keep in the main body if it clearly replaces prose
Priority: appendix
Status: capture only if needed]

## Mini-conclusion

Overall, the implementation choices in EduSecure are strongest when presented as disciplined engineering trade-offs rather than as claims of full operational maturity. The stack is appropriate to the study-project scope, nonce and randomness handling are treated seriously, browser-held encrypted-chat keys are handled with explicit lifecycle boundaries, secrets are separated by purpose, error handling is bounded to avoid leakage, and schema delivery is now versioned and partially verified against a real PostgreSQL instance. Together, these decisions make the artefact easier to justify academically because they show that the cryptographic controls are not only selected well, but also embedded within a more careful implementation model.

## Safe wording reminders for this section

Prefer wording such as:
- "EduSecure is configured to use PostgreSQL as its primary runtime database..."
- "Liquibase is used as the baseline schema-management mechanism for the main runtime path..."
- "The repository contains verified local PostgreSQL schema-delivery evidence..."
- "The encrypted-chat feature keeps private key material on the client device..."
- "Development/demo defaults should be externalised in production-like environments..."
- "Within the study-project scope..."

Avoid wording such as:
- "the system is production-ready"
- "the database is absolutely secure"
- "the database is encrypted" unless the exact mechanism is evidenced
- "multi-device encrypted chat is fully solved"
- "all tests run on PostgreSQL"
- "fallback secrets are production-safe"
- "JWT is encryption"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. the optional code artefact screenshot placeholder
2. repeated stack-justification sentences once `build.gradle` is cited clearly
3. repeated explanation of why H2 is still used for fast tests after the PostgreSQL boundary is stated once
4. secondary error-handling examples beyond one concise paragraph

Keep until the end:
- the key/secret separation table placeholder
- one clear paragraph on randomness and nonce handling
- one clear paragraph on browser-side encrypted-chat key storage and lifecycle trade-offs
- one clear paragraph on secret separation and externalisation boundaries
- the Liquibase/PostgreSQL verification paragraph
- the wording boundary that the repository proves focused PostgreSQL schema delivery, not full production database maturity


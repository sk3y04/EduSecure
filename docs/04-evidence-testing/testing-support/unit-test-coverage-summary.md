# Unit Test Coverage Summary

This document explains what the **unit-focused** backend tests currently cover in EduSecure, and where the suite moves into component or integration territory.

Companion note:
- `docs/04-evidence-testing/testing-support/integration-test-coverage-summary.md`

## 1. Purpose

The backend test suite in `backend/src/test/java/edusecure/edusecure/` mixes several levels of testing:
- pure or near-pure unit tests
- lightweight component/configuration tests
- full integration tests using Spring Boot, MockMvc, repositories, and persistence

This note focuses on the first category so you can explain clearly:
- what logic is covered at unit level
- what is only covered at integration level
- where test gaps still exist

## 2. Classification rule used in this document

For this summary, tests are grouped as follows.

### Unit-focused
A test is treated as unit-focused when it:
- instantiates the class directly
- does not start the full Spring Boot application context
- does not depend on MockMvc, a real repository, or full application wiring
- mainly verifies local algorithmic, cryptographic, validation, or utility behavior

### Lightweight component / configuration
A test is treated as lightweight component/configuration when it:
- still avoids the full application runtime
- but exercises filesystem behavior, configuration binding, or mini-context startup rules

### Integration
A test is treated as integration when it:
- uses `@SpringBootTest`, `MockMvc`, repositories, or real context startup
- verifies multiple layers working together

## 3. Current unit-focused test classes and what they cover

## 3.1 `backend/src/test/java/edusecure/edusecure/service/auth/TotpServiceRfc6238Tests.java`

### What it covers
This is the clearest pure unit test in the auth area.

It verifies that the TOTP implementation:
- generates correct RFC 6238 SHA-1 reference vectors at known timestamps
- accepts Base32 secrets in a case-insensitive and padding-tolerant way
- enforces the configured drift window correctly during validation

### What this proves
At unit level, the repository proves that the core TOTP algorithm logic is not just "working in the app" but aligned with a known reference standard.

### What it does not prove by itself
It does not prove:
- MFA enrollment flow
- challenge issuance
- challenge expiry or replay behavior
- recovery-code behavior
- cookie/session behavior

Those are covered elsewhere by integration tests.

## 3.2 `backend/src/test/java/edusecure/edusecure/service/crypto/AesRsaCryptoServiceTests.java`

### What it covers
This test class directly instantiates `AesRsaCryptoService` and verifies:
- the configured demo signing keypair can be loaded
- signatures created by one service instance verify correctly in another instance using the same configured keypair
- the configured signing algorithm reported by the service is `SHA256withECDSA`
- missing signing-key resources fail fast with a clear exception

### What this proves
At unit level, the repository proves that:
- the configured ECC-style signing setup is stable across service instances
- signature verification is tied to configured key material rather than to a fresh runtime-generated pair
- key-loading errors fail loudly instead of silently degrading behavior

### What it does not prove by itself
It does not prove:
- submission flow wiring
- audit logging behavior
- controller responses
- full report claims about end-to-end submission verification

Those depend on broader integration coverage.

## 3.3 `backend/src/test/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionServiceTests.java`

### What it covers
This is a direct unit-style test of the AES-GCM content-encryption service used for submission storage.

It verifies:
- plaintext can be encrypted and decrypted successfully
- the encryption algorithm label is `AES/GCM/NoPadding`
- a nonce is produced
- ciphertext length metadata is populated
- ciphertext differs from plaintext
- tampered ciphertext fails decryption safely

### What this proves
At unit level, the repository proves that the submission content encryption logic:
- performs reversible encryption/decryption for valid input
- behaves like authenticated encryption rather than plain reversible encoding
- rejects tampered ciphertext instead of returning corrupted plaintext

### What it does not prove by itself
It does not prove:
- storage persistence behavior
- wrapped-key handling
- access control around content retrieval
- audit logging when plaintext is accessed

## 3.4 `backend/src/test/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionServiceTests.java`

### What it covers
This class directly tests the key-wrapping/unwrapping support used for submission content-encryption keys.

It verifies:
- a generated AES content-encryption key can be wrapped and unwrapped successfully
- the reported key-wrap algorithm is `AESWrap`
- the expected key version is carried through the wrap/unwrap flow
- unwrap fails for the wrong key version
- malformed wrapped-key input fails safely

### What this proves
At unit level, the repository proves that:
- content-encryption keys are not just generated, but can be protected and restored correctly
- version mismatches are treated as errors
- malformed wrapped-key input is not accepted silently

### What it does not prove by itself
It does not prove:
- that the wrapped key is actually persisted and retrieved correctly in the full submission workflow
- that storage and crypto are combined safely end to end

That is covered by broader submission integration tests.

## 3.5 `backend/src/test/java/edusecure/edusecure/config/AuthCookieConfigurationValidatorTests.java`

### What it covers
This test directly exercises `AuthCookieConfigurationValidator` without booting the full application.

It verifies:
- local-development defaults are accepted
- `SameSite=None` without `Secure=true` is rejected
- `SameSite=None` with `Secure=true` is allowed
- insecure cookie config in the `prod` profile is rejected
- cookie paths not starting with `/` are rejected

### What this proves
At unit level, the repository proves that core cookie-safety rules are encoded as deterministic validation logic, not just as comments or conventions.

### What it does not prove by itself
It does not prove:
- that the whole application startup will fail in every invalid configuration path
- runtime browser behavior for cookies

Those are covered by configuration startup tests and manual/browser review.

## 4. Lightweight component and configuration tests near the unit boundary

These are not pure unit tests, but they are still smaller and more focused than full integration tests.

## 4.1 `backend/src/test/java/edusecure/edusecure/service/submission/FileSystemSubmissionContentStoreTests.java`

### Why it is not a pure unit test
It uses a real temporary directory via `@TempDir` and performs filesystem reads/writes.

### What it covers
It verifies:
- ciphertext can be stored, read back, and deleted
- stored references use the `submission://` scheme
- deleted content can no longer be read
- invalid references such as traversal-like values are rejected

### Why it matters
This is important evidence for storage-safety behavior, but it is best described as a filesystem component test rather than a pure unit test.

## 4.2 `backend/src/test/java/edusecure/edusecure/config/AuthCookieConfigurationStartupValidationTests.java`

### Why it is not a pure unit test
It uses `ApplicationContextRunner` to test startup behavior in a mini Spring context.

### What it covers
It verifies:
- insecure production cookie configuration fails context startup
- `SameSite=None` without `Secure=true` fails startup
- secure production cookie settings allow startup to succeed

### Why it matters
This is stronger than a direct validator unit test because it proves startup enforcement, but it is still not a full application integration test.

## 5. What the unit-focused tests cover overall

Taken together, the unit-focused portion of the suite currently covers these areas well:

### Cryptographic correctness and safety primitives
- RFC-aligned TOTP generation and validation logic
- configured ECC signing-key loading and cross-instance verification
- AES-GCM encryption/decryption behavior for submission content
- tamper detection for encrypted submission content
- AES key wrapping/unwrapping behavior and version checks

### Defensive configuration logic
- cookie configuration safety rules
- production-cookie startup constraints

### Local storage component behavior
- filesystem-backed submission content storage round trip
- invalid storage-reference rejection

## 6. What the unit-focused tests do **not** cover directly

The following important behaviors are mostly proven by integration tests instead:
- registration, login, logout, and `/me`
- full MFA setup, verify, replay rejection, recovery-code behavior, and disable flow
- submission upload validation via HTTP
- student-versus-student submission access control
- grade authorization and grade lifecycle rules
- space ownership and membership authorization
- audit-log creation during sensitive actions
- Liquibase/PostgreSQL delivery behavior

So if you describe the repository honestly, the correct statement is:
- **unit tests prove the local algorithm/configuration building blocks**, while
- **integration tests prove the cross-layer security behavior**.

## 7. Known gaps and limitations

## 7.1 `backend/src/test/java/edusecure/edusecure/security/JwtServiceTests.java`

This file currently exists but is empty.

Practical meaning:
- there is no meaningful standalone unit-level documentation/evidence for JWT service behavior in that class yet
- JWT integrity and rejection behavior are instead evidenced indirectly by integration tests such as tampered-cookie and tampered-bearer checks

## 7.2 The suite is stronger on integration than on isolated service mocking

This is not necessarily bad.
In fact, for a security-oriented study project, integration-heavy evidence is often more persuasive than heavily mocked tests.

But it does mean the repository currently has:
- a **small, focused unit layer** for crypto/configuration primitives
- a **larger integration layer** for end-to-end security behavior

## 8. Suggested wording for your report

If you want a concise way to explain this in the report, you can say:

> The repository contains a targeted unit-test layer for cryptographic primitives and security-critical configuration rules, including TOTP RFC-vector validation, configured signing-key verification, AES-GCM submission-content protection, AES key wrapping, and cookie-configuration fail-fast checks. Broader access-control, MFA-flow, submission, grade, and space-security behavior is then evidenced by Spring Boot integration tests rather than by isolated mocked unit tests.

## 9. Bottom line

The current unit-focused tests prove that EduSecure's **core security building blocks** behave correctly in isolation.

In particular, they provide local evidence for:
- TOTP correctness
- signature-key loading and verification
- AES-GCM encryption and tamper rejection
- key wrapping/version enforcement
- cookie configuration safety rules

They do **not** by themselves prove the whole system is secure.
That broader claim relies on the integration suite and the manual/browser/deployment checks documented elsewhere in the repository.


# Submission AES Storage Implementation Checklist

This document converts `submission-aes-storage-design.md` into a practical later-coding checklist for the current Spring Boot backend.

It is intentionally implementation-oriented:
- what to change
- where to change it
- what order to follow
- what tests must pass before the feature is considered complete

For exact file touchpoints and recommended sequencing across the existing backend codebase, use `submission-aes-storage-file-by-file-plan.md` alongside this checklist.

## 1. Implementation objective

Implement AES-GCM encryption for **submission content at rest** while preserving the existing submission workflow for:
- digest generation
- digital signature creation/verification
- verification status persistence
- audit recording
- current role-based access rules

## 2. Ground rules before coding

Before touching code, confirm the following are still accepted:
- plaintext content remains the canonical input for `hashDigest`
- signature generation/verification remains based on the plaintext-derived digest
- AES-at-rest does not replace TLS, JWT auth, or audit integrity
- ~~`AesGcmDemoService` remains separate from submission-storage protection~~ *(removed — `AesGcmDemoService` no longer exists; TLS via Certbot/Let's Encrypt handles transmission)*
- the first implementation keeps the existing submission API contract stable unless a change becomes unavoidable
- the later implementation fails closed if encryption, DEK wrapping, or ciphertext persistence fails

## 3. Phase-by-phase checklist

## Phase 1 — Schema and entity preparation

### 1.1 Review the current schema touchpoints
Files to inspect first:
- `backend/src/main/java/edusecure/edusecure/entity/Submission.java`
- `backend/src/main/resources/db/changelog/changes/001-initial-schema.yaml`

### 1.2 Add submission encryption metadata to the schema
Add columns for the chosen at-rest design concepts:
- `storage_encryption_algorithm`
- `storage_encryption_nonce`
- `wrapped_content_encryption_key`
- `key_wrap_algorithm`
- `storage_key_version`
- `ciphertext_length_bytes` (optional but recommended)

Checklist:
- [ ] choose exact column names consistent with the current naming style
- [ ] choose lengths/types suitable for Base64 and algorithm identifiers
- [ ] mark required fields `nullable: false` if every saved submission must be encrypted
- [ ] avoid adding any plaintext-content column to `submissions`
- [ ] confirm the migration strategy is compatible with the current Liquibase baseline

### 1.3 Update the `Submission` entity
File:
- `backend/src/main/java/edusecure/edusecure/entity/Submission.java`

Checklist:
- [ ] add fields matching the new schema columns
- [ ] keep existing digest/signature/verification fields unchanged in meaning
- [ ] retain `storedFileReference` as the ciphertext locator
- [ ] do not add a persisted plaintext-content field

## Phase 2 — Configuration and key-handling preparation

### 2.1 Add dedicated submission-storage configuration
File:
- `backend/src/main/resources/application.properties`

Add separate properties from the AES demo key, for example conceptually:
- `submission.storage.master-key`
- `submission.storage.master-key-version`
- `submission.storage.key-wrap-algorithm`
- `submission.storage.cipher-algorithm`
- optional storage-path/base-location property if using local blob/file storage

Checklist:
- [ ] submission-storage keys are separate from any demo key material *(note: `aes.demo-key` has been removed from the project)*
- [ ] use environment-variable-backed values in the same style as the existing config
- [ ] define a key version for later rotation support
- [ ] document any local development defaults carefully and honestly

### 2.2 Freeze the first implementation key model in code
Checklist:
- [ ] use one configured master key for the feature
- [ ] generate one fresh DEK per submission
- [ ] wrap/protect the DEK before persistence
- [ ] persist the key version used
- [ ] discard plaintext DEK material from working memory as soon as practical

## Phase 3 — New service responsibilities

### 3.1 Add a storage-encryption service
Likely new class:
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`

Suggested responsibilities:
- generate DEK
- generate nonce
- encrypt plaintext submission bytes
- later decrypt ciphertext bytes when needed

Checklist:
- [ ] use `AES/GCM/NoPadding`
- [ ] generate a fresh random nonce per encryption
- [ ] return a structured result containing ciphertext and non-sensitive metadata needed by the caller
- [ ] do not mix demo-endpoint DTOs into this service

### 3.2 Add a key-protection service
Likely new class:
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionService.java`

Suggested responsibilities:
- load the configured master key
- wrap and unwrap per-submission DEKs
- expose or derive the configured key-version value

Checklist:
- [ ] keep wrapping concerns separate from digest/signature concerns
- [ ] fail clearly if the configured master key is invalid
- [ ] keep wrapped-key output in a persistence-safe form such as Base64

### 3.3 Add a ciphertext storage abstraction
Likely new class/interface:
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentStore.java`

Suggested responsibilities:
- persist ciphertext bytes
- return a `storedFileReference`
- read ciphertext bytes back by reference for later decryption

Checklist:
- [ ] decide first implementation target: local file/blob-like storage vs another internal store
- [ ] keep the abstraction small and testable
- [ ] ensure it is safe for transaction/failure handling expectations

## Phase 4 — Submission workflow integration

### 4.1 Refactor `SubmissionService.createSubmission`
File:
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`

Required workflow order:
1. validate request and assignment state
2. derive plaintext bytes from `request.content()`
3. compute `hashDigest` from plaintext bytes
4. sign and verify using the existing signing model
5. encrypt plaintext bytes for storage
6. persist ciphertext via the content store
7. save `Submission` metadata including encryption-related fields
8. write existing audit events

Checklist:
- [ ] preserve the current digest/signature semantics
- [ ] keep verification status behaviour unchanged unless intentionally revised
- [ ] ensure `storedFileReference` now points to ciphertext storage
- [ ] save encryption metadata only after encryption succeeds
- [ ] avoid persisting a `Submission` row if ciphertext persistence fails

### 4.2 Protect failure boundaries
Checklist:
- [ ] if encryption fails, abort the request
- [ ] if key wrapping fails, abort the request
- [ ] if ciphertext storage fails, abort the request
- [ ] do not emit a misleading successful submission audit event on failure
- [ ] keep error responses controlled and free from secret/key detail leakage

## Phase 5 — DTO and contract review

### 5.1 Review the request DTO
File:
- superseded by the later multipart upload contract documented in `docs/pack-04/api-submission-contract.md`

Checklist:
- [ ] treat the original JSON request-DTO assumption as historical planning only
- [ ] keep multipart file validation aligned with the current API contract
- [ ] keep the active implemented scope bounded to UTF-8 `text/plain` uploads unless a later evidence-backed expansion is completed

### 5.2 Review the response DTO
File:
- `backend/src/main/java/edusecure/edusecure/dto/SubmissionResponse.java`

Checklist:
- [ ] keep the response metadata-focused in the first pass
- [ ] do not expose wrapped keys, raw nonce values, or ciphertext by default
- [ ] decide whether `storedFileReference` remains visible as-is or needs future hardening
- [ ] only add non-sensitive storage metadata if there is a clear report/demo reason

## Phase 6 — Audit review

### 6.1 Keep the current audit behaviour valid
Files to inspect:
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
- `backend/src/main/java/edusecure/edusecure/entity/AuditLog.java`
- `backend/src/main/java/edusecure/edusecure/entity/AuditActionType.java`

Checklist:
- [ ] confirm submission creation still writes the expected audit events
- [ ] ensure audit details mention encrypted-at-rest handling if useful
- [ ] do not log plaintext content
- [ ] do not log ciphertext blobs
- [ ] do not log nonce values or wrapped keys

### 6.2 Decide whether new audit events are needed now or later
Checklist:
- [ ] decide whether encryption itself needs a distinct audit action in phase 1
- [ ] defer `SUBMISSION_CONTENT_ACCESSED` unless a real content-retrieval endpoint is added

## Phase 7 — Test implementation

### 7.1 Extend existing integration coverage
Primary file:
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`

Checklist:
- [ ] update the happy-path submission test to prove encrypted storage metadata is persisted
- [ ] assert submission creation still returns digest/signature/verification fields
- [ ] assert retrieval still honours ownership/privileged-role access rules

### 7.2 Add encryption-specific backend tests
Possible test targets:
- a dedicated test class for the new submission encryption service
- integration tests for ciphertext storage and retrieval
- Liquibase/schema validation coverage if already used elsewhere

Checklist:
- [ ] verify ciphertext differs from plaintext
- [ ] verify stored plaintext is not directly present in the submission record
- [ ] verify wrong key / wrong key version / malformed wrapped-key paths fail safely
- [ ] verify tampered ciphertext fails decryption
- [ ] verify encryption/storage failure prevents successful submission persistence

### 7.3 Preserve existing feature behaviour
Checklist:
- [ ] rerun auth tests
- [ ] rerun submission tests
- [ ] rerun grade tests that depend on verified submissions
- [ ] rerun AES demo tests to confirm the demo service remains unaffected

## Phase 8 — Documentation follow-up

After coding begins or completes, update these docs as appropriate:
- `docs/pack-04/submission-aes-storage-design.md`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-08/aes-demo-phase-status-and-evidence.md` if the demo/real-use-case distinction needs refreshed wording
- report/evidence planning docs if screenshots or test outputs change

Checklist:
- [ ] record the exact implemented key model honestly
- [ ] record whether ciphertext store is DB-backed or reference-based
- [ ] record the exact audit behaviour shipped
- [ ] record any deviations from the current design note

## 4. Likely code touchpoints summary

### Existing files likely to change
- `backend/src/main/java/edusecure/edusecure/entity/Submission.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `backend/src/main/java/edusecure/edusecure/dto/SubmissionResponse.java` (only if needed)
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/changelog/changes/001-initial-schema.yaml`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`

### New files likely to be added
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentStore.java`
- one or more focused tests for storage encryption and key wrapping

## 5. Minimum definition of done

Do not consider the feature complete until all of the following are true:
- [ ] submission content is encrypted before durable storage
- [ ] plaintext content is not stored directly in `Submission`
- [ ] digest/signature/verification behaviour still works as before
- [ ] submission creation fails safely if encryption/storage fails
- [ ] audit records do not leak plaintext or key material
- [ ] integration tests prove the encrypted-storage path works
- [ ] configuration is separated from the AES demo key
- [ ] documentation is updated to match the actual implementation


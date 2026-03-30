# Submission AES Storage File-by-File Plan

This document converts the AES-at-rest design and implementation checklist into a **file-by-file coding plan** for the current EduSecure backend.

Use it together with:
- `submission-aes-storage-design.md`
- `submission-aes-storage-implementation-checklist.md`

## 1. Goal of this plan

The goal is to implement AES-GCM protection for submission content at rest while:
- preserving the current submission API shape
- preserving plaintext-based digest and signature semantics
- keeping the AES demo service separate
- maintaining audit and access-control behaviour

## 2. High-level change map

### Existing files expected to change
- `backend/src/main/java/edusecure/edusecure/entity/Submission.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `backend/src/main/java/edusecure/edusecure/dto/SubmissionResponse.java` (review first; change only if justified)
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/src/test/resources/application.properties`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java` (review and likely small change)
- `backend/src/main/java/edusecure/edusecure/entity/AuditActionType.java` (review only unless new events are accepted)

### New files likely to be added
- `backend/src/main/resources/db/changelog/changes/002-submission-aes-storage.yaml`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentStore.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/FileSystemSubmissionContentStore.java`
- optional: `backend/src/main/java/edusecure/edusecure/config/SubmissionStorageProperties.java`
- one or more focused test classes for submission-storage encryption

## 3. File-by-file plan

## A. Database and schema files

### `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
Purpose in this feature:
- include the new submission-storage encryption migration

Planned change:
- add an include for `db/changelog/changes/002-submission-aes-storage.yaml`

Why this file should change:
- the project currently only includes `001-initial-schema.yaml`
- adding a new change set is safer than editing an already-established baseline

Acceptance points:
- `001-initial-schema` remains untouched for existing environments/tests
- the new change set is applied in order after `001`

### `backend/src/main/resources/db/changelog/changes/002-submission-aes-storage.yaml`
Purpose in this feature:
- extend `submissions` with encryption-related metadata columns

Recommended columns:
- `storage_encryption_algorithm`
- `storage_encryption_nonce`
- `wrapped_content_encryption_key`
- `key_wrap_algorithm`
- `storage_key_version`
- `ciphertext_length_bytes`

Recommended posture:
- use a new change set rather than modifying `001-initial-schema.yaml`
- keep plaintext content out of the database schema
- ensure column types/lengths are suitable for Base64 values and algorithm names

Acceptance points:
- migration applies on real Postgres
- Hibernate validation still passes
- no existing tables/constraints are broken

## B. Domain entity files

### `backend/src/main/java/edusecure/edusecure/entity/Submission.java`
Purpose in this feature:
- store encryption metadata alongside existing submission metadata

Planned change:
- add fields matching the new schema columns
- keep `storedFileReference` as the ciphertext locator
- keep existing digest/signature fields semantically unchanged

Important rule:
- do **not** add a persisted plaintext-content field

Acceptance points:
- entity validates cleanly against the migrated schema
- existing code that builds `Submission` objects is updated accordingly
- existing integrity/authorship fields retain their current meaning

## C. Configuration files

### `backend/src/main/resources/application.properties`
Purpose in this feature:
- configure submission-storage encryption separately from the AES demo

Recommended new properties:
- `submission.storage.master-key`
- `submission.storage.master-key-version`
- `submission.storage.key-wrap-algorithm`
- `submission.storage.cipher-algorithm`
- `submission.storage.base-path` (if local file/blob storage is used first)

Important rule:
- ~~do not reuse `aes.demo-key`~~ *(note: `aes.demo-key` and `AesGcmDemoService` have been removed; submission storage uses its own dedicated key material)*

Acceptance points:
- properties are environment-variable-backed in the same style as the current file
- local defaults are explicit and study-project honest
- invalid configuration fails fast

### `backend/src/test/resources/application.properties`
Purpose in this feature:
- provide deterministic test configuration for the new services

Planned change:
- add test-safe defaults for the new submission-storage properties
- ensure tests do not depend on production file paths or missing secrets

Acceptance points:
- H2-based tests still boot cleanly
- encryption-related tests can run deterministically

### Optional: `backend/src/main/java/edusecure/edusecure/config/SubmissionStorageProperties.java`
Purpose in this feature:
- bind the new submission-storage settings in a structured way

Why this file may be worthwhile:
- key material and algorithm configuration are easier to validate in one place
- reduces stringly-typed property usage in services

Acceptance points:
- configuration validation is clear
- services consume typed settings rather than scattered `@Value` fields where practical

## D. Service-layer files

### `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
Purpose in this feature:
- orchestrate the updated secure submission workflow

Planned change:
- keep assignment validation and user lookup as-is
- keep digest/signature generation and verification semantics as-is
- replace placeholder `storedFileReference` generation with real encrypted-content storage
- delegate encryption/key-wrapping/storage to dedicated collaborators

Required workflow order in this file:
1. validate assignment and actor
2. derive plaintext bytes from request content
3. compute `hashDigest`
4. sign and verify digest
5. encrypt plaintext bytes
6. persist ciphertext and obtain `storedFileReference`
7. populate `Submission` with encryption metadata
8. save `Submission`
9. write audit events

Acceptance points:
- submission creation fails closed if encryption or storage fails
- existing API behaviour remains stable unless explicitly revised
- audit is not written as success when encrypted persistence fails

### `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`
Purpose in this feature:
- perform AES-GCM encryption/decryption of submission content

Planned responsibilities:
- generate a per-submission DEK
- generate a fresh nonce
- encrypt plaintext bytes
- later support decryption if a content-access path is added

Important rule:
- ~~do not depend on demo controller DTOs or `AesGcmDemoService`~~ *(note: `AesGcmDemoService` has been removed from the project)*

Acceptance points:
- uses `AES/GCM/NoPadding`
- nonce generation is fresh per encryption
- tampered ciphertext fails decryption

### `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionService.java`
Purpose in this feature:
- protect DEKs under the configured submission master key

Planned responsibilities:
- load/validate master-key material
- wrap DEKs before persistence
- unwrap DEKs for later decryption paths
- expose the effective key version

Acceptance points:
- wrapped key output is persistence-safe
- invalid key config fails fast and clearly
- wrong key version or invalid wrapped key produces safe failure

### `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentStore.java`
Purpose in this feature:
- abstract ciphertext persistence and retrieval

Planned responsibilities:
- persist ciphertext bytes
- return a stable `storedFileReference`
- retrieve ciphertext bytes by reference

Acceptance points:
- interface stays small
- storage implementation is replaceable later
- service boundary is clear enough for unit/integration testing

### `backend/src/main/java/edusecure/edusecure/service/submission/FileSystemSubmissionContentStore.java`
Purpose in this feature:
- provide the first concrete ciphertext store implementation

Why this first implementation is reasonable:
- aligns with the current `storedFileReference` concept
- simpler than redesigning persistence around large binary DB fields
- honest study-project scope

Acceptance points:
- ciphertext is written under a configured base path
- file reference format is stable and parseable internally
- failures are surfaced clearly back to `SubmissionService`

## E. DTO and controller files

### `backend/src/main/java/edusecure/edusecure/dto/CreateSubmissionRequest.java`
Purpose in this feature:
- likely no functional change for the first pass

Planned action:
- review only
- keep the current request shape unless implementation forces a change

Acceptance points:
- request contract remains stable
- frontend compatibility is preserved

### `backend/src/main/java/edusecure/edusecure/dto/SubmissionResponse.java`
Purpose in this feature:
- remain metadata-focused while avoiding leakage of encryption internals

Planned action:
- remove the internal `storedFileReference` from the client-facing metadata contract
- do not expose wrapped keys, nonce values, or ciphertext

Chosen direction:
- keep `GET /api/submissions/{submissionId}` metadata-only
- move plaintext access to a separate `/api/submissions/{submissionId}/content` endpoint

Acceptance points:
- report-relevant metadata remains visible
- no sensitive encryption material is exposed

### `backend/src/main/java/edusecure/edusecure/controller/SubmissionController.java`
Purpose in this feature:
- expose metadata and plaintext retrieval through separate endpoints

Planned action:
- keep the standard metadata endpoint
- add a dedicated content-retrieval endpoint for controlled decrypted access

Acceptance points:
- endpoint paths and role posture remain stable for metadata
- plaintext retrieval exists on a dedicated endpoint with the same backend authorization checks plus audit logging

## F. Audit files

### `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
Purpose in this feature:
- ensure encrypted-storage details can be recorded without leaking sensitive material

Planned action:
- likely small review/change to the `detailsJson` usage pattern from callers rather than a large rewrite here

Acceptance points:
- audit remains append-oriented and HMAC-protected
- callers do not feed plaintext, raw ciphertext, nonce, or wrapped-key values into audit details

### `backend/src/main/java/edusecure/edusecure/entity/AuditActionType.java`
Purpose in this feature:
- decide whether new audit events are needed now

Planned action:
- add `SUBMISSION_CONTENT_ACCESSED` once the dedicated content endpoint is implemented

Acceptance points:
- metadata-only submission review remains unchanged
- successful plaintext retrieval is auditable as a distinct action

## G. Test files

### `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
Purpose in this feature:
- remain the primary end-to-end proof that submissions still work after encryption-at-rest is added

Planned change:
- extend happy-path assertions to prove encrypted storage is used
- verify metadata API output still contains digest/signature/verification data
- verify access-control behaviour is unchanged

New likely assertions:
- ciphertext-backed storage metadata exists
- stored plaintext is not present directly in the submission record
- audit entries still exist and do not leak sensitive material

### `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`
Purpose in this feature:
- protect downstream grade behaviour that currently constructs `Submission` entities directly

Why this file likely changes:
- `ensureSubmissionForStudent(...)` currently builds `Submission` fixtures directly
- once new non-null encryption metadata exists, these fixtures may need updating

Acceptance points:
- grade tests continue to pass with valid `Submission` fixtures
- the new encryption metadata does not break verified-submission grading rules

### `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`
Purpose in this feature:
- prove the new change set applies correctly on real Postgres

Planned change:
- assert the new change set is present
- assert the expected submission encryption columns exist

Acceptance points:
- Postgres migration path stays valid
- schema assertions reflect the new `submissions` structure

### New focused tests
Likely additions:
- unit/integration tests for `SubmissionContentEncryptionService`
- tests for `SubmissionKeyProtectionService`
- tests for `FileSystemSubmissionContentStore`

Acceptance points:
- ciphertext differs from plaintext
- tampered ciphertext fails decryption
- malformed wrapped key fails safely
- storage failures propagate cleanly

## 4. Suggested implementation order

1. add new Liquibase change set and include it
2. update `Submission` entity
3. add config properties and optional `SubmissionStorageProperties`
4. implement key-protection service
5. implement content-encryption service
6. implement content-store abstraction + first file-system store
7. refactor `SubmissionService`
8. update tests that construct `Submission` directly
9. extend integration and Postgres Liquibase tests
10. update status/evidence docs after code is stable

## 5. Decisions to make before coding starts

These should be answered explicitly at the start of implementation:
- whether `storedFileReference` stays exposed in `SubmissionResponse`
- whether encryption metadata fields should all be `nullable = false` from the first migration onward
- whether the first content store is definitely local-file-based
- whether a dedicated audit event for encrypted storage is needed in phase 1
- whether to introduce `SubmissionStorageProperties` immediately or use `@Value` first and refactor later

## 6. Minimum ready-to-code condition

Do not start coding until this file, the design note, and the implementation checklist are all accepted together.

That means the repo now has three levels of preparation:
- `submission-aes-storage-design.md` — architecture and security decisions
- `submission-aes-storage-implementation-checklist.md` — phased execution checklist
- `submission-aes-storage-file-by-file-plan.md` — exact file touchpoints and implementation order


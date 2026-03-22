# Submission AES Storage Design

This document defines the **next-stage design** for turning AES from a standalone demo into a real EduSecure use-case by encrypting **submission content at rest**.

It is intentionally written as a pre-implementation design note so later coding can proceed without ambiguity.

Now that the feature exists in code, the implementation-aligned companion is recorded in `docs/pack-06/submission-content-protection-and-retrieval.md`.

## 1. Purpose

The goal of this design is to add a real confidentiality control to the existing secure-submission workflow without weakening the current integrity/authorship model.

Specifically, this design must ensure that:
- submission content is not stored in plaintext
- existing `SHA-256` digest and digital-signature evidence remains intact
- encryption does not replace transport security, access control, or audit integrity
- the implementation stays small enough for the study-project scope

## 2. Why submission storage encryption was selected

### Selected direction
Use AES-GCM to protect **stored submission content** before introducing encrypted feedback storage/delivery.

### Why this is the better first real AES use-case
Compared with feedback encryption, submission-storage encryption is stronger for the current project because:
- submissions are already a central implemented domain in the backend
- the assignment brief explicitly treats student submissions as a key protected asset
- the existing signature/digest workflow already sits on the submission path
- confidentiality-at-rest fits naturally alongside authorship and tamper-evidence
- it avoids inventing a larger feedback-delivery subsystem before the core submission asset is fully protected

### What this decision does **not** mean
It does **not** mean:
- AES replaces HTTPS/TLS
- AES replaces signatures or hashing
- AES removes the need for authorisation checks
- the system is attempting end-to-end encryption between browser and server

Instead, the chosen posture is:
- `TLS` protects transport
- `AES-GCM` protects stored submission content at rest
- `SHA-256` + digital signature protect integrity and authorship evidence
- `HMAC`-backed audit integrity protects accountability records

## 3. Current state and gap

## Current implemented state
The current submission flow already does the following in `SubmissionService.createSubmission`:
- accepts authenticated student submission content
- computes a `SHA-256` digest over the plaintext submission
- signs and verifies that digest
- persists metadata such as `hashDigest`, `digitalSignature`, `signatureAlgorithm`, and `verificationStatus`
- writes audit records for creation and verification outcomes

The current `Submission` entity contains:
- assignment/user/time metadata
- file name and content type
- `storedFileReference`
- digest/signature/verification fields

## Current gap
The current design does **not** yet define how the submission content itself is stored securely.

At present:
- `storedFileReference` is only a placeholder-style reference
- no ciphertext model is documented
- no nonce/DEK/wrapped-key metadata is defined
- no submission-storage key-handling model exists
- no fail-closed encryption rule is frozen for later coding

This document closes that gap.

## 4. Scope boundary

## In scope for this design
- AES-backed encryption of submission content at rest
- the relationship between plaintext hashing/signing and ciphertext storage
- key-handling assumptions for a study-project implementation
- the minimum `Submission` model additions needed to support encryption
- audit expectations for encryption-sensitive actions
- later test/evidence expectations for the implementation

## Out of scope for this design
- replacing the separate AES demo endpoints
- full KMS/HSM integration
- browser-side key exchange or end-to-end encryption
- encrypted feedback delivery workflow
- large-scale external object-storage architecture
- production-grade secret rotation orchestration across multiple services

## 5. Security objective

The security objective of this change is to add a **confidentiality-at-rest** layer to submissions while preserving the existing submission-integrity narrative.

### Confidentiality
If the database or backing submission store is exposed, plaintext submission content should not be directly recoverable without the configured storage-protection key material.

### Integrity and authorship
Encryption must not change the meaning of the existing digest/signature workflow.

That means:
- the digest still reflects the original submitted plaintext content
- the digital signature still covers the digest derived from plaintext content
- the verification result still describes the authorship/integrity workflow, not the AES layer

### Availability and maintainability
The design must remain implementable and testable in the current Spring Boot project without introducing a disproportionate operational burden.

## 6. Selected design decisions

### Decision 1: encrypt submission content **after** digest/signature processing
The system will:
1. receive plaintext submission content
2. compute the digest on plaintext
3. sign/verify according to the current authorship model
4. encrypt the plaintext for storage
5. persist ciphertext-related metadata alongside the existing submission metadata

Reason:
- keeps authorship/integrity evidence tied to the exact submitted content
- avoids mixing encryption metadata into the signature model
- keeps the report explanation clean

### Decision 2: use `AES/GCM/NoPadding`
Use `AES/GCM/NoPadding` for submission storage encryption.

Reason:
- already consistent with the separate demo implementation
- gives confidentiality plus authenticated encryption
- explicit nonce handling is straightforward to document and test

### Decision 3: do **not** reuse the AES demo key
Submission-storage encryption must use separate configuration and key material from the demo endpoint flow.

Reason:
- the demo service exists for artefact evidence, not storage protection
- using a distinct key avoids coupling real data protection to a teaching/demo endpoint
- it makes report claims more defensible

### Decision 4: use a per-submission DEK wrapped by a configured master key
For the study-project implementation, the preferred model is:
- one environment-configured master key for submission storage protection
- one fresh data-encryption key (DEK) per submission
- ciphertext produced with the per-submission DEK
- the DEK wrapped/protected under the master key before persistence

Reason:
- stronger than one static content-encryption key for all submissions
- much simpler than a full external KMS design
- gives better key-separation and future rotation options

### Decision 5: keep ciphertext outside the main metadata fields
The encrypted content should be stored as a referenced blob/file payload, while the `Submission` row stores only the metadata needed to find and decrypt it.

Reason:
- aligns with the existing `storedFileReference` field
- avoids storing large opaque ciphertext directly in unrelated metadata fields
- keeps the entity and API response clean

### Decision 6: first implementation keeps the existing submission API contract stable
The current submission creation request shape should remain unchanged:
- `fileName`
- `contentType`
- `content`

The existing metadata response can also stay largely unchanged in the first pass.

Reason:
- smallest practical implementation step
- avoids frontend/API churn before the confidentiality layer is stable
- lets the change remain mostly internal to the storage and service layer

## 7. End-to-end workflow

## A. Submission creation workflow
1. student authenticates through the existing auth flow
2. backend validates assignment state and request body
3. backend converts submitted content into canonical plaintext bytes
4. backend computes `hashDigest` over plaintext bytes
5. backend signs the digest and runs verification
6. backend sets `verificationStatus` and `verificationMessage`
7. backend generates a fresh DEK for this submission
8. backend generates a fresh AES-GCM nonce
9. backend encrypts plaintext bytes using the DEK and nonce
10. backend wraps the DEK using the configured master key
11. backend stores the ciphertext in the chosen submission-content store
12. backend persists the `Submission` row with:
    - existing metadata
    - ciphertext reference
    - encryption metadata needed for later decryption
13. backend writes audit records for submission creation and verification outcome
14. transaction succeeds only if the encrypted-storage step is successful

## B. Retrieval workflow for metadata-only views
For `GET /api/submissions/{submissionId}` in the current phase:
- continue returning metadata and verification evidence
- do **not** expose plaintext content
- do **not** expose wrapped keys or nonce values through the standard response

Reason:
- the current frontend/report value is in integrity/authorship visibility
- metadata review does not require automatic plaintext disclosure

## C. Retrieval workflow for future content access
If a later feature needs actual content retrieval:
1. authorisation is checked first
2. ciphertext is loaded via `storedFileReference`
3. wrapped DEK is unwrapped with the configured master key
4. ciphertext is decrypted using AES-GCM
5. plaintext is returned only to an authorised actor
6. an additional audit record should be created for content access/decryption

## 8. Data model impact

## Existing fields retained
The following fields remain valid and should keep their current meaning:
- `storedFileReference`
- `hashDigest`
- `digitalSignature`
- `signatureAlgorithm`
- `verificationStatus`
- `verificationMessage`

## Recommended `Submission` additions
The future implementation should add fields equivalent to the following concepts:
- `storageEncryptionAlgorithm` — e.g. `AES/GCM/NoPadding`
- `storageEncryptionNonce` — Base64-encoded nonce used for AES-GCM
- `wrappedContentEncryptionKey` — wrapped/protected per-submission DEK
- `keyWrapAlgorithm` — the wrapping/protection method used for the DEK
- `storageKeyVersion` — identifies which configured master-key version protected the DEK
- `ciphertextLengthBytes` — optional but useful for diagnostics and tests

### Storage shape rule
- `storedFileReference` points to the ciphertext location
- plaintext submission content must not be stored in `Submission`
- plaintext must not be copied into audit details

## 9. Service and component impact

## Submission service changes
`SubmissionService.createSubmission` will remain the orchestration point, but it should delegate encryption/storage responsibilities to dedicated collaborators.

## Recommended new collaborators
### `SubmissionContentEncryptionService`
Responsibility:
- generate per-submission DEKs
- generate nonces
- encrypt plaintext content
- unwrap/re-wrap or decrypt when needed later

### `SubmissionContentStore`
Responsibility:
- persist ciphertext bytes
- resolve ciphertext by reference
- abstract whether storage is local-file, blob, or another internal mechanism

### `SubmissionKeyProtectionService`
Responsibility:
- load the configured master key
- wrap and unwrap per-submission DEKs
- expose key-version metadata

These may be implemented as separate classes or a smaller combined service, but the responsibilities should remain conceptually distinct.

## Important boundary rule
Do **not** overload `AesGcmDemoService` with submission-storage responsibilities.

That service should remain a demo/evidence utility.

## 10. API impact

## Immediate API posture
For the first implementation pass, keep these endpoints stable:
- `POST /api/assignments/{assignmentId}/submissions`
- `GET /api/submissions/{submissionId}`

## Request impact
No request-body changes are required for the initial storage-encryption enhancement.

## Response impact
`SubmissionResponse` may remain metadata-focused in the first pass.

Optional future additions could include non-sensitive storage metadata, but the initial recommendation is to keep encryption internals out of the standard response.

## Deferred API option
If later plaintext retrieval is needed, prefer a dedicated endpoint such as:
- `GET /api/submissions/{submissionId}/content`

This should be added only when there is a clear report/demo need and clear authorisation rules.

## 11. Audit impact

## Minimum audit rule
Submission encryption must not become a silent data-protection layer with no accountability.

At minimum:
- the existing submission creation audit should still occur
- the details should confirm that encrypted-at-rest storage was used
- plaintext content, nonce values, wrapped keys, and raw ciphertext must **not** be written into audit details

## Recommended future audit events
If later content decryption/retrieval is implemented, define an additional event such as:
- `SUBMISSION_CONTENT_ACCESSED`

If encryption or storage fails before persistence completes, no successful submission-creation event should be written.

## 12. Key-handling design

## Selected study-project key model
### Master key
Use a dedicated environment-configured submission-storage master key.

Example intent:
- property separate from `aes.demo-key`
- versioned so future rotation is possible

### DEK lifecycle
For each submission:
- generate a fresh random DEK
- use it exactly for that submission payload
- wrap it before persistence
- discard plaintext DEK from memory as soon as practical after use

## Key-version rule
Persist a key-version identifier with each submission so later decryption can select the correct master key if rotation is introduced.

## Report honesty note
The report should state clearly that:
- this is a study-project key-management model
- it is stronger than a single hard-coded content key
- it is still simpler than enterprise KMS/HSM deployment

## 13. Failure handling

## Encryption failure
If AES encryption or DEK wrapping fails:
- fail the submission creation request
- do not persist the submission row
- do not write a misleading successful audit entry

## Ciphertext-store failure
If metadata creation succeeds but ciphertext storage fails:
- the transaction must fail as a whole
- no durable submission record should remain pointing to missing ciphertext

## Verification failure
If the project continues using the current “accept but mark failed verification” model:
- submission content should still be encrypted before storage
- verification failure affects integrity/authorship status, not storage confidentiality

## Decryption failure
If a later content-access feature is added and decryption fails:
- return a controlled client/server error depending on the cause
- do not expose internal key details
- write an appropriate audit event if content access was attempted

## 14. Testing and evidence expectations

The later implementation should prove at least the following:

### Functional storage-encryption evidence
- submission creation succeeds and stores ciphertext instead of plaintext
- stored ciphertext differs from plaintext
- ciphertext cannot be decrypted if tampered with
- ciphertext can be decrypted with the correct wrapped DEK and master key

### Integrity/authorship evidence
- digest remains stable for the same plaintext
- signature verification behaviour is unchanged by the encryption layer
- verification metadata remains visible through the existing submission API

### Security behaviour evidence
- plaintext submission content is not stored directly in `Submission`
- audit records do not leak plaintext or key material
- unauthorised actors still cannot access another student's submission metadata
- later content-access endpoints, if added, enforce ownership/privileged-role checks

### Failure evidence
- encryption failure prevents persistence
- malformed wrapped-key or ciphertext data causes safe failure
- using the wrong master key version fails decryption cleanly

## 15. Implementation touchpoints to expect later

The most likely code touchpoints for the later implementation are:
- `backend/src/main/java/edusecure/edusecure/entity/Submission.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `backend/src/main/java/edusecure/edusecure/dto/SubmissionResponse.java` if non-sensitive metadata is expanded
- a new submission-storage encryption service beside the existing crypto services
- configuration properties for submission-storage master-key handling
- integration tests extending `SubmissionFlowIntegrationTests`

## 16. Deferred decisions

The following are deliberately deferred until coding begins:
- exact ciphertext store implementation (local file path vs internal blob strategy)
- exact DEK wrapping implementation detail in code
- whether plaintext retrieval is needed in the API at all
- whether `SubmissionResponse` should expose non-sensitive encryption metadata
- whether a dedicated audit event is added at encryption time or only at later decryption/access time

## 17. Phase gate for later coding

Do not start the AES-at-rest implementation until all of the following are accepted:
- plaintext digest/signature continues to be the canonical integrity/authorship path
- `AesGcmDemoService` remains separate from storage protection
- per-submission DEK + wrapped-key design is accepted
- the minimum `Submission` metadata additions are accepted
- fail-closed behaviour for encryption/storage failures is accepted
- the test/evidence expectations above are accepted

Once those decisions are accepted, use `submission-aes-storage-implementation-checklist.md` as the coding-ready execution checklist.

For exact backend touchpoints and proposed implementation order, use `submission-aes-storage-file-by-file-plan.md`.


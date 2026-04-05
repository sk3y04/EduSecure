# Submission Content Protection and Retrieval

This document records the **implemented** confidentiality boundary for EduSecure submissions after the AES-at-rest enhancement and the later metadata/content API split.

It exists as the implementation-quality companion to the earlier Pack 04 design notes.

## 1. Purpose and scope

The purpose of this document is to describe what the current code now does for submission confidentiality, content exposure, and audited retrieval.

It focuses on the implemented backend behavior around:
- `SubmissionController`
- `SubmissionService`
- `Submission`
- the submission content encryption/key-protection/content-store services
- the audit events triggered by submission creation and content access

This document is intentionally **implementation-aligned**, not speculative.

## 2. Why this document exists

Pack 04 already explains:
- the intended submission API contract
- the planned AES-at-rest design
- the implementation checklist and file-by-file plan

Pack 06 already includes a short status summary.

However, the repository now contains a more complete confidentiality boundary that deserves a dedicated explanation in one place:
- submission content is encrypted at rest
- metadata and plaintext content are no longer exposed through the same endpoint
- successful plaintext retrieval is explicitly audited

That boundary is important both technically and for the report narrative.

## 3. Implemented endpoint split

The current submission flow uses **two different retrieval paths**.

### A. Metadata endpoint
`GET /api/submissions/{submissionId}`

Purpose:
- return submission metadata
- return digest/signature/verification evidence
- avoid exposing protected plaintext content by default

This response is intended for:
- submission review
- report screenshots/evidence
- integrity/authorship visibility

### B. Content endpoint
`GET /api/submissions/{submissionId}/content`

Purpose:
- return decrypted submission content only when explicitly requested
- apply normal authorization before decryption
- create an audit event for successful content access

This endpoint exists so plaintext retrieval is:
- explicit
- controlled
- auditable

### C. Submission creation endpoint
`POST /api/assignments/{assignmentId}/submissions`

Purpose:
- accept an authenticated student's uploaded submission file within the current supported scope
- compute integrity/authorship metadata
- encrypt the stored content before durable persistence

## 4. Current metadata vs content boundary

## Metadata that remains visible through `GET /api/submissions/{submissionId}`
The standard metadata response still includes the fields that support the integrity/authorship narrative:
- `id`
- `assignmentId`
- `studentUserId`
- `submittedAt`
- `fileName`
- `contentType`
- `hashDigest`
- `digitalSignature`
- `signatureAlgorithm`
- `verificationStatus`
- `verificationMessage`

## Data that is intentionally not exposed through the metadata endpoint
The standard metadata response no longer exposes:
- plaintext submission content
- ciphertext content
- wrapped content-encryption keys
- nonce values
- internal ciphertext storage locators such as `storedFileReference`

### Why this matters
This boundary reduces unnecessary exposure and keeps the standard submission API focused on review evidence rather than storage internals.

It also makes the confidentiality story clearer in the report:
- metadata is visible for review
- plaintext is not returned automatically
- content access is a separate privileged action with audit evidence

## 5. Implemented storage and cryptographic flow

The current submission creation path follows this effective order.

### Step 1: authenticated student submits file content
The current implementation now accepts a deliberately narrow multipart upload:
- one required `file` part
- UTF-8 `text/plain` or validated `application/pdf` in the current scope
- a bounded upload size of up to `5MB` per file

The browser selects a local submission file, while the backend derives the effective file metadata and reads the uploaded bytes.

### Step 2: digest is computed on uploaded file bytes
The backend validates uploaded bytes according to the selected type:
- `text/plain` uploads must be valid UTF-8
- PDF uploads must match the claimed PDF type and include a valid `%PDF-` file signature

The backend then computes a `SHA-256` digest over the uploaded file bytes.

This preserves the original integrity model:
- the digest reflects the actual submitted file bytes
- encryption does not redefine what is being signed or verified

### Step 3: signature is created and verified
The backend creates the digital-signature evidence using the current simulated signing model and immediately verifies it.

The resulting values are still stored with the submission metadata:
- `hashDigest`
- `digitalSignature`
- `signatureAlgorithm`
- `verificationStatus`
- `verificationMessage`

### Step 4: submitted content is encrypted for storage
After digest/signature processing:
- a per-submission content-encryption key is generated
- a fresh AES-GCM nonce is generated
- submitted file bytes are encrypted using `AES/GCM/NoPadding`

### Step 5: DEK is protected
The per-submission content-encryption key is wrapped/protected under dedicated submission-storage key material.

The persisted submission metadata now includes fields such as:
- `storageEncryptionAlgorithm`
- `storageEncryptionNonce`
- `wrappedContentEncryptionKey`
- `keyWrapAlgorithm`
- `storageKeyVersion`
- `ciphertextLengthBytes`

### Step 6: ciphertext is stored separately
Ciphertext is written through the submission content store and referenced internally by `storedFileReference`.

This means:
- submitted file bytes are not persisted directly in the `Submission` row
- standard client responses do not need to know where ciphertext is stored

## 6. Current key-handling posture

The current implementation uses a study-project key model that is stronger than a single plaintext content field but still simpler than enterprise KMS/HSM infrastructure.

### Current posture
- submission-storage key configuration is separate from the other application secrets used for auth, MFA, and audit integrity
- a per-submission content-encryption key is used for content encryption
- the content key is wrapped before persistence
- the submission row stores a key-version identifier for later selection/rotation logic

### Scope honesty
This is still a study-project design.

It demonstrates:
- real confidentiality-at-rest behavior
- key separation between protected submission storage and the other cryptographic secrets used in the application
- a more realistic application of AES than blanket API-payload encryption

It does **not** claim:
- enterprise KMS integration
- HSM-backed key protection
- end-to-end client-side encryption
- full operational key rotation maturity

## 7. Access-control matrix

### Submission metadata endpoint
`GET /api/submissions/{submissionId}`

Allowed:
- submission owner while the related assignment remains visible through the student's current space membership
- owning lecturer for the related assignment
- admin

Denied:
- unrelated student
- unrelated lecturer

### Submission content endpoint
`GET /api/submissions/{submissionId}/content`

Allowed:
- submission owner while the related assignment remains visible through the student's current space membership
- owning lecturer for the related assignment
- admin

Denied:
- unrelated student
- unrelated lecturer

### Important note
The content endpoint does not relax authorization relative to metadata.

Instead, it increases accountability by making plaintext retrieval a distinct action that can be audited separately.

## 8. Audit behavior now implemented

The submission flow now has multiple relevant audit actions.

### Existing submission-related actions
- `SUBMISSION_CREATED`
- `SUBMISSION_VERIFIED`
- `SUBMISSION_VERIFICATION_FAILED`

### New content-access action
- `SUBMISSION_CONTENT_ACCESSED`

### Current rule
A successful call to `GET /api/submissions/{submissionId}/content` now creates a dedicated audit event.

### Audit-safety boundary
Audit details should support accountability without leaking sensitive content.

That means audit details should not contain:
- plaintext submission content
- ciphertext payloads
- nonce values
- wrapped keys
- unnecessary internal storage references

## 9. Failure semantics currently expected from this slice

### Submission creation failure
If encryption, key protection, or ciphertext storage fails during submission creation:
- the request fails
- successful persistence should not proceed
- a misleading successful submission event should not be recorded

### Metadata retrieval failure
If the submission does not exist or the actor is not allowed to view it:
- the metadata endpoint returns the appropriate error
- no plaintext retrieval is attempted

### Content retrieval failure
If ciphertext cannot be read or decrypted:
- the content endpoint fails cleanly
- internal crypto/storage details should not be leaked in the response
- partially decoded text or corrupted binary data should not be returned

## 10. Evidence currently available in tests

The strongest code-level evidence for this boundary currently includes:
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionServiceTests.java`
- `backend/src/test/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionServiceTests.java`
- `backend/src/test/java/edusecure/edusecure/service/submission/FileSystemSubmissionContentStoreTests.java`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

### What these tests now prove
They collectively prove that:
- submission content is encrypted before durable storage
- stored ciphertext differs from plaintext
- the metadata response does not expose the internal storage reference
- authorized actors can retrieve TXT or PDF content through the separate endpoint
- unrelated students cannot retrieve another student's content
- student-owned submission access is re-checked against current assignment-space membership
- successful content retrieval creates a dedicated audit event
- the submission AES-at-rest schema change exists in Liquibase-backed PostgreSQL verification

## 11. Report value of this implementation

This slice now supports a stronger report claim than the original submission-only integrity workflow.

### Confidentiality
AES-GCM is now used for a real EduSecure business asset:
- stored submission content

### Integrity and authorship
The existing digest/signature model remains intact and still explains:
- tamper evidence
- proof-of-authorship narrative
- lecturer/admin verification visibility

### Accountability
The separate content endpoint and its audit event strengthen the accountability story:
- metadata review and plaintext access are no longer indistinguishable
- content access is now a separately observable sensitive action

## 12. Known limits and honest boundaries

The current implementation is deliberately scoped.

### It does implement
- encrypted submission storage at rest
- separate metadata and audited file-download retrieval paths
- audited successful content access

### It does not yet implement
- public audit review endpoints
- richer inline preview workflows beyond the current download-first approach
- broader multipart binary upload workflows beyond the current TXT/PDF cut
- enterprise key management
- end-to-end encrypted submission transport
- a final report-ready audit viewer

## 13. Relationship to other docs

### Design and planning history
For the design rationale that led to this implementation, see:
- `docs/pack-04/submission-aes-storage-design.md`
- `docs/pack-04/submission-aes-storage-implementation-checklist.md`
- `docs/pack-04/submission-aes-storage-file-by-file-plan.md`

### Short implementation summary
For the shorter catch-up summary, see:
- `docs/pack-06/submission-phase-status-and-evidence.md`

### Historical note
Pack 08's AES transmission demo note is now historical only. The current repository's implemented symmetric-encryption evidence for submissions comes from the AES-GCM-at-rest storage flow described in this document.


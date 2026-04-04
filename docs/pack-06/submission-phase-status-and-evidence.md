# Submission Phase Status and Evidence

This document records the current implemented status of the first secure submission and audit-integrity slice.

For the detailed implementation explanation of encrypted-at-rest storage, metadata/content separation, and audited plaintext retrieval, see `submission-content-protection-and-retrieval.md`.

## 1. What is now implemented

The backend now includes a minimal Pack 04-aligned implementation for:
- `Assignment`
- `Submission`
- `SubmissionVerificationStatus`
- `AuditLog`
- `AuditActionType`
- submission digest and signature metadata persistence
- AES-GCM-backed encryption of submission content at rest
- HMAC-backed audit integrity values

## 2. Implemented endpoints in this phase

### Assignment endpoints
- `POST /api/assignments`
- `GET /api/assignments`

### Submission endpoints
- `POST /api/assignments/{assignmentId}/submissions`
- `GET /api/submissions/{submissionId}`
- `GET /api/submissions/{submissionId}/content`

## 3. Implemented security behavior

### Assignment creation
- allowed for `LECTURER` and `ADMIN`
- denied for `STUDENT`

### Submission creation
- allowed for `STUDENT`
- requires the authenticated browser session established by the backend-issued `HttpOnly` auth cookie
- now accepts a multipart browser upload rather than pasted JSON submission text
- currently bounded to small UTF-8 `text/plain` uploads for evidence-friendly scope control

### Submission retrieval
- allowed for submission owner
- allowed for privileged lecturer/admin review
- denied to unrelated students

### Submission content retrieval
- exposed through a separate endpoint rather than the standard metadata response
- allowed for submission owner
- allowed for privileged lecturer/admin review
- denied to unrelated students
- audited on successful access

## 4. Implemented cryptographic behavior in this phase

### Submission integrity/authorship metadata
For each submission, the backend now:
- computes a `SHA-256` digest over uploaded file bytes
- creates a digital signature using the current simulated signing model
- verifies that signature immediately
- stores verification status and message with the submission

Current implementation note:
- the simulated ECC signing model now uses a stable configured demo keypair loaded from externalisable resource locations rather than a fresh runtime-generated keypair
- this improves repeatability of report/test evidence while remaining a study-project simulation rather than a full user-held PKI model

### Submission confidentiality at rest
For each submission, the backend now:
- encrypts the stored submission content using `AES/GCM/NoPadding`
- stores ciphertext separately from the standard metadata response
- protects a per-submission content-encryption key under dedicated submission-storage key material
- keeps the normal `GET /api/submissions/{submissionId}` response metadata-only

### Audit integrity
For each relevant action, the backend now:
- writes an append-oriented `AuditLog`
- computes an HMAC-backed integrity value
- stores the previous integrity value when present for simple chaining support

## 5. Current audit actions created in code

Implemented actions currently include:
- `ASSIGNMENT_CREATED`
- `SUBMISSION_CREATED`
- `SUBMISSION_VERIFIED`
- `SUBMISSION_VERIFICATION_FAILED`
- `SUBMISSION_CONTENT_ACCESSED`

## 6. Test evidence currently available

### Existing tests still valid
- `backend/src/test/java/edusecure/edusecure/EduSecureApplicationTests.java`
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`

### New test evidence
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`

This currently proves:
- lecturer can create an assignment
- student can upload a text submission file to an assignment
- submission response contains digest, signature, algorithm, and verification status without exposing the internal storage reference
- lecturer can review submission metadata
- lecturer can retrieve decrypted submission content through the separate controlled endpoint
- unrelated student cannot view another student's submission
- unrelated student cannot retrieve another student's submission content
- unsupported non-text uploads are rejected within the current bounded scope
- student cannot create assignments
- audit entries are created for submission events
- audit entries contain non-empty integrity values

## 7. What is still not implemented

Within the submission slice itself, the main still-open items are now:
- public audit review endpoints
- broader report/evidence assembly and final packaging
- any future expansion beyond the current metadata/content retrieval contract

Later phases added grade-integrity capabilities elsewhere in the project, while the remaining work is now primarily about evidence assembly, optional audit exposure, and final refinement.


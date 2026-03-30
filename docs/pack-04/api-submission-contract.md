# API Submission Contract

This document records the **current minimal submission contract** used by the implemented secure-submission slice, while still noting a few deferred endpoints that were discussed earlier in the design phase.

For the currently implemented metadata/content split and AES-at-rest boundary, see `docs/pack-06/submission-content-protection-and-retrieval.md`.

## 1. Assumptions carried over from auth

From the current auth baseline:
- browser-facing auth is already in place through an `HttpOnly` auth cookie that carries the JWT session
- protected requests only trust JWT claims after the backend verifies the token signature
- students authenticate before submitting work
- lecturers authenticate before reviewing submissions
- endpoint security should continue to be role-aware and minimal

Practical implication:
- frontend requests are sent with credentials enabled
- submission endpoints should be described as requiring an authenticated session, not a JavaScript-managed bearer token

## 2. Endpoint set for the secure-submission slice

| Endpoint | Method | Intended role | Status | Purpose |
|---|---|---|---|---|
| `/api/assignments` | POST | Lecturer/Admin | Implemented | Create a basic assignment |
| `/api/assignments` | GET | Lecturer/Admin/Student | Implemented | List visible assignments |
| `/api/assignments/{assignmentId}/submissions` | POST | Student | Implemented | Create submission with integrity/authorship metadata and encrypted-at-rest storage |
| `/api/submissions/{submissionId}` | GET | Student/Lecturer/Admin | Implemented | View submission metadata and verification status |
| `/api/submissions/{submissionId}/content` | GET | Submission owner or Lecturer/Admin | Implemented | Retrieve decrypted submission content through a separate audited path |
| `/api/assignments/{assignmentId}` | GET | Lecturer/Admin/Student | Deferred | Optional single-assignment retrieval |
| `/api/submissions/{submissionId}/verify` | POST | Lecturer/Admin or system-internal trigger | Deferred | Optional re-verification path |
| `/api/audit/submissions/{submissionId}` | GET | Lecturer/Admin | Deferred | Optional audit-trail read endpoint |

## 3. Current implemented cut

The implemented submission slice currently includes only these endpoints:

1. `POST /api/assignments`
2. `GET /api/assignments`
3. `POST /api/assignments/{assignmentId}/submissions`
4. `GET /api/submissions/{submissionId}`
5. `GET /api/submissions/{submissionId}/content` for controlled plaintext retrieval after the AES-at-rest extension

The verify and audit endpoints remain deferred because verification already occurs during submission creation and audit evidence is currently exposed through tests/database inspection rather than a dedicated REST read endpoint.

## 4. Request/response design

## A. `POST /api/assignments`

### Request body
```json
{
  "title": "Cryptography Coursework 1",
  "description": "Submit your signed PDF report.",
  "dueAt": "2026-04-15T18:00:00Z"
}
```

### Success response
Status: `201 Created`

```json
{
  "id": "assignment-uuid",
  "title": "Cryptography Coursework 1",
  "description": "Submit your signed PDF report.",
  "dueAt": "2026-04-15T18:00:00Z",
  "createdByLecturerId": "lecturer-uuid"
}
```

## B. `GET /api/assignments`

### Success response
Status: `200 OK`

```json
[
  {
    "id": "assignment-uuid",
    "title": "Cryptography Coursework 1",
    "dueAt": "2026-04-15T18:00:00Z"
  }
]
```

## C. `POST /api/assignments/{assignmentId}/submissions`

### Implemented request body
The current backend uses a deliberately simple JSON request body:

### Success response
Status: `201 Created`

```json
{
  "fileName": "coursework.txt",
  "contentType": "text/plain",
  "content": "This is my authentic coursework submission content."
}
```

### Implementation note
- the backend computes the digest itself
- the backend creates and verifies the signature metadata in the current simulated signing model
- the backend encrypts submission content before durable storage

### Failure cases
- `400 Bad Request` for invalid request structure
- `401 Unauthorized` for missing/invalid authenticated session cookie, including malformed, expired, or signature-invalid JWTs
- `403 Forbidden` if role is not allowed
- `404 Not Found` if assignment does not exist
- `500 Internal Server Error` if the submission content cannot be protected for storage

### Storage note
The current implementation encrypts the submitted content at rest internally while returning only metadata through the standard submission response.

## D. `GET /api/submissions/{submissionId}`

### Success response
Status: `200 OK`

```json
{
  "id": "submission-uuid",
  "assignmentId": "assignment-uuid",
  "studentUserId": "student-uuid",
  "fileName": "coursework.pdf",
  "contentType": "application/pdf",
  "submittedAt": "2026-03-15T17:00:00Z",
  "hashDigest": "sha256-hex-or-base64",
  "signatureAlgorithm": "SHA256withRSA",
  "verificationStatus": "VERIFIED",
  "verificationMessage": "Signature verified successfully"
}
```

### Contract hardening note
`GET /api/submissions/{submissionId}` should remain **metadata-only**.

It should not expose:
- plaintext submission content
- ciphertext bytes
- wrapped content-encryption keys
- nonce values
- internal storage locators if those are considered unnecessary client-facing detail

## E. `GET /api/submissions/{submissionId}/content`

### Purpose
Provide controlled access to decrypted submission content without overloading the standard metadata endpoint.

### Success response
Status: `200 OK`

```json
{
  "submissionId": "submission-uuid",
  "fileName": "coursework.txt",
  "contentType": "text/plain",
  "content": "Decrypted submission content"
}
```

### Access rule
- allowed for the submission owner
- allowed for privileged lecturer/admin review
- denied to unrelated students

### Audit rule
Every successful content retrieval should create a dedicated audit event.

### Failure cases
- `401 Unauthorized` if no valid authenticated session is present, including malformed, expired, or signature-invalid JWTs
- `403 Forbidden` if the actor is not permitted to access the submission content
- `404 Not Found` if the submission does not exist
- `500 Internal Server Error` if ciphertext cannot be decrypted or the protected content store is unavailable

## 5. Verification trigger decision

### Selected decision
Verification should occur **during submission creation** in the first implementation pass.

### Why
- simpler lifecycle
- easier testing
- stronger report evidence
- avoids a partially trusted intermediate state for the first artefact

### Optional extension later
A dedicated re-verification endpoint may be added later if needed for demonstration or admin review, but it is not part of the current implemented contract.

## 6. Audit endpoint note

The current implementation creates audit records even though a dedicated audit endpoint is not yet exposed. That keeps the model future-proof without forcing too many endpoints into the first pass.

## 7. Contract stability rule

Do not add speculative grade, encryption-demo, or unrelated course-management endpoints to this contract until the current submission workflow is implemented and evidenced.

## 8. Authentication transport note

Protected submission endpoints should be documented and tested against the current auth baseline:
- authenticated browser requests use the backend-issued `HttpOnly` cookie
- frontend code should not read or persist the JWT directly
- the backend only reads JWT claims after verifying the token signature, so tampered bearer tokens or cookies remain unauthenticated
- server-side compatibility support for `Authorization` headers may exist, but it is no longer the primary browser contract

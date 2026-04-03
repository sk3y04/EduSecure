# API Grade Contract and Update Rules

This document defines the proposed minimal REST contract for grade creation, update, and retrieval.

## 1. Auth assumptions

This contract assumes the current auth baseline from Pack 03 remains in place:
- browser-facing auth uses a backend-issued `HttpOnly` cookie-backed session
- role checks are enforced server-side
- students, lecturers, and admins are distinguishable through roles

Practical implication:
- frontend grade requests are sent with credentials enabled
- protected grade endpoints should be described as requiring an authenticated session, not a frontend-managed bearer token

## 2. Proposed endpoint set

| Endpoint | Method | Intended role | Status | Purpose |
|---|---|---|---|---|
| `/api/submissions/{submissionId}/grade` | POST | Lecturer/Admin | Implemented | Create grade for a submission |
| `/api/submissions/{submissionId}/grade` | GET | Lecturer/Admin | Implemented | Retrieve the existing grade for a submission |
| `/api/grades/{gradeId}` | PUT | Lecturer/Admin | Implemented | Update existing grade |
| `/api/grades/{gradeId}` | GET | Lecturer/Admin | Implemented | View full grade details |
| `/api/my/submissions/{submissionId}/grade` | GET | Student | Implemented | View own grade directly from a submission |
| `/api/my/grades/{gradeId}` | GET | Student | Implemented | View own grade |
| `/api/audit/grades/{gradeId}` | GET | Admin/Lecturer | Deferred | Optional read endpoint for grade-related audit trail |

## 3. Recommended first implementation cut

To keep the phase small, the first coding pass should focus on:

1. `POST /api/submissions/{submissionId}/grade`
2. `PUT /api/grades/{gradeId}`
3. one retrieval path for student visibility
4. automatic audit creation during create/update

A dedicated audit endpoint remains optional because audit records are currently stored and verified through the backend domain/test evidence but are not yet exposed directly through a REST controller.

## 4. Contract details

## A. `POST /api/submissions/{submissionId}/grade`

### Preconditions
- the submission exists
- the actor is authorised
- recommended: the submission is `VERIFIED`

### Request body
```json
{
  "value": 78,
  "feedback": "Good integrity discussion and strong cryptographic reasoning."
}
```

### Grade rule
- `value` must be a whole-number percentage from `0` to `100`

### Success response
Status: `201 Created`

```json
{
  "id": "grade-uuid",
  "submissionId": "submission-uuid",
  "value": 78,
  "feedback": "Good integrity discussion and strong cryptographic reasoning.",
  "gradedByLecturerId": "lecturer-uuid",
  "gradedAt": "2026-03-15T18:00:00Z"
}
```

### Failure cases
- `401 Unauthorized` if no valid authenticated session is present
- `403 Forbidden` if role is not allowed
- `404 Not Found` if submission does not exist
- `400 Bad Request` if the percentage is outside `0..100` or omitted
- `409 Conflict` if a grade already exists and duplicate creation is not allowed
- `422 Unprocessable Entity` if the design enforces verified-submission-only grading and the submission is not verified

## B. `PUT /api/grades/{gradeId}`

### Request body
```json
{
  "value": 81,
  "feedback": "Updated after remarking. Stronger final evaluation."
}
```

### Success response
Status: `200 OK`

```json
{
  "id": "grade-uuid",
  "submissionId": "submission-uuid",
  "value": 81,
  "feedback": "Updated after remarking. Stronger final evaluation.",
  "gradedByLecturerId": "lecturer-uuid",
  "gradedAt": "2026-03-15T18:00:00Z",
  "lastModifiedAt": "2026-03-16T09:00:00Z"
}
```

### Update rules
- the existing grade record may be updated
- the old values do not disappear from accountability, because the update must create a new audit event
- no silent mutation is acceptable

## C. `GET /api/submissions/{submissionId}/grade`

### Success response
Status: `200 OK`

```json
{
  "id": "grade-uuid",
  "submissionId": "submission-uuid",
  "value": 81,
  "feedback": "Updated after remarking. Stronger final evaluation.",
  "gradedByLecturerId": "lecturer-uuid",
  "gradedAt": "2026-03-15T18:00:00Z",
  "lastModifiedAt": "2026-03-16T09:00:00Z"
}
```

### Visibility rule
Lecturer/admin users can reload the existing grade for a submission when revisiting the grading screen.

## D. `GET /api/my/submissions/{submissionId}/grade`

### Success response
Status: `200 OK`

```json
{
  "id": "grade-uuid",
  "submissionId": "submission-uuid",
  "value": 81,
  "feedback": "Updated after remarking. Stronger final evaluation.",
  "lastModifiedAt": "2026-03-16T09:00:00Z"
}
```

### Visibility rule
Students should only be able to view the grade attached to their own submission.

## E. `GET /api/my/grades/{gradeId}`

### Success response
Status: `200 OK`

```json
{
  "id": "grade-uuid",
  "submissionId": "submission-uuid",
  "value": 81,
  "feedback": "Updated after remarking. Stronger final evaluation.",
  "lastModifiedAt": "2026-03-16T09:00:00Z"
}
```

### Visibility rule
Students should only be able to view grade records that belong to their own submission.

## 5. Selected policy decisions

### Decision 1: grades attach to submissions, not directly to assignments
Reason:
- cleaner academic meaning
- better traceability to the exact submitted artefact

### Decision 2: one current grade record plus audit history
Reason:
- simpler than full grade versioning
- sufficient for a study project when combined with append-only audit

### Decision 3: create/update actions must always write audit entries
Reason:
- aligns directly with the assignment's auditing requirement
- supports integrity and accountability claims in the report

### Decision 4: recommended verified-submission-only grading
Reason:
- keeps the integrity narrative consistent
- avoids awarding grades to content that failed authorship/integrity checks

## 6. Endpoint stability note

Do not expand the grade API with moderation, appeals, or advanced workflow endpoints until the minimal integrity-focused grade flow is implemented and evidenced.

## 7. Authentication transport note

Protected grade endpoints should follow the current implemented auth model:
- authenticated browser requests use the backend-issued `HttpOnly` cookie
- frontend code should not store the JWT directly in JavaScript-accessible storage
- the backend only reads JWT claims after verifying the token signature, so tampered bearer tokens or cookies remain unauthenticated
- any remaining `Authorization` header support should be treated as compatibility-only rather than the primary browser contract

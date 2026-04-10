# Space Registration Request Technical Specification

## 1. Feature goal

Introduce a self-service course registration request workflow that fits the repository's existing `Space` model without weakening staff control over final membership.

The first delivery scope is intentionally narrow:
- students submit a registration request for a space by entering a known space code
- a request creates no access by itself and never bypasses staff review
- lecturers can review requests only for spaces they own
- admins can review requests for any space
- approval adds the student to the existing `space_memberships` table
- rejection preserves an auditable record without exposing private roster data to other students
- students can view the status of only their own requests and can cancel only pending requests they created

This feature inherits the existing backend-issued `HttpOnly` cookie authentication model and the existing role model.

## 2. Roles and permissions

### 2.1 Role definitions

- `STUDENT`: request creator and status viewer for own requests only
- `LECTURER`: reviewer for spaces they own
- `ADMIN`: reviewer for any space

### 2.2 Permission matrix

| Action | ADMIN | LECTURER | STUDENT |
|---|---|---|---|
| Create registration request | No | No | Yes |
| List own registration requests | No | No | Yes |
| List review queue | Any space | Owned spaces only | No |
| Approve request | Any space | Owned spaces only | No |
| Reject request | Any space | Owned spaces only | No |
| Cancel pending own request | No | No | Yes |

### 2.3 Authorization rules

- Students cannot directly add themselves to a space or review any request.
- Lecturers can review only requests where `space.createdByUserId == currentUser.id`.
- Admins can review any request.
- Students can view only requests where `studentUserId == currentUser.id`.
- Once a request is approved, the student gains access only through the normal `space_memberships` authorization path already used by spaces, assignments, submissions, and grades.

## 3. Domain model

### 3.1 SpaceRegistrationRequest entity

Purpose: auditable student request for staff-reviewed access to a space.

Fields:
- `id: UUID`
- `spaceId: UUID` required
- `studentUserId: UUID` required
- `status: RegistrationRequestStatus` required
- `requestMessage: String` optional, trimmed, 0 to 500 chars
- `requestedAt: Instant` required
- `reviewedByUserId: UUID` optional
- `reviewedAt: Instant` optional
- `reviewNote: String` optional, trimmed, 0 to 500 chars

Status values:
- `PENDING`: request is awaiting staff action
- `APPROVED`: request was accepted and a membership was created
- `REJECTED`: request was reviewed and declined
- `CANCELLED`: student withdrew the request before review

Constraints:
- exactly one active pending request may exist for a `(space_id, student_user_id)` pair
- a student already assigned to a space cannot create a registration request for that same space
- lecturers and admins cannot create student requests
- `reviewedByUserId` and `reviewedAt` must be set together for `APPROVED` and `REJECTED`
- `reviewedByUserId` and `reviewedAt` must remain null for `PENDING` and `CANCELLED`
- approving a request must create a `space_memberships` record in the same transaction

### 3.2 Read model expectations

Student request summary must expose:
- `id`
- `spaceId`
- `spaceCode`
- `spaceName`
- `status`
- `requestMessage`
- `requestedAt`
- `reviewedAt`
- `reviewNote`

Reviewer queue item must expose:
- all student summary fields except student-only cancellation affordances
- `studentUserId`
- `studentEmail`
- `studentFullName`
- `canReview`

## 4. Backend design

### 4.1 Packages

Add new feature packages following current conventions:
- `entity.registration`
- `repository.registration`
- `dto.registration`
- `service.registration`
- `controller.registration`

### 4.2 Persistence

Liquibase change set adds:
- `space_registration_requests`
- foreign keys to `spaces`, `users(student_user_id)`, and `users(reviewed_by_user_id)`
- an index for reviewer queue filtering by `space_id` and `status`
- an index for student history filtering by `student_user_id` and `requested_at`
- a unique constraint on `(space_id, student_user_id, status)` only for `PENDING` requests, implemented with a partial unique index in SQL

### 4.3 Service responsibilities

`SpaceRegistrationRequestService` must handle:
- student request creation from a submitted space code
- validation that the code resolves to a real, non-archived space
- duplicate prevention against pending requests and existing memberships
- role-aware list behavior for student history and reviewer queue
- approval transaction that creates `space_memberships` and marks the request approved
- rejection transaction that updates request review fields only
- student cancellation for pending own requests only
- audit logging for request lifecycle actions

### 4.4 Validation rules

- `spaceCode` is required, trimmed, uppercased, and validated using the same normalization style as `SpaceService`
- `requestMessage` is optional but limited to 500 characters after trim
- `reviewNote` is optional but limited to 500 characters after trim
- archived spaces cannot receive new registration requests
- if a request reaches review after membership was already created by another path, approval must fail with `409 Conflict`
- requests already in a terminal state cannot be reviewed or cancelled again

### 4.5 Audit requirements

Add audit actions:
- `SPACE_REGISTRATION_REQUEST_CREATED`
- `SPACE_REGISTRATION_REQUEST_APPROVED`
- `SPACE_REGISTRATION_REQUEST_REJECTED`
- `SPACE_REGISTRATION_REQUEST_CANCELLED`

Minimum audit detail payloads:
- create: `spaceCode=<CODE>,studentUserId=<UUID>`
- approve: `spaceCode=<CODE>,studentUserId=<UUID>`
- reject: `spaceCode=<CODE>,studentUserId=<UUID>`
- cancel: `spaceCode=<CODE>,studentUserId=<UUID>`

Audit payloads must avoid storing request message or review note text.

## 5. API design

All endpoints require authentication and live under `/api/space-registration-requests`.

### 5.1 Create registration request

`POST /api/space-registration-requests`

Allowed roles:
- `STUDENT`

Request body:

```json
{
  "spaceCode": "CRYPTO-A",
  "requestMessage": "Please add me to the cohort for the applied cryptography labs."
}
```

Response `201 Created`:

```json
{
  "id": "uuid",
  "spaceId": "uuid",
  "spaceCode": "CRYPTO-A",
  "spaceName": "Applied Cryptography Group A",
  "status": "PENDING",
  "requestMessage": "Please add me to the cohort for the applied cryptography labs.",
  "requestedAt": "2026-04-10T10:00:00Z",
  "reviewedAt": null,
  "reviewNote": null
}
```

Failure cases:
- `400` invalid request body
- `403` caller lacks student role
- `404` space code not found
- `409` membership already exists or a pending request already exists
- `409` target space is archived

### 5.2 List own requests

`GET /api/space-registration-requests/mine`

Allowed roles:
- `STUDENT`

Response `200 OK` returns newest first.

### 5.3 Cancel pending own request

`POST /api/space-registration-requests/{requestId}/cancel`

Allowed roles:
- `STUDENT`

Response `200 OK` returns the updated request summary with `status = CANCELLED`.

Failure cases:
- `403` request does not belong to caller
- `404` request not found
- `409` request is no longer pending

### 5.4 List review queue

`GET /api/space-registration-requests/review`

Allowed roles:
- `LECTURER`
- `ADMIN`

Behavior:
- admin receives all pending requests
- lecturer receives only pending requests for spaces they own

Response `200 OK`:

```json
[
  {
    "id": "uuid",
    "spaceId": "uuid",
    "spaceCode": "CRYPTO-A",
    "spaceName": "Applied Cryptography Group A",
    "studentUserId": "uuid",
    "studentEmail": "student@example.com",
    "studentFullName": "Student Example",
    "status": "PENDING",
    "requestMessage": "Please add me to the cohort for the applied cryptography labs.",
    "requestedAt": "2026-04-10T10:00:00Z",
    "reviewedAt": null,
    "reviewNote": null,
    "canReview": true
  }
]
```

### 5.5 Approve request

`POST /api/space-registration-requests/{requestId}/approve`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "reviewNote": "Confirmed against the current class list."
}
```

Response `200 OK` returns the reviewer item with `status = APPROVED`.

Failure cases:
- `403` caller cannot review this request
- `404` request not found
- `409` request is no longer pending
- `409` membership already exists

### 5.6 Reject request

`POST /api/space-registration-requests/{requestId}/reject`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "reviewNote": "This space is restricted to a different cohort."
}
```

Response `200 OK` returns the reviewer item with `status = REJECTED`.

Failure cases:
- `403` caller cannot review this request
- `404` request not found
- `409` request is no longer pending

## 6. Frontend UX design

### 6.1 Student request page

Add a dedicated page reachable from main navigation for students:
- shows a form with `spaceCode` and optional request message
- shows the student's request history below the form
- exposes cancel only for pending rows created by that student
- never reveals roster size beyond what the existing authorized space endpoints already provide

### 6.2 Reviewer queue page

Add a dedicated page for lecturers and admins:
- shows pending requests only
- lecturers see requests only for owned spaces
- each item exposes approve and reject actions with optional review note
- after approve or reject, update the row in place and remove it from the pending queue view

### 6.3 Navigation rules

- student navigation label: `Registration Requests`
- staff navigation label: `Registration Review`
- route guards must reuse existing `roles` meta handling in the router

## 7. Acceptance tests

Minimum automated backend coverage:
- student can create a pending request for an existing non-archived space by code
- duplicate pending request is rejected
- request is rejected when membership already exists
- student can list only own request history
- student cannot cancel another student's request
- lecturer can list only reviewable pending requests for owned spaces
- lecturer cannot review another lecturer's space request
- admin can review any pending request
- approval creates a space membership and changes status atomically
- rejection changes status without creating membership
- terminal requests cannot be re-reviewed or cancelled
- audit records exist for create, approve, reject, and cancel without storing free-text notes

## 8. Implementation sequence

1. add persistence model and Liquibase migration
2. add DTOs, repositories, service, controller, and audit action types
3. add integration tests covering student and reviewer flows
4. add frontend types, services, routes, and the student or reviewer pages
5. validate end-to-end behavior against the existing space access model

## 9. Scope limits for this P1 slice

This P1 feature does not yet include:
- open public browsing of all spaces for students
- prerequisite validation or timetable conflict checks
- batch approval tools
- notifications or email delivery
- waitlists or automatic seat limits

Those can be added later without changing the core request-review-membership pattern defined here.
# Space Management Technical Specification

## 1. Feature goal

Introduce `Space` as an academic collaboration area used to group students for coursework, resources, and activity coordination.

The first delivery scope is intentionally narrow:
- lecturers can create spaces they own and manage
- admins can create or manage any space for oversight
- students can be added to and removed from spaces only by the owning lecturer or an admin
- students can view spaces they belong to
- authorized viewers can access the shared per-space chat from the same detail screen
- roster management is auditable and role-constrained

This feature inherits the existing backend-issued `HttpOnly` cookie authentication model already used by the repository.

## 2. Roles and permissions

### 2.1 Role definitions

- `ADMIN`: global space administrator
- `LECTURER`: space owner for spaces they create
- `STUDENT`: read-only consumer of spaces they belong to

### 2.2 Permission matrix

| Action | ADMIN | LECTURER | STUDENT |
|---|---|---|---|
| Create space | Yes | Yes | No |
| List spaces | All spaces | Only spaces they created | Only spaces they belong to |
| View space detail | Any space | Only spaces they created | Only spaces they belong to |
| Update space metadata | Any space | Only spaces they created | No |
| Archive or unarchive space | Any space | Only spaces they created | No |
| Add student to space | Any space | Only spaces they created | No |
| Remove student from space | Any space | Only spaces they created | No |
| View full roster | Any space | Only spaces they created | No |

### 2.3 Authorization rules

- A lecturer is authorized to manage only spaces where `createdByUserId == currentUser.id`.
- An admin is authorized to manage any space.
- A student is authorized to view only spaces where a membership exists for their user id.
- Students cannot self-enroll, update metadata, or manipulate memberships.
- Full roster visibility is limited to the space owner or an admin.

## 3. Domain model

### 3.1 Space entity

Purpose: primary collaboration container.

Fields:
- `id: UUID`
- `name: String` required, trimmed, 3 to 120 chars
- `code: String` required, trimmed, normalized to uppercase, 3 to 32 chars, unique, pattern `[A-Z0-9-]+`
- `description: String` required, trimmed, 10 to 2000 chars
- `createdByUserId: UUID` required
- `createdAt: Instant` required
- `updatedAt: Instant` required
- `archived: boolean` required, default `false`

Constraints:
- `code` must be unique repository-wide
- archived spaces remain readable to authorized users
- archived spaces cannot accept new student memberships until unarchived
- archived spaces keep existing chat history visible but block new chat messages

### 3.2 SpaceMembership entity

Purpose: membership link between a space and a student.

Fields:
- `id: UUID`
- `spaceId: UUID` required
- `studentUserId: UUID` required
- `addedByUserId: UUID` required
- `addedAt: Instant` required

Constraints:
- unique pair on `(space_id, student_user_id)`
- target user must hold `STUDENT`
- deleting a space cascades membership deletion

### 3.3 Read model expectations

Space summary must expose:
- `id`
- `name`
- `code`
- `description`
- `archived`
- `memberCount`
- `canManage`
- `isMember`

Space detail must expose:
- all summary fields
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `memberships` only when `canManage == true`

Membership detail must expose:
- `studentUserId`
- `studentEmail`
- `studentFullName`
- `addedByUserId`
- `addedAt`

## 4. Backend design

### 4.1 Packages

Add new feature packages following current conventions:
- `entity.space`
- `repository.space`
- `dto.space`
- `service.space`
- `controller.space`

### 4.2 Persistence

Liquibase change set adds:
- `spaces`
- `space_memberships`
- foreign keys to `users`
- foreign key from `space_memberships.space_id` to `spaces.id`
- unique constraint on `spaces.code`
- unique constraint on `space_memberships(space_id, student_user_id)`
- indexes for common list and lookup paths

### 4.3 Service responsibilities

`SpaceService` must handle:
- role-aware list behavior
- space creation
- detail retrieval with authorization
- metadata updates
- roster add/remove operations
- normalization and duplicate checks
- audit logging for management actions

### 4.4 Audit requirements

Add audit actions:
- `SPACE_CREATED`
- `SPACE_UPDATED`
- `SPACE_STUDENT_ADDED`
- `SPACE_STUDENT_REMOVED`

Minimum audit detail payloads:
- create: `code=<CODE>`
- update: `code=<CODE>,archived=<BOOLEAN>`
- add student: `spaceCode=<CODE>,studentUserId=<UUID>`
- remove student: `spaceCode=<CODE>,studentUserId=<UUID>`

## 5. API design

All endpoints are under `/api/spaces` and require authentication.

### 5.1 Create space

`POST /api/spaces`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "name": "Applied Cryptography Group A",
  "code": "CRYPTO-A",
  "description": "Shared space for lectures, secure resources, and submission coordination."
}
```

Response `201 Created`:

```json
{
  "id": "uuid",
  "name": "Applied Cryptography Group A",
  "code": "CRYPTO-A",
  "description": "Shared space for lectures, secure resources, and submission coordination.",
  "archived": false,
  "memberCount": 0,
  "canManage": true,
  "isMember": false,
  "createdByUserId": "uuid",
  "createdAt": "2026-04-02T10:00:00Z",
  "updatedAt": "2026-04-02T10:00:00Z",
  "memberships": []
}
```

Failure cases:
- `400` invalid request body
- `403` caller lacks role
- `409` space code already exists

### 5.2 List spaces

`GET /api/spaces`

Behavior:
- admin receives all spaces
- lecturer receives only owned spaces
- student receives only assigned spaces

Response `200 OK`:

```json
[
  {
    "id": "uuid",
    "name": "Applied Cryptography Group A",
    "code": "CRYPTO-A",
    "description": "Shared space for lectures, secure resources, and submission coordination.",
    "archived": false,
    "memberCount": 18,
    "canManage": true,
    "isMember": false
  }
]
```

### 5.3 Get space detail

`GET /api/spaces/{spaceId}`

Authorization:
- admin: any
- lecturer: own spaces only
- student: assigned spaces only

Response `200 OK` returns full detail. For student callers, `memberships` must be an empty array.

Failure cases:
- `403` not authorized for that space
- `404` space not found

### 5.4 Update space

`PUT /api/spaces/{spaceId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "name": "Applied Cryptography Group A",
  "code": "CRYPTO-A",
  "description": "Updated guidance and resource summary.",
  "archived": false
}
```

Rules:
- owner-or-admin only
- code uniqueness remains enforced
- archiving is a soft state transition

Failure cases:
- `403` not owner/admin for target space
- `404` space not found
- `409` duplicate code

### 5.5 Add student to space

`POST /api/spaces/{spaceId}/students`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "studentEmail": "student@example.com"
}
```

Rules:
- target user must exist
- target user must hold `STUDENT`
- duplicate memberships are rejected
- archived spaces reject new additions

Response `201 Created`:

```json
{
  "studentUserId": "uuid",
  "studentEmail": "student@example.com",
  "studentFullName": "Student Example",
  "addedByUserId": "uuid",
  "addedAt": "2026-04-02T10:05:00Z"
}
```

Failure cases:
- `403` not owner/admin for target space
- `404` space or student not found
- `409` membership already exists or space archived
- `422` target user is not a student

### 5.6 Remove student from space

`DELETE /api/spaces/{spaceId}/students/{studentUserId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

Rules:
- owner-or-admin only
- removing a non-member returns `404`

Response:
- `204 No Content`

## 6. UI requirements

### 6.1 Navigation

- Add a `Spaces` primary navigation item for all authenticated users.

### 6.2 Space list view

Shared requirements:
- load role-aware list from `GET /api/spaces`
- show name, code, description, archive state, and member count
- provide action to open detail view

Privileged staff requirements:
- show create-space form
- show manage-oriented explanatory copy that reflects owner-scoped lecturer control and admin override

Student requirements:
- no create or management controls
- empty state must clearly state that spaces appear only after staff assignment

### 6.3 Space detail view

Shared requirements:
- show metadata and status
- show the shared space chat panel in the same screen
- show permission-sensitive messaging based on `canManage`

Shared chat requirements:
- authorized viewers can read existing messages for the space
- authorized viewers can post new messages only while the space is not archived
- polling-based refresh is acceptable for MVP
- message content must be rendered as plain text only

Privileged staff requirements:
- editable metadata form
- archive toggle
- add-student form using student email
- roster list with remove action
- success and failure alerts for roster changes
- lecturer-facing controls appear only for spaces they own; admin can manage any space

Student requirements:
- read-only metadata view only
- no roster, edit, archive, or membership-management controls
- chat remains visible if the student is an authorized member of the space

### 6.4 Validation UX

- preserve server validation messages
- disable submit buttons while requests are in flight
- clear prior success state before a new mutation

## 7. Edge cases

- duplicate space code after normalization must be rejected
- lecturer must not manage another lecturer's space
- lecturer must not view roster data for another lecturer's space
- student must not open detail for a space they are not a member of
- archived spaces remain visible but reject new additions
- archived spaces remain visible and their chat history remains readable, but new chat posts are rejected
- adding a lecturer or admin as a member must fail with `422`
- removing a student who is not a current member must return `404`
- whitespace-only values must fail validation
- normalized code must remain uppercase in storage and responses

## 8. Test cases

### 8.1 Backend integration tests

Required scenarios:
- lecturer can create a space, add a student, update metadata, and remove the student
- student sees only the space they were assigned to
- student cannot create, update, or modify memberships
- lecturer cannot manage another lecturer's space
- admin can manage a lecturer-owned space
- duplicate space code returns `409`
- duplicate membership returns `409`
- adding non-student user returns `422`
- archived space rejects new student addition
- audit log entries are produced for create, update, add, and remove actions

### 8.2 Frontend verification

Required scenarios:
- lecturer/admin sees create form and management controls
- student sees role-filtered list without management controls
- space detail updates after add or remove without full page reload
- validation and server errors render in the UI
- router prevents unauthenticated access using the existing auth store flow

## 9. Out-of-scope items

The first implementation does not include:
- resource upload inside spaces
- announcements or activity feeds
- lecturer co-ownership or delegated moderators
- bulk CSV membership assignment
- search, pagination, or invite workflows

### Implemented follow-on integration note
Assignments are now linked to spaces through `Assignment.spaceId` in the backend.
This means:
- students see assignments only for spaces they currently belong to
- lecturers can create assignments only in spaces they own, unless acting as `ADMIN`
- assignment-linked student submission/grade self-service now depends on current space membership

## 10. Implementation order

1. Liquibase schema for `spaces` and `space_memberships`
2. entities, repositories, DTOs, and service authorization rules
3. controller endpoints and audit actions
4. backend integration tests
5. frontend types and API client
6. Vue routes, list view, and detail-management view
# Exam Scheduling Technical Specification

## 1. Feature goal

Introduce the first exam-administration feature for EduSecure by adding a space-scoped exam scheduling workflow that fits the repository's current access model.

The initial delivery scope is intentionally narrow:
- lecturers create and manage exam schedule entries only for spaces they own
- admins create and manage exam schedule entries for any space
- students see published exam schedule entries only for spaces they belong to
- unpublished exam entries remain staff-only
- schedule entries are validated for sensible timing and basic overlap conflicts inside the same space
- exam scheduling is implemented as a conventional CRUD-style domain with audit coverage, not as a full exam-board or invigilation system

This feature inherits the existing backend-issued `HttpOnly` cookie session model, existing roles, and the current `Space` membership boundary.

## 2. Problem analysis

The repository currently has a secure coursework workflow but no separate exam administration model. That creates a gap against the assignment brief in two places:
- staff cannot create or publish a formal exam timetable
- students cannot open a dedicated exam schedule view independent of coursework deadlines

The smallest technically honest solution is to add a new domain object for scheduled exams that is attached to an existing `Space`. That choice reuses:
- the existing lecturer ownership rule through `space.createdByUserId`
- the existing student visibility rule through `space_memberships`
- the existing audit service for staff actions
- the current frontend browser session model and page routing style

This first P2 slice does not attempt to solve:
- exam results publication
- seating plans or invigilator allocation
- cross-space university-wide clash detection
- room-booking integration with an external calendar system

Those remain later extensions. The current goal is a defensible timetable layer that can support later exam-result or attendance work.

## 3. Roles and permissions

### 3.1 Role definitions

- `STUDENT`: read published exams for spaces they belong to
- `LECTURER`: create, update, and list exams for spaces they own
- `ADMIN`: create, update, and list exams for any space

### 3.2 Permission matrix

| Action | ADMIN | LECTURER | STUDENT |
|---|---|---|---|
| Create exam | Any space | Owned spaces only | No |
| Update exam | Any space | Owned spaces only | No |
| List exams | All exams | Owned-space exams | Published exams in enrolled spaces only |
| View individual exam | Any exam | Owned-space exams | Published exams in enrolled spaces only |

### 3.3 Authorization rules

- Students never create or modify exams.
- Lecturers can manage exams only where `space.createdByUserId == currentUser.id`.
- Admins can manage exams for any space.
- Students can see exams only where they are current members of the linked space.
- Students must not see unpublished exams.

## 4. Domain model

### 4.1 Exam entity

Purpose: a scheduled assessment event attached to a space.

Fields:
- `id: UUID`
- `spaceId: UUID` required
- `title: String` required, trimmed, 3 to 160 chars
- `description: String` optional, trimmed, 0 to 2000 chars
- `location: String` required, trimmed, 2 to 160 chars
- `startsAt: Instant` required
- `endsAt: Instant` required
- `published: boolean` required
- `createdByUserId: UUID` required
- `createdAt: Instant` required
- `updatedAt: Instant` required

Constraints:
- `endsAt` must be strictly after `startsAt`
- create requests must target a future start time
- archived spaces cannot receive new exams or exam updates
- the same space cannot contain overlapping exams where time windows intersect
- unpublished exams are staff-visible only

### 4.2 Read model expectations

Exam summary response must expose:
- `id`
- `spaceId`
- `spaceCode`
- `spaceName`
- `title`
- `description`
- `location`
- `startsAt`
- `endsAt`
- `published`
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `canManage`

## 5. Backend design

### 5.1 Packages

Add new feature packages following current conventions:
- `entity.exam`
- `repository.exam`
- `dto.exam`
- `service.exam`
- `controller.exam`

### 5.2 Persistence

Liquibase change set adds:
- `exams`
- foreign keys to `spaces` and `users(created_by_user_id)`
- an index on `(space_id, starts_at)` for space timetable queries
- an index on `(published, starts_at)` for student-visible reads

### 5.3 Service responsibilities

`ExamService` must handle:
- creating exams for an authorized space
- validating timing and overlap conflicts
- listing exams according to caller role and visibility
- retrieving an individual exam with the same authorization rules
- updating exam fields and publish state under staff-only control
- audit logging for create and update actions

### 5.4 Validation rules

- `title` is required and limited to 160 characters after trim
- `description` is optional and limited to 2000 characters after trim
- `location` is required and limited to 160 characters after trim
- `startsAt` and `endsAt` are required
- `startsAt` must be in the future on create
- `endsAt` must be after `startsAt`
- a create or update request that introduces an overlap inside the same space must fail with `409 Conflict`
- archived spaces reject create and update actions with `409 Conflict`

### 5.5 Audit requirements

Add audit actions:
- `EXAM_CREATED`
- `EXAM_UPDATED`

Minimum audit detail payloads:
- create: `spaceCode=<CODE>,published=<BOOLEAN>,startsAt=<INSTANT>,endsAt=<INSTANT>`
- update: `spaceCode=<CODE>,published=<BOOLEAN>,startsAt=<INSTANT>,endsAt=<INSTANT>`

Audit payloads must avoid storing the free-text description.

## 6. API design

All endpoints require authentication and live under `/api/exams`.

### 6.1 Create exam

`POST /api/exams`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "spaceId": "uuid",
  "title": "Applied Cryptography Final Exam",
  "description": "Closed-book end-of-term assessment for the space.",
  "location": "Room B201",
  "startsAt": "2026-06-12T09:00:00Z",
  "endsAt": "2026-06-12T11:00:00Z",
  "published": true
}
```

Response `201 Created` returns the exam summary.

Failure cases:
- `400` invalid request body
- `403` caller lacks staff permission for the space
- `404` space not found
- `409` space archived or exam conflicts with another exam in the same space

### 6.2 List visible exams

`GET /api/exams`

Allowed roles:
- authenticated users

Behavior:
- admin receives all exams ordered by `startsAt`
- lecturer receives exams for owned spaces only
- student receives only published exams for enrolled spaces

### 6.3 Get exam by id

`GET /api/exams/{examId}`

Allowed roles:
- authenticated users, subject to visibility rules

### 6.4 Update exam

`PUT /api/exams/{examId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body matches create shape.

Response `200 OK` returns the updated exam summary.

Failure cases:
- `400` invalid request body
- `403` caller lacks staff permission for the space
- `404` exam or space not found
- `409` archived space or overlapping exam conflict

## 7. Frontend design

### 7.1 Route and navigation

Add a top-level authenticated route:
- `/exams`

Add a navigation item in the main app shell:
- visible to all authenticated users

### 7.2 Screen behavior

Single page `ExamSchedule` should:
- load visible exams on mount
- show a chronological exam list for all roles
- show a create form only for lecturers and admins
- show an inline edit form for manageable exams
- show clear published or draft status badges
- show the linked space code and name for context

### 7.3 Frontend API client

Add `examsService` with:
- `list()`
- `create(payload)`
- `update(examId, payload)`

Add exam types in `frontend/src/types/exam.ts`.

## 8. Test design and acceptance criteria

### 8.1 Backend integration tests

Add an exam flow integration test class covering at minimum:
- lecturer creates and updates an exam for an owned space
- student sees only published exams in enrolled spaces
- student cannot see draft exams
- lecturer cannot create or update an exam for another lecturer's space
- overlapping exam creation in the same space fails with `409 Conflict`
- exam create and update actions write audit rows with non-empty integrity values

### 8.2 Acceptance criteria

The feature is acceptable when:
- staff can create a future exam in a valid space
- student schedule view is filtered to enrolled spaces and published records only
- draft exams remain hidden from students
- overlapping same-space exams are rejected
- archived spaces reject exam changes
- the frontend exposes one working schedule page and the backend tests pass

## 9. Out-of-scope for this delivery

- exam results and transcripts
- room-capacity validation or seating plans
- recurring assessments
- calendar export and external timetable sync
- feedback-form workflows
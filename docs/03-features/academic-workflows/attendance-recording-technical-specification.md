# Attendance Recording Technical Specification

## 1. Feature goal

Introduce attendance recording as the first operational teaching feature for EduSecure by adding a space-scoped attendance workflow that fits the repository's current access model.

The initial delivery scope is intentionally narrow:
- lecturers create and manage attendance sessions only for spaces they own
- admins create and manage attendance sessions for any space
- students see attendance only for sessions linked to spaces they currently belong to
- each attendance session snapshots the current student roster at creation time so later membership changes do not erase historical attendance evidence
- staff record per-student attendance using bounded status values rather than free-text notes
- the feature provides basic reporting totals, not a full timetabling or analytics suite

This feature inherits the existing backend-issued `HttpOnly` cookie session model, current role model, current `Space` ownership rule, and existing `Space` membership boundary.

## 2. Problem analysis

The repository already supports secure spaces, assignments, submissions, grades, exam scheduling, exam results, and feedback forms. However, it still lacks a teaching-operations workflow for confirming whether students attended teaching sessions.

That creates a gap against the assignment brief because staff currently cannot:
- create a dedicated attendance session for a class meeting
- record presence, absence, lateness, or excused absence per student
- review a compact attendance summary for a session

The smallest technically honest solution is to add a new attendance domain that attaches directly to `Space`.

This choice reuses:
- lecturer ownership through `space.createdByUserId`
- student visibility through `space_memberships`
- the existing audit service for staff actions
- the current frontend route, session, and page conventions

A key design decision for this feature is to snapshot the session roster when a session is created. That avoids a common integrity problem where attendance history silently changes later if students are added to or removed from a space after the class already happened.

This first P3 slice does not attempt to solve:
- automatic timetabling or recurring teaching schedules
- QR code or geolocation check-in
- real-time mobile attendance capture
- university-wide reporting dashboards
- attendance alerts, notifications, or escalation workflows

## 3. Roles and permissions

### 3.1 Role definitions

- `STUDENT`: read own attendance status for sessions in spaces they currently belong to
- `LECTURER`: create attendance sessions, update attendance sessions, and record attendance only for spaces they own
- `ADMIN`: create attendance sessions, update attendance sessions, and record attendance for any space

### 3.2 Permission matrix

| Action | ADMIN | LECTURER | STUDENT |
|---|---|---|---|
| Create attendance session | Any space | Owned spaces only | No |
| Update attendance session metadata | Any space | Owned spaces only | No |
| List attendance sessions | All sessions | Owned-space sessions | Enrolled-space sessions only |
| View session roster and records | Any session | Owned-space sessions | No |
| Record or revise attendance | Any session | Owned-space sessions | No |
| View own attendance status | No special route needed; staff already see summaries | No special route needed; staff already see summaries | Yes, for enrolled-space sessions only |

### 3.3 Authorization rules

- Students never create or modify attendance sessions or attendance records.
- Lecturers can manage attendance only where `space.createdByUserId == currentUser.id`.
- Admins can manage attendance for any space.
- Students can see attendance only where they are current members of the linked space.
- Session roster detail is staff-only because it exposes other students' identities.

## 4. Domain model

### 4.1 Attendance status enum

`AttendanceStatus` values:
- `PRESENT`
- `LATE`
- `ABSENT`
- `EXCUSED`

These values are intentionally closed and auditable.

### 4.2 AttendanceSession entity

Purpose: a scheduled teaching or attendance-taking event attached to a space.

Fields:
- `id: UUID`
- `spaceId: UUID` required
- `title: String` required, trimmed, 3 to 160 chars
- `description: String` optional, trimmed, 0 to 1000 chars
- `startsAt: Instant` required
- `endsAt: Instant` required
- `createdByUserId: UUID` required
- `createdAt: Instant` required
- `updatedAt: Instant` required

Constraints:
- `endsAt` must be strictly after `startsAt`
- archived spaces cannot receive new attendance sessions or session updates
- session ownership is inherited from the linked space, not stored separately as a new authorization rule

### 4.3 AttendanceRecord entity

Purpose: immutable roster snapshot entry plus mutable attendance status for one student in one attendance session.

Fields:
- `id: UUID`
- `sessionId: UUID` required
- `studentUserId: UUID` required
- `status: AttendanceStatus` optional until staff records attendance
- `recordedByUserId: UUID` optional
- `recordedAt: Instant` optional

Constraints:
- one record exists for each student who was a member of the space at session creation time
- `(session_id, student_user_id)` must be unique
- `recordedByUserId` and `recordedAt` must be populated together whenever `status` is populated
- if `status` is cleared back to null, `recordedByUserId` and `recordedAt` must also become null

### 4.4 Roster snapshot rule

When a staff user creates an attendance session, the backend must:
1. resolve the target space
2. verify staff authority for that space
3. read the current `space_memberships`
4. create one `AttendanceRecord` row per current member with `status = null`

This makes the session roster historically stable even if the space membership changes later.

### 4.5 Read model expectations

Attendance session summary response must expose:
- `id`
- `spaceId`
- `spaceCode`
- `spaceName`
- `title`
- `description`
- `startsAt`
- `endsAt`
- `createdByUserId`
- `createdAt`
- `updatedAt`
- `canManage`
- `myStatus` nullable for students and optionally null for staff
- `memberCount`
- `recordedCount`
- `presentCount`
- `lateCount`
- `absentCount`
- `excusedCount`

Session roster response must expose:
- session summary metadata
- per-student rows containing `studentUserId`, `studentEmail`, `studentFullName`, `status`, `recordedByUserId`, and `recordedAt`

## 5. Backend design

### 5.1 Packages

Add new feature packages following current conventions:
- `entity.attendance`
- `repository.attendance`
- `dto.attendance`
- `service.attendance`
- `controller.attendance`

### 5.2 Persistence

Liquibase change set adds:
- `attendance_sessions`
- `attendance_records`
- foreign keys to `spaces`, `users(created_by_user_id)`, `users(student_user_id)`, and `users(recorded_by_user_id)`
- index on `(space_id, starts_at)` for session listing
- unique constraint on `(session_id, student_user_id)`
- index on `(student_user_id, session_id)` for own-attendance lookups

### 5.3 Service responsibilities

`AttendanceService` must handle:
- creating a session for an authorized space
- snapshotting the current roster into `attendance_records`
- listing sessions according to caller role and visibility
- updating session metadata under staff-only control
- returning a staff-only session roster view
- applying batch attendance updates against the snapshot roster
- computing summary counts per session
- audit logging for session create, session update, and attendance recording changes

### 5.4 Validation rules

- `title` is required and limited to 160 characters after trim
- `description` is optional and limited to 1000 characters after trim
- `startsAt` and `endsAt` are required
- `endsAt` must be after `startsAt`
- archived spaces reject session create and session update with `409 Conflict`
- attendance updates must reference only students already present in the session snapshot roster
- duplicate student ids inside one batch update request must be rejected with `400 Bad Request`

### 5.5 Audit requirements

Add audit actions:
- `ATTENDANCE_SESSION_CREATED`
- `ATTENDANCE_SESSION_UPDATED`
- `ATTENDANCE_RECORDS_UPDATED`

Minimum audit detail payloads:
- session create: `spaceCode=<CODE>,memberCount=<COUNT>,startsAt=<INSTANT>,endsAt=<INSTANT>`
- session update: `spaceCode=<CODE>,memberCount=<COUNT>,startsAt=<INSTANT>,endsAt=<INSTANT>`
- records update: `spaceCode=<CODE>,sessionId=<UUID>,updatedCount=<COUNT>`

Audit payloads must avoid storing free-text description content or student email addresses.

## 6. API design

All endpoints require authentication.

### 6.1 Create attendance session

`POST /api/attendance-sessions`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "spaceId": "uuid",
  "title": "Week 4 cryptography seminar",
  "description": "Attendance taken during the applied cryptography seminar.",
  "startsAt": "2026-05-14T09:00:00Z",
  "endsAt": "2026-05-14T11:00:00Z"
}
```

Response `201 Created` returns the session summary including snapshot counts.

### 6.2 List visible attendance sessions

`GET /api/attendance-sessions`

Allowed roles:
- authenticated users

Behavior:
- admin receives all sessions ordered by `startsAt` descending
- lecturer receives sessions for owned spaces only
- student receives sessions for enrolled spaces only, with `myStatus` populated from the student's own attendance record where available

### 6.3 Update attendance session metadata

`PUT /api/attendance-sessions/{sessionId}`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "title": "Week 4 cryptography seminar",
  "description": "Updated teaching-session description.",
  "startsAt": "2026-05-14T09:00:00Z",
  "endsAt": "2026-05-14T11:00:00Z"
}
```

Response `200 OK` returns the updated session summary.

### 6.4 Get session roster and attendance detail

`GET /api/attendance-sessions/{sessionId}/records`

Allowed roles:
- `LECTURER`
- `ADMIN`

Response `200 OK` returns the session summary plus per-student roster rows.

### 6.5 Record attendance for a session

`PUT /api/attendance-sessions/{sessionId}/records`

Allowed roles:
- `LECTURER`
- `ADMIN`

Request body:

```json
{
  "records": [
    {
      "studentUserId": "uuid",
      "status": "PRESENT"
    },
    {
      "studentUserId": "uuid",
      "status": "LATE"
    },
    {
      "studentUserId": "uuid",
      "status": null
    }
  ]
}
```

Behavior:
- non-null `status` records or updates attendance for that student
- `null` clears an existing attendance status for that student while keeping the snapshot roster row

Response `200 OK` returns the refreshed session roster response.

Failure cases:
- `400` invalid request body or duplicate `studentUserId` entries
- `403` caller lacks staff permission for the space
- `404` session not found
- `409` archived space or request includes a student not present in the session roster

## 7. Frontend design

### 7.1 Route and navigation

Add a top-level authenticated route:
- `/attendance`

Add a navigation item in the main app shell:
- visible to all authenticated users

### 7.2 Screen behavior

Single page `Attendance` should:
- load visible attendance sessions on mount
- show a create form only for lecturers and admins
- show session summaries for all roles
- show a staff-only roster editor for a selected manageable session
- show student-specific own-status badges when the caller is a student
- show basic per-session reporting totals for present, late, absent, excused, and not-yet-recorded rows

### 7.3 Frontend API client

Add `attendanceService` with:
- `listSessions()`
- `createSession(payload)`
- `updateSession(sessionId, payload)`
- `getSessionRecords(sessionId)`
- `updateSessionRecords(sessionId, payload)`

Add attendance types in `frontend/src/types/attendance.ts`.

## 8. Test design and acceptance criteria

### 8.1 Backend integration tests

Add an attendance flow integration test class covering at minimum:
- lecturer creates an attendance session for an owned space and the current roster is snapshotted
- lecturer records attendance statuses for that session
- student sees only attendance sessions for enrolled spaces and only their own status
- lecturer cannot manage another lecturer's attendance session
- archived spaces reject new attendance sessions and roster updates
- audit rows are written for session create, session update, and records update with non-empty integrity values

### 8.2 Acceptance criteria

The feature is acceptable when:
- staff can create an attendance session for a valid writable space
- session creation snapshots the current roster into attendance records
- staff can update attendance statuses without exposing roster detail to students
- students can view only own attendance status for sessions in spaces they belong to
- attendance summaries show basic reporting counts
- archived spaces reject attendance changes
- the frontend exposes one working attendance page and the backend tests pass

## 9. Out-of-scope for this delivery

- recurring session generation
- attendance notifications or threshold alerts
- biometric, QR, NFC, or GPS-based check-in
- anonymous attendance analytics
- integration with external timetable systems


# Attendance Phase Status and Evidence

This document records the current implemented status of the attendance-recording slice.

## 1. What is now implemented

The repository now includes a bounded attendance implementation for:
- `AttendanceSession`
- `AttendanceRecord`
- attendance session create, update, list, and staff roster-detail endpoints
- batch attendance status recording for a snapshotted session roster
- student-visible own-attendance status for sessions in currently accessible spaces
- audit records for attendance session create, update, and attendance-record update actions

## 2. Implemented endpoints in this phase

- `GET /api/attendance-sessions`
- `POST /api/attendance-sessions`
- `PUT /api/attendance-sessions/{sessionId}`
- `GET /api/attendance-sessions/{sessionId}/records`
- `PUT /api/attendance-sessions/{sessionId}/records`

## 3. Implemented security behavior

### Session creation and update
- allowed for the lecturer who owns the related space
- allowed for `ADMIN`
- denied for `STUDENT`
- requires the authenticated browser session established by the backend-issued `HttpOnly` auth cookie

### Attendance record visibility
- staff can open full roster detail only for spaces they are allowed to manage
- student callers can list only attendance sessions for spaces they currently belong to
- student callers receive only `myStatus` in session summaries and cannot open staff roster endpoints
- unrelated lecturers are denied access to another lecturer's attendance sessions

## 4. Implemented integrity behavior

### Roster snapshot policy
When a staff user creates an attendance session, the backend snapshots the current space roster into per-student attendance records. Later membership changes do not rewrite that historical roster.

### Attendance status recording
The backend currently supports the bounded status values:
- `PRESENT`
- `LATE`
- `ABSENT`
- `EXCUSED`

Batch updates reject duplicate student entries and reject students who are not part of the snapshotted session roster.

### Audit behavior
For attendance session creation, metadata updates, and roster recording updates, the backend now:
- writes append-oriented audit events
- computes non-empty integrity values
- avoids storing free-text description content or student email addresses in audit details

## 5. Current attendance audit actions created in code

Implemented actions currently include:
- `ATTENDANCE_SESSION_CREATED`
- `ATTENDANCE_SESSION_UPDATED`
- `ATTENDANCE_RECORDS_UPDATED`

## 6. Test evidence currently available

### New attendance-phase evidence
- `backend/src/test/java/edusecure/edusecure/AttendanceFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

This currently proves:
- lecturer can create an attendance session for an owned space
- session creation snapshots the current roster into attendance records
- later membership changes do not retroactively alter the session roster snapshot
- lecturer can record attendance statuses for a snapshotted roster
- students can see only attendance sessions for enrolled spaces and only their own attendance status
- students cannot open staff roster-detail endpoints
- unrelated lecturers cannot manage another lecturer's attendance sessions
- admins retain cross-space attendance oversight
- archived spaces reject new attendance changes
- duplicate student entries in one attendance batch update are rejected
- audit entries are created for attendance-sensitive actions
- audit entries contain non-empty integrity values

## 7. Frontend evidence currently available

- `frontend/src/pages/Attendance/index.vue`
- `frontend/src/services/attendance.ts`
- `frontend/src/types/attendance.ts`
- `frontend/src/router/index.ts`
- `frontend/src/components/AppShell.vue`

The frontend now provides:
- a top-level authenticated attendance page
- staff-only attendance session creation
- staff session metadata editing
- staff roster attendance recording UI
- student own-attendance status summaries
- basic per-session reporting totals

## 8. What is still not implemented

Within the attendance slice itself, the main still-open items are now:
- recurring timetable-linked session generation
- attendance notifications or absence-threshold alerts
- self-check-in workflows such as QR or mobile scanning
- broader analytics dashboards or export tooling

The current implementation is intentionally bounded to a credible attendance-recording workflow that matches the repository's existing space and authorization model.


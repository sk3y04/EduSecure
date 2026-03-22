# Grade Phase Status and Evidence

This document records the current implemented status of the Pack 05-aligned grade-integrity slice.

## 1. What is now implemented

The backend now includes a minimal grade-integrity implementation for:
- `Grade`
- `GradeRepository`
- grade create/update/retrieve endpoints
- verified-submission-only grading enforcement
- audit records for grade create and update actions
- student-only own-grade retrieval
- lecturer/admin review retrieval

## 2. Implemented endpoints in this phase

- `POST /api/submissions/{submissionId}/grade`
- `PUT /api/grades/{gradeId}`
- `GET /api/grades/{gradeId}`
- `GET /api/my/grades/{gradeId}`

## 3. Implemented security behavior

### Grade creation and update
- allowed for `LECTURER` and `ADMIN`
- denied for `STUDENT`
- requires the authenticated browser session established by the backend-issued `HttpOnly` auth cookie

### Grade retrieval
- lecturer/admin can view full grade details
- student can view only their own grade via `/api/my/grades/{gradeId}`
- unrelated students are denied access

## 4. Implemented integrity behavior

### Verified-submission grading policy
The backend currently enforces the recommended Pack 05 rule:
- only `SubmissionVerificationStatus.VERIFIED` submissions can be graded

### Audit behavior
For grade creation and update, the backend now:
- writes append-oriented audit events
- computes non-empty HMAC-backed integrity values
- retains previous integrity support through the existing audit model

## 5. Current grade audit actions created in code

Implemented actions currently include:
- `GRADE_CREATED`
- `GRADE_UPDATED`

## 6. Test evidence currently available

### Existing tests still valid
- `backend/src/test/java/edusecure/edusecure/EduSecureApplicationTests.java`
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`

### New grade-phase evidence
- `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`

This currently proves:
- lecturer can create a grade for a verified submission
- lecturer can update an existing grade
- student can retrieve only their own grade
- student cannot create or update grades
- duplicate grade creation is rejected
- non-verified submission cannot be graded
- unrelated student cannot read another student's grade
- audit entries are created for grade-sensitive actions
- audit entries contain non-empty integrity values

## 7. What is still not implemented

Within the grade-integrity slice itself, the main still-open items are now:
- public audit review endpoints
- final report packaging/evidence assembly
- any later moderation or expanded academic workflow features beyond the current minimal grade flow

The separate AES-demo phase is now implemented elsewhere in the project; the main remaining work is evidence assembly and optional audit-surface expansion.


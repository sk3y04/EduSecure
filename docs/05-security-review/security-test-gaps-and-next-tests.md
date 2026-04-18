# Security Test Gaps and Next Tests

This document captures the highest-value follow-up work that emerged from reviewing the current implementation.

It is intentionally different from the main scenario matrix:
- the matrix tells you **what to test**
- this file tells you **where the code suggests a risk, ambiguity, or missing assurance**

## 1. Highest-priority policy and access-control questions

## 1.1 Cross-lecturer access to submissions should stay owner-scoped

Relevant code:
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- method: `requireAccessibleSubmission(...)`

What the code does now:
- `ADMIN` remains globally privileged for submission metadata and content access
- `LECTURER` access is scoped to assignments they own via `Assignment.createdByLecturerId`
- unrelated lecturers should be denied for submission metadata, content, and assignment submission listings

Why this matters:
- this is the intended least-privilege academic boundary for lecturer-owned assignment spaces
- regression here would reintroduce a high-severity broken-access-control issue

Manual test to run:
1. create lecturer A, lecturer B, and student S
2. have lecturer A create an assignment
3. have student S submit to lecturer A's assignment
4. sign in as lecturer B
5. request:
   - `GET /api/submissions/{submissionId}`
   - `GET /api/submissions/{submissionId}/content`
6. record whether lecturer B is denied with `403 Forbidden`

Decision now frozen in code/tests:
- lecturer-by-owner visibility for submissions
- admin-wide visibility for oversight

## 1.2 Cross-lecturer access to grades should stay owner-scoped

Relevant code:
- `backend/src/main/java/edusecure/edusecure/controller/grade/GradeController.java`
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java`

What the code does now:
- `ADMIN` remains globally allowed to create, update, and read grades
- `LECTURER` grade access is scoped to submissions whose assignments they own
- unrelated lecturers should be denied for grade create, read, and update actions

Why this matters:
- grade integrity is not only about students being blocked
- it also requires one lecturer being unable to alter another lecturer's grading domain

Manual tests to run:
- unrelated lecturer reads grade by `gradeId`
- unrelated lecturer updates grade by `gradeId`
- unrelated lecturer creates grade for another lecturer's assignment submission

Recommended outcome now frozen in code/tests:
- lecturer-by-owner visibility and mutation for grades
- admin-wide visibility and mutation for oversight

## 1.3 Student access after space-membership removal should be confirmed against policy

Relevant code:
- `backend/src/main/java/edusecure/edusecure/service/assignment/AssignmentService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java`

What the code does now:
- students only see assignments whose `spaceId` matches one of their current memberships
- student submission metadata/content reads re-check current assignment visibility through space membership
- student grade self-service reads re-check current assignment visibility through space membership
- once a student is removed from the assignment's space, those student-facing submission/grade reads return `403 Forbidden`

Why this matters:
- this is a strong least-privilege posture
- some academic workflows may instead expect historical access to remain available after class or space removal

Manual test to run:
- create a lecturer, student, space, assignment, submission, and grade
- remove the student from the assignment's space
- call:
  - `GET /api/submissions/{submissionId}`
  - `GET /api/submissions/{submissionId}/content`
  - `GET /api/my/grades/{gradeId}`
  - `GET /api/my/submissions/{submissionId}/grade`
- confirm whether immediate `403 Forbidden` matches the intended academic policy

## 2. Browser and deployment hardening checks that still need explicit proof

## 2.1 CSRF posture needs a browser-level review

Relevant code:
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- CSRF is enabled for unsafe methods through Spring Security's cookie-token pattern
- auth is cookie-backed
- cookie uses `SameSite=Lax` by default

What this means:
- server-side anti-CSRF enforcement is now present through the `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header requirement
- the remaining review should confirm that hostile-origin requests fail in real browser conditions and that the frontend bootstrap/header behavior stays aligned with deployment topology

Manual tests to run:
- from a different origin, attempt a form post or scripted request to a state-changing endpoint while the victim is logged in
- try endpoints such as:
  - `POST /api/auth/logout`
  - `POST /api/auth/mfa/setup`
  - `POST /api/spaces`
  - `POST /api/submissions/{submissionId}/grade`
- validate actual browser behavior, not just raw HTTP server behavior

What to capture:
- whether the cookie is sent
- whether CORS allows the response to be read
- whether the action succeeds or fails
- whether production cookie settings differ from local development as expected

## 2.2 CORS should be tested from an unapproved origin

Relevant code:
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`

What the code does now:
- allowed origins are configuration-driven
- credentials are allowed
- allowed headers are wildcarded

Why this matters:
- with credentialed browser sessions, a mistaken origin allowlist can become a serious data-exposure issue

Manual tests to run:
- send browser requests from an origin not listed in `app.cors.allowed-origins`
- confirm the API does not echo permissive CORS headers for that origin
- confirm credentialed reads are blocked by the browser

## 2.3 Production secrets must be reviewed directly

Relevant code/config:
- `backend/src/main/resources/application.properties`

Observed risk:
- the repository contains default fallback values for:
  - `jwt.secret`
  - `mfa.secret-encryption-key`
  - `audit.hmac-secret`
  - `submission.storage.master-key`

Why this matters:
- defaults in source are acceptable for local development only
- if any real deployment accidentally uses them, token signing, audit integrity, MFA secret protection, and submission-at-rest protection are weakened materially

Review action:
- confirm deployed environments override all of these values
- confirm secrets are stored in environment variables or a secrets manager
- confirm no lower environment with real data is still using the repo defaults

## 3. Tests that would strengthen assurance if added to the automated suite

These are the best next automated additions if you want to convert more of the manual scenarios into repository evidence.

### Recommended additions

1. **Unauthenticated submission endpoint tests**
   - assert `401` for `GET /api/submissions/{id}`
   - assert `401` for `GET /api/submissions/{id}/content`

2. **Submission filename traversal test**
   - upload with `../` or `..\\` in the filename and assert `400`

3. **Closed-assignment submission test**
   - set assignment `open=false` and assert submission attempt returns `409`

4. **Student forbidden on privileged grade read endpoints**
   - assert `403` for `GET /api/grades/{id}`
   - assert `403` for `GET /api/submissions/{submissionId}/grade`

5. **Student forbidden on `GET /api/my/submissions/{submissionId}/grade` for another student's submission**
   - this complements the existing `gradeId`-based forbidden test

6. **Member-view space confidentiality test**
   - assert that a student member can retrieve space details but does not receive full `memberships`

7. **MFA max-attempt test that explicitly checks `429`**
   - the code supports this behavior; an explicit integration test would strengthen the evidence

8. **Ownership-scope regression tests**
   - keep explicit regression tests for cross-lecturer submission denial
   - keep explicit regression tests for cross-lecturer grade denial

9. **Historical-access policy regression tests**
   - keep explicit tests for student assignment visibility through current space membership
   - keep explicit tests for student submission/grade denial after membership removal if that policy remains intended

## 4. Severity-driven triage list

If you want a short list of what to investigate first, use this order:

### Immediate review
- student historical access after space-membership removal
- login brute-force resistance
- CSRF behavior in real browser conditions
- CORS allowlist behavior with credentials
- production secret overrides

### Important follow-up
- closed-assignment submission rejection
- filename traversal rejection
- student access to privileged grade read endpoints
- student member view of spaces does not reveal full rosters

### Lower risk / assurance improvement
- additional negative validation tests around malformed input
- explicit audit-log content review for non-secret details
- operational review of audit-log storage access restrictions

## 5. Practical pass/fail rule for your security review

Treat a scenario as **passed** only if:
- the endpoint returns the expected denial or controlled success
- no sensitive data is leaked in the response
- no unexpected side effect occurs
- audit behavior matches expectations for successful sensitive actions
- the result is aligned with your intended academic policy, not just with what the current code happens to do

That last point is especially important here: the biggest remaining questions in this codebase are not simple input-validation bugs, but **whether the implemented trust boundaries match the institutional policy you actually want**.


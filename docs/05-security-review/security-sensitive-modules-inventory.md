# Security-Sensitive Modules Inventory

This document maps the current backend implementation to the main security boundaries you should target during review.

## 1. Global authentication boundary

Source:
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`

Current rule:
- `OPTIONS /**` is public
- `GET /api/system/health` is public
- `/api/auth/register`, `/api/auth/login`, `/api/auth/logout`, and `/api/auth/mfa/verify` are public
- every other request requires authentication

Security meaning:
- endpoints without method-level role annotations may still be protected by the global authenticated-session requirement
- any endpoint that handles sensitive data but relies only on service-layer checks should still be tested for both `401` and `403` paths

High-value tests:
- unauthenticated access to protected endpoints returns `401`
- forged, expired, or tampered credentials are rejected before business logic runs

## 2. Session and auth-cookie controls

Sources:
- `backend/src/main/java/edusecure/edusecure/security/AuthCookieService.java`
- `backend/src/main/java/edusecure/edusecure/config/AuthCookieConfigurationValidator.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`

Current rule:
- auth uses an `HttpOnly` cookie named `EDUSECURE_AUTH` by default
- default cookie posture is `SameSite=Lax`
- default non-prod config sets `secure=false`
- prod profile requires `auth.cookie.secure=true`
- `SameSite=None` is forbidden unless `secure=true`
- unsafe browser requests are protected by Spring Security CSRF using the `XSRF-TOKEN` cookie and `X-XSRF-TOKEN` header pair

Security meaning:
- browser scripts should not be able to read the auth cookie directly
- production deployment should not start with insecure cookie transport settings
- browser-side CSRF now has a server-side token defence in addition to cookie settings and origin restrictions
- hostile-origin browser behavior still merits manual review, especially for deployment-specific cross-site topologies

High-value tests:
- tampered auth cookie is rejected
- tampered bearer token is rejected
- prod profile fails startup if `auth.cookie.secure=false`
- cross-site state-changing request behavior is validated manually

## 3. Auth and managed-user privilege model

Sources:
- `backend/src/main/java/edusecure/edusecure/controller/auth/AuthController.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`

Current rule:
- `POST /api/auth/register` creates only `STUDENT` accounts
- `POST /api/auth/users` is limited to `ADMIN` and `LECTURER`
- `ADMIN` can create `LECTURER` or `STUDENT`
- `LECTURER` can create `STUDENT` only
- no caller can create `ADMIN` through `POST /api/auth/users`

Security meaning:
- role escalation through the managed-user endpoint should be blocked
- student users should never be able to create staff accounts

High-value tests:
- student tries `POST /api/auth/users`
- lecturer tries to create lecturer/admin
- admin creates lecturer/student successfully
- duplicate email registration is rejected cleanly

## 4. MFA challenge and account-hardening boundary

Sources:
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`

Current rule:
- MFA-enabled users receive an `MFA_REQUIRED` login response instead of a full authenticated session
- MFA verification challenge has a TTL and maximum attempt count
- consumed challenges cannot be reused
- recovery codes are one-time use
- disabling MFA requires both password verification and TOTP/recovery-code verification

Security meaning:
- MFA replay and challenge reuse should fail
- partial login state should not unlock protected endpoints
- brute force should be bounded at the challenge level

High-value tests:
- replay a previously used MFA challenge
- submit invalid codes until the max-attempt boundary is reached
- try expired challenge
- try disabling MFA with wrong password and valid TOTP
- try disabling MFA with valid password and wrong TOTP

## 5. Submission metadata and submission-content access boundary

Sources:
- `backend/src/main/java/edusecure/edusecure/controller/submission/SubmissionController.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`

Current rule:
- `POST /api/assignments/{assignmentId}/submissions` requires `STUDENT`
- `GET /api/assignments/{assignmentId}/submissions/me` requires `STUDENT`
- `GET /api/assignments/{assignmentId}/submissions` requires `LECTURER` or `ADMIN`
- `GET /api/submissions/{submissionId}` and `GET /api/submissions/{submissionId}/content` require authentication globally, then service-level ownership/privilege checks
- service rule: owner student may access their own submission only while the related assignment remains visible through current space membership; `LECTURER` may access only submissions for assignments they own; `ADMIN` may access any submission

Security meaning:
- the most important broken-access-control test is whether one student can read another student's submission metadata or download content
- metadata and plaintext content are intentionally split; content access should be audited
- lecturer access is now owner-scoped by assignment; admin access remains global for oversight

High-value tests:
- student A tries to read student B's metadata
- student A tries to download student B's file
- unauthenticated caller tries both endpoints
- privileged user download is audited
- response does not leak storage internals such as ciphertext location, nonce, or wrapped key material

## 6. Submission upload validation boundary

Source:
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`

Current rule:
- empty files are rejected
- invalid filenames and path-like names containing `..` are rejected
- current limit is `5MB`
- only `text/plain` and `application/pdf` are supported
- `text/plain` must be valid UTF-8
- PDF uploads must start with a valid `%PDF-` header
- assignment must still be open

Security meaning:
- this protects against basic malicious upload abuse, path tricks, and unsafe content handling assumptions

High-value tests:
- upload empty file
- upload oversized file
- upload binary disguised as `text/plain`
- upload fake PDF without real PDF header
- upload filename containing traversal patterns
- upload after assignment is closed

## 7. Grade integrity and authorization boundary

Sources:
- `backend/src/main/java/edusecure/edusecure/controller/grade/GradeController.java`
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java`

Current rule:
- create/update/direct privileged retrieval endpoints require `LECTURER` or `ADMIN`
- student-facing grade endpoints require `STUDENT`
- student can only read grade if the underlying submission belongs to that student and the related assignment is still visible through the student's current space membership
- lecturer can create/read/update grades only for submissions whose assignments they own
- admin can create/read/update grades globally
- only verified submissions can be graded
- duplicate grade creation is rejected
- valid grade range is `0..100`

Security meaning:
- a student should not be able to create, update, or read another student's grade
- grade tampering should be blocked by role checks and assignment-ownership checks
- privilege boundaries around `gradeId` and `submissionId` enumeration should be tested

High-value tests:
- student tries create/update grade
- student tries privileged grade read endpoints
- student tries to read another student's grade using `gradeId`
- create grade twice for same submission
- try grading failed-verification submission

## 8. Space-management authorization boundary

Sources:
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceController.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
- `docs/03-features/academic-workflows/space-management-technical-specification.md`

Current rule:
- `LECTURER` and `ADMIN` can create spaces
- `LECTURER` may manage only spaces they created
- `ADMIN` may manage any space
- students can view only spaces where they are members
- students cannot self-enroll, update metadata, or manipulate memberships
- roster details are returned only when `canManage=true`

Security meaning:
- this is one of the strongest ownership boundaries currently present in the codebase
- it is a good reference model for how cross-tenant ownership restrictions could look elsewhere

High-value tests:
- student tries to create/update/add/remove membership
- lecturer tries to update another lecturer's space
- lecturer tries to add/remove students in another lecturer's space
- student tries to open a non-member space directly by `spaceId`
- student confirms roster data is not exposed for member-only view

## 9. Audit integrity boundary

Sources:
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
- `backend/src/main/java/edusecure/edusecure/entity/audit/AuditLog.java`

Current rule:
- sensitive actions are recorded with an HMAC-backed integrity value
- each record includes the previous integrity value, forming a tamper-evident chain
- submission creation, verification result, content retrieval, grade create/update, and space-management actions are auditable

Security meaning:
- if a protected action succeeds, evidence should exist in audit storage
- audit details should not leak plaintext submissions, ciphertext blobs, nonces, or wrapped keys

High-value tests:
- successful sensitive actions generate audit rows
- integrity value is non-empty
- audit details remain concise and non-secret
- no ordinary public API exposes audit data to students

## 10. Policy-sensitive observations from code review

These are not necessarily defects, but they are security decisions that must be tested and approved explicitly:

1. **Assignment listing is now space-scoped for students and owner-scoped for lecturers.**
   - `AssignmentService.listAssignments(...)` returns all assignments only to `ADMIN`
   - lecturers see only assignments they created
   - students see only assignments whose `spaceId` matches a current membership
   - this behavior should now be treated as implemented policy rather than an open visibility question

2. **Brute-force protection is clearly visible for MFA challenge verification, but not clearly visible for login itself.**
   - `AuthService.login(...)` validates credentials, but no obvious account/IP rate-limit control is visible in the reviewed code
   - this should be tested and documented as a likely next-hardening area

3. **Student historical submission/grade access is revoked immediately after assignment-space membership removal.**
   - `SubmissionService.requireAccessibleSubmission(...)` and student-facing grade reads now re-check current assignment visibility through space membership
   - this is a deliberate least-privilege posture in code, but it should be confirmed against the intended academic retention/access policy


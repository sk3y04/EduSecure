# Security Test Scenarios Matrix

This matrix turns the implemented control surface into concrete abuse cases you can execute manually, with Postman, with browser dev tools, or by extending the integration-test suite.

## How to read this matrix

- **Priority**
  - `P1`: high-risk broken-access-control or auth-bypass test
  - `P2`: important integrity, validation, or privilege-boundary test
  - `P3`: configuration or assurance test
- **Expected result** is based on the current codebase.
- **Evidence status** tells you whether the repository already has strong automated coverage or whether this should be a manual review target.

## 1. Authentication, session, and MFA scenarios

| ID | Priority | Scenario | Actor | Target | Expected secure result | Evidence status |
|---|---|---|---|---|---|---|
| AUTH-01 | P1 | Unauthenticated user requests current identity | No session | `GET /api/auth/me` | `401 Unauthorized` | Already evidenced in `AuthControllerIntegrationTests` |
| AUTH-02 | P1 | Tampered auth cookie is replayed | Any user with modified cookie | protected endpoint such as `GET /api/auth/me` | `401 Unauthorized` | Already evidenced |
| AUTH-03 | P1 | Tampered bearer token is replayed | Any user with modified bearer token | `GET /api/auth/me` | `401 Unauthorized` | Already evidenced |
| AUTH-04 | P1 | Student attempts managed-user creation | `STUDENT` | `POST /api/auth/users` | `403 Forbidden` | Already evidenced |
| AUTH-05 | P1 | Lecturer attempts to create another lecturer | `LECTURER` | `POST /api/auth/users` with role `LECTURER` | `403 Forbidden` | Already evidenced |
| AUTH-06 | P1 | Lecturer attempts to create admin | `LECTURER` | `POST /api/auth/users` with role `ADMIN` | `403 Forbidden` | Already evidenced |
| AUTH-07 | P2 | Replay an already-consumed MFA challenge | MFA-enabled user | `POST /api/auth/mfa/verify` twice with same challenge | second call returns `410 Gone` | Already evidenced |
| AUTH-08 | P2 | Submit an expired MFA challenge | MFA-enabled user | `POST /api/auth/mfa/verify` after TTL | `410 Gone` | Already evidenced |
| AUTH-09 | P2 | Reuse a recovery code | MFA-enabled user | `POST /api/auth/mfa/verify` using same recovery code twice | second call returns `401 Unauthorized` | Already evidenced |
| AUTH-10 | P2 | Exhaust MFA verification attempts | MFA-enabled user / attacker with password | repeated `POST /api/auth/mfa/verify` invalid codes | capped with `429 Too Many Requests` | Code path exists; verify manually if not already asserted end-to-end |
| AUTH-11 | P2 | Disable MFA with wrong password | Authenticated MFA-enabled user | `POST /api/auth/mfa/disable` | `401 Unauthorized` | Already evidenced |
| AUTH-12 | P2 | Disable MFA with wrong TOTP/recovery code | Authenticated MFA-enabled user | `POST /api/auth/mfa/disable` | `401 Unauthorized` | Manual follow-up recommended |
| AUTH-13 | P2 | Weak password registration | New user | `POST /api/auth/register` | `400 Bad Request` with field errors | Already evidenced |
| AUTH-14 | P1 | Password brute-force against login endpoint | External attacker | `POST /api/auth/login` repeated attempts | ideally throttled, locked, or detected; current code does not clearly show login rate limiting | Manual high-priority gap test |
| AUTH-15 | P3 | CSRF-like cross-site state-changing request using browser cookie | External site with victim browser | any state-changing endpoint such as `POST /api/auth/logout`, `POST /api/auth/mfa/setup`, `POST /api/submissions/.../grade` | request should fail unless the hostile origin can supply a valid CSRF token/header pair; verify in a real browser as deployment defence-in-depth | Backend CSRF enforcement now implemented; browser hostile-origin review still recommended |

## 2. Submission access-control and file-retrieval scenarios

| ID | Priority | Scenario | Actor | Target | Expected secure result | Evidence status |
|---|---|---|---|---|---|---|
| SUB-01 | P1 | Unauthenticated submission metadata read | No session | `GET /api/submissions/{submissionId}` | `401 Unauthorized` | Manual review target |
| SUB-02 | P1 | Unauthenticated submission content download | No session | `GET /api/submissions/{submissionId}/content` | `401 Unauthorized` | Manual review target |
| SUB-03 | P1 | Student reads another student's submission metadata | `STUDENT` | `GET /api/submissions/{submissionId}` | `403 Forbidden` | Already evidenced |
| SUB-04 | P1 | Student downloads another student's submission file | `STUDENT` | `GET /api/submissions/{submissionId}/content` | `403 Forbidden` | Already evidenced |
| SUB-05 | P1 | Student attempts lecturer-only assignment submission listing | `STUDENT` | `GET /api/assignments/{assignmentId}/submissions` | `403 Forbidden` | Manual follow-up recommended |
| SUB-06 | P2 | Student uploads unsupported content type | `STUDENT` | `POST /api/assignments/{assignmentId}/submissions` | `415 Unsupported Media Type` | Already evidenced |
| SUB-07 | P2 | Student uploads fake PDF with `.pdf` name but invalid header | `STUDENT` | submission upload | `415 Unsupported Media Type` | Already evidenced |
| SUB-08 | P2 | Student uploads invalid UTF-8 declared as `text/plain` | `STUDENT` | submission upload | `400 Bad Request` | Already evidenced |
| SUB-09 | P2 | Student uploads oversized file | `STUDENT` | submission upload | `413 Payload Too Large` | Already evidenced |
| SUB-10 | P2 | Student uploads empty file | `STUDENT` | submission upload | `400 Bad Request` | Already evidenced |
| SUB-11 | P2 | Student uploads path-traversal style filename | `STUDENT` | filename like `..\\secret.txt` or `../secret.txt` | `400 Bad Request` | Manual follow-up recommended |
| SUB-12 | P2 | Student submits after assignment closes | `STUDENT` | submission upload to closed assignment | `409 Conflict` | Manual follow-up recommended |
| SUB-13 | P2 | Submission metadata response leaks storage internals | any authorized reader | `GET /api/submissions/{submissionId}` | response should not contain `storedFileReference`, wrapped keys, nonce, or ciphertext | Partially evidenced (`storedFileReference` already checked); extend manually |
| SUB-14 | P2 | Successful privileged/plaintext download leaves audit evidence | `STUDENT`, `LECTURER`, or `ADMIN` with valid access | `GET /api/submissions/{submissionId}/content` | download succeeds and audit row exists with non-secret details | Already evidenced for success path |
| SUB-15 | P1 | Lecturer from unrelated context downloads another lecturer's student's submission | `LECTURER` | `GET /api/submissions/{submissionId}/content` | `403 Forbidden` because lecturer access is scoped to owned assignments | Already evidenced in `SubmissionFlowIntegrationTests` |
| SUB-16 | P2 | Student tries to read own submission after losing membership in the assignment's space | `STUDENT` | `GET /api/submissions/{submissionId}` or `GET /api/submissions/{submissionId}/content` | `403 Forbidden` under the current least-privilege model | Already evidenced in `SubmissionFlowIntegrationTests`; confirm policy manually |

## 3. Grade integrity and privilege-abuse scenarios

| ID | Priority | Scenario | Actor | Target | Expected secure result | Evidence status |
|---|---|---|---|---|---|---|
| GRADE-01 | P1 | Student creates a grade | `STUDENT` | `POST /api/submissions/{submissionId}/grade` | `403 Forbidden` | Already evidenced |
| GRADE-02 | P1 | Student updates a grade | `STUDENT` | `PUT /api/grades/{gradeId}` | `403 Forbidden` | Already evidenced |
| GRADE-03 | P1 | Student calls privileged grade-read endpoint | `STUDENT` | `GET /api/grades/{gradeId}` or `GET /api/submissions/{submissionId}/grade` | `403 Forbidden` | Manual follow-up recommended |
| GRADE-04 | P1 | Student reads another student's grade through student endpoint | `STUDENT` | `GET /api/my/grades/{gradeId}` | `403 Forbidden` | Already evidenced |
| GRADE-05 | P1 | Student reads another student's grade by submission id | `STUDENT` | `GET /api/my/submissions/{submissionId}/grade` | `403 Forbidden` | Manual follow-up recommended |
| GRADE-06 | P2 | Create duplicate grade for same submission | `LECTURER`/`ADMIN` | `POST /api/submissions/{submissionId}/grade` twice | second request returns `409 Conflict` | Already evidenced |
| GRADE-07 | P2 | Grade non-verified submission | `LECTURER`/`ADMIN` | create/update grade for failed verification submission | `422 Unprocessable Content` | Already evidenced for create path |
| GRADE-08 | P2 | Grade outside 0..100 range | `LECTURER`/`ADMIN` | create/update grade | `400 Bad Request` | Already evidenced for create path |
| GRADE-09 | P2 | Grade creation/update should be audited | `LECTURER`/`ADMIN` | create/update flows | audit rows exist with non-empty integrity values | Already evidenced |
| GRADE-10 | P1 | Unrelated lecturer reads or changes another lecturer's grade | `LECTURER` | `GET /api/grades/{gradeId}` or `PUT /api/grades/{gradeId}` | `403 Forbidden` because grade access is scoped to owned assignments | Already evidenced in `GradeFlowIntegrationTests` |
| GRADE-11 | P1 | Unrelated lecturer creates a grade on another lecturer's assignment submission | `LECTURER` | `POST /api/submissions/{submissionId}/grade` | `403 Forbidden` because grade creation is scoped to owned assignments | Already evidenced in `GradeFlowIntegrationTests` |
| GRADE-12 | P2 | Student tries to read own grade after losing membership in the assignment's space | `STUDENT` | `GET /api/my/grades/{gradeId}` or `GET /api/my/submissions/{submissionId}/grade` | `403 Forbidden` under the current least-privilege model | Already evidenced in `GradeFlowIntegrationTests`; confirm policy manually |

## 4. Space-management authorization scenarios

| ID | Priority | Scenario | Actor | Target | Expected secure result | Evidence status |
|---|---|---|---|---|---|---|
| SPACE-01 | P1 | Student creates a space | `STUDENT` | `POST /api/spaces` | `403 Forbidden` | Manual follow-up recommended |
| SPACE-02 | P1 | Student updates a space | `STUDENT` | `PUT /api/spaces/{spaceId}` | `403 Forbidden` | Manual follow-up recommended |
| SPACE-03 | P1 | Student self-enrolls into a space | `STUDENT` | `POST /api/spaces/{spaceId}/students` | `403 Forbidden` | Manual follow-up recommended |
| SPACE-04 | P1 | Student removes another student from a space | `STUDENT` | `DELETE /api/spaces/{spaceId}/students/{studentUserId}` | `403 Forbidden` | Manual follow-up recommended |
| SPACE-05 | P1 | Student opens non-member space directly by id | `STUDENT` | `GET /api/spaces/{spaceId}` | `403 Forbidden` | Covered by service logic; verify manually or via existing tests |
| SPACE-06 | P1 | Lecturer updates another lecturer's space | `LECTURER` | `PUT /api/spaces/{spaceId}` | `403 Forbidden` | Already evidenced by `SpaceFlowIntegrationTests` |
| SPACE-07 | P1 | Lecturer adds student to another lecturer's space | `LECTURER` | `POST /api/spaces/{spaceId}/students` | `403 Forbidden` | Already evidenced / code strongly suggests this |
| SPACE-08 | P1 | Lecturer removes student from another lecturer's space | `LECTURER` | `DELETE /api/spaces/{spaceId}/students/{studentUserId}` | `403 Forbidden` | Already evidenced / code strongly suggests this |
| SPACE-09 | P2 | Admin manages any lecturer-owned space | `ADMIN` | update/add/remove membership on any space | success if resource exists | Already evidenced |
| SPACE-10 | P2 | Student who is a member sees the space but not full roster | `STUDENT` member | `GET /api/spaces/{spaceId}` | allowed view, but `memberships` should be empty because `canManage=false` | Manual high-value confidentiality check |
| SPACE-11 | P2 | Archived space rejects new membership additions | `LECTURER`/`ADMIN` | `POST /api/spaces/{spaceId}/students` on archived space | `409 Conflict` | Code path exists; verify manually |

## 5. Audit, crypto, and configuration assurance scenarios

| ID | Priority | Scenario | Actor | Target | Expected secure result | Evidence status |
|---|---|---|---|---|---|---|
| AUDIT-01 | P2 | Sensitive actions generate audit records | Privileged or owner actor | submission create/download, grade create/update, space changes | audit rows written | Already evidenced for key flows |
| AUDIT-02 | P2 | Audit integrity field is non-empty and chained | DB reviewer / automated test | `audit_logs` rows | `integrityValue` populated, `previousIntegrityValue` chained where applicable | Already evidenced in tests; chain logic visible in code |
| AUDIT-03 | P2 | Audit details do not leak plaintext submissions or key material | DB reviewer | `audit_logs.details_json` values | details remain concise and non-secret | Manual follow-up recommended |
| CFG-01 | P3 | Production profile rejects insecure cookie config | deployment validation | startup with `prod` and `auth.cookie.secure=false` | application startup fails | Already evidenced by configuration tests and validator code |
| CFG-02 | P3 | `SameSite=None` without `Secure=true` is rejected | deployment validation | startup with invalid cookie settings | application startup fails | Already evidenced by validator code/tests |
| CFG-03 | P1 | Deployment does not rely on hard-coded fallback secrets | ops/security reviewer | env/config review for `JWT_SECRET`, `AUDIT_HMAC_SECRET`, MFA key, submission master key | production must override defaults; defaults in source should never remain live | Manual high-priority configuration review |
| CORS-01 | P2 | Disallowed origin cannot use credentialed cross-origin access | external browser origin | browser request from unapproved `Origin` | browser should not receive permissive CORS headers | Manual high-priority browser test |
| CSRF-01 | P1 | Cookie-authenticated state-changing request from hostile origin | hostile web page + victim browser | any unsafe method endpoint | should fail because the server now requires the `XSRF-TOKEN`/`X-XSRF-TOKEN` pair in addition to browser cookie/origin protections | Backend CSRF enforcement implemented; manual high-priority browser test still recommended |

## 6. Suggested execution order

If you only have time for a focused expert review, start with these scenarios first:

1. `SUB-03` and `SUB-04` — broken object-level authorization on submissions
2. `GRADE-01` to `GRADE-05` — unprivileged grade access and tampering
3. `SPACE-06` to `SPACE-08` — ownership enforcement between lecturers
4. `AUTH-07` to `AUTH-12` — MFA replay, reuse, and disable-abuse paths
5. `SUB-15`, `GRADE-10`, and `GRADE-11` — owner-scope regression tests for lecturer access
6. `SUB-16` and `GRADE-12` — confirm whether post-membership student access revocation matches policy
7. `CFG-03`, `CORS-01`, and `CSRF-01` — deployment/security-hardening validation

## 7. Evidence to capture for each scenario

For each executed test, save:
- actor identity and role
- request path and method
- request body if relevant
- response status code
- response body
- whether the action produced or did not produce an audit record
- whether the result matches the expected security posture
- whether the scenario reveals a code bug, a deployment bug, or a policy ambiguity


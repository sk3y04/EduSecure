 # Manual Security Testing Playbook

This playbook turns Pack 11 into a practical manual review runbook.

Use it when you want to execute the scenarios yourself as a security reviewer using:
- a browser and dev tools
- Postman or Insomnia
- optional database inspection for audit evidence

This file is intentionally procedural.
For the security rationale and code mapping, use:
- `security-sensitive-modules-inventory.md`
- `security-test-scenarios-matrix.md`
- `security-test-gaps-and-next-tests.md`

---

## 1. Goal of this playbook

The purpose of this playbook is to help you answer questions like:
- can an unauthenticated or unprivileged user read protected submission data?
- can a student create, modify, or read grades they should not control?
- can one lecturer manage another lecturer's protected area?
- can MFA be replayed, brute-forced, or disabled improperly?
- do browser-session settings behave safely under CORS and cross-site conditions?

This playbook is based on the **currently implemented backend and frontend contract** in the repository.

---

## 2. Recommended test tooling

## 2.1 Minimum tools

Recommended:
- browser: Chrome or Edge
- API client: Postman or Insomnia
- optional DB client: IntelliJ database tool, DBeaver, or `psql`
- optional authenticator app for MFA: Microsoft Authenticator, Google Authenticator, Authy, 1Password, etc.

## 2.2 Why both browser and API-client testing matter

Use the API client when you want:
- exact control of request bodies
- reusable collections/environments
- fast negative testing
- manual cookie reuse between accounts

Use the browser when you want to test:
- cookie transport behavior
- CORS and SameSite effects
- CSRF-related behavior
- frontend state restoration and role-aware UI behavior

---

## 3. Environment preparation

## 3.1 Base assumptions from the repository

From the reviewed repository:
- backend default port is `8080`
- frontend development origin is expected to be `http://localhost:5173`
- backend default allowed frontend origin is `http://localhost:5173`
- auth is transported in an `HttpOnly` cookie named `EDUSECURE_AUTH`
- development defaults use `SameSite=Lax` and `Secure=false`
- production should use the `prod` profile and secure cookies

Related references:
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`
- `docs/pack-03/api-auth-contract.md`
- `frontend/README.md`

## 3.2 Confirm the target environment before you start

Record these values before testing:

| Item | Example | Your value |
|---|---|---|
| Backend base URL | `http://localhost:8080` | |
| Frontend URL | `http://localhost:5173` | |
| Active profile | `default` or `prod` | |
| Cookie secure flag | `false` locally / `true` in prod | |
| Allowed frontend origins | `http://localhost:5173` | |
| Database used | H2 / PostgreSQL | |

## 3.3 Suggested run posture

For access-control tests, local development is fine.
For cookie/CORS/CSRF tests, run both:
- local development profile
- production-like profile if available

Why:
- local behavior shows implementation defaults
- production-like behavior shows whether cookie/CORS settings become stricter as intended

---

## 4. Test personas to prepare

Create or obtain these accounts before you begin:

| Persona | Purpose |
|---|---|
| `ADMIN_A` | managed-user creation and global admin checks |
| `LECTURER_A` | owner lecturer |
| `LECTURER_B` | unrelated lecturer for cross-owner checks |
| `STUDENT_A` | owner student |
| `STUDENT_B` | unrelated student |
| `MFA_STUDENT` | MFA replay/lockout/disable testing |

## 4.1 Suggested naming convention

Use unique emails so your evidence stays traceable, for example:
- `admin-a+20260405@example.com`
- `lecturer-a+20260405@example.com`
- `lecturer-b+20260405@example.com`
- `student-a+20260405@example.com`
- `student-b+20260405@example.com`
- `mfa-student+20260405@example.com`

## 4.2 Account creation strategy

Use `POST /api/auth/register` for ordinary student self-registration.
Use `POST /api/auth/users` for staff-managed account creation.

Expected implemented rules:
- self-registration creates only `STUDENT`
- `ADMIN` may create `LECTURER` and `STUDENT`
- `LECTURER` may create `STUDENT` only
- nobody may create `ADMIN` via managed-user creation

---

## 5. Session handling guidance

## 5.1 Postman / Insomnia

Recommended setup:
- create one environment per persona, or
- create one collection with separate cookie jars for each persona

Important note:
- because auth is `HttpOnly` cookie-based, you do **not** need to copy JWTs into request bodies or headers for the main browser-style tests
- let the API client store the `Set-Cookie` response automatically whenever possible

Useful checkpoints after login:
- verify `Set-Cookie` exists
- verify cookie name is `EDUSECURE_AUTH`
- verify `HttpOnly` is present
- record `SameSite`, `Path`, and `Secure`

## 5.2 Browser

In dev tools, use:
- **Network** tab to inspect request/response behavior
- **Application > Cookies** to confirm cookie attributes
- **Console** only for same-origin frontend checks, not for reading the JWT directly

Expected behavior:
- frontend uses credentialed requests
- browser stores the auth cookie
- frontend should not expose the JWT in local storage

---

## 6. Evidence capture template

For every scenario, capture these fields:

| Field | What to record |
|---|---|
| Scenario ID | e.g. `SUB-04` |
| Actor | e.g. `STUDENT_B` |
| Endpoint | e.g. `GET /api/submissions/{submissionId}/content` |
| Preconditions | account and data setup |
| Request body | exact JSON if any |
| Response status | e.g. `403` |
| Response body | exact error or success payload |
| Cookie/CORS observations | if relevant |
| Audit evidence | present / absent / not checked |
| Pass/Fail | against intended policy |
| Notes | bug, ambiguity, or deployment issue |

Recommended screenshot set:
- request in client/browser
- response body/status
- cookie attributes if relevant
- audit row or DB evidence if checked

---

## 7. Data setup sequence for the core review

Use this order so later scenarios reuse earlier data.

1. Create `ADMIN_A`
2. Create `LECTURER_A` and `LECTURER_B`
3. Create `STUDENT_A` and `STUDENT_B`
4. Log in as `LECTURER_A` and create an assignment
5. Log in as `STUDENT_A` and submit a file to that assignment
6. Log in as `LECTURER_A` and create a grade for `STUDENT_A`
7. Log in as `LECTURER_A` and create a space
8. Add `STUDENT_A` to that space
9. Prepare `MFA_STUDENT` and enable MFA

If you do this once, most of the playbook can reuse the same IDs.

---

## 8. Priority execution path

If you want the fastest high-value review, execute in this order:

1. submission broken-access-control tests
2. grade privilege and ownership tests
3. space ownership and roster confidentiality tests
4. MFA replay / attempt-limit / disable-abuse tests
5. browser-side CORS and CSRF checks
6. configuration and deployment secret review

---

## 9. Step-by-step execution guidance

## 9.1 Submission security review

### A. Student cannot read another student's submission metadata

Scenario ID:
- `SUB-03`

Preconditions:
- `LECTURER_A` has created an assignment
- `STUDENT_A` has submitted a file
- you know the resulting `submissionId`

Steps:
1. Log in as `STUDENT_B`
2. Send `GET /api/submissions/{submissionId}`
3. Record the status and body

Expected secure result:
- `403 Forbidden`
- response should not leak protected metadata for the other student's object

### B. Student cannot download another student's file content

Scenario ID:
- `SUB-04`

Steps:
1. Stay logged in as `STUDENT_B`
2. Send `GET /api/submissions/{submissionId}/content`
3. Record the status and body

Expected secure result:
- `403 Forbidden`
- no file content is returned

### C. Unauthenticated caller cannot access submission endpoints

Scenario IDs:
- `SUB-01`
- `SUB-02`

Steps:
1. Clear the auth cookie or use a fresh unauthenticated client
2. Request:
   - `GET /api/submissions/{submissionId}`
   - `GET /api/submissions/{submissionId}/content`

Expected secure result:
- both return `401 Unauthorized`

### D. Traversal-style filename is rejected on upload

Related automated gap now covered:
- traversal filename rejection

Steps:
1. Log in as `STUDENT_A`
2. Submit multipart upload to `POST /api/assignments/{assignmentId}/submissions`
3. Use a suspicious filename such as:
   - `../secret.txt`
   - `..\\secret.txt`
4. Keep content otherwise valid `text/plain`

Expected secure result:
- `400 Bad Request`
- message indicates invalid submission filename

### E. Closed assignment rejects new submissions

Steps:
1. Close or prepare a closed assignment if your environment supports state manipulation
2. Log in as `STUDENT_A`
3. Attempt upload to the closed assignment

Expected secure result:
- `409 Conflict`
- message indicates assignment is closed

### F. Cross-lecturer submission access policy test

Scenario ID:
- `SUB-15`

Why this matters:
- current code suggests any `LECTURER` can access any submission
- you must decide whether that is policy or defect

Steps:
1. Ensure `LECTURER_A` owns the assignment
2. Ensure `STUDENT_A` submitted to it
3. Log in as unrelated `LECTURER_B`
4. Request:
   - `GET /api/submissions/{submissionId}`
   - `GET /api/submissions/{submissionId}/content`

Record carefully:
- whether access is granted
- whether your academic policy says it should be granted

Interpretation:
- if granted and policy says this is acceptable, document that explicitly
- if granted and policy says only owner lecturer should access it, log it as a high-severity authorization issue

---

## 9.2 Grade integrity review

### A. Student cannot create or update grades

Scenario IDs:
- `GRADE-01`
- `GRADE-02`

Preconditions:
- there is a valid `submissionId`
- there is an existing `gradeId` for update testing

Steps:
1. Log in as `STUDENT_A` or `STUDENT_B`
2. Attempt:
   - `POST /api/submissions/{submissionId}/grade`
   - `PUT /api/grades/{gradeId}`

Expected secure result:
- `403 Forbidden`

### B. Student cannot use privileged grade-read endpoints

Scenario ID:
- `GRADE-03`

Steps:
1. Log in as `STUDENT_A` or `STUDENT_B`
2. Request:
   - `GET /api/grades/{gradeId}`
   - `GET /api/submissions/{submissionId}/grade`

Expected secure result:
- `403 Forbidden`

### C. Student cannot read another student's grade through student endpoints

Scenario IDs:
- `GRADE-04`
- `GRADE-05`

Steps:
1. Log in as `STUDENT_B`
2. Request:
   - `GET /api/my/grades/{gradeId}`
   - `GET /api/my/submissions/{submissionId}/grade`

Expected secure result:
- `403 Forbidden`

### D. Duplicate grade creation is rejected

Scenario ID:
- `GRADE-06`

Steps:
1. Log in as `LECTURER_A`
2. Create one grade for the submission
3. Repeat the same grade-creation request

Expected secure result:
- second request returns `409 Conflict`

### E. Non-verified submission cannot be graded

Scenario ID:
- `GRADE-07`

Steps:
1. Prepare a submission with failed or non-verified status
2. Log in as `LECTURER_A`
3. Attempt grade creation

Expected secure result:
- `422 Unprocessable Content`

### F. Cross-lecturer grade policy tests

Scenario IDs:
- `GRADE-10`
- `GRADE-11`

Steps:
1. Ensure `LECTURER_A` owns the assignment and the original grade context
2. Log in as unrelated `LECTURER_B`
3. Attempt:
   - `GET /api/grades/{gradeId}`
   - `PUT /api/grades/{gradeId}`
   - `POST /api/submissions/{submissionId}/grade` when no grade exists yet

Interpretation:
- if allowed, compare with intended academic moderation policy
- if your intended policy is owner-only grading, treat allowance as a serious authorization issue

---

## 9.3 Space-management review

### A. Student cannot manage spaces

Scenario IDs:
- `SPACE-01`
- `SPACE-02`
- `SPACE-03`
- `SPACE-04`

Steps:
1. Log in as `STUDENT_A` or `STUDENT_B`
2. Attempt:
   - `POST /api/spaces`
   - `PUT /api/spaces/{spaceId}`
   - `POST /api/spaces/{spaceId}/students`
   - `DELETE /api/spaces/{spaceId}/students/{studentUserId}`

Expected secure result:
- all return `403 Forbidden`

### B. Lecturer cannot manage another lecturer's space

Scenario IDs:
- `SPACE-06`
- `SPACE-07`
- `SPACE-08`

Steps:
1. Have `LECTURER_A` create a space
2. Log in as unrelated `LECTURER_B`
3. Attempt:
   - update the space
   - add a student to the space
   - remove a student from the space

Expected secure result:
- `403 Forbidden`

### C. Student member can view a space but not the roster

Scenario ID:
- `SPACE-10`

Preconditions:
- `LECTURER_A` created a space
- `STUDENT_A` is a member
- there may be other student members as well

Steps:
1. Log in as `STUDENT_A`
2. Request `GET /api/spaces/{spaceId}`

Expected secure result:
- request succeeds
- `canManage=false`
- `isMember=true`
- `memberships` is empty or withheld for non-managers

This is an important confidentiality check, not just an authorization check.

---

## 9.4 MFA manual review

### A. MFA setup and enablement works normally

Preconditions:
- use the `MFA_STUDENT` account

Steps:
1. Log in as `MFA_STUDENT`
2. Request `POST /api/auth/mfa/setup`
3. Capture `manualEntryKey` and/or QR-compatible `otpauthUri`
4. Add the secret to an authenticator app
5. Generate a current TOTP code
6. Request `POST /api/auth/mfa/enable`

Expected secure result:
- returns `mfaEnabled=true`
- recovery codes are shown once

### B. MFA login challenge cannot be replayed

Scenario ID:
- `AUTH-07`

Steps:
1. Log out
2. Log in again as `MFA_STUDENT`
3. Capture `challengeId`
4. Verify successfully once using `POST /api/auth/mfa/verify`
5. Replay the same request again with the same `challengeId`

Expected secure result:
- second call returns `410 Gone`

### C. MFA recovery code cannot be reused

Scenario ID:
- `AUTH-09`

Steps:
1. Start MFA login challenge
2. Verify with a recovery code
3. Start a new challenge
4. Try to verify with the same recovery code again

Expected secure result:
- second use fails with `401 Unauthorized`

### D. MFA challenge lockout after repeated failures

Scenario ID:
- `AUTH-10`

Steps:
1. Start MFA login challenge
2. Submit an invalid code repeatedly until limit is reached
3. Record the response on the final denied attempt
4. Try again with a valid code after lockout

Expected secure result:
- final failure returns `429 Too Many Requests`
- valid code still fails after the challenge is locked

### E. MFA disable requires both password and MFA proof

Scenario IDs:
- `AUTH-11`
- `AUTH-12`

Steps:
1. While logged in as `MFA_STUDENT`, request `POST /api/auth/mfa/disable`
2. Try these variants:
   - wrong password + correct TOTP
   - correct password + wrong TOTP
   - correct password + correct TOTP

Expected secure result:
- first two fail with `401 Unauthorized`
- last one succeeds with `204 No Content`

---

## 9.5 Browser-only CSRF and CORS review

These tests should be run in a real browser, not only in Postman.

### A. CSRF-oriented check for cookie-backed unsafe requests

Scenario IDs:
- `AUTH-15`
- `CSRF-01`

Why this matters:
- server-side CSRF protection is now enabled for unsafe requests through the `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header pair
- browser/deployment review is still needed to confirm hostile-origin requests cannot satisfy the token requirement accidentally and that cross-site deployment choices do not weaken the posture

Test idea:
1. Keep the victim logged into EduSecure in one tab
2. From a different origin, attempt a cross-site form submit or script-driven request to a state-changing endpoint such as:
   - `POST /api/auth/logout`
   - `POST /api/auth/mfa/setup`
   - `POST /api/spaces`
   - `POST /api/submissions/{submissionId}/grade`
3. Observe whether the cookie is sent and whether the action succeeds

What to record:
- whether request was sent with cookies
- whether browser blocked the request
- whether server processed the state change
- whether result differs between dev and prod cookie settings

### B. CORS allowlist verification from an unapproved origin

Scenario ID:
- `CORS-01`

Steps:
1. Use a browser page hosted on an origin not listed in `APP_CORS_ALLOWED_ORIGINS`
2. Attempt credentialed fetches to the API
3. Check network response headers

Expected secure result:
- API should not grant a permissive credentialed CORS response to the unapproved origin
- browser should block script access to the response

Important note:
- CORS is not an authorization control by itself
- you are checking whether the browser-exposure layer is correctly configured for the cookie-based session model

---

## 9.6 Configuration and deployment review

### A. Cookie hardening review

Review against:
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`
- `frontend/README.md`

Check:
- cookie is `HttpOnly`
- production uses `Secure=true`
- `SameSite=None` is never used without `Secure=true`
- `prod` profile fails fast for insecure combinations

### B. Secret-management review

High-priority manual review item:
- confirm deployed environments override fallback defaults for:
  - `JWT_SECRET`
  - `MFA_SECRET_ENCRYPTION_KEY`
  - `AUDIT_HMAC_SECRET`
  - `SUBMISSION_STORAGE_MASTER_KEY`

Expected secure result:
- no real environment relies on source-controlled fallback secrets

### C. Audit-evidence spot check

Optional but recommended:
- after successful sensitive actions, inspect audit storage if available
- confirm:
  - rows exist for expected actions
  - integrity values are non-empty
  - audit details do not leak plaintext, ciphertext, nonce, or wrapped keys

---

## 10. Recommended report wording for findings

When a test passes because access is denied correctly:
- "The system correctly enforced role and object-level authorization by returning `403 Forbidden` and withholding sensitive content."

When a test passes because unauthenticated access is denied:
- "The protected endpoint remained inaccessible without a valid authenticated session and returned `401 Unauthorized`."

When a behavior is allowed but policy is uncertain:
- "The implementation currently permits this action for the tested role. This is acceptable only if the intended academic policy explicitly authorizes cross-owner access. Otherwise, it should be treated as a broken-access-control defect."

When a browser/config issue is found:
- "The backend behavior may be functionally correct at the API layer, but the browser-session deployment posture requires hardening because cookie/CORS/CSRF assumptions are not sufficiently enforced in the tested environment."

---

## 11. Suggested one-day manual review schedule

### Session 1 — Access control
- run submission tests
- run grade tests
- run space tests

### Session 2 — Auth hardening
- run MFA setup/replay/reuse/lockout tests
- run managed-user privilege tests if needed

### Session 3 — Browser/deployment checks
- run CSRF-style browser tests
- run CORS unapproved-origin checks
- inspect cookie settings and environment configuration
- inspect optional audit evidence

---

## 12. Exit criteria

Treat Pack 11 manual execution as complete only when:
- high-priority scenarios have been executed or consciously deferred
- every executed scenario has recorded evidence
- policy-sensitive outcomes are explicitly classified as either intended or defective
- browser/deployment assumptions have been tested, not merely inferred
- any newly discovered issue is linked back to:
  - a specific endpoint
  - a specific role/actor
  - a concrete response/effect
  - a severity and remediation recommendation


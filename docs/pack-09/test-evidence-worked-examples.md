# Test Evidence Worked Examples

This document provides **worked sample records** showing how to fill in `docs/pack-09/test-evidence-collection-template.md`.

Important scope note:
- these are **example writeups** based on the implemented Pack 11 scenario design and the repository's current evidenced behavior
- they are meant to show the standard and style of evidence capture
- they should not be presented as fresh environment-specific execution evidence unless you actually run the scenarios and attach real screenshots/exports

Use this file as a model when creating your own final evidence records.

Related files:
- `docs/pack-09/test-evidence-collection-template.md`
- `docs/pack-11/security-test-scenarios-matrix.md`
- `docs/pack-11/manual-security-testing-playbook.md`

---

## Example 1 — `SUB-04` Student downloads another student's submission content

```markdown
# Scenario Evidence Record

## A. Traceability
- Scenario ID: `SUB-04`
- Scenario title: Student downloads another student's submission file
- Source document:
  - `docs/pack-11/security-test-scenarios-matrix.md`
  - `docs/pack-11/manual-security-testing-playbook.md`
- Priority: `P1`
- Date executed: 2026-04-05
- Tester: Example template record

## B. Environment
- Backend base URL: `http://localhost:8080`
- Frontend URL (if relevant): `http://localhost:5173`
- Active profile: `default`
- Database used: H2 for automated evidence / local runtime for manual execution
- Browser or API client used: Postman
- Cookie posture observed:
  - `HttpOnly`: yes
  - `Secure`: no (development default)
  - `SameSite`: `Lax`

## C. Actor and target
- Actor identity/email: `student-b+pack11@example.com`
- Actor role: `STUDENT`
- Target endpoint: `/api/submissions/{submissionId}/content`
- HTTP method: `GET`
- Target entity IDs:
  - assignmentId: captured earlier in setup
  - submissionId: captured earlier in setup
  - gradeId: not applicable
  - spaceId: not applicable
  - challengeId: not applicable

## D. Preconditions
- Accounts prepared: `LECTURER_A`, `STUDENT_A`, `STUDENT_B`
- Seed data prepared: assignment created by `LECTURER_A`; submission uploaded by `STUDENT_A`
- Authentication state before request: authenticated session for `STUDENT_B`
- Extra setup notes: target `submissionId` belongs to a different student

## E. Request summary
- Request body used: none
- Request headers of note: none beyond normal cookie session handling
- Cookie state of note: valid `EDUSECURE_AUTH` cookie for `STUDENT_B`
- File uploaded (if any): none

## F. Expected result
- Expected status code: `403 Forbidden`
- Expected response behavior: no file bytes returned; caller is denied access
- Expected side effects: no state change
- Expected audit behavior: no successful content-access audit should be created for a denied request

## G. Observed result
- Observed status code: `403 Forbidden`
- Observed response body summary: protected resource access denied for a non-owner student
- Observed response headers/cookies summary: normal error response; no new auth cookie expected
- Observed side effects: none
- Observed audit result: not expected for denied access path

## H. Evidence captured
- Screenshot(s): `2026-04-05_SUB-04_student-b_download-other-student-content_response.png`
- Postman export / response capture: `2026-04-05_SUB-04_student-b_download-other-student-content_postman.json`
- Browser network capture: not required for this API-only example
- Cookie screenshot: optional
- Audit screenshot / DB output: optional note showing no successful content-access event for this denied request
- Additional notes file: `2026-04-05_SUB-04_student-b_download-other-student-content_notes.md`

## I. Assessment
- Outcome:
  - Pass
- Severity if failed:
  - High
- Is this a code defect, deployment defect, or policy ambiguity?: none observed in this run; control behaved as intended
- Does the result match the intended academic policy?: yes, assuming students must not access each other's protected submissions

## J. Report-ready observation
- Short factual statement for report: "A non-owner student was prevented from downloading another student's submission content and received `403 Forbidden`, demonstrating object-level access control on the plaintext retrieval endpoint."
- Residual limitation or caveat: this does not prove browser-side CSRF/CORS posture; it proves the server-side authorization boundary for this endpoint
- Recommended remediation or next step: retain this as a core broken-access-control evidence item in the report appendix
```

---

## Example 2 — `AUTH-07` Replay consumed MFA challenge

```markdown
# Scenario Evidence Record

## A. Traceability
- Scenario ID: `AUTH-07`
- Scenario title: Replay an already-consumed MFA challenge
- Source document:
  - `docs/pack-11/security-test-scenarios-matrix.md`
  - `docs/pack-11/manual-security-testing-playbook.md`
- Priority: `P2`
- Date executed: 2026-04-05
- Tester: Example template record

## B. Environment
- Backend base URL: `http://localhost:8080`
- Frontend URL (if relevant): `http://localhost:5173`
- Active profile: `default`
- Database used: H2 for automated evidence / local runtime for manual execution
- Browser or API client used: Postman plus authenticator app
- Cookie posture observed:
  - `HttpOnly`: yes
  - `Secure`: no (development default)
  - `SameSite`: `Lax`

## C. Actor and target
- Actor identity/email: `mfa-student+pack11@example.com`
- Actor role: `STUDENT`
- Target endpoint: `/api/auth/mfa/verify`
- HTTP method: `POST`
- Target entity IDs:
  - assignmentId: not applicable
  - submissionId: not applicable
  - gradeId: not applicable
  - spaceId: not applicable
  - challengeId: captured from MFA-required login response

## D. Preconditions
- Accounts prepared: `MFA_STUDENT`
- Seed data prepared: MFA setup and enablement already completed
- Authentication state before request: password accepted, login response returned `MFA_REQUIRED`
- Extra setup notes: first verification request must succeed before replaying the same challenge

## E. Request summary
- Request body used: JSON containing the captured `challengeId` and a valid current TOTP code
- Request headers of note: `Content-Type: application/json`
- Cookie state of note: challenge response has not yet established a final authenticated session until successful verify
- File uploaded (if any): none

## F. Expected result
- Expected status code: second use of the same challenge should return `410 Gone`
- Expected response behavior: challenge is treated as expired/no longer valid after successful consumption
- Expected side effects: no second authenticated session should be issued from the same challenge
- Expected audit behavior: not the focus of this scenario

## G. Observed result
- Observed status code: `410 Gone` on replay attempt
- Observed response body summary: MFA challenge is expired or no longer valid after first successful verification
- Observed response headers/cookies summary: no second success cookie issuance expected from replayed request
- Observed side effects: replay blocked; no duplicate successful MFA completion
- Observed audit result: not checked in this worked example

## H. Evidence captured
- Screenshot(s): `2026-04-05_AUTH-07_mfa-replay_response.png`
- Postman export / response capture: `2026-04-05_AUTH-07_mfa-replay_postman.json`
- Browser network capture: optional if run through frontend flow
- Cookie screenshot: optional if showing session issuance only on first verify
- Audit screenshot / DB output: not required for this scenario
- Additional notes file: `2026-04-05_AUTH-07_mfa-replay_notes.md`

## I. Assessment
- Outcome:
  - Pass
- Severity if failed:
  - High
- Is this a code defect, deployment defect, or policy ambiguity?: none observed in this run; replay protection behaved as intended
- Does the result match the intended academic policy?: yes, MFA challenges should be one-time use only

## J. Report-ready observation
- Short factual statement for report: "A previously consumed MFA challenge could not be replayed; the system returned `410 Gone`, showing that challenge state is enforced after successful MFA completion."
- Residual limitation or caveat: this proves replay protection for the challenge object, not broader phishing resistance or device trust features
- Recommended remediation or next step: use alongside the MFA lockout and recovery-code one-time-use evidence for a stronger MFA-hardening section
```

---

## Example 3 — `GRADE-03` Student uses privileged grade-read endpoint

```markdown
# Scenario Evidence Record

## A. Traceability
- Scenario ID: `GRADE-03`
- Scenario title: Student calls privileged grade-read endpoint
- Source document:
  - `docs/pack-11/security-test-scenarios-matrix.md`
  - `docs/pack-11/manual-security-testing-playbook.md`
- Priority: `P1`
- Date executed: 2026-04-05
- Tester: Example template record

## B. Environment
- Backend base URL: `http://localhost:8080`
- Frontend URL (if relevant): `http://localhost:5173`
- Active profile: `default`
- Database used: H2 for automated evidence / local runtime for manual execution
- Browser or API client used: Postman
- Cookie posture observed:
  - `HttpOnly`: yes
  - `Secure`: no (development default)
  - `SameSite`: `Lax`

## C. Actor and target
- Actor identity/email: `student-b+pack11@example.com`
- Actor role: `STUDENT`
- Target endpoint: `/api/grades/{gradeId}`
- HTTP method: `GET`
- Target entity IDs:
  - assignmentId: captured earlier in setup
  - submissionId: captured earlier in setup
  - gradeId: captured earlier in setup
  - spaceId: not applicable
  - challengeId: not applicable

## D. Preconditions
- Accounts prepared: `LECTURER_A`, `STUDENT_A`, `STUDENT_B`
- Seed data prepared: a grade exists for `STUDENT_A`'s verified submission
- Authentication state before request: valid student session for `STUDENT_B`
- Extra setup notes: this endpoint is reserved for `LECTURER` / `ADMIN` callers

## E. Request summary
- Request body used: none
- Request headers of note: none beyond authenticated cookie session
- Cookie state of note: valid `EDUSECURE_AUTH` cookie for `STUDENT_B`
- File uploaded (if any): none

## F. Expected result
- Expected status code: `403 Forbidden`
- Expected response behavior: student cannot use privileged grade retrieval path
- Expected side effects: none
- Expected audit behavior: none required for denied read attempt

## G. Observed result
- Observed status code: `403 Forbidden`
- Observed response body summary: access denied because student role is not allowed on the privileged endpoint
- Observed response headers/cookies summary: no session change expected
- Observed side effects: none
- Observed audit result: not checked in this worked example

## H. Evidence captured
- Screenshot(s): `2026-04-05_GRADE-03_student-b_privileged-grade-read_response.png`
- Postman export / response capture: `2026-04-05_GRADE-03_student-b_privileged-grade-read_postman.json`
- Browser network capture: optional
- Cookie screenshot: optional
- Audit screenshot / DB output: not required for denied access example
- Additional notes file: `2026-04-05_GRADE-03_student-b_privileged-grade-read_notes.md`

## I. Assessment
- Outcome:
  - Pass
- Severity if failed:
  - High
- Is this a code defect, deployment defect, or policy ambiguity?: none observed in this run; the endpoint was correctly restricted by role
- Does the result match the intended academic policy?: yes, direct privileged grade endpoints should not be callable by students

## J. Report-ready observation
- Short factual statement for report: "A student caller was denied access to the privileged grade retrieval endpoint with `403 Forbidden`, showing that direct grade-inspection endpoints remain restricted to staff roles."
- Residual limitation or caveat: this does not replace the need to test student-facing ownership checks such as `/api/my/grades/{gradeId}` and `/api/my/submissions/{submissionId}/grade`
- Recommended remediation or next step: pair this with student-ownership checks and owner-scope lecturer regression checks to present a balanced grade-security section
```

---

## How to use these examples in practice

1. Copy the nearest example from this file.
2. Replace the sample values with the real environment, actor, IDs, and file references.
3. Replace the illustrative evidence filenames with real screenshots/exports.
4. Keep the assessment wording short and factual.
5. Only mark a scenario as final evidence once the actual outputs are attached.

## Bottom line

These worked examples exist to reduce friction.

Instead of starting from a blank template every time, you now have realistic sample records for:
- broken-access-control evidence (`SUB-04`)
- MFA replay evidence (`AUTH-07`)
- privileged grade-read denial (`GRADE-03`)

That should make it much easier to build a consistent appendix and a stronger Section 8 technical artefact summary.


# Pack 11 Postman Bundle

This folder contains an importable Postman structure for the Pack 11 security review.

## Files

- `EduSecure-Pack-11.postman_collection.json`
- `EduSecure-Local.postman_environment.json`
- `EduSecure-ProdLike.postman_environment.json`
- `../postman-fixtures/README.md`
- `../postman-fixtures/student-a.txt`
- `../postman-fixtures/traversal.txt`

## Purpose

The collection mirrors the API-executable parts of:
- `docs/pack-11/security-test-scenarios-matrix.md`
- `docs/pack-11/manual-security-testing-playbook.md`
- `docs/pack-11/security-test-gaps-and-next-tests.md`

It is designed for:
- auth and privilege-boundary checks
- MFA challenge and replay checks
- submission access-control checks
- grade-integrity checks
- space-authorization checks

It is **not** the right tool for fully proving:
- browser `SameSite` behavior
- true browser-enforced CORS behavior
- CSRF behavior in a real hostile-origin page
- database-side audit inspection
- deployment secret handling

Those still belong in the browser/config/manual steps in `manual-security-testing-playbook.md`.

## Import order

1. Import `EduSecure-Pack-11.postman_collection.json`
2. Import one environment file:
   - `EduSecure-Local.postman_environment.json` for localhost work
   - `EduSecure-ProdLike.postman_environment.json` for a production-like API target
3. Select the environment in Postman
4. Fill in the credential variables before running requests

## Cookie handling model

EduSecure uses an `HttpOnly` auth cookie.

Recommended usage in Postman:
- let Postman store cookies in its cookie jar automatically
- log in as the actor you need before running a scenario request
- do **not** try to manually parse or inject the JWT for normal Pack 11 testing unless you are deliberately testing bearer-token compatibility

Because the cookie jar is shared per API origin, the simplest flow is:
1. run the actor-specific login request
2. immediately run the protected request for that same actor
3. switch actor by running another login request

## Built-in assertion behavior

The collection now includes built-in Postman tests for many requests, including:
- login success checks
- expected `authStatus` checks
- key setup request success codes
- captured ID checks for assignment/submission/grade/space setup
- deterministic response-shape checks for seeded assignment, submission, grade, and space requests
- deterministic confidentiality checks such as `SUB-13` and roster-suppression checks for `SPACE-10`
- expected `401`, `403`, `410`, and `429` outcomes on key negative scenarios

Important distinction:
- ordinary security checks are asserted directly
- assignment-visibility questions remain policy-sensitive
- cross-lecturer submission/grade probes should now be treated as owner-scope regression checks with expected `403` denial

## Variables you must set

At minimum, review these environment values:
- `baseUrl`
- `frontendOrigin`
- `hostileOrigin`
- `adminEmail`
- `adminPassword`
- `lecturerAEmail`
- `lecturerAPassword`
- `lecturerBEmail`
- `lecturerBPassword`
- `studentAEmail`
- `studentAPassword`
- `studentBEmail`
- `studentBPassword`
- `mfaStudentEmail`
- `mfaStudentPassword`
- `studentAUploadPath`
- `studentTraversalUploadPath`
- `mfaEnableCode`
- `mfaLoginCode`
- `mfaDisableCode`
- `invalidMfaCode`

Notes:
- `frontendOrigin` and `hostileOrigin` are kept in the environment as reference values for the browser/CORS/CSRF checks described in `manual-security-testing-playbook.md`.
- The imported Postman requests do not currently send these origins automatically; they exist to keep the API-client bundle aligned with the wider Pack 11 manual review context.

The collection will populate these collection variables automatically when setup requests succeed:
- `assignmentId`
- `submissionId`
- `gradeId`
- `spaceId`
- `challengeId`
- `latestStatusCode`
- `latestResponseBody`
- `latestRequestName`
- `latestRequestMethod`
- `latestRequestUrl`
- `latestScenarioId`
- `latestPolicyProbe`

These latest-* variables are useful when copying evidence into the Pack 09 evidence log/template because they preserve the most recently executed request identity alongside the response outcome.

Variable still requiring manual attention for one policy probe:
- `submissionIdUngraded`
  - populate this manually with a verified submission that does not already have a grade before running `GRADE-11`

## Recommended run order

### 1. Persona and seed-data setup
Run the folder:
- `00 Persona & Seed Data`

This gives you:
- `LECTURER_A`
- `LECTURER_B`
- `STUDENT_A`
- `STUDENT_B`
- `MFA_STUDENT`
- an assignment created by `LECTURER_A`
- a submission created by `STUDENT_A`
- a grade created by `LECTURER_A`
- a space created by `LECTURER_A`

### 2. Run security folders in order
Then run:
- `01 Auth & Session`
- `02 MFA`
- `03 Submission Access`
- `04 Grade Integrity`
- `05 Space Authorization`

Notable deterministic evidence requests added/refined in the collection:
- `SUB-13 Authorized metadata omits storage internals`
- `SPACE-10 Student member views space without roster`
- richer MFA replay/lockout/disable assertions under `02 MFA`

## Important caveats

### Unauthenticated checks
Requests that should return `401` require a clean cookie jar or an unauthenticated client tab.

Before running those requests:
- clear Postman cookies for the API host, or
- open the request in a fresh workspace/client where no auth cookie exists

### File-upload checks
The upload requests use file-path variables such as `{{studentAUploadPath}}`.
Set those variables to real files on your machine before running them.

Suggested local examples:
```text
C:\Users\skey\IdeaProjects\EduSecure\docs\pack-11\postman-fixtures\student-a.txt
C:\Users\skey\IdeaProjects\EduSecure\docs\pack-11\postman-fixtures\traversal.txt
```

Repository fixtures are now provided in:
- `docs/pack-11/postman-fixtures/student-a.txt`
- `docs/pack-11/postman-fixtures/traversal.txt`

For the traversal scenario, remember that the security signal is the transmitted multipart filename.
If Postman preserves the on-disk filename only, manually rename the part to something like:
- `../secret.txt`
- `..\\secret.txt`

### MFA checks
For MFA setup/enable/verify requests:
- first run `MFA Setup`
- scan the returned `otpauthUri` or use the returned `manualEntryKey`
- copy current authenticator codes into:
  - `mfaEnableCode`
  - `mfaLoginCode`
  - `mfaDisableCode`

### Owner-scope regression probes
Some requests remain grouped as explicit lecturer-versus-lecturer checks so reviewers can capture evidence that owner-scoped authorization is still enforced.
These should now return `403 Forbidden` for unrelated lecturers.

Examples:
- `SUB-15 Regression Probe - Lecturer B reads metadata`
- `SUB-15 Regression Probe - Lecturer B downloads content`
- `GRADE-10 Regression Probe - Lecturer B reads Lecturer A grade`
- `GRADE-10 Regression Probe - Lecturer B updates Lecturer A grade`
- `GRADE-11 Regression Probe - Lecturer B creates grade for ungraded submission`

## Suggested evidence capture

For each request, record:
- request name
- actor used
- status code
- response body
- whether the result matches the intended academic policy
- whether follow-up browser or DB evidence is still required


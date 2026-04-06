# Test Evidence Collection Template

Use this template when executing security checks so the results can be reused directly in:
- the report body
- the appendix
- screenshot selections
- viva/demo preparation
- final evidence assembly

This template is designed to work with:
- `docs/pack-11/security-test-scenarios-matrix.md`
- `docs/pack-11/manual-security-testing-playbook.md`
- `docs/pack-11/postman/README.md`
- `docs/pack-09/unit-test-coverage-summary.md`
- `docs/pack-09/integration-test-coverage-summary.md`
- `docs/pack-09/manual-test-coverage-summary.md`
- `docs/pack-09/csrf-browser-evidence-capture-note.md`
- `docs/pack-09/security-review-evidence-log.md`

---

## 1. How to use this template

Recommended usage:
- copy the **single-scenario evidence record** for each important scenario you execute
- keep one **session log** per testing session or per day
- store screenshots, Postman exports, and browser captures using stable filenames
- fill in the outcome as one of:
  - `Pass`
  - `Fail`
  - `Needs policy decision`

Use `Needs policy decision` especially for cases like:
- whether student self-service access should remain revoked immediately after assignment-space membership removal

If you want a lightweight running log before expanding full records, start with:
- `docs/pack-09/security-review-evidence-log.md`

---

## 2. Suggested evidence file naming

Use stable names so you can refer to them from the report later.

Recommended pattern:

```text
YYYY-MM-DD_<scenario-id>_<short-description>_<actor>_<artifact-type>
```

Examples:

```text
2026-04-05_SUB-04_student-b_download-other-student-content_response.png
2026-04-05_GRADE-03_student-b_privileged-grade-read_postman.json
2026-04-05_AUTH-07_mfa-replay_browser-network.png
2026-04-05_CSRF-01_cross-site-grade-post_browser-notes.md
```

Artifact types you may want:
- `response.png`
- `postman.json`
- `browser-network.png`
- `cookies.png`
- `audit-row.png`
- `notes.md`

---

## 3. Single-scenario evidence record template

Copy this section once per tested scenario.

```markdown
# Scenario Evidence Record

## A. Traceability
- Scenario ID:
- Scenario title:
- Source document:
  - `docs/pack-11/security-test-scenarios-matrix.md`
  - `docs/pack-11/manual-security-testing-playbook.md`
- Priority:
- Date executed:
- Tester:

## B. Environment
- Backend base URL:
- Frontend URL (if relevant):
- Active profile:
- Database used:
- Browser or API client used:
- Cookie posture observed:
  - `HttpOnly`:
  - `Secure`:
  - `SameSite`:

## C. Actor and target
- Actor identity/email:
- Actor role:
- Target endpoint:
- HTTP method:
- Target entity IDs:
  - assignmentId:
  - submissionId:
  - gradeId:
  - spaceId:
  - challengeId:

## D. Preconditions
- Accounts prepared:
- Seed data prepared:
- Authentication state before request:
- Extra setup notes:

## E. Request summary
- Request body used:
- Request headers of note:
- Cookie state of note:
- File uploaded (if any):

## F. Expected result
- Expected status code:
- Expected response behavior:
- Expected side effects:
- Expected audit behavior:

## G. Observed result
- Observed status code:
- Observed response body summary:
- Observed response headers/cookies summary:
- Observed side effects:
- Observed audit result:

## H. Evidence captured
- Screenshot(s):
- Postman export / response capture:
- Browser network capture:
- Cookie screenshot:
- Audit screenshot / DB output:
- Additional notes file:

## I. Assessment
- Outcome:
  - Pass / Fail / Needs policy decision
- Severity if failed:
  - Informational / Low / Medium / High / Critical
- Is this a code defect, deployment defect, or policy ambiguity?
- Does the result match the intended academic policy?

## J. Report-ready observation
- Short factual statement for report:
- Residual limitation or caveat:
- Recommended remediation or next step:
```

---

## 4. Session log template

Use this when you execute several scenarios in one sitting and want a compact run log.

```markdown
# Security Test Session Log

- Session date:
- Tester:
- Environment:
- Goal of session:

| Scenario ID | Actor | Endpoint / check | Expected | Observed | Outcome | Evidence refs | Notes |
|---|---|---|---|---|---|---|---|
| SUB-03 | STUDENT_B | `GET /api/submissions/{submissionId}` | `403` |  |  |  |  |
| SUB-04 | STUDENT_B | `GET /api/submissions/{submissionId}/content` | `403` |  |  |  |  |
| GRADE-03 | STUDENT_B | `GET /api/grades/{gradeId}` | `403` |  |  |  |  |
| AUTH-07 | MFA_STUDENT | replay consumed MFA challenge | `410` |  |  |  |  |
| SPACE-06 | LECTURER_B | `PUT /api/spaces/{spaceId}` | `403` |  |  |  |  |
```

---

## 5. Audit-evidence mini-template

Use this when the scenario is supposed to create or verify audit evidence.

```markdown
## Audit Evidence Check
- Audit expected for this scenario?:
- Audit action type(s) expected:
- Audit entity type:
- Audit entity id:
- Audit present?:
- Integrity value present and non-empty?:
- Sensitive data leaked in audit details?:
- Evidence reference:
- Audit conclusion:
```

---

## 6. Browser/security-behavior mini-template

Use this for CORS, CSRF, cookie, and SameSite observations where Postman is not enough.

For a report-focused CSRF evidence checklist and wording guidance, pair this mini-template with `docs/pack-09/csrf-browser-evidence-capture-note.md`.

```markdown
## Browser Security Check
- Browser used:
- Origin of page making request:
- Target endpoint:
- Request type:
  - fetch / XHR / form POST / navigation / image / script / other
- Was cookie sent?:
- Was response readable by page script?:
- Was request blocked by browser?:
- Was state change processed server-side?:
- Relevant response headers observed:
- Evidence reference:
- Browser-security conclusion:
```

---

## 7. Policy-decision mini-template

Use this when a result is technically consistent with the code but may still be undesirable.

```markdown
## Policy Decision Record
- Scenario ID:
- Current observed behavior:
- Why this may be acceptable:
- Why this may be a security problem:
- Stakeholder/policy decision:
  - intended behavior / defect / undecided
- Temporary classification in report:
- Follow-up action:
```

---

## 8. Suggested priority set for evidence collection

If you are short on time, capture full evidence records first for:
- `SUB-03`
- `SUB-04`
- `GRADE-01`
- `GRADE-03`
- `GRADE-05`
- `SPACE-06`
- `AUTH-07`
- `AUTH-10`
- `CSRF-01`
- `CORS-01`
- owner-scope regression probes such as `SUB-15`, `GRADE-10`, and `GRADE-11`

These give the strongest mix of:
- broken-access-control assurance
- MFA hardening evidence
- browser/deployment review evidence
- policy-sensitive findings

---

## 9. Suggested wording shortcuts for outcomes

### Pass
- "The scenario behaved as expected and the control was enforced successfully."

### Fail
- "The observed behavior did not match the intended security posture and should be treated as a defect until remediated."

### Needs policy decision
- "The implementation is consistent, but its acceptability depends on whether the intended academic policy allows or forbids this historical-access behavior."

---

## 10. Bottom line

This template is meant to stop evidence from becoming scattered across screenshots, browser tabs, and memory.

If you use it consistently, you will end up with:
- traceable scenario execution records
- reusable appendix material
- cleaner Section 8 technical artefact evidence
- stronger and more honest final report claims



# Security Review Evidence Log

Use this file as the **live execution log** when you run Pack 11 scenarios for real.

This document sits between:
- the reusable structure in `docs/pack-09/test-evidence-collection-template.md`
- the sample records in `docs/pack-09/test-evidence-worked-examples.md`

Related scenario sources:
- `docs/pack-11/security-test-scenarios-matrix.md`
- `docs/pack-11/manual-security-testing-playbook.md`
- `docs/pack-11/security-test-gaps-and-next-tests.md`

## How to use this log

Recommended workflow:
1. record each executed scenario in the master table
2. attach stable evidence file references as you go
3. mark the outcome as:
   - `Pass`
   - `Fail`
   - `Needs policy decision`
4. if a scenario is important enough for the report appendix, expand it into a full record using `docs/pack-09/test-evidence-collection-template.md`

This file is meant to stay compact and practical.

---

## 1. Execution context

- Review session start date:
- Review session end date:
- Tester:
- Backend base URL:
- Frontend URL:
- Active profile:
- Database used:
- Browser(s) used:
- API client used:
- Notes about cookie posture / deployment setup:

---

## 2. Master scenario log

| Scenario ID | Area | Actor | Endpoint / check | Date | Expected | Observed | Outcome | Severity if failed | Policy decision needed? | Evidence refs | Report use | Notes |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| AUTH-01 | Auth | Unauthenticated | `GET /api/auth/me` |  | `401` |  |  |  | No |  | Section 8 / appendix |  |
| AUTH-07 | MFA | MFA_STUDENT | replay consumed MFA challenge |  | `410` |  |  |  | No |  | Section 8 / appendix |  |
| AUTH-10 | MFA | MFA_STUDENT | repeated invalid MFA verify until lockout |  | `429` |  |  |  | No |  | Section 8 / appendix |  |
| SUB-03 | Submission | STUDENT_B | `GET /api/submissions/{submissionId}` |  | `403` |  |  |  | No |  | Section 8 / appendix |  |
| SUB-04 | Submission | STUDENT_B | `GET /api/submissions/{submissionId}/content` |  | `403` |  |  |  | No |  | Section 8 / appendix |  |
| SUB-15 | Submission | LECTURER_B | cross-lecturer submission access probe |  | `403` |  |  | High | No |  | Section 8 / appendix | owner-scoped lecturer regression check |
| GRADE-01 | Grade | STUDENT_B | `POST /api/submissions/{submissionId}/grade` |  | `403` |  |  |  | No |  | Section 8 / appendix |  |
| GRADE-03 | Grade | STUDENT_B | `GET /api/grades/{gradeId}` |  | `403` |  |  |  | No |  | Section 8 / appendix |  |
| GRADE-10 | Grade | LECTURER_B | cross-lecturer grade read/update probe |  | `403` |  |  | High | No |  | Section 8 / appendix | owner-scoped lecturer regression check |
| GRADE-11 | Grade | LECTURER_B | cross-lecturer grade create probe |  | `403` |  |  | High | No |  | Section 8 / appendix | owner-scoped lecturer regression check |
| SPACE-06 | Space | LECTURER_B | `PUT /api/spaces/{spaceId}` |  | `403` |  |  |  | No |  | Section 8 / appendix |  |
| SPACE-10 | Space | STUDENT_A | member view without roster disclosure |  | success without roster |  |  |  | No |  | Section 8 / appendix |  |
| CORS-01 | Browser/config | hostile origin | unapproved-origin credentialed fetch |  | blocked / unreadable |  |  |  | No |  | Section 8 / appendix |  |
| CSRF-01 | Browser/config | hostile origin + victim session | cross-site unsafe request |  | blocked / ineffective |  |  |  | No |  | Section 8 / appendix |  |
| CFG-03 | Config | reviewer | secret override review |  | overrides confirmed |  |  |  | No |  | Section 6 / appendix |  |

---

## 3. Auth and MFA log

| Scenario ID | Actor | Action | Expected | Observed | Outcome | Evidence refs | Notes |
|---|---|---|---|---|---|---|---|
| AUTH-01 | Unauthenticated | `GET /api/auth/me` | `401` |  |  |  |  |
| AUTH-04 | STUDENT | `POST /api/auth/users` | `403` |  |  |  |  |
| AUTH-05 | LECTURER | create lecturer via managed-user endpoint | `403` |  |  |  |  |
| AUTH-07 | MFA_STUDENT | replay consumed challenge | `410` |  |  |  |  |
| AUTH-09 | MFA_STUDENT | reuse recovery code | `401` |  |  |  |  |
| AUTH-10 | MFA_STUDENT | challenge lockout | `429` |  |  |  |  |
| AUTH-11 | MFA_STUDENT | disable MFA with wrong password | `401` |  |  |  |  |
| AUTH-12 | MFA_STUDENT | disable MFA with wrong verification code | `401` |  |  |  |  |
| AUTH-15 | Browser victim | CSRF-like state change attempt | browser/deployment dependent |  |  |  |  |

---

## 4. Submission log

| Scenario ID | Actor | Action | Expected | Observed | Outcome | Evidence refs | Notes |
|---|---|---|---|---|---|---|---|
| SUB-01 | Unauthenticated | `GET /api/submissions/{submissionId}` | `401` |  |  |  |  |
| SUB-02 | Unauthenticated | `GET /api/submissions/{submissionId}/content` | `401` |  |  |  |  |
| SUB-03 | STUDENT_B | read another student's metadata | `403` |  |  |  |  |
| SUB-04 | STUDENT_B | download another student's content | `403` |  |  |  |  |
| SUB-11 | STUDENT_A | traversal-style filename upload | `400` |  |  |  |  |
| SUB-12 | STUDENT_A | submit to closed assignment | `409` |  |  |  |  |
| SUB-15 | LECTURER_B | cross-lecturer metadata/content access | `403` |  |  |  | owner-scoped lecturer regression check |

---

## 5. Grade log

| Scenario ID | Actor | Action | Expected | Observed | Outcome | Evidence refs | Notes |
|---|---|---|---|---|---|---|---|
| GRADE-01 | STUDENT_B | create grade | `403` |  |  |  |  |
| GRADE-02 | STUDENT_B | update grade | `403` |  |  |  |  |
| GRADE-03 | STUDENT_B | privileged grade read | `403` |  |  |  |  |
| GRADE-04 | STUDENT_B | read another student's grade by grade id | `403` |  |  |  |  |
| GRADE-05 | STUDENT_B | read another student's grade by submission id | `403` |  |  |  |  |
| GRADE-06 | LECTURER_A | duplicate grade creation | `409` |  |  |  |  |
| GRADE-07 | LECTURER_A | grade non-verified submission | `422` |  |  |  |  |
| GRADE-10 | LECTURER_B | cross-lecturer grade read/update | `403` |  |  |  | owner-scoped lecturer regression check |
| GRADE-11 | LECTURER_B | cross-lecturer grade create | `403` |  |  |  | owner-scoped lecturer regression check |

---

## 6. Space log

| Scenario ID | Actor | Action | Expected | Observed | Outcome | Evidence refs | Notes |
|---|---|---|---|---|---|---|---|
| SPACE-01 | STUDENT_B | create space | `403` |  |  |  |  |
| SPACE-02 | STUDENT_B | update space | `403` |  |  |  |  |
| SPACE-03 | STUDENT_B | self-enroll/add member | `403` |  |  |  |  |
| SPACE-04 | STUDENT_B | remove member | `403` |  |  |  |  |
| SPACE-06 | LECTURER_B | update Lecturer A space | `403` |  |  |  |  |
| SPACE-07 | LECTURER_B | add student to Lecturer A space | `403` |  |  |  |  |
| SPACE-08 | LECTURER_B | remove student from Lecturer A space | `403` |  |  |  |  |
| SPACE-10 | STUDENT_A | member view without roster disclosure | success without roster |  |  |  |  |
| SPACE-11 | LECTURER_A | add student to archived space | `409` |  |  |  |  |

---

## 7. Browser, config, and operational review log

| Scenario ID | Reviewer action | Expected | Observed | Outcome | Evidence refs | Notes |
|---|---|---|---|---|---|---|
| CORS-01 | unapproved-origin credentialed fetch | browser blocks exposure |  |  |  |  |
| CSRF-01 | hostile-origin unsafe request | browser/session posture prevents abuse |  |  |  |  |
| CFG-01 | prod insecure cookie startup check | startup fails |  |  |  |  |
| CFG-02 | `SameSite=None` without `Secure=true` | startup fails |  |  |  |  |
| CFG-03 | secret override review | all production secrets overridden |  |  |  |  |
| AUDIT-03 | audit detail review | no secret/plaintext leakage |  |  |  |  |

---

## 8. Policy decision tracker

Use this section for findings that are not simple pass/fail outcomes.

| Scenario ID | Current observed behavior | Why acceptable? | Why concerning? | Decision status | Owner / stakeholder | Follow-up |
|---|---|---|---|---|---|---|
| SUB-15 |  |  |  | undecided |  |  |
| GRADE-10 |  |  |  | undecided |  |  |
| GRADE-11 |  |  |  | undecided |  |  |

---

## 9. Appendix candidate shortlist

Use this section to nominate the strongest records for the final appendix.

| Scenario ID | Why include it? | Evidence refs | Report section | Chosen? |
|---|---|---|---|---|
| SUB-04 | strong broken-access-control evidence |  | Section 8 / appendix |  |
| AUTH-07 | strong MFA replay evidence |  | Section 8 / appendix |  |
| GRADE-03 | strong role-based authorization evidence |  | Section 8 / appendix |  |
| SPACE-06 | clear ownership-boundary evidence |  | Section 8 / appendix |  |
| CORS-01 | browser/deployment assurance evidence |  | Section 8 / appendix |  |
| CSRF-01 | browser/deployment assurance evidence |  | Section 8 / appendix |  |

---

## 10. End-of-session summary

- Number of scenarios executed:
- Number passed:
- Number failed:
- Number needing policy decision:
- Highest-severity issue found:
- Most useful appendix evidence collected:
- Follow-up actions before report freeze:


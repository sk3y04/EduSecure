# EduSecure Security Testing Pack 11

Pack 11 translates the currently implemented backend security controls into practical attack and abuse-case scenarios you can execute as a security reviewer.

## Purpose

This pack is designed for questions such as:
- can an unprivileged student download another student's submission?
- can an unprivileged user create or modify grades?
- can one lecturer manage another lecturer's space?
- can MFA be bypassed, replayed, or brute-forced?
- do sensitive actions leave tamper-evident audit traces?

The scenarios in this pack are based on the **current implementation actually present in the repository**, not on generic best-practice assumptions.

## What this pack covers

- implemented auth and session boundaries
- MFA abuse and replay scenarios
- managed-user creation privilege boundaries
- submission metadata and content access control
- grade integrity and privilege abuse scenarios
- space-management authorization scenarios
- audit and configuration review checks
- security gaps and policy questions that still need explicit verification

## Repository evidence used to build this pack

Primary implementation sources reviewed:
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- `backend/src/main/java/edusecure/edusecure/controller/auth/AuthController.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
- `backend/src/main/java/edusecure/edusecure/controller/submission/SubmissionController.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `backend/src/main/java/edusecure/edusecure/controller/grade/GradeController.java`
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java`
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceController.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`

Primary automated evidence reviewed:
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/SpaceFlowIntegrationTests.java`

Related evidence/docs already in the repo:
- `docs/pack-05/test-and-evidence-plan.md`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-07/grade-phase-status-and-evidence.md`
- `docs/pack-09/final-implementation-evidence-map.md`

## Contents

- `manual-security-testing-playbook.md`
- `postman/README.md`
- `postman/EduSecure-Pack-11.postman_collection.json`
- `postman/EduSecure-Local.postman_environment.json`
- `postman/EduSecure-ProdLike.postman_environment.json`
- `postman-fixtures/README.md`
- `postman-fixtures/student-a.txt`
- `postman-fixtures/traversal.txt`
- `security-sensitive-modules-inventory.md`
- `security-test-scenarios-matrix.md`
- `security-test-gaps-and-next-tests.md`

## How to use this pack

1. Start with `security-sensitive-modules-inventory.md` to understand the actual trust boundaries.
2. Use `manual-security-testing-playbook.md` when you want a reviewer-friendly step-by-step execution guide.
3. Import the files in `postman/` when you want a reusable API-client structure for the Pack 11 checks.
4. Use `security-test-scenarios-matrix.md` as the scenario checklist and prioritization table.
5. Use `security-test-gaps-and-next-tests.md` to investigate areas that are either policy-sensitive or not yet strongly evidenced by automated tests.

## Important reading of the current system

This pack distinguishes between:
- **implemented and already evidenced protections** — where the code and tests already show the control working
- **manual security-review scenarios** — where the code suggests a risk boundary, but you should still probe it directly
- **policy questions / possible gaps** — where the code may be secure only if a broader business rule has been intentionally accepted

That last category now matters most for post-membership student access and other future scope decisions, because assignments are now space-scoped for students while submissions, grades, and spaces all follow owner-scoped lecturer access with admin override.


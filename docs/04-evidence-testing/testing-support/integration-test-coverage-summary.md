# Integration Test Coverage Summary

This document explains what the **integration-style** backend tests currently cover in EduSecure, and how that coverage complements the smaller unit-focused test layer.

Related companion note:
- `docs/04-evidence-testing/testing-support/unit-test-coverage-summary.md`

## 1. Purpose

The backend test suite in `backend/src/test/java/edusecure/edusecure/` is intentionally stronger on end-to-end and cross-layer checks than on isolated mocking.

This document focuses on the tests that prove behavior across:
- controllers
- services
- repositories
- Spring Security
- cookie-based authentication
- persistence and migration wiring

That is the layer where the repository demonstrates most of its real security behavior.

## 2. Classification rule used in this document

For this summary, a test is treated as integration-style when it does at least one of the following:
- starts the Spring Boot application context with `@SpringBootTest`
- uses `MockMvc` to exercise HTTP endpoints
- depends on repositories and persistence
- verifies security, validation, and business rules across multiple layers together
- starts a real infrastructure dependency such as PostgreSQL via Testcontainers

This means the integration layer in EduSecure is not limited to "happy path" API tests.
It also includes real security boundary checks such as:
- `401` vs `403` behavior
- role restrictions
- object-level access control
- MFA challenge handling
- audit creation
- real PostgreSQL/Liquibase startup validation

## 3. Main integration-style test classes and what they cover

## 3.1 `backend/src/test/java/edusecure/edusecure/EduSecureApplicationTests.java`

### What it covers
This is the baseline boot and availability smoke test.

It verifies:
- the Spring application context loads successfully
- the public health endpoint responds with `200 OK`
- `/api/system/health` returns `status = UP`

### What this proves
At integration level, the repository proves that the application can boot with its configured web/security stack and expose its minimal public availability endpoint.

### What it does not prove by itself
It does not prove:
- role-based auth behavior
- database hardening
- end-to-end business workflows

It is best treated as baseline boot evidence.

## 3.2 `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`

### What it covers
This class provides the main integration evidence for the non-MFA auth foundation.

It verifies:
- registration works end to end
- login works end to end
- `/api/auth/me` requires authentication and returns the current user when authenticated
- duplicate registration is rejected
- password policy validation is enforced for registration
- validation failures return structured field-level responses
- invalid credentials return structured auth-domain errors
- admins can create lecturer and student accounts through the managed-user endpoint
- lecturers can create students but not lecturers or admins
- students cannot access managed-user creation
- logout clears the auth cookie
- tampered auth cookies are rejected
- tampered bearer tokens are rejected

### What this proves
At integration level, the repository proves that:
- the auth controller, service, validation, JWT parsing, and cookie transport work together
- role restrictions on managed user creation are enforced at the HTTP/API level
- signature-invalid tokens are rejected before protected identity lookup succeeds
- the browser-facing auth model is cookie-based rather than JavaScript token storage-based

### What it does not prove by itself
It does not fully prove MFA-specific behavior, which is covered by the MFA integration suite.

## 3.3 `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`

### What it covers
This is the main integration evidence for MFA enrollment and challenge handling.

It verifies:
- MFA setup, enablement, login challenge, verification, and disablement all work together
- a full authenticated cookie session is established only after successful MFA verification
- a consumed challenge cannot be reused
- a recovery code can be used only once
- invalid TOTP codes are rejected
- expired MFA challenges are rejected
- field-level validation exists for MFA enable, verify, and disable endpoints
- malformed MFA verification requests are rejected cleanly
- invalid credentials during MFA disable are rejected
- the challenge-attempt lockout now results in `429 Too Many Requests`

### What this proves
At integration level, the repository proves that:
- MFA is not just algorithmically correct but properly wired into login branching and session issuance
- the system distinguishes between password-only and full MFA-authenticated sessions
- replay and reuse protections work through the real API surface
- the lockout boundary is enforced through persisted challenge state, not just local calculation

### What it does not prove by itself
It does not prove unrelated authorization domains such as submissions, grades, or spaces.

## 3.4 `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`

### What it covers
This class provides the main end-to-end evidence for assignments, submissions, stored content protection, and submission access control.

It verifies:
- lecturers can create assignments
- students only see assignments for spaces they are currently enrolled in
- students can upload valid submissions
- students can retrieve their latest submission for an assignment
- lecturers can list submissions for an assignment
- student assignment listing includes the student's own latest submission metadata
- students get `404` when no submission exists for the assignment
- one student cannot read another student's submission metadata
- one student cannot download another student's submission content
- unauthenticated callers cannot read submission metadata or content
- supported upload validation rules are enforced, including:
  - unsupported content type rejection
  - PDF header validation
  - empty upload rejection
  - invalid UTF-8 rejection
  - file-size limit rejection
  - traversal-style filename rejection
  - closed-assignment rejection
- successful submission content retrieval works for authorized actors
- submission storage metadata shows encryption-at-rest behavior
- audit records are produced for sensitive submission actions
- unrelated lecturers are denied submission listing, metadata access, and content retrieval for assignments they do not own
- admins retain cross-assignment submission access for oversight
- student-owned submission reads are revoked when the student's space membership is removed

### What this proves
At integration level, the repository proves that:
- multipart submission upload flows through controller, validation, service, crypto, persistence, and storage boundaries correctly
- submission metadata and plaintext content retrieval are separated but protected consistently
- student-to-student broken-access-control is blocked on the real API surface
- lecturer access is scoped to assignments they own, with `ADMIN` override for submission review
- encrypted-at-rest submission handling is not just unit-tested but embedded in a working submission lifecycle
- sensitive content retrieval is auditable

## 3.5 `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`

### What it covers
This is the main integration evidence for grade integrity and grade authorization.

It verifies:
- lecturers can create grades
- lecturers can update grades
- students can retrieve their own grades while the related assignment remains visible through current space membership
- students cannot create or update grades
- duplicate grade creation is rejected
- non-verified submissions cannot be graded
- one student cannot read another student's grade by grade id
- one student cannot read another student's grade by submission id
- students cannot use privileged grade-read endpoints
- grade range validation is enforced
- grade actions produce audit records
- graded submissions appear as graded in lecturer submission listing
- unrelated lecturers are denied grade create/read/update actions for assignments they do not own
- admins retain cross-assignment grade access for oversight
- student self-service grade reads are revoked when the student's assignment-space membership is removed

### What this proves
At integration level, the repository proves that:
- the grade controller, service, validation, submission linkage, and audit logic work together
- student grade tampering is blocked through the real endpoint layer
- lecturer grade access is scoped to assignments they own, with `ADMIN` override for oversight
- grade creation depends on submission verification status in the live workflow
- grade-sensitive operations are auditable

## 3.6 `backend/src/test/java/edusecure/edusecure/SpaceFlowIntegrationTests.java`

### What it covers
This class provides the main integration evidence for the space-management authorization model.

It verifies:
- lecturers can create and manage spaces
- lecturers can add and remove student memberships
- students can see spaces they belong to
- student members do not receive manager-level roster disclosure
- students cannot create spaces
- students cannot update spaces
- students cannot add or remove memberships
- one lecturer cannot manage another lecturer's space
- admins can manage any lecturer-owned space
- duplicate space codes are rejected
- duplicate memberships are rejected
- non-students cannot be added as student members
- archived spaces reject new student additions
- space-management actions are audited

### What this proves
At integration level, the repository proves that:
- spaces follow the same owner-scoped lecturer model now used for submissions and grades
- students are correctly limited to member-level visibility
- manager-only roster exposure is enforced through the actual API response
- admin override behavior works across the real controller/service/repository path

## 3.7 `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

### What it covers
This is the real-database integration evidence for schema delivery and repository wiring.

It verifies against a PostgreSQL Testcontainer that:
- Liquibase change sets apply successfully
- the initial schema change set is recorded
- the submission AES-storage change set is recorded
- the assignment-to-space change set is recorded
- expected core tables exist
- expected constraints and indexes exist
- submission encryption-related columns exist in the `submissions` table
- the `assignments.space_id` column, foreign key, and supporting index exist
- reference roles are seeded
- repositories can persist and retrieve data against real PostgreSQL
- the `user_roles` join table behaves correctly

### What this proves
At integration level, the repository proves that:
- PostgreSQL is not just a documented target but a tested one
- Liquibase runs before Hibernate validation successfully in a real database context
- the schema required by the auth and role model is present and usable
- the later submission-encryption schema additions are represented in migration history

### What it does not prove by itself
It does not prove:
- broad production-ready operational hardening
- every business endpoint against PostgreSQL
- backup/restore or transport-hardening maturity

## 4. What the integration suite covers overall

Taken together, the integration-style tests cover these areas strongly:

### Auth and session security
- registration and login behavior
- structured validation and error handling
- cookie-based authentication
- token tampering rejection
- managed-user role restrictions
- logout cookie clearing

### MFA hardening
- setup, enablement, challenge creation, verification, replay rejection, expiry rejection
- recovery-code one-time use
- lockout after repeated invalid verification attempts
- disable flow with dual proof requirements

### Submission and assignment security
- assignment creation
- upload validation boundaries
- encrypted submission workflow behavior
- student-vs-student access control
- plaintext retrieval authorization
- audit evidence for sensitive actions

### Grade integrity
- privileged creation/update
- student ownership restrictions
- verification-status dependency
- audit creation
- owner-scoped lecturer grade authorization with admin override

### Space authorization
- lecturer ownership boundaries
- student management denial
- admin override
- confidentiality of non-manager member views

### Delivery and database evidence
- application boot and public health endpoint
- Liquibase + PostgreSQL delivery validation

## 5. What the integration suite does **not** fully cover

Even though the integration layer is strong, it still does not fully prove everything.

Important remaining areas include:
- true browser-enforced CORS behavior
- true browser and hostile-origin CSRF behavior under real cookie rules
- database-side audit review as an operational process
- production secret-management discipline
- full production hardening of PostgreSQL and deployment infrastructure

Those areas still require:
- manual/browser testing
- deployment review
- policy clarification
- operational documentation

## 6. Relationship to the unit test layer

The best way to describe the test strategy honestly is:
- **unit tests cover the security-critical building blocks in isolation**
- **integration tests cover the real cross-layer security behavior**

In EduSecure, the integration layer is the more important source of evidence for claims such as:
- "students cannot access other students' submissions"
- "unprivileged actors cannot modify grades"
- "MFA replay is blocked"
- "space ownership rules are enforced"

Those are not claims that a small isolated unit test can prove convincingly.

## 7. Suggested wording for your report

If you want a concise way to explain this in the report, you can say:

> The repository relies heavily on Spring Boot integration tests to evidence real security behavior across the controller, service, repository, security, and persistence layers. These tests cover authentication, MFA, submission access control, grade authorization, space ownership, audit creation, and PostgreSQL/Liquibase delivery. This integration-focused evidence complements the smaller unit-test layer for cryptographic and configuration primitives.

## 8. Bottom line

The current integration-style tests provide most of the repository's strongest evidence for practical security behavior.

In particular, they prove that EduSecure currently enforces and evidences:
- API-layer authentication and validation behavior
- MFA workflow security boundaries
- submission and grade access-control rules
- space-management ownership restrictions
- audit creation for sensitive actions
- real PostgreSQL schema delivery via Liquibase

They still do not remove the need for browser/deployment review or policy decisions.
But they are the main reason the repository can make credible security claims beyond isolated crypto/configuration correctness.


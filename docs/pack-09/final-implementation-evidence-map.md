# Final Implementation Evidence Map

This document links the current EduSecure backend implementation to concrete evidence that can be reused in the report.

## 1. Implemented backend surface

### Auth foundation and MFA hardening
Controller:
- `backend/src/main/java/edusecure/edusecure/controller/AuthController.java`

Services / domain:
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthTokenService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/TotpService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java`
- `backend/src/main/java/edusecure/edusecure/entity/User.java`
- `backend/src/main/java/edusecure/edusecure/entity/MfaChallenge.java`
- `backend/src/main/java/edusecure/edusecure/entity/MfaRecoveryCode.java`

Cookie/security configuration:
- `backend/src/main/java/edusecure/edusecure/security/AuthCookieService.java`
- `backend/src/main/java/edusecure/edusecure/config/AuthCookieProperties.java`
- `backend/src/main/java/edusecure/edusecure/config/CorsProperties.java`
- `backend/src/main/java/edusecure/edusecure/config/AuthCookieConfigurationValidator.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`

Evidence/tests:
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/config/AuthCookieConfigurationValidatorTests.java`
- `backend/src/test/java/edusecure/edusecure/config/AuthCookieConfigurationStartupValidationTests.java`

What it proves:
- registration works
- bcrypt-backed password handling is in place
- registration rejects passwords that do not meet the uppercase/lowercase/number/special-character policy
- registration returns structured field-level validation feedback for weak passwords
- auth-domain failures now use a shared structured error envelope for both validation and non-validation auth errors
- login issues JWT-backed authenticated cookie sessions for non-MFA users
- login returns `MFA_REQUIRED` for MFA-enabled users
- TOTP-based MFA setup and enablement work
- authenticated cookie session is established only after successful MFA verification for MFA-enabled users
- recovery codes are generated, hashed, and one-time use
- invalid and expired MFA challenges are rejected
- authenticated identity lookup works
- logout clears the auth cookie
- cookie transport is `HttpOnly` and validated in integration tests
- unsafe cookie settings fail fast during startup validation

## 2. Assignment and submission integrity

Controllers:
- `backend/src/main/java/edusecure/edusecure/controller/AssignmentController.java`
- `backend/src/main/java/edusecure/edusecure/controller/SubmissionController.java`

Services / domain:
- `AssignmentService.java`
- `SubmissionService.java`
- `Submission.java`
- `SubmissionVerificationStatus.java`

Evidence/tests:
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionServiceTests.java`
- `backend/src/test/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionServiceTests.java`
- `backend/src/test/java/edusecure/edusecure/service/submission/FileSystemSubmissionContentStoreTests.java`
- `backend/src/test/java/edusecure/edusecure/service/crypto/AesRsaCryptoServiceTests.java`
- `docs/pack-06/submission-content-protection-and-retrieval.md`

What it proves:
- lecturer can create assignments
- student can upload a bounded UTF-8 `text/plain` submission file through the browser-facing multipart contract
- `SHA-256` digest metadata is created
- digital signature metadata is created
- the submission-signature workflow uses a stable configured demo ECC keypair rather than a fresh runtime-generated keypair
- verification status is stored and returned
- submission content is encrypted at rest
- submission metadata and plaintext retrieval are separated into different endpoints
- successful plaintext retrieval is auditable
- unrelated student cannot read another student's submission
- unsupported non-text uploads are rejected
- empty uploads are rejected
- invalid UTF-8 uploads are rejected
- assignment/submission endpoints work under the cookie-authenticated session model used by the browser-facing frontend

## 3. Audit integrity

Service / domain:
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
- `backend/src/main/java/edusecure/edusecure/entity/AuditLog.java`
- `backend/src/main/java/edusecure/edusecure/entity/AuditActionType.java`

Evidence/tests:
- `SubmissionFlowIntegrationTests.java`
- `GradeFlowIntegrationTests.java`

What it proves:
- sensitive actions create audit records
- audit records contain non-empty HMAC-backed integrity values
- append-oriented audit evidence exists for key actions

## 4. Grade integrity

Controller:
- `backend/src/main/java/edusecure/edusecure/controller/GradeController.java`

Service / domain:
- `GradeService.java`
- `Grade.java`

Evidence/tests:
- `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`

What it proves:
- only lecturer/admin can create or update grades
- students can retrieve only their own grades
- duplicate grade creation is rejected
- non-verified submissions cannot be graded
- grade-sensitive actions are audited
- grade endpoints remain protected under the same cookie-authenticated session model

## 5. Secure transmission — TLS via Certbot/Let's Encrypt

The standalone AES-GCM demo endpoints (`AesDemoController`, `AesGcmDemoService`) have been removed. Secure message/file transmission is covered at the infrastructure level by TLS 1.3 enforced via Certbot and Let's Encrypt on deployment. This is the correct and complete solution: all client-server traffic is protected by TLS, which itself uses AES-GCM internally.

AES-GCM symmetric encryption remains evidenced in the codebase through two real business controls:
- MFA secret protection at rest (`MfaSecretCryptoService`)
- Submission content encryption at rest (`SubmissionContentEncryptionService`, `SubmissionKeyProtectionService`)

What the deployment proves:
- all client-server traffic is encrypted in transit (TLS 1.3, Certbot/Let's Encrypt)
- the "secure file/message transmission" artefact requirement is satisfied by real infrastructure TLS rather than a standalone demo endpoint

## 6. Baseline availability / boot evidence

Evidence/tests:
- `backend/src/test/java/edusecure/edusecure/EduSecureApplicationTests.java`

What it proves:
- application context loads
- health endpoint is public and working

## 7. Database delivery and PostgreSQL verification

Configuration / delivery evidence:
- `compose.yaml`
- `backend/src/main/resources/application.properties`
- `backend/src/main/java/edusecure/edusecure/config/LiquibaseConfig.java`
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/src/main/resources/db/changelog/changes/001-initial-schema.yaml`

Evidence/tests:
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`
- `backend/src/test/resources/application-postgres-liquibase.properties`

What it proves:
- PostgreSQL is the intended runtime database path
- schema creation is represented in versioned Liquibase changelogs rather than relying only on Hibernate mutation
- Liquibase is wired to run before Hibernate validation in the application context
- the baseline schema can be applied against a real PostgreSQL container in automated tests
- core tables, constraints, and indexes exist after migration
- seed roles are available after startup against PostgreSQL
- a minimal repository round-trip works against real PostgreSQL, including the `user_roles` join table

What it does not prove:
- production hardening of PostgreSQL transport or storage encryption
- broad API-level PostgreSQL coverage across the whole suite
- operational backup/restore maturity

## 8. Minimum cryptographic techniques now evidenced

The implemented artefact now contains evidence for at least these techniques:
- password hashing with `bcrypt`
- TOTP-based MFA using HMAC-derived one-time-password verification
- hashing with `SHA-256`
- digital signature workflow with ECC-based signing/verification logic
- HMAC-backed integrity for audit records
- AES-GCM symmetric encryption demo
- AES-GCM protection of stored MFA secrets at rest
- AES-GCM protection of stored submission content at rest

This exceeds the brief's minimum of three implemented cryptographic technique areas.

## 9. Browser-session hardening evidence

Additional documentation/evidence worth citing in the report:
- `frontend/src/services/http.ts` shows credentialed requests with `withCredentials: true`
- `frontend/src/stores/auth.ts` shows that the frontend no longer stores the auth JWT in `localStorage`
- `frontend/README.md` documents the production profile and cookie deployment expectations
- `docs/pack-03/api-auth-contract.md` records the implemented `Set-Cookie`-based auth contract and logout behaviour


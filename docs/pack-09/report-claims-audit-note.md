# Report Claims Audit Note

This note is a **final report wording control** for EduSecure.

Its purpose is to reduce over-claiming by separating:

- what is implemented and evidenced in the repository
- what is planned or optional future work
- what is only appendix/support material
- what security claims should be stated carefully

Use this note when reviewing Section 3 of `docs/pack-09/final-submission-checklist.md`.

For a control-by-control wording baseline, use `docs/pack-09/final-cryptography-claims-matrix.md`.
For the current conclusion on diagram sufficiency, use `docs/pack-09/uml-refresh-assessment.md`.

## 1. Claims that are safe to make

The following claims are supported by repository evidence and can be described as implemented:

- EduSecure is a backend-focused cryptography case-study artefact built with Spring Boot.
- EduSecure also includes a small Vue frontend MVP that demonstrates the implemented auth, MFA, assignment, and submission flows.
- The backend implements authentication, MFA hardening, assignment/submission handling, audit integrity, and grade integrity.
- The backend uses AES-GCM in two real business roles: MFA secret protection at rest and submission content encryption at rest.
- Secure client-server transmission is enforced by TLS 1.3 via Certbot/Let's Encrypt on deployment.
- Browser-facing authentication uses an `HttpOnly` cookie-backed session rather than frontend-managed JWT storage.
- The submission-signature workflow now uses a stable configured demo RSA keypair within the existing simulated signing model.
- The artefact contains evidence for `bcrypt`, `TOTP`, `SHA-256`, `RSA`-based signing/verification, `HMAC-SHA-256`, and `AES-GCM`.
- PostgreSQL is the intended runtime database path.
- Schema delivery is now represented in Liquibase changelogs rather than relying only on Hibernate schema mutation.
- The repository contains a dedicated PostgreSQL/Testcontainers smoke test that verifies the Liquibase baseline against a real PostgreSQL instance.
- The broader fast test suite still relies mainly on H2 for speed.

These claims are supported by:

- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/postgresql-setup-and-security.md`
- `frontend/README.md`
- `frontend/src/services/http.ts`
- `frontend/src/stores/auth.ts`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
- `backend/src/main/resources/db/changelog/changes/001-initial-schema.yaml`
- `backend/src/test/java/edusecure/edusecure/LiquibasePostgresIntegrationTests.java`

## 2. Claims that should be phrased as planned, optional, or future-facing

The following should **not** be described as already implemented unless separate evidence is added:

- a production deployment of EduSecure
- a complete CI/CD pipeline for this repository
- full operational PostgreSQL hardening
- enterprise-grade deployment maturity
- full API-wide PostgreSQL-backed integration coverage across the whole test suite
- a fully feature-complete frontend covering every optional grade/AES/admin screen
- a user-held production PKI signing model for submissions

Safer wording examples:

- "The repository contains a small implemented Vue frontend MVP, while additional frontend screens remain optional polish rather than the main artefact."
- "CI/CD and home-server deployment are discussed as appendix/support material rather than as fully implemented proof in this repository."
- "The repository now proves a local PostgreSQL migration/verification path, not full production database operations maturity."
- "The submission-signature workflow uses a stable configured demo keypair to improve repeatability, while remaining a bounded study-project signing simulation."

## 3. Appendix wording boundary

Appendix material should be described as:

- support material
- deployment awareness
- reproducibility planning
- future engineering direction

Appendix material should **not** be described as if it were already fully operational unless direct EduSecure evidence exists.

Safe wording:

- "The appendix outlines how EduSecure could be packaged and deployed beyond local development."
- "Deployment notes are included as supporting engineering evidence, not as proof of a finished production platform."

Unsafe wording:

- "EduSecure is deployed in production"
- "The repository proves a finished CI/CD platform"
- "Nginx and home-server deployment are fully implemented here" 

Primary supporting references:

- `docs/pack-09/appendix-cicd-and-deployment-plan.md`
- `docs/pack-09/README.md`

## 4. PostgreSQL proof boundary

The following claims are supported:

- PostgreSQL is configured as the runtime database target.
- Liquibase changelogs define the baseline schema.
- A dedicated PostgreSQL/Testcontainers smoke test proves that the baseline changelog can run on a real PostgreSQL instance.
- Hibernate is used to validate mappings against the migrated schema.

The following claims are **not** fully supported:

- that the whole automated suite runs on PostgreSQL
- that PostgreSQL operations are fully production-ready
- that backups/restores are operationally proven
- that transport/storage hardening is complete

Safe wording:

- "The repository contains verified local PostgreSQL schema-delivery evidence."
- "Real PostgreSQL verification is present as a focused smoke test rather than full-suite replacement."

Unsafe wording:

- "The database layer is fully production-ready."
- "All integration tests run on PostgreSQL."

Primary supporting reference:

- `docs/pack-09/postgresql-setup-and-security.md`

## 5. Database-encryption wording boundary

Be especially careful to separate **application-level secret protection** from **whole-database encryption**.

Supported claim:

- MFA secrets are encrypted at rest within the application model.

This is supported by the MFA implementation documentation and code.

Not fully supported from repository evidence alone:

- that PostgreSQL storage as a whole is encrypted at rest
- that PostgreSQL traffic is definitely encrypted in transit
- that the entire database is encrypted

Safe wording:

- "Certain sensitive application secrets, such as MFA secrets, are protected with encryption at rest within the application design."
- "Whole-database storage encryption and transport encryption require explicit deployment/platform configuration and should not be assumed from the repository alone."

Unsafe wording:

- "The PostgreSQL database is encrypted"
- "All database traffic is encrypted"
- "The database is absolutely secure"

Primary supporting references:

- `docs/pack-03/mfa-cryptography-implementation.md`
- `docs/pack-09/postgresql-setup-and-security.md`

## 6. AES and TLS wording boundary

The standalone AES-GCM demo endpoints (`AesDemoController`, `AesGcmDemoService`, `AesDemoIntegrationTests`) have been removed. Secure transmission is now covered by real infrastructure TLS via Certbot/Let's Encrypt.

AES-GCM remains in the codebase in two real business roles only:
- MFA secret encryption at rest
- submission content encryption at rest

Supported claims:

- EduSecure uses AES-GCM as a confidentiality-at-rest control for both MFA secrets and stored submission content.
- Secure client-server transmission is enforced by TLS 1.3 via Certbot/Let's Encrypt, which itself uses AES-GCM internally.

Safe wording:

- "AES-GCM is used in EduSecure as the encryption mechanism for MFA secrets and submission content at rest."
- "Secure transmission in EduSecure is enforced by TLS 1.3 via Certbot/Let's Encrypt rather than a separate application-layer AES demo."
- "The 'secure file/message transmission' artefact requirement is satisfied by real infrastructure TLS rather than a standalone demo endpoint."

Unsafe wording:

- "EduSecure includes a standalone AES demo endpoint." *(removed)*
- "The AES demo proves symmetric encryption." *(no longer present)*
- "There is no evidence of AES usage beyond key wrapping." *(wrong — AES-GCM is used for MFA secrets and submission content)*

Primary supporting references:

- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `docs/pack-03/mfa-cryptography-implementation.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`
- `docs/pack-09/appendix-cicd-and-deployment-plan.md`

## 7. Quick final-review questions

Before finalising the report, check:

- Does every implementation claim point to a real file, test, or evidence note?
- Are implemented frontend flows distinguished clearly from optional or still-unbuilt frontend polish?
- Are appendix sections written as support material rather than proven production delivery?
- Are PostgreSQL claims limited to verified local schema delivery and focused PostgreSQL smoke-test evidence?
- Are database-encryption claims limited to mechanisms that are explicitly evidenced?
- Is no future work described as if it has already been completed?

## 8. Recommended usage

Use this note together with:

- `docs/pack-09/final-submission-checklist.md`
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`
- `docs/pack-09/report-section-to-evidence-map.md`
- `docs/pack-09/uml-refresh-assessment.md`

This note is not a replacement for the final report; it is a wording-control aid to keep the final submission honest, defensible, and aligned with the repository evidence.


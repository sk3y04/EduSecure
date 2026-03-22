# EduSecure Documentation-First Receipt and Plan

This repository is being developed as a **study-focused cryptography case study** for the `500IT Cryptography` assignment described in `docs/assignment_brief.md`.

## Current position

- The `backend/` project has now been realigned into a simplified Spring Boot study-project foundation.
- The `Christialattion/` project is a **reference project**, useful for reusable structural patterns, but not for copying business-domain logic.
- The immediate priority is **documentation before coding** so that every later implementation step can be justified in the report.

## Implemented backend baseline

The backend now includes a first real security/domain slice aligned with the planning packs:

- simplified Spring Boot stack: REST, Security, Validation, JPA, PostgreSQL
- local PostgreSQL-oriented configuration and Docker Compose support
- public system health endpoint
- `User` and `Role` entities with a minimal education-platform auth model
- `bcrypt` password hashing through Spring Security
- JWT-based registration/login foundation
- authenticated `/api/auth/me` endpoint
- integration tests covering health, registration, login, and authenticated identity lookup
- assignment creation and listing foundation
- submission creation and retrieval foundation
- submission verification metadata persistence
- append-oriented audit logging with HMAC-backed integrity values
- grade creation, update, and retrieval foundation
- verified-submission-only grading enforcement
- AES-GCM secure transmission demo endpoints

## What this first planning pack does

The `docs/pack-01/` folder establishes the baseline needed before backend or frontend feature work begins:

1. Assignment traceability
2. Reference-project reuse matrix
3. Scope and assumptions
4. Initial risk register with propositional-logic style mitigation reasoning
5. UML and documentation plan
6. Future agent instructions for controlled implementation

The follow-up `docs/pack-02/` phase now extends this baseline with:

1. cryptographic decision and justification material
2. refined risk methodology and stronger formal reasoning
3. CIA evaluation
4. implementation planning and practical crypto considerations
5. actual UML source artefacts
6. report assembly guidance

The current `docs/pack-03/` phase is a documentation catch-up layer added after the auth foundation was implemented. Its purpose is to document the real implemented state and prevent further code-first drift before the next cryptography-heavy feature phase.

The `docs/pack-04/` phase now serves as the pre-implementation design gate for secure assignment submission, digital signatures, hash verification, and audit-integrity features.

The `docs/pack-05/` phase now serves as the pre-implementation design gate for grade integrity and the AES-based secure transmission demonstration.

The `docs/pack-06/` phase now records the implemented status and evidence for the secure submission and audit-integrity slice.

The `docs/pack-07/` phase now records the implemented status and evidence for the grade-integrity slice.

The `docs/pack-08/` phase now records the implemented status and evidence for the AES-GCM secure transmission demo.

The `docs/pack-09/` phase now serves as the final support pack for report evidence mapping, Vue frontend planning, and appendix planning for CI/CD and deployment.

## Planned build direction

The intended implementation direction remains:

- Vue.js frontend
- Spring Boot REST API
- PostgreSQL database
- cryptography features demonstrated in a way that supports the report

For the current PostgreSQL setup, security limits, and the new Liquibase-backed schema-management baseline, see `docs/pack-09/postgresql-setup-and-security.md`.

## Ground rules

- Keep the project academically original.
- Reuse patterns from `Christialattion`, not its employee-management domain.
- Keep the scope suitable for a module artefact, not a production platform.
- Ensure each feature maps back to the assignment brief, a documented risk, and a cryptographic control.

## Next recommended step after this pack

After reviewing `docs/pack-01/`, `docs/pack-02/`, `docs/pack-03/`, `docs/pack-04/`, `docs/pack-05/`, `docs/pack-06/`, `docs/pack-07/`, `docs/pack-08/`, and `docs/pack-09/`, the next phase should focus primarily on final evidence assembly, report writing, optional Vue frontend MVP work, and appendix polish rather than major new backend feature work.


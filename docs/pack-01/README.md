# EduSecure Planning Pack 01

This pack converts `docs/assignment_brief.md` into a practical project baseline before coding begins.

## Purpose

The assignment is a **report and artefact** for a cryptography module, so the project must be driven by:

- risk analysis
- cryptographic control selection
- UML-based secure system design
- a small but functional technical artefact

Rather than starting from code, this pack fixes the project scope, maps deliverables to repository outputs, and records what can be reused from `Christialattion/`.

## Contents

- `assignment-traceability.md` — maps brief requirements to planned repo outputs
- `reference-project-reuse-matrix.md` — identifies what may be adopted, adapted, or avoided from `Christialattion/`
- `scope-assumptions.md` — defines the intended study-project scope
- `risk-register.md` — initial security and cryptography risk baseline using NIST SP 800-30 style wording
- `uml-documentation-plan.md` — diagrams and evidence to prepare before implementation
- `agent-instructions.md` — repo-specific operating instructions for future work
- `grade-gap-analysis.md` — audit of Pack 01 against the assignment brief and top-grade risks

## Key references used in this pack

- `docs/assignment_brief.md`
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`
- `compose.yaml`
- `Christialattion/backend/EmployeeManager/pom.xml`
- `Christialattion/backend/EmployeeManager/src/main/java/dev/jh/employeemanager/config/SecurityConfig.java`
- `Christialattion/backend/EmployeeManager/src/main/java/dev/jh/employeemanager/service/AuthService.java`
- `Christialattion/backend/EmployeeManager/src/main/java/dev/jh/employeemanager/security/JwtAuthenticationFilter.java`
- `Christialattion/backend/EmployeeManager/src/main/resources/application.properties`
- `Christialattion/backend/EmployeeManager/Dockerfile`
- `Christialattion/docker-compose.yml`
- `Christialattion/frontend/src/router/index.ts`
- `Christialattion/frontend/src/stores/auth.ts`
- `Christialattion/frontend/src/services/api.ts`

## Working principle

Use `Christialattion/` only as a **pattern reference**:

- yes: layered architecture, validation style, auth flow structure, local Docker/dev setup ideas
- no: employee-management domain, endpoint naming, entity names, business logic, report wording

## Intended outcome of Pack 01

After this pack, the project should be ready for:

1. detailed UML production
2. backend simplification and module planning
3. a crypto-focused REST design
4. later Vue integration based on documented API contracts


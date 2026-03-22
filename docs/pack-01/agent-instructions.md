# EduSecure Future Agent Instructions

These instructions are intended for future work in this repository.

## 1. Project identity

Treat EduSecure as a **cryptography assignment case study** first and a web application second.

All future work must remain aligned to:
- `docs/assignment_brief.md`
- the planning baseline in `docs/pack-01/`

## 2. Core operating rules

1. Do documentation before feature implementation.
2. Keep the scope suitable for an academic artefact.
3. Reuse `Christialattion/` only for patterns, not domain copying.
4. Preserve originality in naming, modelling, and report reasoning.
5. Prefer the smallest architecture that still demonstrates the required cryptographic controls.

## 3. Mandatory traceability rule

Before implementing a feature, identify and document:

- which brief requirement it serves
- which risk(s) it mitigates
- which cryptographic control(s) it uses
- which CIA property it supports
- which UML/API/data-model artefact should be updated

If that mapping does not exist, documentation must be updated first.

## 4. Technical direction rules

### Backend

Prefer:
- Spring Boot REST
- Spring Security
- Validation
- JPA
- PostgreSQL

Treat the advanced scaffold currently present in `backend/build.gradle` as optional, not committed design. Do not expand LDAP, SAML, GraphQL, OAuth2 server, WebAuthn, or Modulith usage unless a documented reason exists.

### Frontend

Use Vue.js in a way that complements the report:
- clear role-based views
- simple auth state management
- explicit API contracts
- minimal UI complexity beyond what is needed to demonstrate the workflows

### Cryptography

Apply cryptography only where it truly fits:
- `bcrypt` for password hashing
- `AES` for symmetric confidentiality demonstrations
- `RSA` or `ECC` for digital signatures/asymmetric operations
- `SHA-256` and/or `HMAC` for integrity

Do not claim that cryptography fixes SQL injection. Address injection with secure coding, validation, and ORM-driven access patterns.

## 5. Christialattion reuse rules

Permitted pattern reuse:
- controller/service/repository layering
- auth flow separation
- DTO validation style
- env-based configuration
- Docker dev setup ideas
- Vue auth store, axios interceptor, route guard concepts

Not permitted as direct carryover:
- employee-management domain entities
- endpoint naming tied to EmployeeManager
- business logic and report text
- dev bypasses presented as final security controls

## 6. Documentation update rules

Any non-trivial change should trigger review of one or more of:
- `assignment-traceability.md`
- `reference-project-reuse-matrix.md`
- `scope-assumptions.md`
- `risk-register.md`
- `uml-documentation-plan.md`

## 7. Use of specialist agents

### Planning specialist
Use at the start of each major phase:
- risk-analysis refinement
- UML and architecture planning
- backend module design
- API design
- report assembly planning

### Security remediation specialist
Use whenever:
- new security or cryptography dependencies are introduced
- authentication libraries change
- frontend or backend dependencies are updated significantly
- final pre-submission dependency review is needed

## 8. Evidence expectations

When implementation begins, each major feature should leave evidence in the repository:
- documentation update
- source implementation
- test or runnable demo evidence
- short explanation of how it supports the report

## 9. Success criterion

A good EduSecure change is not merely one that works. It is one that is:
- easy to explain in the report
- clearly linked to the assignment brief
- scoped appropriately for a study project
- demonstrably relevant to confidentiality, integrity, or authenticity


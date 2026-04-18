# Scope and Assumptions

This document defines the intended scope of the EduSecure implementation before coding begins.

## 1. Scope statement

EduSecure will be developed as a **small, reportable study project** that demonstrates the secure design of an online education platform using selected cryptographic controls.

The goal is not to build a full production learning-management system. The goal is to produce a credible technical artefact that supports the cryptography report.

## 2. In-scope capabilities

### Core functional scope

- user authentication for Student, Lecturer, and Admin roles
- course-related access control at a simplified level
- assignment submission workflow
- grade viewing workflow
- grade modification workflow for authorised staff
- audit logging for sensitive changes

### Security and cryptography scope

- password hashing with `bcrypt`
- documented transport protection via `HTTPS/TLS` in the secure design
- symmetric encryption demonstration for secure file/message transmission using `AES`
- hashing and/or `HMAC` for integrity protection
- digital signature creation and verification for assignment authorship
- tamper-evident treatment of sensitive grade-change records

### Technical stack scope

- Vue.js frontend
- Spring Boot REST backend
- PostgreSQL database
- local Docker-based development environment

## 3. Out-of-scope items

The following are intentionally out of scope unless later justified:

- full production deployment hardening
- SAML, LDAP, WebAuthn, and OAuth2 authorization-server complexity
- large-scale video streaming or real-time classroom implementation
- advanced multi-tenant institutional management
- complete CI/CD, cloud deployment, or high-availability architecture
- full certificate lifecycle infrastructure
- large-scale file storage and malware scanning systems

## 4. Key design assumptions

1. The project will be assessed as an individual academic artefact.
2. The implementation may simulate parts of the platform rather than reproduce a full LMS.
3. HTTPS may be represented in documentation and local dev configuration without requiring a production-grade PKI rollout.
4. JWT may be used for stateless frontend-backend sessions, but only as a signed token mechanism.
5. Sensitive business data should remain in the database or protected responses, not embedded into token claims.
6. PostgreSQL is the intended relational store for application data.
7. Spring Data JPA and validation will be used to reduce avoidable injection-prone coding patterns.

## 5. Initial domain model direction

The EduSecure domain should be designed around education-platform concepts, not the employee-management concepts in `Christialattion/`.

Initial likely entities:

- `User`
- `Role`
- `Course`
- `Enrollment`
- `Assignment`
- `Submission`
- `Grade`
- `AuditLog`
- optional `SignatureRecord` or `SubmissionIntegrityRecord`

## 6. Carryovers from the reference project

The following ideas from `Christialattion/` are acceptable carryovers:

- layered package structure
- validation-oriented DTO design
- auth flow separation between controller and service
- route guarding and auth-state management ideas for Vue
- local environment configuration through environment variables
- Docker build layering similar to `Christialattion/backend/EmployeeManager/Dockerfile`

These carryovers must be adapted to the EduSecure academic context and renamed to fit the new domain.

## 7. Current stack assessment note

`backend/build.gradle` currently includes several advanced features such as LDAP, SAML2, OAuth2 server/client/resource-server support, WebAuthn, GraphQL DGS, and Modulith tooling.

For this assignment, those features should be treated as **non-committed scaffold dependencies** until a later documented decision confirms whether any are actually needed. The probable implementation should be simplified to match the brief and the study-project scope.


# UML and Documentation Plan

This document defines the diagrams and supporting documents that should be produced before implementation accelerates.

Sequence diagrams remain the **main UML evidence** for this assignment, because the brief explicitly asks for secure interaction design and insecure-vs-secure flows. However, they should be supported by a small set of additional diagrams that make the risk analysis, access policy, deployment assumptions, and implementation structure easier to explain in the report.

## 1. Required documentation mindset

The assignment explicitly expects secure system design and UML sequence diagrams. For EduSecure, diagrams should not be decorative: each one must prove a security point, implementation decision, or access-control rule.

## 2. Diagram set to prepare

## A. Use case diagram

Purpose:
- serve as an introduction to the report
- show role boundaries for Student, Lecturer, and Admin
- define the RBAC policy at a high level
- show where sensitive actions exist

Planned use cases:
- authenticate
- submit assignment
- verify assignment authorship
- view grade
- update grade
- review audit trail
- manage users/courses

Why it is important:
- it quickly establishes who is allowed to do what
- it gives context before the more detailed sequence diagrams
- it supports the access-policy discussion already identified in the planning pack

## B. Component diagram

Purpose:
- show high-level architecture and trust boundaries

Planned components:
- Vue frontend
- Spring Boot REST API
- Crypto service/module
- PostgreSQL database
- Audit logging component
- optional key-management abstraction for study purposes

## C. Class diagram

Purpose:
- support the Implementation Plan and Technical Artefact discussion
- document how cryptographic responsibilities will appear in code
- show how controllers, services, entities, and cryptographic abstractions relate to each other

Why it is worth including:
- it shows the supervisor how cryptography is planned in the code, not only in theory
- it bridges the report design section and the later Spring Boot implementation
- it makes reusable structural ideas from `Christialattion/` visible without copying its domain model

Planned contents:
- `User` entity with `passwordHash` instead of plaintext password
- planned MFA-related `User` metadata such as `mfaEnabled` and protected TOTP-secret fields
- short-lived `MfaChallenge` and one-time `MfaRecoveryCode` records for the MFA phase
- optional metadata such as `salt` if explicitly used in the chosen implementation approach
- `Assignment` or `Submission` entity with integrity-related fields such as `digitalSignature`, `hashDigest`, and signer metadata
- `Grade`
- `AuditLog`
- `Role`
- `ICryptoService` interface
- a concrete crypto implementation such as `AesRsaCryptoService`
- `AuthService` and related security services inspired structurally by `Christialattion`
- controller-to-service dependencies for secure operations

Implementation relationships to show:
- controllers depend on service interfaces rather than embedding cryptographic code directly
- auth-related flows depend on `AuthService` and security configuration patterns
- cryptographic operations are centralised in dedicated services
- audit generation is triggered by sensitive service-layer actions

## D. ERD / domain model diagram

Purpose:
- show the minimum academic data structure clearly

Initial entities:
- User
- Role
- Course
- Enrollment
- Assignment
- Submission
- Grade
- AuditLog
- SignatureRecord or equivalent

The ERD/domain view should remain simpler than the class diagram. Its job is to show the academic data model, whereas the class diagram shows implementation structure.

## E. Sequence diagrams

These remain the most important diagrams for the brief.

### 1. Insecure login flow
Show:
- plaintext credential risk
- lack of TLS
- token interception possibility
- absence of secure password handling assumptions

### 2. Secure login flow
Show:
- client sends credentials over TLS
- backend verifies `bcrypt` hash
- if MFA is disabled, backend issues session token
- if MFA is enabled, backend returns a short-lived challenge and issues the session token only after second-factor verification
- token used for protected API access

### 3. Insecure assignment submission flow
Show:
- file submitted without integrity check
- lecturer cannot confirm authorship
- possible tampering between sender and receiver

### 4. Secure assignment submission flow
Show:
- file hash creation
- signature generation by student
- backend stores file metadata, digest, signature, and signer identity
- lecturer or backend verifies signature before acceptance/review

### 5. Insecure grade update/retrieval flow
Show:
- grade modification without audit trail
- integrity/non-repudiation weakness

### 6. Secure grade update/retrieval flow
Show:
- authenticated lecturer/admin action
- role check
- audit record creation
- integrity protection for the audit record
- authorised student grade retrieval over protected channel

## F. Deployment diagram

Purpose:
- support the Risk Assessment and Secure System Design sections
- provide a bird's-eye view of the physical/logical infrastructure and data flow
- show local dev deployment for the artefact
- separate browser/frontend, backend, database, and transport path

Include:
- browser
- Vue application
- Spring Boot API
- PostgreSQL
- Docker/dev environment where relevant

Two versions should be produced:

### 1. Insecure deployment view
Show:
- student's laptop or browser
- public Wi-Fi or other untrusted network path
- backend server
- PostgreSQL database
- communication over plain HTTP
- plaintext or otherwise insufficiently protected sensitive data at rest

Main risks illustrated:
- eavesdropping
- man-in-the-middle exposure
- credential theft
- disclosure of sensitive data if storage is compromised

### 2. Secure deployment view
Show:
- communication over `HTTPS/TLS 1.3`
- password storage as hashed values such as `bcrypt` or `Argon2` conceptually
- AES-based protection for sensitive data at rest where relevant to the artefact design
- clearer trust boundaries between client, backend, and database

Main security improvements illustrated:
- encrypted transport
- hashed password storage
- stronger data-at-rest protection assumptions
- better alignment with the chosen cryptographic controls

## 3. Non-UML documentation to produce next

1. algorithm comparison table: AES, RSA, ECC, SHA-256, bcrypt, HMAC
2. REST API contract draft
3. data classification table for EduSecure assets
4. key-handling assumptions note
5. CIA evaluation table

## 4. Diagram quality rules

Every diagram should:
- label actors clearly
- show trust boundaries where relevant
- distinguish insecure and secure flows explicitly
- identify where a cryptographic primitive is applied
- be simple enough to explain in the report without extra guessing

Additional expectations for key diagram types:
- the use case diagram must make RBAC obvious at a glance
- the class diagram must show where cryptographic code lives in the Spring Boot design
- the deployment diagram must visibly distinguish insecure and secure infrastructure assumptions
- the sequence diagrams must remain the most detailed diagrams in the report

## 5. Recommended order of production

1. use case diagram
2. component diagram
3. class diagram
4. ERD/domain model
5. insecure vs secure sequence diagrams
6. deployment diagram
7. algorithm and CIA support tables


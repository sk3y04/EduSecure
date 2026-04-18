# Submission Scope and Phase Gate

This document defines the boundaries of the next feature phase before any code is written for submissions, signatures, grades, or audit integrity.

## 1. Purpose of the next feature phase

The next feature phase exists to implement the first assignment-focused cryptographic workflow in EduSecure:

- a student submits an assignment
- the system records a file digest
- the system verifies or simulates proof of authorship using a digital signature
- the system preserves enough integrity metadata for lecturer review and later report evidence
- related sensitive actions become auditable

## 2. Brief alignment

This phase directly supports the assignment brief requirements for:
- secure file/message transmission simulation
- hashing / integrity checking
- digital signature creation and verification
- proof of authorship
- tamper prevention and auditing for sensitive actions

## 3. Risks addressed

This phase primarily targets the following already-documented risks:
- `R4` assignment tampering / weak authorship proof
- `R5` untraceable sensitive actions affecting grades or review actions
- `R6` key or secret exposure, through documented key-handling assumptions
- `R7` scope drift, by keeping the feature phase intentionally small

## 4. In-scope items for the next coding phase

### Submission/authorship scope
- minimal `Assignment` entity
- minimal `Submission` entity
- `SHA-256` digest generation for uploaded content
- `ECC + SHA-256` digital signature generation and verification
- persistence of integrity/authorship metadata
- lecturer-facing verification status at API level

### Audit scope
- minimal `AuditLog` entity for submission verification and later grade-related changes
- integrity protection design for audit records
- logging of at least the most sensitive actions in the new phase

### Access-control scope
- students can submit their own assignments
- lecturers can review submissions and verification status
- admins can review audit history or system-level sensitive actions if needed

## 5. Explicitly out of scope for the next coding phase

The next phase should not attempt to build:
- a full LMS workflow
- detailed course timetabling or content management
- real external PKI or certificate management
- production-grade key escrow or HSM integration
- advanced anti-malware file scanning
- large-scale file storage strategy
- complete gradebook UX or complex appeal workflows

## 6. Chosen signing model for the study project

### Selected approach
Use a **system-simulated student signing model** for the first artefact implementation.

### Meaning
- each student submission is associated with a signing identity within the system model
- the implementation may generate and manage per-user signing material server-side for the artefact demonstration
- the report must clearly state that this simulates student-controlled signing in a study-project environment

### Why this approach was selected
- it keeps the implementation feasible in Spring Boot
- it still demonstrates proof-of-authorship logic
- it avoids premature frontend or client-device key-management complexity
- it is easier to test, explain, and present in the report

## 7. Go condition for coding

Coding for the next phase may begin only if all of the following are true:
- the submission entity responsibilities are accepted
- the submission endpoint contract is accepted
- the digest/signature workflow is accepted
- the audit-integrity plan is accepted
- the evidence and tests are accepted

## 8. No-Go condition for coding

Do not start the next feature phase if any of the following remain unresolved:
- uncertainty over where `hashDigest` is stored
- uncertainty over where `digitalSignature` is stored
- uncertainty over whether verification occurs on upload or review
- uncertainty over which actions must create audit records
- uncertainty over what tests will prove the feature works

## 9. Follow-on extension note: AES at-rest for submissions

After the baseline submission/authorship flow is accepted, the preferred next AES enhancement is:
- encrypt **submission content at rest** before attempting encrypted feedback workflows

This extension is preferred because:
- submissions are already a first-class protected asset in the existing design
- confidentiality-at-rest complements the established digest/signature workflow cleanly
- it keeps AES tied to a real EduSecure asset rather than only a standalone demo

The detailed design for that later implementation is recorded in:
- `submission-aes-storage-design.md`

The coding-ready execution steps for that later implementation are recorded in:
- `submission-aes-storage-implementation-checklist.md`

The exact backend file touchpoints for that later implementation are recorded in:
- `submission-aes-storage-file-by-file-plan.md`


# Audit Integrity and Evidence Plan

This document defines the minimum audit-integrity design for the next feature phase and the evidence required for testing and reporting.

For the current implemented submission-content access audit behavior, see `docs/04-evidence-testing/submissions-audit/submission-content-protection-and-retrieval.md`.

## 1. Why audit must be included

The assignment brief explicitly highlights missing logging and verification for sensitive actions. Even though secure submission is the main next feature, the implementation should start introducing auditable records now so the later grade-integrity phase has a consistent foundation.

## 2. Sensitive actions to log in the next phase

The following actions should create audit records in the first submission-focused pass:
- assignment created
- submission created
- submission verification succeeded
- submission verification failed
- submission content accessed through a controlled retrieval endpoint

If grade work is added later, additional actions should include:
- grade created
- grade updated

## 3. Selected integrity approach for the first audit pass

### Selected recommendation
Use `HMAC-SHA-256` as the primary integrity mechanism for audit records in the first implementation pass.

### Why
- stronger authenticated integrity than plain hashing alone
- directly aligned with the existing crypto decision matrix
- relatively simple to implement in Java
- suitable for internal system audit records where a shared secret is acceptable

### How it should be applied
For each audit event, compute an integrity value over a stable representation of the record, for example:
- action type
- actor identifier
- entity type
- entity identifier
- timestamp
- selected detail fields

Optional extension:
- include the previous audit integrity value to create a simple chain for stronger tamper-evidence

## 4. Minimum `AuditLog` behavior

The first implementation should aim for:
- append-oriented writes only
- no ordinary update endpoint
- one integrity value stored per event
- enough fields to reconstruct who did what and when

This is sufficient for a study project and avoids overbuilding.

## 5. Relationship to submission and grade phases

### Submission phase
Audit should immediately support:
- proof that a submission event occurred
- proof that verification was attempted
- proof whether verification succeeded or failed
- proof that decrypted content access was separately authorised and recorded when used

### Later grade phase
The same model should later support:
- grade creation/update logging
- integrity-protected sensitive academic actions
- lecturer/admin accountability discussion in the report

## 6. Test and evidence expectations

The next implementation phase should define tests that prove at least the following:

### Positive cases
- submission creation writes an audit record
- successful verification writes the expected audit action
- successful submission content retrieval writes the expected audit action
- persisted audit record contains a non-empty integrity value

### Negative or edge cases
- failed verification produces a failed-verification audit event
- modifying the canonical audit payload changes the computed HMAC result
- unauthorised actors cannot invoke protected submission-review actions
- unrelated students cannot retrieve another student's decrypted submission content

## 7. Report evidence expectations

The report should later be able to include evidence such as:
- API response showing submission verification status
- API response showing controlled plaintext retrieval via a separate endpoint
- database or logged output showing audit record creation
- test evidence showing different integrity values for modified audit payloads
- explanation of why `HMAC` was chosen instead of plain hash for internal audit protection

## 8. Scope control note

Do not try to build a full SIEM, alerting system, or enterprise audit framework in this module artefact.

The goal is to show:
- accountable sensitive actions
- tamper-evident integrity support
- clear linkage to the cryptography brief

That is enough for the study-project scope.


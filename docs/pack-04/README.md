# EduSecure Planning Pack 04

Pack 04 is the **pre-implementation design pack** for the next major feature phase: secure assignment submission, proof of authorship, and tamper-evident grade/audit handling.

## Why this pack exists

Pack 03 explicitly stopped further feature coding until the next phase was documented. Pack 04 satisfies that requirement by freezing the design choices that were still unresolved:

- `Assignment` and `Submission` structure
- `Grade` and `AuditLog` responsibilities
- where `hashDigest` and `digitalSignature` belong
- how AES can later become a real submission-protection control at rest
- which endpoints are required for the first secure submission demo
- how signature verification should work
- what evidence and tests must exist before calling the next phase complete

## Scope of Pack 04

This pack does **not** implement the next feature phase.

It documents:
- feature boundaries
- domain responsibilities
- API contracts
- cryptographic workflow design
- audit-integrity design
- test and report evidence expectations

Auth baseline carried into this pack:
- protected submission endpoints assume the Pack 03 authenticated browser-session model
- browser clients use the backend-issued `HttpOnly` auth cookie
- frontend requests are expected to be sent with credentials enabled rather than a frontend-managed bearer token

## Contents

- `submission-scope-and-phase-gate.md`
- `submission-grade-audit-domain-design.md`
- `api-submission-contract.md`
- `signature-hash-workflow.md`
- `submission-aes-storage-design.md`
- `submission-aes-storage-implementation-checklist.md`
- `submission-aes-storage-file-by-file-plan.md`
- `audit-integrity-and-evidence-plan.md`
- `uml/README.md`
- `uml/*.puml`

## Design posture

All design choices in Pack 04 are deliberately scoped for a study project:
- small enough to implement cleanly
- strong enough to support the assignment brief
- honest about simulated aspects
- traceable to risk, CIA, and report evidence needs

## Outcome expected from Pack 04

After this pack, the project should be ready to begin the next coding phase for secure submission and audit features **without relying on undocumented assumptions**.

That includes avoiding undocumented auth assumptions: the inherited security model for this pack is the implemented cookie-backed authenticated session described in `../pack-03/api-auth-contract.md`.


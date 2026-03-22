# EduSecure Planning Pack 05

Pack 05 is the next documentation-only gate after Pack 04.

## Why this pack exists

Pack 04 freezes the design for secure submission, signatures, hashing, and initial audit logging. However, two important areas still need to be documented before more feature work can continue in a disciplined way:

1. grade integrity and grade update governance
2. the AES-based secure transmission demo required for the technical artefact

## Purpose

Pack 05 defines:
- the minimum grade-integrity workflow
- the grade API contract and update rules
- the AES-GCM transmission demo boundary and assumptions
- the test/evidence expectations for both areas
- UML addenda for secure grade handling and AES transmission

Inherited auth baseline for this pack:
- protected grade and AES-demo endpoints assume the Pack 03 authenticated session model
- browser-facing clients use the backend-issued `HttpOnly` auth cookie
- frontend requests are expected to use credentialed transport rather than a frontend-managed bearer token

## Contents

- `grade-integrity-scope-and-phase-gate.md`
- `api-grade-contract-and-update-rules.md`
- `aes-secure-transmission-demo-design.md`
- `test-and-evidence-plan.md`
- `uml/README.md`
- `uml/*.puml`

## Outcome expected from Pack 05

After this pack, the remaining pre-coding uncertainty around grade integrity and the AES demonstration should be low enough to proceed without repeating the earlier code-first drift.

The detailed auth transport rule for this pack remains the one defined in `../pack-03/api-auth-contract.md`.


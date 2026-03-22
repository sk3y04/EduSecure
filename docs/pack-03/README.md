# EduSecure Planning Pack 03

Pack 03 is a **documentation catch-up phase** created after the initial backend auth foundation was implemented.

## Why this pack exists

Implementation moved ahead far enough to create a real backend baseline:

- simplified Spring Boot REST backend
- bcrypt-based password handling
- JWT-backed session flow transported to browser clients in an `HttpOnly` auth cookie
- public health endpoint
- integration-tested auth endpoints

That progress is useful, but it also means the documentation must catch up before additional feature coding continues.

## Purpose

Pack 03 does four things:

1. documents what is already implemented in the backend
2. records the current auth/API contract in report-friendly form
3. explains the current data-model choices and their limits
4. creates a documentation gate before submission, signature, hash, and audit features are implemented
5. explains the cryptographic rationale of the implemented MFA design

The detailed source of truth for the current auth transport model is:
- `api-auth-contract.md`
- `implementation-status-and-evidence.md`

Those documents now describe the browser-facing auth baseline as a cookie-backed authenticated session,
including `POST /api/auth/logout`, credentialed frontend requests, and production/startup validation notes.

## Contents

- `implementation-status-and-evidence.md`
- `api-auth-contract.md`
- `data-model-rationale.md`
- `mfa-cryptography-implementation.md`
- `phase-gate-next-coding.md`

## Rule for the next phase

No new submission, signature, hash, AES, HMAC, grade-integrity, or audit feature work should begin until the Pack 03 phase gate is satisfied.


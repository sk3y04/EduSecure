# Data Model Rationale

This document explains the current authentication-related data model and records the approved model extensions needed before MFA coding begins.

The purpose of this update is to make the next auth phase explicit instead of letting MFA be added ad hoc.

## 1. Current implemented auth model

## `User` — Implemented

Current fields in code:
- `id`
- `email`
- `passwordHash`
- `fullName`
- `roles`

### Why these fields exist
- `email` is the login identifier
- `passwordHash` directly supports the plaintext-password mitigation in the brief
- `fullName` provides a minimal human-readable identity field for the app and report
- `roles` supports RBAC and aligns with the planned Student/Lecturer/Admin model

### Why the current model was intentionally small
The original auth baseline only needed enough identity structure to support bcrypt-protected passwords, JWT login, and role-based access. That is why profile, course, and cryptographic ownership details were originally deferred.

## `Role` — Implemented

Current fields:
- `id`
- `name`

### Why it exists as a separate entity
A separate `Role` entity keeps the RBAC model explicit and extensible. It also makes the class diagram and access-control explanation cleaner for the report.

## `RoleName` — Implemented

Current enum values:
- `STUDENT`
- `LECTURER`
- `ADMIN`

### Why these values were chosen
They match the actor model already established in Pack 01 and support the access-separation discussion in the report.

## 2. MFA extension approved for the next coding phase

The next authentication change will use **optional TOTP-based MFA**. That choice affects the data model in three places:

1. the `User` record needs MFA state
2. login needs a temporary MFA challenge record
3. MFA recovery needs one-time backup codes

## `User` — Planned MFA additions

Planned added fields:
- `mfaEnabled: boolean`
- `mfaMethod: MfaMethod?`
- `mfaSecretCiphertext: String?`
- `mfaSecretNonce: String?`
- `mfaSecretKeyVersion: String?`
- `mfaEnabledAt: Instant?`

### Why these fields are needed
- `mfaEnabled` prevents the login flow from guessing based on null secret values
- `mfaMethod` keeps the design extensible even though the first method is only TOTP
- `mfaSecretCiphertext` stores the TOTP seed in protected form rather than plaintext
- `mfaSecretNonce` supports authenticated encryption at rest if AES-GCM is used to protect the secret
- `mfaSecretKeyVersion` allows later key rotation without data ambiguity
- `mfaEnabledAt` supports auditability and UI visibility

### Why the secret should not be plaintext
Unlike a password hash, a TOTP secret must be recoverable by the server to validate future login codes. That means hashing alone is not sufficient. The correct design goal is therefore:

- keep the secret decryptable by the application
- but store it encrypted at rest under a separate application-managed key

This is different from `passwordHash`, and the report should describe that distinction clearly.

## `MfaMethod` — Planned

Planned initial enum values:
- `TOTP`

Why keep an enum with one value initially:
- it documents the chosen factor explicitly
- it prevents magic strings in later code
- it allows future extension without redesigning every auth response

## `MfaChallenge` — Planned

Planned fields:
- `id`
- `user`
- `challengeType`
- `expiresAt`
- `consumedAt`
- `attemptCount`
- `maxAttempts`
- `createdAt`

### Why this entity is needed
For MFA-enabled users, password verification should succeed before a JWT is issued. The system therefore needs a short-lived server-side record that says:

- the password step already succeeded
- the user still owes a second factor
- the second-factor attempt expires quickly
- repeated guessing can be limited

This is safer than issuing a partially trusted bearer token before MFA is complete.

### Why server-side challenge state is acceptable
The current API is stateless only after full authentication. A short-lived MFA challenge record does not change the final bearer-token model; it only protects the pre-auth gap between factor 1 and factor 2.

## `MfaRecoveryCode` — Planned

Planned fields:
- `id`
- `user`
- `codeHash`
- `usedAt`
- `createdAt`

### Why recovery codes are separate records
- each code is one-time use
- each code should be independently consumable
- remaining-code counts are easier to calculate
- hashing one code per record is cleaner than storing a single blob of reusable secrets

### Why recovery codes should be hashed
Recovery codes behave like backup passwords. They are not repeatedly derived from a single master secret like TOTP values. Because the plaintext code itself does not need to be recoverable later, hashing is the safer storage model.

## 3. Security rationale of the auth + MFA model

### Password representation
The code stores `passwordHash`, not plaintext password. This directly aligns the implementation with:
- the brief's plaintext-password problem
- the class diagram rationale in Pack 02
- the secure login narrative

### MFA secret representation
The planned MFA secret storage is intentionally different from password storage:
- password = one-way hash
- TOTP secret = encrypted-at-rest secret that the server can later decrypt for verification

That distinction must stay explicit in both code and report text.

### Role relationship
The many-to-many relationship allows:
- future RBAC flexibility
- later extension of permissions without redesigning the identity model
- consistent integration with Spring Security authorities

### Recovery fallback rationale
Recovery codes reduce the risk of permanent user lockout when the authenticator device is unavailable. They improve operational resilience, but they also introduce extra secret material, so they must be treated as one-time high-sensitivity credentials.

## 4. What is intentionally not in the first MFA model

The first MFA phase should not model:
- SMS provider metadata
- email delivery queues for OTP
- WebAuthn credentials / passkey attestation data
- trusted-device cookies
- organisation-level enforcement or exemption policies

These are valid future topics, but adding them now would complicate a small, explainable Spring/JWT implementation.

## 5. Required data-model decisions before MFA coding starts

Before the MFA code phase begins, the following must remain fixed:

### A. Secret protection design
- where the MFA encryption key comes from
- whether the secret ciphertext and nonce are Base64-encoded strings or binary columns
- how key versioning is represented

### B. Challenge lifecycle design
- challenge expiry duration
- maximum attempts per challenge
- whether challenge cleanup is lazy, scheduled, or both

### C. Recovery-code design
- how many codes are generated initially
- display format for user-facing codes
- whether regeneration is in scope for phase 1 or deferred

### D. JWT auth-context design
- whether the token includes `mfa=true`
- whether the token includes `amr`
- whether downstream code needs to distinguish `pwd` from `pwd+otp`

## 6. Recommendation

Proceed to MFA implementation only with the following documented set:
- updated API contract for branching login responses and MFA endpoints
- explicit `User` MFA fields
- a short-lived `MfaChallenge` model
- hashed one-time `MfaRecoveryCode` records
- a documented secret-at-rest protection strategy

This keeps the project aligned with the documentation-first rule and makes the MFA code phase much less risky than changing the auth flow directly in code.


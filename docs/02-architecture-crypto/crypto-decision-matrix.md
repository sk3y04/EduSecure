# Cryptographic Decision Matrix

This document compares the algorithms explicitly named in the assignment brief and records the planned EduSecure selections.

## 1. Decision criteria

The selected mechanisms should satisfy four needs:

1. fit the incidents and risks in the EduSecure case study
2. be implementable using standard Java/Spring-compatible libraries
3. be explainable in a 2,500-word academic report
4. remain small enough for a study-project artefact

## 2. Comparison matrix

| Primitive / algorithm | Primary purpose | Strengths | Limitations | Best-fit EduSecure use |
|---|---|---|---|---|
| `AES` | Symmetric encryption | Fast, standardised, strong for confidentiality, widely supported | Requires secure key handling; insecure modes or nonce misuse can break security | Secure file/message transmission simulation; protection of selected sensitive content at rest |
| `RSA` | Asymmetric encryption and digital signatures | Well-known, easy to explain academically, strong library support, suitable for signatures | Larger key sizes and slower than ECC; not ideal for bulk encryption | Digital signature creation and verification for assignment authorship |
| `ECC` | Asymmetric encryption/signatures with smaller keys | Strong security with smaller keys, efficient modern design | More conceptually difficult to explain; implementation choices can be less familiar to beginners | Alternative to RSA; useful to compare but not necessary for first artefact implementation |
| `SHA-256` | Cryptographic hashing | Widely accepted, efficient, suitable for integrity digests | Hash alone does not authenticate sender; not suitable for password storage | File digest generation, audit-chain hashing, signature pre-processing |
| `bcrypt` | Password hashing | Designed for password storage; salted by design; adaptive work factor; directly addresses plaintext password problem | Not suitable for general-purpose hashing or file integrity | User credential storage |
| `HMAC` | Message authentication / integrity with shared secret | Provides integrity and authenticity for data shared between trusted components; simple to implement | Requires shared secret distribution and protection; symmetric trust model | Tamper-evident audit integrity and protected message checks |

## 3. Final EduSecure selections

## A. Password storage

### Selected
- `bcrypt`

### Rationale
- directly addresses the brief's plaintext-password problem
- explicitly named in the brief
- easy to implement with Spring Security
- academically easy to justify

### Not selected instead
- plain `SHA-256` is rejected for password storage because it is too fast and not designed for password hashing

## B. Symmetric confidentiality control

### Selected
- `AES-GCM`

### Rationale
- satisfies the brief's AES comparison requirement while choosing a modern authenticated-encryption mode
- protects confidentiality and also adds integrity for the encrypted payload
- suitable for secure file/message transmission simulation

### Not selected instead
- older modes such as ECB are unsuitable
- CBC would require separate integrity protection and is easier to misuse

## C. Digital signature / proof of authorship

### Selected
- `ECC` with `SHA-256`

### Rationale
- provides smaller keys and a more modern asymmetric-signature design than RSA
- provides a clear proof-of-authorship story for assignment submissions
- well supported in the standard Java cryptography architecture

### Why RSA is still discussed
- RSA should still be compared in the report because the brief expects comparison
- RSA remains a valid alternative and may be mentioned as a simpler comparison point

## D. Integrity checking

### Selected
- `SHA-256` for digests
- `HMAC-SHA-256` for tamper-evident audit integrity where shared-secret protection is appropriate

### Rationale
- `SHA-256` is good for file fingerprints and signature input preparation
- `HMAC` strengthens integrity claims where a shared secret is available
- this creates a cleaner distinction between simple hash-based checking and authenticated integrity protection

## E. Session/authentication support

### Selected design note
- `JWT` may be used for stateless API sessions, but it is not treated as one of the assignment's core cryptographic controls

### Rationale
- useful for frontend/backend session handling
- must be protected by `HTTPS/TLS`
- must not be described as encryption of user data

## 4. Final control-to-risk mapping

| Selected control | Main risks addressed |
|---|---|
| `bcrypt` | plaintext credential disclosure |
| `AES-GCM` | insecure file/message confidentiality |
| `ECC + SHA-256` signature | assignment tampering, weak authorship proof |
| `SHA-256` digest | file integrity checking, audit-chain input |
| `HMAC-SHA-256` | tamper-evident protection for sensitive records/logging |
| `TLS 1.3` in secure design | token interception, eavesdropping, MITM exposure |

## 5. Final implementation recommendation

For the first EduSecure artefact, the recommended control set is:

1. `bcrypt` for user passwords
2. `AES-GCM` for secure file/message transmission simulation
3. `ECC + SHA-256` for digital signature generation and verification
4. `SHA-256` and/or `HMAC-SHA-256` for integrity and tamper-evident audit support

This selection covers the brief well while keeping implementation manageable in Java Spring Boot.


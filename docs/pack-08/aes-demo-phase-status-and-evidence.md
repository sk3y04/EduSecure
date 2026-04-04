> **⚠️ Deprecated (updated 2026-03-30):** The retired standalone symmetric-crypto slice documented in this file has been removed from the codebase. Its former controller, service, tests, and dedicated key material have all been deleted. Secure transmission is now handled by TLS 1.3 via Certbot/Let's Encrypt. This file is retained as a historical record only and must not be cited as active evidence in the report.

# Retired Symmetric-Crypto Slice Status and Evidence

This document records the former implemented status of the Pack 05-aligned standalone symmetric-transport slice.

## 1. What was implemented in the removed slice

The removed backend demo previously included:
- AES message encryption via a dedicated endpoint
- explicit nonce packaging
- ciphertext packaging separate from nonce
- AES-GCM decryption
- clean failure handling for tampered ciphertext and malformed input
- authenticated access to the demo endpoints

## 2. Former endpoints in this phase

- `POST /api/crypto-demo/encrypt`
- `POST /api/crypto-demo/decrypt`

## 3. Implemented behavior

### Encryption
The backend now:
- accepts plaintext input
- generates a fresh nonce using `SecureRandom`
- encrypts using `AES/GCM/NoPadding`
- returns:
  - `algorithm`
  - `nonce`
  - `ciphertext`

### Decryption
The backend previously:
- accepts `nonce` and `ciphertext`
- decrypts using a dedicated demo key configured for the old standalone demo
- returns plaintext on success
- rejects tampered or malformed input with a client-error response

## 4. Security and design notes implemented

### Authentication
The retired slice's endpoints were not public. They relied on the existing authenticated API baseline.

### Key handling
The retired slice used an environment-configurable AES key so that local testing was repeatable.

### Scope honesty
That retired implementation remained separate from the normal auth, submission, and grade business flows. It demonstrated symmetric encryption as an artefact capability rather than pretending the normal application payloads were all application-layer AES-encrypted.

If AES is later promoted into a real EduSecure business control, the preferred first target is encrypted submission storage at rest rather than blanket API-payload encryption. The design note for that future implementation is recorded in `docs/pack-04/submission-aes-storage-design.md`.

That promotion has now been implemented for the submission domain, and the current code-aligned explanation is recorded in `docs/pack-06/submission-content-protection-and-retrieval.md`.

## 5. Historical test evidence previously available

### Former slice evidence
- the retired slice's dedicated integration test class

This historically proved:
- authenticated user can encrypt a message
- ciphertext differs from plaintext
- authenticated user can decrypt with the correct nonce/ciphertext
- tampered ciphertext is rejected
- malformed Base64 input is rejected
- unauthenticated access is rejected
- empty plaintext fails validation

## 6. What is now covered in the current implemented artefact

At this point, the implementation now includes working examples of:
- password hashing with `bcrypt`
- hashing/signature-backed submission integrity workflow
- AES-GCM-backed encryption of submission content at rest
- HMAC-backed audit integrity for sensitive actions

That retired standalone symmetric-transport slice is no longer part of the current codebase.

This gives the project strong coverage of the assignment's required technical artefact areas.

## 7. What still remains

The main remaining work is now less about core feature coding and more about:
- final report evidence assembly
- optional audit review endpoint exposure if desired
- final documentation/report polishing
- final submission packaging


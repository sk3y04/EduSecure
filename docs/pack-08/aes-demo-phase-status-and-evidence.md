# AES Demo Phase Status and Evidence

This document records the current implemented status of the Pack 05-aligned AES-GCM secure transmission demonstration.

## 1. What is now implemented

The backend now includes a dedicated AES-GCM demo implementation for:
- AES message encryption via a dedicated endpoint
- explicit nonce packaging
- ciphertext packaging separate from nonce
- AES-GCM decryption
- clean failure handling for tampered ciphertext and malformed input
- authenticated access to the demo endpoints

## 2. Implemented endpoints in this phase

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
The backend now:
- accepts `nonce` and `ciphertext`
- decrypts using the configured AES demo key
- returns plaintext on success
- rejects tampered or malformed input with a client-error response

## 4. Security and design notes implemented

### Authentication
The AES demo endpoints are not public. They currently rely on the existing authenticated API baseline.

### Key handling
The demo uses an environment-configurable AES key so that local testing is repeatable.

### Scope honesty
This implementation remains separate from the normal auth, submission, and grade business flows. It demonstrates symmetric encryption as an artefact capability rather than pretending the normal application payloads are all application-layer AES-encrypted.

If AES is later promoted into a real EduSecure business control, the preferred first target is encrypted submission storage at rest rather than blanket API-payload encryption. The design note for that future implementation is recorded in `docs/pack-04/submission-aes-storage-design.md`.

That promotion has now been implemented for the submission domain, and the current code-aligned explanation is recorded in `docs/pack-06/submission-content-protection-and-retrieval.md`.

## 5. Test evidence currently available

### New AES-phase evidence
- `backend/src/test/java/edusecure/edusecure/AesDemoIntegrationTests.java`

This currently proves:
- authenticated user can encrypt a message
- ciphertext differs from plaintext
- authenticated user can decrypt with the correct nonce/ciphertext
- tampered ciphertext is rejected
- malformed Base64 input is rejected
- unauthenticated access is rejected
- empty plaintext fails validation

## 6. What is now covered in the implemented artefact

At this point, the implementation now includes working examples of:
- password hashing with `bcrypt`
- hashing/signature-backed submission integrity workflow
- AES-GCM-backed encryption of submission content at rest
- HMAC-backed audit integrity for sensitive actions
- AES-GCM secure transmission demo

This gives the project strong coverage of the assignment's required technical artefact areas.

## 7. What still remains

The main remaining work is now less about core feature coding and more about:
- final report evidence assembly
- optional audit review endpoint exposure if desired
- final documentation/report polishing
- final submission packaging


# AES Secure Transmission Demo Design

> **Historical planning note:** The standalone AES transmission demo described below was later removed from the codebase. Keep this file only as design history. The current implementation evidence for symmetric encryption comes from AES-GCM protection of MFA secrets and submission content at rest, while transport security is handled in the deployment narrative via TLS.

This document defines the AES-based confidentiality demonstration that will support the technical artefact.

## 1. Why this demo originally needed its own design note

At the time this note was written, the assignment was being satisfied with a standalone symmetric-encryption demo in addition to the broader secure-system design.

## 2. Scope of the retired standalone symmetric-crypto slice

### Selected recommendation
Use a **small message/file-content encryption demonstration** that is separate from the main authenticated REST business flow.

### Why
- it clearly proves the use of symmetric encryption in the artefact
- it avoids overclaiming that normal grade retrieval is application-layer AES-encrypted end-to-end
- it keeps the report honest: `TLS` protects transport in the secure system design, while `AES-GCM` is demonstrated as a separate confidentiality mechanism

## 3. Selected algorithm and mode

### Selected
- `AES-GCM`

### Why
- already chosen in the crypto decision matrix
- provides confidentiality and authenticated integrity for the encrypted payload
- widely supported in Java cryptography APIs
- more appropriate than ECB or bare CBC for a study-project demo

## 4. Demonstration boundary

The demo should show:
1. plaintext message or file content
2. AES key generation or loading in the study-project model
3. fresh nonce/IV generation with `SecureRandom`
4. ciphertext output
5. decryption back to the original plaintext
6. failure or mismatch behavior if ciphertext or auth tag is altered

## 5. Nonce / IV rules

For that retired standalone slice:
- generate a fresh nonce/IV for every encryption operation
- never reuse the same nonce/IV with the same key
- package the nonce/IV alongside the ciphertext for later decryption
- make the packaging format explicit in code or API response

Recommended simple payload structure:
- `nonce`
- `ciphertext`
- `authenticationTag` or combined GCM output representation

## 6. Key-handling assumptions

### Study-project recommendation
Keep AES key handling deliberately simple and explicitly documented.

Acceptable first artefact options:
- system-generated transient key for demo runs
- environment-configured demo key for repeatable local testing

### Important report note
This is a cryptographic demonstration, not a production key-management deployment. The report should say so clearly.

## 7. Historical relationship to TLS

### Important distinction
- `TLS` protects normal client-server traffic in the secure deployment design
- `AES-GCM` in this artefact demonstrates symmetric encryption as a separate cryptographic control

Do not blur these two things together in the report.

## 8. Possible demo shapes

### Option A: message demo
Encrypt/decrypt a short message such as:
- lecturer feedback text
- confidential exam note
- sensitive announcement payload

### Option B: file-content demo
Encrypt/decrypt file bytes such as:
- small text file content
- mock assignment payload bytes

### Selected recommendation
Use **message-first, file-compatible design**.

Reason:
- easier testability
- easier output inspection in unit/integration tests
- still easy to explain as a secure transmission simulation

## 9. Minimum success criteria

The later implementation should demonstrate:
- encrypting plaintext produces non-readable ciphertext
- decrypting with the correct key and nonce restores the original plaintext
- tampering with ciphertext or auth tag causes decryption failure
- nonce reuse is documented as prohibited even if not demonstrated destructively in code

## 10. Suggested implementation boundary

Keep that retired standalone slice in a dedicated crypto endpoint or service, not mixed into grade retrieval or auth endpoints.

That keeps the artefact clearer and avoids confusing the pedagogical purpose of the demo.


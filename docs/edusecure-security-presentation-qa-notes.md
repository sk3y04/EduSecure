# EduSecure Presentation Q&A Cheat Sheet

This file is a short backup note for likely presentation questions.

---

## 1. What cryptographic techniques did you actually implement?
**Short answer:**
- `bcrypt` for password hashing
- TOTP MFA using an HMAC-based OTP model
- `SHA-256` for submission digests
- ECC-based digital signature workflow using `SHA256withECDSA`
- `HMAC-SHA-256` for audit integrity values
- `AES-GCM` for MFA secret protection at rest
- `AES-GCM` for submission content encryption at rest
- `AESWrap` for wrapping per-submission content keys

**Safe evidence anchors:**
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`

---

## 2. Why did you use `bcrypt` instead of `SHA-256` for passwords?
**Answer:**
Because passwords need a password-hashing function, not a fast general-purpose digest. `bcrypt` is designed for storing password verifiers and is much better suited than raw `SHA-256` for the plaintext-password problem in the brief.

**Good one-liner:**
> `SHA-256` is good for file integrity; `bcrypt` is the correct control for password storage.

---

## 3. Why did you use TOTP MFA?
**Answer:**
TOTP is realistic, cryptographically meaningful, and achievable in a study-project scope. It adds a second proof of identity without needing external SMS/email systems. It also gave me a strong way to discuss secure randomness, secret storage, recovery codes, and delayed token issuance.

**Honest limitation:**
It is stronger than password-only auth, but not as phishing-resistant as WebAuthn/passkeys.

---

## 4. Why are TOTP secrets encrypted, but passwords hashed?
**Answer:**
Passwords only need verification, so hashing is correct. TOTP secrets must be recovered later so the server can recompute valid codes, so they must be encrypted rather than hashed. That is why I used AES-GCM for stored MFA secrets.

**Good one-liner:**
> Passwords are verifier-only secrets; TOTP seeds are reusable computation secrets.

---

## 5. Why did you choose AES-GCM?
**Answer:**
Because AES-GCM provides authenticated encryption, so it protects both confidentiality and ciphertext integrity. I used it in two real places in the application: MFA secrets at rest and submission content at rest.

**Avoid overclaiming:**
Do not say this proves whole-database encryption.

---

## 6. Why ECC instead of RSA for signatures?
**Answer:**
ECC is a modern asymmetric choice with smaller key sizes and efficient signatures. It is also well supported by Java crypto APIs. RSA is still a valid comparison point and should be discussed academically, but ECC was the implemented signing choice.

**Honest limitation:**
The signing model uses a stable demo keypair, not a full student-held PKI system.

---

## 7. How do you prove submissions are protected?
**Answer:**
The upload flow computes a `SHA-256` digest, signs it, verifies it immediately, stores verification status, encrypts the content with AES-GCM, wraps the per-submission key, and stores ciphertext separately from metadata. The integration tests prove that ciphertext differs from plaintext, that metadata does not expose storage internals, and that only authorised users can retrieve content.

**Evidence anchors:**
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `docs/pack-06/submission-content-protection-and-retrieval.md`

---

## 8. How do you stop students reading each other’s work or grades?
**Answer:**
Access control is enforced through Spring Security plus service-layer ownership checks. Students can access only their own visible submissions and grades. Lecturers are limited to assignments they own, while admins keep oversight access.

**Good one-liner:**
> I combined RBAC with object-level authorization checks, not just role checks alone.

---

## 9. What protects against CSRF and token exposure?
**Answer:**
The session JWT is stored in an `HttpOnly` cookie, so frontend JavaScript does not read it directly. Unsafe requests require the CSRF token pair: readable `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header. The frontend uses credentialed requests and does not store the auth JWT in `localStorage`.

---

## 10. Did you implement HTTPS/TLS?
**Safe answer:**
TLS 1.3 via Certbot/Let's Encrypt is documented as the intended deployment-side transport control. That is the control I would point to for reducing eavesdropping and MITM risk against credentials, session cookies, and API traffic in transit. In the repository, the strongest directly implemented symmetric-encryption evidence is AES-GCM for MFA secrets and stored submission content. So I present TLS as the secure deployment design unless I also show direct deployment proof.

**Important:**
Do not say “HTTPS is fully proven by the repository” unless you actually show deployment evidence.

---

## 10a. If someone asks, “Where is the MITM protection?”
**Safe answer:**
MITM risk comes from the brief’s insecure-transport scenario, especially token interception on public Wi‑Fi. In EduSecure, the intended control against that is TLS 1.3 in deployment. Inside the app itself, I also reduce session exposure by using an `HttpOnly` auth cookie instead of frontend token storage, but the main anti-MITM control is still TLS at the transport layer.

**Best one-liner:**
> Browser-side cookie hardening helps protect the session model, but TLS is the main control against MITM in transit.

**Boundary:**
Do not say MITM is fully prevented by the repository alone unless you also show deployed HTTPS evidence.

---

## 11. What is the strongest security property in your application?
**Answer:**
Integrity is the strongest property in the current artefact. The project is especially strong in submission integrity, grade integrity, and tamper-evident audit accountability. Confidentiality is also materially improved, especially through password hashing, MFA-secret encryption, and encrypted submission storage.

---

## 12. What evidence did you use to justify your claims?
**Answer:**
I used three evidence layers:
- unit tests for crypto/configuration building blocks
- integration tests for real API/security behavior
- manual-security review docs for browser and deployment checks

I also ran the key backend integration tests for auth, MFA, submissions, and grades successfully before finalising the presentation.

---

## 13. What are your main limitations?
**Answer:**
- no full production deployment proof in the repo
- TLS is a deployment design claim unless separately demonstrated, so MITM reduction is documented rather than deployment-proven
- signing uses a stable demo ECC keypair, not full PKI
- no public audit-review UI yet
- no claim of whole-database encryption
- not enterprise-grade key-management maturity

**Good closer:**
> The project is strongest when described as a well-evidenced, bounded cryptography case study rather than a finished enterprise platform.

---

## 14. Best final answer if asked “why is this a strong cryptography project?”
**Answer:**
Because it does not just mention algorithms — it applies the right primitive to the right problem:
- `bcrypt` for passwords
- TOTP for stronger authentication
- `SHA-256` for integrity evidence
- ECC signatures for authorship and tamper-evidence
- `HMAC-SHA-256` for audit integrity
- `AES-GCM` for sensitive data at rest
- secure cookie + CSRF handling for the real web-app environment

That makes it a coherent secure web-application implementation rather than a set of disconnected crypto demos.


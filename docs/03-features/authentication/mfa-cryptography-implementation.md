# MFA Cryptography Implementation Note

This document explains the **implemented** Multi-Factor Authentication (MFA) design in EduSecure from a cryptography-focused perspective.

Its purpose is not only to describe the API behaviour, but to explain **why the chosen security mechanisms are cryptographically appropriate**, what threats they mitigate, and what limitations still remain.

It should be read alongside:
- `docs/03-features/authentication/api-auth-contract.md`
- `docs/02-architecture-crypto/data-model-rationale.md`
- `docs/04-evidence-testing/authentication/implementation-status-and-evidence.md`
- `docs/02-architecture-crypto/crypto-decision-matrix.md`
- `docs/02-architecture-crypto/cia-evaluation.md`
- `docs/01-governance-risk-traceability/risk-register-refined.md`

---

## 1. What problem this MFA implementation is solving

EduSecure already protected passwords with `bcrypt`, which is the correct response to the brief's plaintext-password risk. However, password hashing alone does **not** solve all authentication risk.

The current backend also enforces a basic password-creation rule at registration time:
- minimum length of 8 characters
- at least one uppercase letter
- at least one lowercase letter
- at least one number
- at least one special character

That rule is not a cryptographic control by itself, but it improves the quality of the secret being protected by `bcrypt` and later supplemented by MFA.

### Main residual problem after bcrypt
Even if passwords are stored safely in the database, a user account can still be compromised when:
- the user reuses a password from another breached service
- the password is phished
- the password is guessed or brute-forced elsewhere
- malware or shoulder-surfing exposes the password on the client side

In other words:
- `bcrypt` protects the password **at rest**
- MFA strengthens the login process **at use time**

That distinction matters academically. Password hashing and MFA solve **different parts** of the authentication problem.

### Risk linkage
This MFA implementation primarily strengthens the project against:
- password-only account takeover risk
- stronger downstream protection of JWT-bearing sessions
- reduced trust in a single secret as the only proof of identity

This aligns with the risk logic already documented in `docs/01-governance-risk-traceability/risk-register-refined.md`, especially:
- credential compromise risk
- token/session abuse risk
- secret-exposure risk

---

## 2. Why TOTP was chosen

EduSecure implements **TOTP (Time-Based One-Time Password)** using an authenticator-app model.

### Why TOTP is a strong first choice for this project
TOTP was selected because it is:
- cryptographically meaningful
- realistic for an education-platform case study
- small enough for a study-project artefact
- implementable using standard Java cryptographic primitives
- easier to justify academically than a purely UI-driven factor such as email OTP

### Why not SMS or email OTP first
SMS and email OTP can add operational friction and delivery dependencies, but they are weaker as a cryptography-centred educational artefact because they depend heavily on external transport channels and operational controls rather than a neat, self-contained cryptographic flow.

They also introduce extra infrastructure concerns:
- message delivery services
- mailbox trust assumptions
- telecom interception or SIM-swap risk for SMS

### Why not WebAuthn/passkeys first
WebAuthn would be stronger in several respects, especially phishing resistance, but it would also increase implementation complexity significantly:
- attestation model
- browser/platform authenticators
- credential registration and ceremony handling
- more frontend and platform-specific work

For this project, TOTP gives a **good balance between cryptographic seriousness and achievable scope**.

---

## 3. Cryptographic basis of the implemented TOTP design

The implemented MFA mechanism is based on the standard TOTP idea:

1. server and authenticator app share the same secret seed
2. both sides combine that secret with the current time window
3. both sides compute a short code
4. if the independently computed values match within an allowed time window, the server accepts the second factor

### Practical use with a smartphone authenticator app
This design is intended to work with standard smartphone authenticator applications.

In practice, the user can enroll EduSecure in apps such as:
- Google Authenticator
- Microsoft Authenticator
- Authy
- other apps that support the standard TOTP `otpauth://` format

During MFA setup, the backend returns:
- a Base32 manual-entry key
- a standard `otpauth://totp/...` URI

That means the frontend can either:
- render the `otpauth` URI as a QR code for the user to scan, or
- display the manual-entry key so the user can type it into their phone app

After enrollment, the phone app generates a 6-digit code every 30 seconds, and the user enters the current code into EduSecure to enable MFA or complete login.

In EduSecure, this logic is implemented in:
- `backend/src/main/java/edusecure/edusecure/service/auth/TotpService.java`

### Cryptographic primitive actually used
The implementation uses:
- `HMAC-SHA1`

Important note:
- the security here comes from the **keyed MAC construction**, not from using SHA-1 as a plain general-purpose hash for integrity storage
- this is a standard TOTP-style choice and is different from saying “SHA-1 is good for passwords” or “SHA-1 is good for general modern integrity design”

### What HMAC contributes here
HMAC gives the TOTP system two important properties:
- only a party that knows the secret seed can generate valid codes
- the code changes with time, so the same value is not valid indefinitely

This means the second factor is not just another static password. It is a **time-bound proof derived from a shared cryptographic secret**.

### Current implementation parameters
From the backend implementation:
- algorithm: `HmacSHA1`
- secret length: `20` random bytes
- code length: `6` digits
- time step: `30` seconds
- allowed window: `±1` step

That means the server accepts codes for the current time slice and a narrow neighbouring window to tolerate small device clock drift.

### Why the time window matters
A one-time password must balance two competing goals:
- reject stale codes aggressively
- tolerate realistic clock differences between client device and server

The allowed window improves usability, but it slightly increases the acceptance surface. This is an intentional trade-off between cryptographic strictness and practical authentication reliability.

---

## 4. Randomness and secret generation

The TOTP system is only as strong as the underlying shared secret.

EduSecure generates TOTP secrets using `SecureRandom`.

### Why `SecureRandom` matters
If the shared secret were predictable, the whole MFA system would collapse because an attacker could compute valid future codes.

Cryptographically secure randomness is therefore essential for:
- TOTP seed generation
- AES-GCM nonce generation for secret encryption
- recovery-code generation

This is one of the most important implementation-quality points in a cryptography project: **strong algorithms fail if their secret material is generated badly**.

---

## 5. Why passwords are hashed but TOTP seeds are encrypted

This is the most important conceptual distinction in the MFA implementation.

### Password storage: one-way hashing is correct
Passwords are stored as `bcrypt` hashes because the server does **not** need to recover the original plaintext password.

At login time, the server only needs to ask:
- does the submitted password match the stored verifier?

That is exactly what password hashing is for.

Important supporting note:
- password policy and password hashing are different layers
- the policy improves password quality at creation time
- `bcrypt` protects the stored verifier after creation

Both matter, but only `bcrypt` is the cryptographic storage mechanism.

### TOTP seed storage: one-way hashing is not enough
A TOTP secret is different.

To verify a submitted TOTP code, the server must be able to:
- recover the original secret seed
- recompute the expected code for the current time window
- compare that computed code against what the user supplied

If the TOTP seed were only hashed, the server could not recompute valid codes later.

### Correct design consequence
Therefore the TOTP secret must be:
- recoverable by the server
- but still protected at rest

That is why EduSecure stores the TOTP secret as:
- encrypted ciphertext
- with a separate nonce
- under an application-managed symmetric key

This logic is implemented in:
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java`

### Academic summary of the distinction
- password: **verifier only** -> hash it
- TOTP seed: **future computation input** -> encrypt it

That is a strong report point because it shows correct understanding of the difference between:
- non-recoverable secret verification
- recoverable secret protection

---

## 6. Why AES-GCM is appropriate for TOTP secret protection

EduSecure protects stored MFA secrets using:
- `AES/GCM/NoPadding`

with:
- `12` byte nonce
- `128` bit authentication tag

### Why AES-GCM is a strong choice
AES-GCM is an authenticated-encryption mode. That means it provides both:
- confidentiality of the stored secret
- integrity/authenticity of the ciphertext during decryption

This is important because protecting a stored TOTP seed is not just about hiding it. The application should also detect if the protected secret material has been tampered with.

### Why authenticated encryption is preferable here
If a weaker or misused encryption mode were chosen, an attacker might try to:
- tamper with encrypted data
- trigger malformed decryptions
- exploit unauthenticated ciphertext handling

AES-GCM reduces that risk by binding integrity to the encrypted data.

### Key separation
The implementation deliberately keeps the MFA secret-encryption key separate from other project secrets such as:
- JWT signing secret
- audit HMAC secret
- submission-storage key material

This follows good cryptographic hygiene:
- one key should not be reused for unrelated security functions
- compromise of one secret should not automatically collapse other controls

This separation directly supports the secret-exposure reasoning in `docs/01-governance-risk-traceability/risk-register-refined.md`.

---

## 7. Why recovery codes are hashed

EduSecure generates recovery codes when MFA is enabled.

These codes are intended as backup credentials for cases such as:
- lost authenticator device
- temporary access failure to the authenticator app

### Why recovery codes are not encrypted like TOTP seeds
Recovery codes are not reusable seeds for future computation.

They function more like backup passwords:
- user presents the code once
- server checks whether it matches a stored verifier
- once used, it is invalidated

Because the server does **not** need to recover the original plaintext recovery code later, hashing is the safer design.

### Implemented design
EduSecure stores recovery codes as:
- one-time values
- hashed with the existing password encoder
- consumed on first successful use

This means that even if the recovery-code table were exposed, plaintext backup codes would not be directly revealed.

### Why one record per code is a good model
The implementation stores recovery codes separately, which makes it easy to:
- count remaining codes
- invalidate only the used code
- keep one-time-use semantics clean

This is both a security and data-modelling advantage.

---

## 8. Why JWT issuance is delayed until MFA completes

A crucial security design choice in this implementation is:

**No bearer token is issued to an MFA-enabled user until the second factor succeeds.**

### Why this matters
If the server issued a normal JWT immediately after password verification, then the system would already have granted a reusable authenticated token before the second factor completed.

That would weaken MFA substantially because:
- the protected session would already exist
- the second factor would become advisory rather than decisive
- an attacker with the password could still gain a token before completing MFA

### Implemented design
Instead, EduSecure does the following:
- password step succeeds
- server creates a short-lived `MfaChallenge`
- client receives `MFA_REQUIRED`
- client submits TOTP or recovery code to `/api/auth/mfa/verify`
- only then does the server issue a JWT

This is implemented mainly across:
- `AuthService`
- `MfaService`
- `AuthTokenService`

### Security benefit
This preserves a strict trust boundary:
- password success alone is **not yet full authentication** for MFA-enabled users
- the JWT represents **completed** authentication, not partial progress

That is an important and defensible cryptographic-authentication design decision.

---

## 9. Challenge lifecycle and online guessing resistance

The MFA flow introduces server-side challenge records.

### Why challenge state is needed
The system must remember that:
- the password step already succeeded
- the user is expected to complete factor 2
- the attempt is temporary
- failed guesses should be bounded

### Implemented controls
Current backend properties fix:
- challenge lifetime: `300` seconds
- maximum attempts: `5`

### Why this matters cryptographically
Even though TOTP codes are short, the system should not permit unbounded online guessing against a valid challenge.

Attempt limits and expiry reduce the practical attack window.

This does not make brute force impossible in theory, but it makes online abuse much less feasible in practice.

---

## 10. JWT auth-context claims and their meaning

When full authentication completes, EduSecure issues JWTs with explicit authentication context.

Implemented claims include:
- `mfa`
- `amr`

### Meaning of these claims
- `mfa=true` indicates that MFA was satisfied for that authentication event
- `amr` (Authentication Methods References) shows which factors contributed, for example:
  - `pwd`
  - `pwd, otp`

### Why this is useful
These claims help preserve semantic clarity about how the session was established.

That is useful for:
- future auditing
- report explanation
- possible later step-up authorisation decisions

Important limitation:
- these claims are only as trustworthy as the JWT signing key and the backend logic that created them

---

## 11. Implemented security strengths

This MFA implementation has several strong cryptographic and architectural properties.

### Strength 1: factor separation
The user must prove two different things:
- knowledge of the password
- possession of the TOTP-generating secret or recovery credential

### Strength 2: correct storage semantics
The design correctly distinguishes between:
- hashed passwords
- encrypted recoverable TOTP seeds
- hashed one-time recovery codes

### Strength 3: delayed token issuance
JWT is only issued after full authentication for MFA-enabled users.

### Strength 4: authenticated encryption for secret-at-rest protection
AES-GCM protects both confidentiality and integrity of stored MFA seeds.

### Strength 5: bounded challenge abuse
Short-lived challenge state and attempt limits reduce practical online guessing exposure.

### Strength 6: recovery without plaintext storage
Recovery codes improve availability without requiring plaintext persistence.

---

## 12. Residual risks and honest limitations

A strong cryptography report should not pretend that MFA removes all risk.

### Remaining limitations
1. **TOTP is not fully phishing-resistant**
   - a real-time phishing proxy could still capture password and current TOTP code and replay them quickly

2. **Endpoint compromise still matters**
   - malware on the user device may still capture both factors

3. **Server secret compromise is serious**
   - if the MFA secret-encryption key is exposed, encrypted TOTP seeds may become recoverable

4. **Clock assumptions remain part of the system**
   - TOTP depends on reasonably aligned time between server and authenticator device

5. **Recovery codes become sensitive backup credentials**
   - if the user stores them insecurely, they weaken the second-factor model

6. **This is still not hardware-backed MFA**
   - TOTP is stronger than password-only authentication, but generally weaker than phishing-resistant authenticators such as WebAuthn/passkeys

### Why this limitation discussion is important
Cryptography in a real system is never just about selecting a strong algorithm name. Security depends on:
- correct storage semantics
- key handling
- lifecycle design
- client behaviour
- residual operational risk

That is exactly why the implementation should be described as **stronger authentication**, not “perfect security”.

---

## 13. How this supports the cryptography focus of the whole project

Although MFA is an authentication feature, it is strongly relevant to the cryptography theme of EduSecure because it demonstrates correct use of several cryptographic concepts in one coherent workflow.

### Concepts demonstrated by this implementation
- `bcrypt` for password hashing
- `HMAC` as the basis of TOTP verification
- `AES-GCM` for confidential and integrity-protected storage of recoverable secrets
- secure randomness for seed and recovery generation
- signed JWTs carrying post-authentication security context

### Why this is academically valuable
This implementation is stronger than a generic “add MFA screen” feature because it shows understanding of:
- the difference between hashing and encryption
- the difference between confidentiality and authenticity/integrity goals
- why time-based OTP is a cryptographic protocol rather than just a UI step
- why token issuance timing affects the security meaning of authentication

In report terms, this MFA implementation helps demonstrate that EduSecure does not use cryptography only for stored passwords or future file-security features. It also applies cryptographic reasoning directly to **identity assurance**, which is central to any secure education platform.

---

## 14. Implementation evidence in code

The main implementation evidence is in:

### Core auth orchestration
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthTokenService.java`
- `backend/src/main/java/edusecure/edusecure/controller/AuthController.java`

### MFA cryptographic logic
- `backend/src/main/java/edusecure/edusecure/service/auth/TotpService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`

### MFA persistence model
- `backend/src/main/java/edusecure/edusecure/entity/User.java`
- `backend/src/main/java/edusecure/edusecure/entity/MfaChallenge.java`
- `backend/src/main/java/edusecure/edusecure/entity/MfaRecoveryCode.java`

### Integration-test evidence
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`

---

## 15. Final conclusion

EduSecure's MFA implementation is cryptographically defensible because it does not treat MFA as a superficial add-on.

Instead, it applies correct cryptographic reasoning across the whole flow:
- passwords are hashed because they only need verification
- TOTP seeds are encrypted because they must later be used again
- recovery codes are hashed because they act as one-time backup passwords
- JWTs are withheld until the second factor succeeds
- time-bounded challenge state constrains online abuse

This makes the implemented MFA feature a strong contribution to the overall cryptography narrative of the project. It shows that authentication, storage protection, key separation, randomness, and token semantics have all been designed with explicit cryptographic intent rather than added as disconnected features.


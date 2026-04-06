# EduSecure PowerPoint-Ready Presentation Script (13 Slides)

This is the **presentation-build version** of the EduSecure deck for a **15–20 minute talk**.

Use this file when creating the final slides in PowerPoint.

## How to use this file
- Put **only 3–4 bullets** on each slide.
- Use **one diagram** and **one screenshot/demo item** per slide.
- Keep the slide text short; use the speaker script for the deeper explanation.
- Speak for roughly **45–75 seconds per slide**.
- Keep wording evidence-safe: describe TLS as the **intended deployment transport control**, not as repository-proven HTTPS enforcement unless you have deployment proof.

---

## Slide 1 — EduSecure as a cryptography case study

**Final slide title**  
**EduSecure: Building a Cryptographically Stronger and More Secure Education Platform**

**Slide bullets**
- Cryptography case-study artefact for `500IT`
- Focused on authentication, submissions, grades, and accountability
- Spring Boot backend with a Vue frontend MVP
- Built around implemented, testable security controls

**Diagram to include**
- `docs/pack-09/uml/use-case-security-focused.puml`

**Screenshot/demo item to include**
- Login screen from `frontend/src/pages/Login/index.vue`

**Speaker script**
> My project is not just a normal web application. It is a cryptography-focused case study built around the EduSecure assignment scenario. The goal was to redesign an insecure education platform so that authentication, submissions, grades, and sensitive actions are protected with appropriate cryptographic controls. The main artefact is the Spring Boot backend, because that is where the trust boundaries, cryptographic logic, and access control are enforced. The Vue frontend is there to demonstrate the implemented flows in a realistic browser context.

**Timing target**
- 50–60 seconds

---

## Slide 2 — The security problems from the brief

**Final slide title**  
**What Was Wrong in the Original EduSecure Scenario?**

**Slide bullets**
- Passwords stored in plaintext
- No HTTPS, exposing traffic to interception and MITM
- Student submissions could be tampered with
- Grade changes were not trustworthy or auditable

**Diagram to include**
- `docs/pack-02/uml/deployment-insecure.puml`

**Screenshot/demo item to include**
- A simple problem-to-control summary table you create in PowerPoint

**Speaker script**
> The assignment brief starts from a deliberately insecure baseline. Passwords were stored in plaintext, transport was not protected, submissions could be modified before lecturer review, grades could be altered, and sensitive actions were not verifiable. In practical terms, the lack of HTTPS means the platform was exposed to eavesdropping and man-in-the-middle interception, especially on public or campus Wi-Fi. That matters because these are not all the same problem. Password storage, secure transport, tamper-evidence, authorship, and auditability all require different security controls.

**Timing target**
- 60–75 seconds

---

## Slide 3 — Secure architecture overview

**Final slide title**  
**How the Secure EduSecure Architecture Is Organised**

**Slide bullets**
- Backend-first trust model in Spring Boot
- Cookie-based browser authentication with RBAC
- Dedicated services for auth, MFA, submissions, grades, and audit
- Frontend demonstrates flows but does not own security decisions

**Diagram to include**
- `docs/pack-09/uml/dfd-context-current-state.puml`

**Screenshot/demo item to include**
- Authenticated workspace after login, e.g. `frontend/src/pages/AssignmentList/index.vue` or `frontend/src/pages/SpaceList/index.vue`

**Speaker script**
> I deliberately kept the system backend-first from a security point of view. The browser is a client, but it is not the trust anchor. The backend handles authentication, authorization, cryptography, audit recording, and persistence. The frontend makes credentialed requests and renders the implemented flows, but it does not decide whether a user is authorized or whether a sensitive action is valid. This architecture is important academically because it makes the security story more defensible: the strongest security controls are all enforced in code that is tested and centrally managed.

**Timing target**
- 60 seconds

---

## Slide 4 — Current-state trust boundaries and data movement

**Final slide title**  
**Where Sensitive Data Moves and Where Controls Apply**

**Slide bullets**
- Distinct flows for auth, MFA, submissions, grades, and audit
- Browser session uses `HttpOnly` cookie transport
- Submission plaintext, metadata, ciphertext, grades, and audit records are separated
- Security controls are applied at different stages for different assets

**Diagram to include**
- `docs/pack-09/uml/dfd-level-1-current-state.puml`

**Screenshot/demo item to include**
- Browser devtools or network/cookie view showing browser-session behavior

**Speaker script**
> This slide gives the high-level data movement story. Auth data, MFA data, submission content, grade records, and audit logs do not all follow one generic path. The current-state DFD is useful because it shows where sensitive data enters the system, where it is transformed by cryptographic controls, and where it is stored. For example, submission plaintext becomes both integrity evidence and encrypted content, while grades and audit records follow their own integrity-sensitive paths. This is one of the clearest ways to show that EduSecure applies different security mechanisms to different assets rather than pretending one mechanism solves everything.

**Timing target**
- 60–75 seconds

---

## Slide 5 — Authentication hardening

**Final slide title**  
**How I Secured Authentication and Sessions**

**Slide bullets**
- Passwords protected with `bcrypt`
- Optional TOTP MFA for stronger login assurance
- JWT-based session issued only after full authentication
- Browser session stored in an `HttpOnly` cookie

**Diagram to include**
- `docs/pack-02/uml/sequence-login-secure.puml`

**Screenshot/demo item to include**
- MFA challenge screen from `frontend/src/pages/MfaChallenge/index.vue`

**Speaker script**
> Authentication was one of the most important areas to fix because the brief explicitly started from plaintext password storage. I replaced that with `bcrypt`, which is the correct control for stored password verifiers. I also implemented optional TOTP MFA, which strengthens login assurance beyond password-only authentication. A particularly strong design point is that MFA-enabled users do not receive a fully authenticated session after the password step alone. They first get `MFA_REQUIRED`, and only after successful factor-two verification does the backend issue the JWT-backed authenticated session. That session is transported in an `HttpOnly` cookie rather than being stored in frontend JavaScript-accessible storage.

**Timing target**
- 70–80 seconds

---

## Slide 6 — MFA as applied cryptography

**Final slide title**  
**Why the MFA Design Is Cryptographically Strong**

**Slide bullets**
- TOTP uses an HMAC-based one-time-password model
- TOTP secrets are generated with `SecureRandom`
- Stored MFA secrets are encrypted with `AES-GCM`
- Recovery codes are hashed and one-time use

**Diagram to include**
- `docs/pack-02/uml/sequence-login-secure.puml` as contextual support for the MFA branch

**Screenshot/demo item to include**
- MFA setup/status screen from `frontend/src/pages/AccountSecurity/index.vue`

**Speaker script**
> This slide is where I show cryptographic reasoning rather than just a feature list. Passwords are hashed because the server only needs to verify them. TOTP secrets are different: the server must recover them later to recompute valid codes, so they must be encrypted rather than hashed. That is why EduSecure protects stored MFA secrets using AES-GCM. Recovery codes are closer to backup passwords, so they are hashed and consumed once. This distinction is important because it shows correct use of different primitives for different security goals. It also shows that MFA here is not just a user-interface add-on — it is a properly designed cryptographic workflow.

**Timing target**
- 75 seconds

---

## Slide 7 — Browser security and the MITM boundary

**Final slide title**  
**Browser Session Security and the MITM Boundary**

**Slide bullets**
- Frontend uses `withCredentials: true`
- JWT is not stored in `localStorage`
- CSRF uses `XSRF-TOKEN` plus `X-XSRF-TOKEN`
- TLS 1.3 is the intended control against eavesdropping and MITM in transit

**Diagram to include**
- `docs/pack-02/uml/deployment-secure.puml`

**Screenshot/demo item to include**
- Browser devtools screenshot showing `EDUSECURE_AUTH` and `XSRF-TOKEN` cookies

**Speaker script**
> A secure web application also has to handle browser realities properly. In EduSecure, the frontend uses credentialed requests, but it does not store the auth JWT in `localStorage`. Instead, the session token is transported in the backend-issued `HttpOnly` auth cookie. Unsafe requests also require the CSRF token pair: a readable `XSRF-TOKEN` cookie and the mirrored `X-XSRF-TOKEN` header. These controls help reduce browser-side session abuse. However, for eavesdropping and MITM risk in transit, the main control is still TLS 1.3 at deployment. That is an important boundary to state honestly.

**Timing target**
- 65–75 seconds

---

## Slide 8 — Submission integrity and authorship

**Final slide title**  
**How I Protected Submission Integrity and Authorship**

**Slide bullets**
- Submission bytes are hashed with `SHA-256`
- Digest is signed using ECC with `SHA256withECDSA`
- Verification status is stored with the submission
- Lecturers can review digest, signature, and verification evidence

**Diagram to include**
- `docs/pack-04/uml/sequence-submission-secure-pack04.puml`

**Screenshot/demo item to include**
- Submission detail screen from `frontend/src/pages/SubmissionDetail/index.vue` showing `hashDigest`, `digitalSignature`, and `verificationStatus`

**Speaker script**
> This is one of the strongest cryptographic workflows in the whole application. When a student uploads a submission, the backend computes a `SHA-256` digest over the submitted bytes. That digest is then signed using the project’s configured demo ECC keypair and immediately verified. The system stores the digest, the digital signature, the signature algorithm, the verification status, and the verification message. This gives lecturers a visible integrity and authorship evidence trail rather than just a plain uploaded file. It is important to say this carefully: it is a strong study-project signing model, but it is not a full user-held PKI deployment.

**Timing target**
- 75 seconds

---

## Slide 9 — Submission confidentiality at rest

**Final slide title**  
**How I Protected Submission Confidentiality at Rest**

**Slide bullets**
- Submission content is encrypted with `AES-GCM`
- Per-submission content key is wrapped with `AESWrap`
- Metadata and plaintext retrieval are separated
- Plaintext retrieval is a controlled and audited action

**Diagram to include**
- `docs/pack-04/uml/sequence-lecturer-submission-decryption.puml`

**Optional secondary diagram if you want the broader upload + retrieval story**
- `docs/pack-04/uml/sequence-submission-aes-at-rest-retrieval-pack04.puml`

**Screenshot/demo item to include**
- Submission content retrieval/download flow from `frontend/src/pages/SubmissionDetail/index.vue`

**Speaker script**
> Integrity is only part of the submission story. I also strengthened confidentiality by encrypting stored submission content with AES-GCM. The project uses a per-submission content-encryption key, and that key is protected separately using key wrapping. Another important design decision is that metadata and plaintext retrieval are not treated as the same thing. The normal submission metadata endpoint does not expose plaintext, ciphertext, wrapped keys, nonce values, or internal storage references. Plaintext retrieval happens through a separate endpoint, after authorization checks, and successful access is audited. That gives the project a much stronger confidentiality boundary than a simple “upload and read everything back” model.

**Timing target**
- 75–85 seconds

---

## Slide 10 — Grade integrity

**Final slide title**  
**How I Protected Grade Integrity**

**Slide bullets**
- Only verified submissions can be graded
- Grade changes are restricted to assignment-owning lecturer or `ADMIN`
- Students can read only their own grades
- Grade handling remains aligned with submission verification

**Diagram to include**
- `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`

**Screenshot/demo item to include**
- Grade panel on `frontend/src/pages/SubmissionDetail/index.vue`

**Speaker script**
> Grades are treated as integrity-sensitive academic outcomes, not just as ordinary editable data. The system enforces that only verified submissions can be graded, which keeps the grade workflow aligned with the earlier submission-integrity workflow. Grade create and update actions are limited to the assignment-owning lecturer or an admin, while student grade access is restricted to ownership-limited views. This is important because it means grades are not only access-controlled, but also linked logically to prior integrity checks on the submitted work.

**Timing target**
- 60–70 seconds

---

## Slide 11 — Audit integrity and accountability

**Final slide title**  
**How I Made Sensitive Actions Tamper-Evident**

**Slide bullets**
- Sensitive actions create append-oriented audit records
- Audit integrity uses `HMAC-SHA-256`
- Records carry previous integrity value for simple chaining
- Submission and grade events become accountable and reviewable

**Diagram to include**
- `docs/pack-04/uml/sequence-audit-integrity-secure.puml`

**Screenshot/demo item to include**
- Audit-event summary table or evidence screenshot showing events like `SUBMISSION_CREATED`, `SUBMISSION_VERIFIED`, `SUBMISSION_CONTENT_ACCESSED`, `GRADE_CREATED`, and `GRADE_UPDATED`

**Speaker script**
> The brief also required sensitive actions to become verifiable. I addressed that with an append-oriented audit model protected by HMAC-backed integrity values. In simple terms, the system does not just log events — it computes an integrity value over each record and links it to the previous one. That means submission creation, verification, content retrieval, and grade actions become more tamper-evident within the project’s system model. I am careful not to describe this as a full enterprise SIEM platform, but it is a strong improvement over having sensitive actions occur without trustworthy evidence.

**Timing target**
- 70 seconds

---

## Slide 12 — Evidence that the implementation works

**Final slide title**  
**Why My Security Claims Are Defensible**

**Slide bullets**
- Integration tests prove cross-layer security behavior
- Unit tests cover core crypto and configuration primitives
- Manual/browser review supports CSRF, CORS, and deployment checks
- Key backend security tests passed before this presentation

**Diagram to include**
- None on this slide — use evidence instead

**Screenshot/demo item to include**
- Terminal screenshot of the successful integration test run

**Speaker script**
> I do not want this presentation to sound like design-only security. The repository includes strong Spring Boot integration tests for authentication, MFA, submission protection, grade authorization, and PostgreSQL/Liquibase verification. It also includes unit tests for important building blocks like TOTP behavior, AES-GCM encryption, key wrapping, and cookie-configuration safety. For browser and deployment questions that backend automation cannot fully prove, there is also a manual review pack. Before finalising this presentation, I ran the key backend security integration tests successfully.

**Timing target**
- 60–70 seconds

---

## Slide 13 — Honest limitations and conclusion

**Final slide title**  
**What Is Strong Already, and What Remains Bounded**

**Slide bullets**
- Strong evidence for `bcrypt`, TOTP, `SHA-256`, ECC signatures, `HMAC-SHA-256`, and `AES-GCM`
- TLS is the intended deployment transport control, not deployment-proven here
- Signing remains a stable demo ECC keypair, not full PKI
- EduSecure is strongest as a bounded, evidence-backed cryptography case study

**Diagram to include**
- `docs/pack-02/uml/deployment-secure.puml`

**Screenshot/demo item to include**
- A clean two-column “Implemented now / bounded claims” summary slide you create in PowerPoint

**Speaker script**
> To conclude, EduSecure is strongest when described as a well-evidenced cryptography case study rather than as a finished enterprise platform. The implemented artefact clearly demonstrates multiple cryptographic techniques: password hashing, MFA, digesting, digital signatures, HMAC-backed audit integrity, and AES-GCM protection at rest. At the same time, I should be honest about boundaries. TLS is documented as the intended transport control, not directly proven as deployed HTTPS in this repository. The submission-signature workflow uses a stable demo keypair, not a full PKI. So the right final claim is that EduSecure is a strong, layered, and evidence-backed secure web application within study-project scope.

**Timing target**
- 75 seconds

---

## Optional build tips for PowerPoint
- Keep diagrams large and readable; do not overcrowd slides with too much code or text.
- Use bold only for algorithm names and key security terms.
- Put full explanations in speaker notes, not on the slide body.
- If time gets tight, merge Slides 3 and 4, or merge Slides 10 and 11.
- If a screenshot is too busy, crop to only the security-relevant part.

---

## Best fallback 10-slide version if you need to shorten live
Use these slides only:
1. Slide 1 — case-study framing
2. Slide 2 — insecure baseline
3. Slide 3 — secure architecture overview
4. Slide 5 — authentication hardening
5. Slide 6 — MFA as applied cryptography
6. Slide 8 — submission integrity and authorship
7. Slide 9 — submission confidentiality at rest
8. Slide 10 — grade integrity
9. Slide 12 — evidence
10. Slide 13 — limitations and conclusion

---

## Safe closing sentence
> EduSecure does not rely on one security mechanism; it applies the right cryptographic control to the right problem, and that is what makes it a stronger and more defensible secure web application.


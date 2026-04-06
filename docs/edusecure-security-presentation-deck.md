# EduSecure Security Presentation Deck

## Presentation goal
Explain how `EduSecure` was redesigned and implemented as a **cryptographically stronger and more secure web application**, using only claims that are supported by the repository.

**Short version:** 7–9 slides, 6–8 minutes  
**Detailed version:** 12–13 slides, 15–20 minutes  
**Primary theme:** integrity first, then confidentiality, then accountable access control

## UML selection rule for the deck
- Use the UML/DFD files in `docs/pack-02/uml`, `docs/pack-04/uml`, `docs/pack-05/uml`, and `docs/pack-09/uml` as **design-level support**, not as exact code-generated mirrors.
- Prefer one primary diagram per slide so the presentation stays readable.
- Do **not** use `docs/pack-05/uml/sequence-aes-secure-transmission-demo.puml` as active evidence; it is marked as historical only.

---

## Slide 1 — Title / one-line message
**Title:**  
**EduSecure: Building a Cryptographically Stronger and More Secure Education Platform**

**On-slide bullets:**
- Case-study artefact for the `500IT` cryptography assignment
- Focused on securing authentication, submissions, grades, and sensitive actions
- Implemented with a Spring Boot backend and a small Vue frontend MVP

**Speaker notes:**
- My project responds directly to the EduSecure assignment scenario, where passwords were insecure, communication was unprotected, submissions could be tampered with, grades could be altered, and sensitive actions were not verifiable.
- Instead of building isolated crypto demos, I implemented several cryptographic controls inside one realistic web application workflow.
- The backend is the main artefact, and the frontend is mainly there to demonstrate the implemented flows.

**Suggested visual:**
- App home/login screen or system architecture screenshot
- Primary diagram from docs: `docs/pack-09/uml/use-case-security-focused.puml`
- Fallback overview diagram: `docs/pack-02/uml/use-case.puml`

**Evidence anchors:**
- `docs/assignment_brief.md`
- `frontend/README.md`
- `docs/pack-09/final-implementation-evidence-map.md`

---

## Slide 2 — What the assignment brief required me to fix
**Title:**  
**Security Problems in the EduSecure Brief**

**On-slide bullets:**
- Plaintext credential storage
- No HTTPS / insecure transport
- MITM exposure on public Wi‑Fi and untrusted networks
- Student submissions could be tampered with
- Grade changes lacked integrity and traceability
- Sensitive actions were not properly logged or verifiable

**Speaker notes:**
- The brief gave five core security failures: plaintext passwords, lack of transport security, tamperable submissions, altered grades, and no trustworthy logging.
- In practical terms, the lack of HTTPS means EduSecure was exposed to eavesdropping and man-in-the-middle interception, especially on public or campus Wi‑Fi.
- My implementation maps those risks to specific controls rather than using one generic “security layer.”
- That is important because different risks require different cryptographic primitives: hashing, encryption, signatures, MACs, and secure session handling all solve different problems.

**Suggested visual:**
- Simple 2-column table: “brief problem” → “implemented control”
- Primary diagram from docs: `docs/pack-02/uml/deployment-insecure.puml`
- Optional secondary diagram: `docs/pack-02/uml/sequence-login-insecure.puml`

**Evidence anchors:**
- `docs/assignment_brief.md:63-89`
- `docs/pack-02/crypto-decision-matrix.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`

---

## Slide 3 — Security architecture in one view
**Title:**  
**Implemented Secure Architecture**

**On-slide bullets:**
- **Backend-first security design** in Spring Boot
- **Cookie-based browser session** with `HttpOnly` auth cookie
- **RBAC** for `ADMIN`, `LECTURER`, and `STUDENT`
- Separate security services for auth, MFA, submissions, crypto, grades, and audit
- Vue frontend used as an evidence-oriented client, not as the trust anchor

**Speaker notes:**
- I deliberately kept the most security-sensitive logic on the backend because it is easier to secure, test, and justify academically.
- The frontend does not own the trust model. The browser sends credentialed requests, while the backend enforces authentication, authorization, cryptography, and audit logging.
- This also makes the claims easier to defend, because the core security behavior lives in code that is strongly tested.

**Suggested visual:**
- Simple architecture diagram: Browser → Vue frontend → Spring Boot API → PostgreSQL / file storage
- Primary diagram from docs: `docs/pack-09/uml/dfd-context-current-state.puml`
- Secondary option: `docs/pack-02/uml/deployment-secure.puml`

**Evidence anchors:**
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- `frontend/src/services/http.ts`
- `frontend/src/stores/auth.ts`
- `docs/pack-09/final-report-draft-sections-6-to-9.md`

---

## Slide 4 — Authentication hardening
**Title:**  
**How I Secured Authentication and Sessions**

**On-slide bullets:**
- Passwords protected with **`bcrypt`** instead of plaintext
- Optional **TOTP MFA** with authenticator-app setup and recovery codes
- JWT-based session issued **only after full authentication**
- Browser session stored in an **`HttpOnly` cookie**, not `localStorage`
- **CSRF protection** added for unsafe browser requests

**Speaker notes:**
- Passwords are hashed with `BCryptPasswordEncoder`, which fixes the plaintext-password problem from the brief.
- MFA is implemented using TOTP, so users can enrol with a normal authenticator app. Recovery codes are generated as one-time backups and stored hashed.
- One of the strongest design choices is that MFA-enabled users do not get a full authenticated session after the password step alone. They first receive `MFA_REQUIRED`, then only get the JWT-backed session after successful MFA verification.
- The JWT is transported in an `HttpOnly` cookie, which means JavaScript cannot directly read it.
- For browser safety, unsafe requests also require the CSRF token pair: `XSRF-TOKEN` cookie plus `X-XSRF-TOKEN` header.

**Suggested visual:**
- Login → MFA challenge → authenticated cookie flow
- Optional screenshot of MFA setup or challenge screen
- Primary diagram from docs: `docs/pack-02/uml/sequence-login-secure.puml`
- Presenter note: treat this as the secure-login baseline, then explain that TOTP MFA is a later implementation refinement on top of that flow

**Evidence anchors:**
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java:45-63`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java:71-89`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java:119-183`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthTokenService.java:22-49`
- `backend/src/main/java/edusecure/edusecure/security/AuthCookieService.java:27-69`
- `frontend/src/services/http.ts:18-98`
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`

**Key talking line:**
> `bcrypt` protects the password at rest, while TOTP MFA strengthens identity at login time.

---

## Slide 5 — Why the MFA implementation is cryptographically strong
**Title:**  
**MFA: Correct Use of Hashing vs Encryption**

**On-slide bullets:**
- TOTP uses **`HMAC-SHA1`** in the standard time-based OTP model
- TOTP secrets are generated with **`SecureRandom`**
- Stored MFA secrets are protected with **`AES/GCM/NoPadding`**
- Recovery codes are **hashed**, not encrypted
- Separate keys are used for different security roles

**Speaker notes:**
- This slide is where you show cryptographic understanding, not just feature implementation.
- Passwords are hashed because the server only needs to verify them.
- TOTP secrets are different: the server must recover them later to recompute valid codes, so they must be encrypted rather than hashed.
- I used AES-GCM for MFA secret storage because it gives both confidentiality and integrity for the protected secret material.
- Recovery codes behave like backup passwords, so they are hashed and consumed once.
- I also kept secret roles separated: JWT signing, MFA secret encryption, audit HMAC, and submission storage all use different secrets.

**Suggested visual:**
- Tiny comparison diagram: password → hash / TOTP seed → encrypt / recovery code → hash
- Best fit from docs: no single UML is a perfect match for this crypto-semantics slide
- If you want one supporting diagram from docs, reuse `docs/pack-02/uml/sequence-login-secure.puml` only as context for the MFA flow

**Evidence anchors:**
- `docs/pack-03/mfa-cryptography-implementation.md`
- `backend/src/main/java/edusecure/edusecure/service/auth/TotpService.java:15-117`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java:17-69`
- `backend/src/main/resources/application.properties:31-39`

**Key talking line:**
> One of the strongest parts of the project is that it distinguishes correctly between secrets that should be hashed and secrets that must remain recoverable but protected.

---

## Slide 6 — Submission security: integrity, authorship, and confidentiality
**Title:**  
**How I Secured Assignment Submissions**

**On-slide bullets:**
- Submission bytes are hashed with **`SHA-256`**
- Digest is signed using **ECC + `SHA256withECDSA`**
- Verification status is stored with the submission
- File content is encrypted at rest with **`AES-GCM`**
- Per-submission key is wrapped using **`AESWrap`**
- Metadata and plaintext retrieval are separated into different endpoints

**Speaker notes:**
- This is the strongest end-to-end cryptographic workflow in the application.
- When a student uploads a submission, the backend computes a `SHA-256` digest of the plaintext bytes.
- That digest is signed using the project’s configured demo ECC keypair and immediately verified.
- I store the digest, signature, signature algorithm, verification status, and verification message so the lecturer can review the integrity evidence.
- Then I separately protect confidentiality at rest by encrypting the actual submission content with AES-GCM.
- The content-encryption key is wrapped and versioned before persistence, which is more realistic than just storing plaintext or one shared content key everywhere.
- I also split metadata and content access. The normal metadata endpoint does not expose plaintext, ciphertext, wrapped keys, nonce values, or internal storage references. Plaintext retrieval happens through a separate controlled endpoint.

**Suggested visual:**
- Pipeline diagram: upload → hash → sign → verify → encrypt → wrap key → store ciphertext
- Primary diagram from docs: `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- Secondary diagram from docs: `docs/pack-04/uml/sequence-submission-aes-at-rest-retrieval-pack04.puml`
- Optional supporting structure diagram: `docs/pack-04/uml/class-diagram-submission-addendum.puml`

**Evidence anchors:**
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java:63-132`
- `backend/src/main/java/edusecure/edusecure/service/crypto/AesRsaCryptoService.java:45-87`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java:25-55`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionService.java:19-47`
- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java:120-194`

**Key talking line:**
> Integrity and authorship are shown through digest plus signature, while confidentiality is protected separately through AES-GCM at-rest encryption.

---

## Slide 7 — Grade integrity and audit accountability
**Title:**  
**Protecting Grades and Sensitive Actions**

**On-slide bullets:**
- Only **verified submissions** can be graded
- Grade create/update allowed only for the owning lecturer or `ADMIN`
- Students can read **only their own grades**
- Sensitive actions create append-oriented audit records
- Audit records use **`HMAC-SHA-256`** integrity values with chaining support

**Speaker notes:**
- The grade flow is designed to protect academic integrity, not just data storage.
- The system refuses to grade a submission unless its verification status is `VERIFIED`.
- This connects the submission-authorship workflow to the grade-integrity workflow.
- Sensitive actions such as grade creation, grade update, submission creation, verification, and content access all generate audit entries.
- The audit trail is not just plain logging. It uses HMAC-backed integrity values, and each record carries the previous integrity value to provide simple chaining.
- That makes tampering with the audit history harder to hide within the system model.

**Suggested visual:**
- Table: action / who can do it / integrity protection
- Primary diagram from docs: `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- Secondary diagram from docs: `docs/pack-04/uml/sequence-audit-integrity-secure.puml`

**Evidence anchors:**
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java:38-90`
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java:165-195`
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java:21-43`
- `backend/src/main/java/edusecure/edusecure/service/crypto/AesRsaCryptoService.java:77-87`
- `docs/pack-07/grade-phase-status-and-evidence.md`
- `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java:81-129`

**Key talking line:**
> I did not treat grades as ordinary editable data; I tied them to verified submissions, role checks, and tamper-evident audit evidence.

---

## Slide 8 — Browser security and secure session handling
**Title:**  
**Making the Web App Safer in the Browser**

**On-slide bullets:**
- `withCredentials: true` used by the frontend
- JWT not stored in `localStorage`
- `HttpOnly` auth cookie protects the session token from direct JS access
- CSRF bootstrap endpoint provides `XSRF-TOKEN`
- Production cookie settings are validated at startup

**Speaker notes:**
- A secure web app is not only about backend cryptography; browser session handling also matters.
- The frontend uses credentialed requests, but it does not manage the auth JWT in `localStorage`.
- The CSRF flow is explicit: the frontend first bootstraps the CSRF token, then mirrors it into the `X-XSRF-TOKEN` header for unsafe methods.
- These controls reduce browser-side session abuse, but transport-layer interception and MITM risk are addressed separately through the intended TLS 1.3 deployment design.
- On the backend, configuration validation rejects unsafe production combinations such as `SameSite=None` with `Secure=false`.
- This is important for showing that the project is not just cryptographically interesting, but also web-security aware.

**Suggested visual:**
- Browser devtools screenshot showing `EDUSECURE_AUTH` and `XSRF-TOKEN` cookies
- Best diagram from docs: `docs/pack-09/uml/dfd-level-1-current-state.puml`
- Secondary option: `docs/pack-02/uml/deployment-secure.puml`

**Evidence anchors:**
- `frontend/src/services/http.ts:18-98`
- `frontend/src/stores/auth.ts:37-58`
- `frontend/README.md:37-63`
- `docs/pack-03/implementation-status-and-evidence.md:123-129`
- `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java:235-272`

---

## Slide 9 — Evidence that the controls actually work
**Title:**  
**Why These Claims Are Defensible**

**On-slide bullets:**
- Strong **Spring Boot integration-test** coverage across security boundaries
- Unit tests for crypto and configuration primitives
- Manual/browser review pack for CSRF, CORS, cookie, and deployment checks
- Key security test classes ran successfully before this presentation

**Speaker notes:**
- I have not based this presentation only on design notes.
- The repository includes integration tests for authentication, MFA, submissions, grades, spaces, and PostgreSQL/Liquibase verification.
- It also includes targeted unit tests for the TOTP algorithm, ECC signing-key loading, AES-GCM encryption/decryption, key wrapping, and cookie configuration validation.
- For browser behavior that backend automation cannot fully prove, the repo also contains manual-security review guidance.
- Before finalising this deck, I ran the key integration tests for auth, MFA, submissions, and grades, and they all passed.

**Suggested visual:**
- Terminal screenshot of successful test run
- Best fit from docs: no UML is strongly suited to a testing/evidence slide
- Recommended approach: use test output as the main visual and keep UML off this slide

**Evidence anchors:**
- `docs/pack-09/integration-test-coverage-summary.md`
- `docs/pack-09/unit-test-coverage-summary.md`
- `docs/pack-09/manual-test-coverage-summary.md`
- Verified run: key backend integration tests passed on `2026-04-06`

**Verified command used:**
```powershell
Set-Location "C:\Users\skey\IdeaProjects\EduSecure\backend"
.\gradlew.bat test --tests edusecure.edusecure.AuthControllerIntegrationTests --tests edusecure.edusecure.MfaAuthIntegrationTests --tests edusecure.edusecure.SubmissionFlowIntegrationTests --tests edusecure.edusecure.GradeFlowIntegrationTests
```

---

## Slide 10 — Honest limitations and next steps
**Title:**  
**What Is Strong Already, and What Remains Bounded**

**On-slide bullets:**
- Strong implemented controls: `bcrypt`, TOTP MFA, `SHA-256`, ECC signatures, `HMAC-SHA-256`, `AES-GCM`
- TLS 1.3 is the intended control for reducing eavesdropping and MITM risk in transit
- Current signing model is a **stable demo ECC keypair**, not full user-held PKI
- No full public audit-review UI yet
- Not claiming whole-database encryption or enterprise production maturity

**Speaker notes:**
- A strong presentation should be confident but honest.
- The project clearly exceeds the brief’s minimum requirement of three cryptographic techniques.
- However, I should not overclaim. TLS is part of the secure deployment design narrative unless I show direct deployment proof, so I present it as the intended transport control against eavesdropping and MITM rather than as repository-proven HTTPS enforcement. The signing model is a bounded study-project simulation, not enterprise PKI. MFA strengthens authentication, but it is not phishing-resistant like WebAuthn.
- These honest boundaries actually make the project more defensible.

**Suggested visual:**
- Two-column slide: “implemented now” vs “future/protected claims boundary” 
- Best fit from docs: no dedicated limitations UML is required
- If you want one supporting diagram, reuse `docs/pack-02/uml/deployment-secure.puml` when discussing TLS as the intended transport control boundary

**Evidence anchors:**
- `docs/pack-09/report-claims-audit-note.md`
- `docs/pack-09/final-cryptography-claims-matrix.md`
- `docs/pack-09/high-mark-report-blueprint.md`

**Closing line:**
> EduSecure is strongest where the brief needed it most: secure authentication, submission integrity, confidential storage, grade accountability, and evidence-backed security controls.

---

## Optional final slide — 30-second conclusion
**Title:**  
**Conclusion**

**On-slide bullets:**
- Replaced weak security with layered cryptographic controls
- Prioritised academic integrity, confidentiality, and accountability
- Implemented and tested a realistic secure web-application case study

**Speaker notes:**
- In summary, I transformed EduSecure from an insecure academic platform into a layered security case study.
- The project now demonstrates multiple cryptographic control types working together: password hashing, MFA, signatures, authenticated encryption, HMAC-backed auditing, and secure browser session handling.
- The result is not “perfect security,” but it is a far stronger and better-justified web application than the insecure baseline from the brief.

**Suggested visual:**
- Best fit from docs: `docs/pack-09/uml/dfd-context-current-state.puml` if you want one final summary diagram
- Otherwise, do not force a UML on the conclusion slide

---

## Best screenshot/demo picks for the live presentation
1. Login success or MFA challenge screen
2. MFA setup screen showing QR/manual key flow
3. Submission response showing `hashDigest`, `digitalSignature`, and `verificationStatus`
4. Submission content retrieval or lecturer review screen
5. Grade create/update screen or response
6. Terminal output showing successful test run

## Best slide-to-diagram mapping summary
- Slide 1 → `docs/pack-09/uml/use-case-security-focused.puml`
- Slide 2 → `docs/pack-02/uml/deployment-insecure.puml`
- Slide 3 → `docs/pack-09/uml/dfd-context-current-state.puml`
- Slide 4 → `docs/pack-02/uml/sequence-login-secure.puml`
- Slide 5 → no strong dedicated UML; keep this as a crypto-explanation slide
- Slide 6 → `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- Slide 7 → `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- Slide 8 → `docs/pack-09/uml/dfd-level-1-current-state.puml`
- Slide 9 → no UML; use test evidence screenshot instead
- Slide 10 → no required UML; optionally reuse `docs/pack-02/uml/deployment-secure.puml`

## Recommended final 7-slide presentation build

Use this if you want a clean version for tomorrow with **exactly one diagram and one screenshot/demo item per slide**.

### Slide 1 — Title + problem framing
- **Use content from:** current `Slide 1` + `Slide 2`
- **Diagram to include:** `docs/pack-02/uml/deployment-insecure.puml`
- **Screenshot/demo item to include:** login screen from `frontend/src/pages/Login/index.vue`
- **Why this works:** it opens with the insecure baseline and immediately shows why the redesign was necessary.

### Slide 2 — Secure architecture overview
- **Use content from:** current `Slide 3`
- **Diagram to include:** `docs/pack-09/uml/dfd-context-current-state.puml`
- **Screenshot/demo item to include:** authenticated main workspace after login, e.g. the spaces/assignments area from `frontend/src/pages/SpaceList/index.vue` or `frontend/src/pages/AssignmentList/index.vue`
- **Why this works:** it gives the audience one current-state trust-boundary view before you dive into controls.

### Slide 3 — Authentication and session hardening
- **Use content from:** current `Slide 4`
- **Diagram to include:** `docs/pack-02/uml/sequence-login-secure.puml`
- **Screenshot/demo item to include:** MFA challenge screen from `frontend/src/pages/MfaChallenge/index.vue`, or the login success flow if that is easier to capture
- **Why this works:** it shows the secure login sequence and the real browser-facing session model together.

### Slide 4 — Why the MFA crypto design is strong
- **Use content from:** current `Slide 5`
- **Diagram to include:** `docs/pack-02/uml/sequence-login-secure.puml` reused as contextual support for the MFA branch
- **Screenshot/demo item to include:** MFA management/setup screen from `frontend/src/pages/AccountSecurity/index.vue`
- **Why this works:** the screenshot shows the real feature, while your spoken explanation carries the hashing-vs-encryption distinction.

### Slide 5 — Submission integrity, authorship, and confidentiality
- **Use content from:** current `Slide 6`
- **Diagram to include:** `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- **Screenshot/demo item to include:** submission detail screen from `frontend/src/pages/SubmissionDetail/index.vue` showing `hashDigest`, `digitalSignature`, and `verificationStatus`
- **Why this works:** this is the strongest end-to-end cryptographic workflow in the project and deserves a full slide.

### Slide 6 — Grade integrity and audit accountability
- **Use content from:** current `Slide 7`
- **Diagram to include:** `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- **Screenshot/demo item to include:** grade panel on the submission detail page from `frontend/src/pages/SubmissionDetail/index.vue`
- **Why this works:** it connects verified submissions, restricted grading, and audit-backed accountability in one place.

### Slide 7 — Honest limitations + evidence-backed conclusion
- **Use content from:** current `Slide 9` + `Slide 10` + optional conclusion
- **Diagram to include:** `docs/pack-02/uml/deployment-secure.puml`
- **Screenshot/demo item to include:** terminal screenshot of the successful integration test run
- **Why this works:** it lets you close with confidence while still being honest about the TLS/deployment boundary and other scoped limitations.

## One-diagram / one-screenshot quick list
- Slide 1 → Diagram: `docs/pack-02/uml/deployment-insecure.puml` | Screenshot: `frontend/src/pages/Login/index.vue`
- Slide 2 → Diagram: `docs/pack-09/uml/dfd-context-current-state.puml` | Screenshot: authenticated workspace (`frontend/src/pages/SpaceList/index.vue` or `frontend/src/pages/AssignmentList/index.vue`)
- Slide 3 → Diagram: `docs/pack-02/uml/sequence-login-secure.puml` | Screenshot: `frontend/src/pages/MfaChallenge/index.vue`
- Slide 4 → Diagram: `docs/pack-02/uml/sequence-login-secure.puml` | Screenshot: `frontend/src/pages/AccountSecurity/index.vue`
- Slide 5 → Diagram: `docs/pack-04/uml/sequence-submission-secure-pack04.puml` | Screenshot: `frontend/src/pages/SubmissionDetail/index.vue`
- Slide 6 → Diagram: `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml` | Screenshot: grade panel in `frontend/src/pages/SubmissionDetail/index.vue`
- Slide 7 → Diagram: `docs/pack-02/uml/deployment-secure.puml` | Screenshot: successful test run terminal output

## Recommended detailed 13-slide presentation build (15–20 minutes)

Use this version if you want a fuller presentation tomorrow with more explanation, more cryptographic reasoning, and more space to discuss what is actually implemented.

Aim for roughly **60–90 seconds per slide**.

### Slide 1 — Title and case-study framing
- **Purpose:** introduce the project as a cryptography case study, not just a general web app
- **Use content from:** current `Slide 1`
- **Diagram to include:** `docs/pack-09/uml/use-case-security-focused.puml`
- **Screenshot/demo item to include:** login screen from `frontend/src/pages/Login/index.vue`
- **Key message:** EduSecure is an education-platform security redesign focused on authentication, submissions, grades, and accountability.

### Slide 2 — The insecure baseline from the assignment brief
- **Purpose:** show exactly what was wrong before your redesign
- **Use content from:** current `Slide 2`
- **Diagram to include:** `docs/pack-02/uml/deployment-insecure.puml`
- **Screenshot/demo item to include:** simple problem-to-control table you create in PowerPoint
- **Key message:** the brief required me to solve plaintext passwords, insecure transport, MITM exposure, tamperable submissions, grade-integrity failures, and missing auditability.

### Slide 3 — Secure architecture overview
- **Purpose:** show how the secure system is organised at a high level
- **Use content from:** current `Slide 3`
- **Diagram to include:** `docs/pack-09/uml/dfd-context-current-state.puml`
- **Screenshot/demo item to include:** authenticated workspace after login, e.g. `frontend/src/pages/SpaceList/index.vue` or `frontend/src/pages/AssignmentList/index.vue`
- **Key message:** the backend is the trust anchor; the frontend demonstrates flows, while the backend enforces security controls.

### Slide 4 — Current-state trust boundaries and data movement
- **Purpose:** explain where sensitive data moves and where controls are applied
- **Use content from:** current `Slide 3` + `Slide 8` evidence language
- **Diagram to include:** `docs/pack-09/uml/dfd-level-1-current-state.puml`
- **Screenshot/demo item to include:** browser devtools or network/cookie view showing browser-session behavior
- **Key message:** auth data, MFA data, submissions, grades, and audit records all move through distinct protected paths in the implemented system.

### Slide 5 — Authentication hardening
- **Purpose:** explain how you fixed the plaintext-password problem and strengthened login
- **Use content from:** current `Slide 4`
- **Diagram to include:** `docs/pack-02/uml/sequence-login-secure.puml`
- **Screenshot/demo item to include:** MFA challenge screen from `frontend/src/pages/MfaChallenge/index.vue`, or login-success flow if easier to capture
- **Key message:** passwords are protected with `bcrypt`, sessions are cookie-based, and MFA-enabled users are not fully authenticated until factor two succeeds.

### Slide 6 — MFA as applied cryptography
- **Purpose:** explain the cryptographic reasoning inside the MFA design
- **Use content from:** current `Slide 5`
- **Diagram to include:** `docs/pack-02/uml/sequence-login-secure.puml` reused as contextual support
- **Screenshot/demo item to include:** MFA management/setup screen from `frontend/src/pages/AccountSecurity/index.vue`
- **Key message:** the MFA implementation is strong because it correctly distinguishes hashed passwords, encrypted TOTP secrets, and hashed one-time recovery codes.

### Slide 7 — Browser session security and MITM boundary
- **Purpose:** explain what the browser layer does and does not protect against
- **Use content from:** current `Slide 8`
- **Diagram to include:** `docs/pack-02/uml/deployment-secure.puml`
- **Screenshot/demo item to include:** browser devtools screenshot showing `EDUSECURE_AUTH` and `XSRF-TOKEN` cookies
- **Key message:** `HttpOnly` cookies and CSRF reduce browser-side session abuse, while TLS 1.3 is the intended transport control against eavesdropping and MITM.

### Slide 8 — Submission integrity and authorship
- **Purpose:** explain the digest + signature workflow clearly
- **Use content from:** current `Slide 6` with emphasis on `SHA-256` and ECC signatures
- **Diagram to include:** `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- **Screenshot/demo item to include:** submission detail screen from `frontend/src/pages/SubmissionDetail/index.vue` showing `hashDigest`, `digitalSignature`, and `verificationStatus`
- **Key message:** the system computes a digest, signs it, verifies it immediately, and stores the verification result as reviewable integrity evidence.

### Slide 9 — Submission confidentiality at rest
- **Purpose:** separate confidentiality from integrity so the marker sees both clearly
- **Use content from:** current `Slide 6` + `docs/pack-06/submission-content-protection-and-retrieval.md`
- **Diagram to include:** `docs/pack-04/uml/sequence-lecturer-submission-decryption.puml`
- **Optional secondary diagram:** `docs/pack-04/uml/sequence-submission-aes-at-rest-retrieval-pack04.puml`
- **Screenshot/demo item to include:** submission download/content retrieval flow from `frontend/src/pages/SubmissionDetail/index.vue`
- **Key message:** submission plaintext is not returned automatically; AES-GCM protects stored content at rest and retrieval is a separate audited action.

### Slide 10 — Grade integrity
- **Purpose:** show that grades are treated as integrity-sensitive academic records
- **Use content from:** current `Slide 7`
- **Diagram to include:** `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- **Screenshot/demo item to include:** grade panel on `frontend/src/pages/SubmissionDetail/index.vue`
- **Key message:** only verified submissions can be graded, grade actions are restricted to privileged roles, and student access is ownership-limited.

### Slide 11 — Audit integrity and accountability
- **Purpose:** give audit integrity its own space rather than burying it inside grading
- **Use content from:** current `Slide 7` + audit notes
- **Diagram to include:** `docs/pack-04/uml/sequence-audit-integrity-secure.puml`
- **Screenshot/demo item to include:** a database/result screenshot or slide table summarising `SUBMISSION_CREATED`, `SUBMISSION_VERIFIED`, `SUBMISSION_CONTENT_ACCESSED`, `GRADE_CREATED`, and `GRADE_UPDATED`
- **Key message:** sensitive actions are recorded with HMAC-backed integrity values and simple chaining support, making the audit trail tamper-evident within project scope.

### Slide 12 — Evidence that the implementation really works
- **Purpose:** support your claims with tests and verification, not just design language
- **Use content from:** current `Slide 9`
- **Diagram to include:** no UML on this slide; use evidence instead
- **Screenshot/demo item to include:** terminal screenshot of the successful integration test run
- **Key message:** the strongest claims are backed by integration tests, unit tests, and manual/browser review materials.

### Slide 13 — Honest limitations and conclusion
- **Purpose:** finish confidently without overclaiming
- **Use content from:** current `Slide 10` + optional conclusion slide
- **Diagram to include:** `docs/pack-02/uml/deployment-secure.puml`
- **Screenshot/demo item to include:** a clean two-column “Implemented now / bounded claims” slide you create in PowerPoint
- **Key message:** EduSecure is a strong, evidence-backed study-project security artefact, but not a fully production-proven enterprise platform.

## Detailed 13-slide quick list
- Slide 1 → Diagram: `docs/pack-09/uml/use-case-security-focused.puml` | Screenshot: `frontend/src/pages/Login/index.vue`
- Slide 2 → Diagram: `docs/pack-02/uml/deployment-insecure.puml` | Screenshot: your own problem-to-control summary table
- Slide 3 → Diagram: `docs/pack-09/uml/dfd-context-current-state.puml` | Screenshot: authenticated workspace (`SpaceList` / `AssignmentList`)
- Slide 4 → Diagram: `docs/pack-09/uml/dfd-level-1-current-state.puml` | Screenshot: browser devtools or browser session/network view
- Slide 5 → Diagram: `docs/pack-02/uml/sequence-login-secure.puml` | Screenshot: `frontend/src/pages/MfaChallenge/index.vue`
- Slide 6 → Diagram: `docs/pack-02/uml/sequence-login-secure.puml` | Screenshot: `frontend/src/pages/AccountSecurity/index.vue`
- Slide 7 → Diagram: `docs/pack-02/uml/deployment-secure.puml` | Screenshot: browser cookies (`EDUSECURE_AUTH`, `XSRF-TOKEN`)
- Slide 8 → Diagram: `docs/pack-04/uml/sequence-submission-secure-pack04.puml` | Screenshot: `frontend/src/pages/SubmissionDetail/index.vue`
- Slide 9 → Diagram: `docs/pack-04/uml/sequence-submission-aes-at-rest-retrieval-pack04.puml` | Screenshot: submission content retrieval/download flow
- Slide 10 → Diagram: `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml` | Screenshot: grade panel in `frontend/src/pages/SubmissionDetail/index.vue`
- Slide 11 → Diagram: `docs/pack-04/uml/sequence-audit-integrity-secure.puml` | Screenshot: audit-event summary table or evidence screenshot
- Slide 12 → Diagram: none | Screenshot: successful test run terminal output
- Slide 13 → Diagram: `docs/pack-02/uml/deployment-secure.puml` | Screenshot: implemented-vs-bounded-claims summary slide

---

## Safe phrases to use during the presentation
- “The repository evidences...”
- “Within the study-project scope...”
- “The implemented backend demonstrates...”
- “This materially reduces the risk of...”
- “Residual risk remains because...”

## Phrases to avoid
- “fully secure”
- “production-ready”
- “end-to-end encrypted”
- “unhackable”
- “complete non-repudiation”
- “the whole database is encrypted”

---

## Recommended speaking order if you need to cut the deck to 5 minutes
Use these 6 slides only:
1. Slide 1 — title
2. Slide 2 — brief problems
3. Slide 4 — authentication hardening
4. Slide 6 — submission security
5. Slide 7 — grade integrity and audit
6. Slide 10 — limitations and conclusion


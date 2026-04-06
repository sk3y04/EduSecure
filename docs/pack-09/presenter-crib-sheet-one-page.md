# EduSecure Presenter Crib Sheet (One Page)

Use this as a **live speaking aid** alongside `docs/edusecure-presentation-script-13-slide.md`.

## Opening reminder
- Frame EduSecure as a **bounded, evidence-backed cryptography case study**.
- Keep saying: **the repository evidences**, **within study-project scope**, **TLS is the intended deployment-side control**.
- Avoid saying: **fully secure**, **production-ready**, **end-to-end encrypted**, **full PKI**, **repository proves deployed HTTPS**.

---

## Slide-by-slide live cues

| Slide | Core line to say | Proof anchor | Visual cue | Boundary warning |
|---|---|---|---|---|
| 1 | EduSecure is a cryptography-focused study artefact, not just a normal CRUD app. | `docs/assignment_brief.md`, `docs/pack-09/final-implementation-evidence-map.md` | Login screen | Do not describe it as enterprise-complete. |
| 2 | The brief started from plaintext passwords, no HTTPS, tamperable submissions, weak grade trust, and poor accountability. | `docs/assignment_brief.md` | Problem-to-control table | TLS is the intended answer, not deployment-proven here. |
| 3 | Security decisions are enforced in the Spring Boot backend, while the frontend demonstrates flows. | `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`, `frontend/src/services/http.ts` | Workspace after login | Do not imply the browser is the trust anchor. |
| 4 | Different assets follow different protected paths: auth, MFA, submissions, grades, and audit. | `docs/pack-09/final-implementation-evidence-map.md` | DFD + devtools/network view | Do not claim one control protects every asset equally. |
| 5 | Passwords use `bcrypt`, MFA is optional, and MFA-enabled users get a session only after full verification. | `AuthService.java`, `MfaService.java`, auth integration tests | MFA challenge screen | Do not say password step alone completes MFA login. |
| 6 | The MFA design is strong because it hashes recovery codes but encrypts recoverable TOTP secrets with `AES-GCM`. | `MfaSecretCryptoService.java`, `TotpService.java`, `docs/pack-03/mfa-cryptography-implementation.md` | Account security / MFA setup | Do not generalise this into whole-database encryption. |
| 7 | EduSecure avoids `localStorage` JWTs, uses an `HttpOnly` auth cookie, and pairs it with CSRF protection. | `frontend/src/services/http.ts`, `SecurityConfig.java` | Cookies: `EDUSECURE_AUTH`, `XSRF-TOKEN` | MITM protection in transit depends on TLS at deployment. |
| 8 | Submission bytes are hashed with `SHA-256`, signed with ECC, and stored with visible verification evidence. | `SubmissionService.java`, `docs/pack-04/signature-hash-workflow.md`, submission tests | Submission detail with digest/signature/verification | Do not claim full user-held PKI or complete non-repudiation. |
| 9 | Submission content is encrypted at rest with `AES-GCM`, with separate key protection and controlled plaintext retrieval. | `SubmissionContentEncryptionService.java`, `SubmissionKeyProtectionService.java`, submission tests | Retrieval/download flow | Keep claim to confidentiality at rest, not end-to-end encrypted transport. |
| 10 | Grades can only be created for verified submissions and only by the owning lecturer or `ADMIN`. | `GradeService.java`, grade tests | Grade panel | Do not turn this into a full gradebook-platform claim. |
| 11 | Sensitive actions become tamper-evident through `HMAC-SHA-256` audit integrity values with chaining support. | `AuditService.java`, submission/grade tests | Audit event summary | Do not call it a full SIEM or forensic platform. |
| 12 | My strongest claims are backed by integration tests, unit tests, and manual/browser evidence guidance. | `docs/pack-09/integration-test-coverage-summary.md`, targeted test run | Successful test terminal screenshot | Do not imply every browser or deployment property is fully automated. |
| 13 | EduSecure is strongest as a layered, evidence-backed cryptography case study with honest boundaries. | `docs/pack-09/final-cryptography-claims-matrix.md`, `docs/pack-09/report-claims-audit-note.md` | Implemented-now vs bounded-claims summary | Re-state: TLS intended, demo ECC keypair, study-project scope. |

---

## Fallback 10-slide live order
If time drops, keep: **1, 2, 3, 5, 6, 8, 9, 10, 12, 13**.

## Best phrases to reuse live
- "The repository evidences..."
- "Within the study-project scope..."
- "The implemented backend demonstrates..."
- "This materially reduces the risk of..."
- "TLS is the intended deployment-side transport control..."

## Safe closing sentence
> EduSecure does not rely on one security mechanism; it applies the right cryptographic control to the right problem, which is what makes it a stronger and more defensible secure web application.


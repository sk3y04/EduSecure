# Presentation Evidence Appendix

This document is the **slide-to-evidence companion** for `docs/edusecure-presentation-script-13-slide.md`.

## Purpose
- Use `docs/edusecure-presentation-script-13-slide.md` as the **PowerPoint build source of truth**.
- Use this appendix to keep each slide tied to:
  - one safe core claim
  - one repository evidence set
  - one diagram choice
  - one screenshot/demo target
  - one wording boundary to avoid overclaiming
- Keep `docs/edusecure-security-presentation-deck.md` as a **supporting long-form deck brief**, not the final slide-build source.

## Verified evidence status

### Targeted backend security test run
Verified on **2026-04-06** with the following command:

```powershell
Set-Location "C:\Users\skey\IdeaProjects\EduSecure\backend"
.\gradlew.bat test --tests edusecure.edusecure.AuthControllerIntegrationTests --tests edusecure.edusecure.MfaAuthIntegrationTests --tests edusecure.edusecure.SubmissionFlowIntegrationTests --tests edusecure.edusecure.GradeFlowIntegrationTests
```

Observed result:
- `BUILD SUCCESSFUL`

### Screenshot target sanity check
The following presentation screenshot targets are present in the current frontend workspace:
- `frontend/src/pages/Login/index.vue`
- `frontend/src/pages/MfaChallenge/index.vue`
- `frontend/src/pages/AccountSecurity/index.vue`
- `frontend/src/pages/SubmissionDetail/index.vue`
- `frontend/src/pages/AssignmentList/index.vue`
- `frontend/src/pages/SpaceList/index.vue`

Manual capture items still need to be produced outside the repository:
- browser devtools cookie/network screenshots
- PowerPoint-made summary tables
- terminal screenshot of the successful test run
- implemented-now vs bounded-claims summary slide

---

## Slide-by-slide evidence map

### Slide 1 — EduSecure as a cryptography case study
- **Safe core claim:** EduSecure is a backend-focused cryptography case-study artefact built around authentication, submissions, grades, and accountability.
- **Repository anchors:**
  - `docs/assignment_brief.md`
  - `docs/pack-09/final-implementation-evidence-map.md`
  - `docs/pack-09/report-claims-audit-note.md`
- **Diagram:** `docs/pack-09/uml/use-case-security-focused.puml`
- **Screenshot/demo target:** `frontend/src/pages/Login/index.vue`
- **Wording boundary:** do not describe EduSecure as a finished enterprise platform; describe it as a bounded, evidence-backed study-project artefact.

### Slide 2 — The security problems from the brief
- **Safe core claim:** The assignment scenario began with plaintext passwords, insecure transport, tamperable submissions, weak grade trust, and poor accountability.
- **Repository anchors:**
  - `docs/assignment_brief.md`
  - `docs/pack-02/crypto-decision-matrix.md`
  - `docs/pack-09/final-cryptography-claims-matrix.md`
- **Diagram:** `docs/pack-02/uml/deployment-insecure.puml`
- **Screenshot/demo target:** presenter-created problem-to-control summary table
- **Wording boundary:** describe HTTPS/TLS as the control required by the brief and the intended deployment answer, not as repository-proven live deployment unless direct deployment evidence is shown.

### Slide 3 — Secure architecture overview
- **Safe core claim:** EduSecure uses a backend-first trust model in which the Spring Boot backend enforces authentication, authorization, cryptography, and audit behavior.
- **Repository anchors:**
  - `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
  - `frontend/src/services/http.ts`
  - `frontend/src/stores/auth.ts`
  - `docs/pack-09/final-implementation-evidence-map.md`
- **Diagram:** `docs/pack-09/uml/dfd-context-current-state.puml`
- **Screenshot/demo target:** `frontend/src/pages/AssignmentList/index.vue` or `frontend/src/pages/SpaceList/index.vue`
- **Wording boundary:** do not imply the frontend is the trust anchor; present it as a browser client that demonstrates backend-enforced flows.

### Slide 4 — Current-state trust boundaries and data movement
- **Safe core claim:** Different EduSecure assets follow different protected paths, including auth, MFA, submissions, grades, and audit records.
- **Repository anchors:**
  - `docs/pack-09/final-implementation-evidence-map.md`
  - `frontend/src/services/http.ts`
  - `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- **Diagram:** `docs/pack-09/uml/dfd-level-1-current-state.puml`
- **Screenshot/demo target:** browser devtools cookie/network view captured manually during a logged-in session
- **Wording boundary:** keep this slide architectural; do not overstate any single mechanism as protecting every asset equally.

### Slide 5 — Authentication hardening
- **Safe core claim:** EduSecure replaces plaintext-password handling with `bcrypt`, supports optional TOTP MFA, and issues the browser session only after full authentication.
- **Repository anchors:**
  - `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
  - `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
  - `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
  - `backend/src/main/java/edusecure/edusecure/service/auth/AuthTokenService.java`
  - `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
  - `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`
- **Diagram:** `docs/pack-02/uml/sequence-login-secure.puml`
- **Screenshot/demo target:** `frontend/src/pages/MfaChallenge/index.vue`
- **Wording boundary:** do not claim MFA is complete at the password step; the stronger claim is that MFA-enabled users receive `MFA_REQUIRED` first and only become authenticated after verification.

### Slide 6 — MFA as applied cryptography
- **Safe core claim:** The MFA design correctly distinguishes between hashed values and recoverable secrets by hashing recovery codes while encrypting stored TOTP secrets with `AES-GCM`.
- **Repository anchors:**
  - `docs/pack-03/mfa-cryptography-implementation.md`
  - `backend/src/main/java/edusecure/edusecure/service/auth/TotpService.java`
  - `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java`
  - `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
- **Diagram:** reuse `docs/pack-02/uml/sequence-login-secure.puml` as contextual support only
- **Screenshot/demo target:** `frontend/src/pages/AccountSecurity/index.vue`
- **Wording boundary:** do not generalise encrypted MFA-secret storage into whole-database encryption.

### Slide 7 — Browser session security and the MITM boundary
- **Safe core claim:** EduSecure avoids frontend JWT storage, uses credentialed requests with an `HttpOnly` auth cookie, and pairs this with CSRF token handling, while TLS remains the intended deployment-side transport control.
- **Repository anchors:**
  - `frontend/src/services/http.ts`
  - `frontend/src/stores/auth.ts`
  - `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
  - `docs/pack-09/report-claims-audit-note.md`
  - `docs/pack-09/final-cryptography-claims-matrix.md`
- **Diagram:** `docs/pack-02/uml/deployment-secure.puml`
- **Screenshot/demo target:** browser devtools screenshot showing `EDUSECURE_AUTH` and `XSRF-TOKEN` cookies
- **Wording boundary:** do not present HTTPS enforcement as repository-proven unless deployment evidence is added; say TLS 1.3 is the intended deployment transport control.

### Slide 8 — Submission integrity and authorship
- **Safe core claim:** EduSecure hashes submitted content with `SHA-256`, signs the digest using ECC-based signing, verifies it immediately, and stores the verification result as visible evidence.
- **Repository anchors:**
  - `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
  - `docs/pack-04/signature-hash-workflow.md`
  - `docs/pack-09/final-cryptography-claims-matrix.md`
  - `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- **Diagram:** `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- **Screenshot/demo target:** `frontend/src/pages/SubmissionDetail/index.vue`
- **Wording boundary:** do not claim full user-held PKI or enterprise non-repudiation; describe this as a strong study-project signing workflow using a stable demo ECC keypair.

### Slide 9 — Submission confidentiality at rest
- **Safe core claim:** EduSecure protects stored submission content with `AES-GCM`, separately protects the per-submission content key, and keeps metadata review separate from plaintext retrieval.
- **Repository anchors:**
  - `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
  - `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`
  - `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionService.java`
  - `docs/pack-06/submission-content-protection-and-retrieval.md`
  - `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- **Diagram:** `docs/pack-04/uml/sequence-lecturer-submission-decryption.puml`
- **Optional secondary diagram:** `docs/pack-04/uml/sequence-submission-aes-at-rest-retrieval-pack04.puml`
- **Screenshot/demo target:** controlled content retrieval/download flow from `frontend/src/pages/SubmissionDetail/index.vue`
- **Wording boundary:** do not claim end-to-end encrypted upload transport or enterprise KMS maturity; keep the claim focused on application-layer confidentiality at rest and controlled retrieval.

### Slide 10 — Grade integrity
- **Safe core claim:** EduSecure only permits grading for verified submissions and restricts grade create/update access to the assignment-owning lecturer or `ADMIN`.
- **Repository anchors:**
  - `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java`
  - `docs/pack-07/grade-phase-status-and-evidence.md`
  - `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`
- **Diagram:** `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- **Screenshot/demo target:** grade panel from `frontend/src/pages/SubmissionDetail/index.vue`
- **Wording boundary:** do not expand this into a full gradebook or moderation platform claim; keep the focus on verified-submission gating, access control, and integrity-sensitive handling.

### Slide 11 — Audit integrity and accountability
- **Safe core claim:** Sensitive submission and grade actions create append-oriented audit records protected by `HMAC-SHA-256` integrity values with previous-value chaining support.
- **Repository anchors:**
  - `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
  - `docs/pack-04/audit-integrity-and-evidence-plan.md`
  - `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
  - `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`
- **Diagram:** `docs/pack-04/uml/sequence-audit-integrity-secure.puml`
- **Screenshot/demo target:** audit-event summary table showing `SUBMISSION_CREATED`, `SUBMISSION_VERIFIED`, `SUBMISSION_CONTENT_ACCESSED`, `GRADE_CREATED`, and `GRADE_UPDATED`
- **Wording boundary:** do not describe this as a full SIEM or public audit-investigation platform; describe it as tamper-evident internal audit evidence within study-project scope.

### Slide 12 — Evidence that the implementation works
- **Safe core claim:** The strongest security claims are supported by integration tests, unit tests, and browser/manual evidence guidance rather than design-only description.
- **Repository anchors:**
  - `docs/pack-09/final-implementation-evidence-map.md`
  - `docs/pack-09/integration-test-coverage-summary.md`
  - `docs/pack-09/unit-test-coverage-summary.md`
  - `docs/pack-09/manual-test-coverage-summary.md`
  - `backend/src/test/java/edusecure/edusecure/AuthControllerIntegrationTests.java`
  - `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`
  - `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
  - `backend/src/test/java/edusecure/edusecure/GradeFlowIntegrationTests.java`
- **Diagram:** none; use evidence instead
- **Screenshot/demo target:** terminal screenshot of the successful 2026-04-06 targeted integration test run
- **Wording boundary:** do not imply that every browser/deployment property is fully automated; distinguish backend test proof from manual/browser capture evidence.

### Slide 13 — Honest limitations and conclusion
- **Safe core claim:** EduSecure is strongest as a bounded, evidence-backed cryptography case study with implemented controls for authentication, integrity, confidentiality at rest, and accountability.
- **Repository anchors:**
  - `docs/pack-09/final-cryptography-claims-matrix.md`
  - `docs/pack-09/report-claims-audit-note.md`
  - `docs/pack-09/final-implementation-evidence-map.md`
- **Diagram:** `docs/pack-02/uml/deployment-secure.puml`
- **Screenshot/demo target:** presenter-created two-column summary slide for implemented-now vs bounded claims
- **Wording boundary:** explicitly keep TLS as intended deployment control, the signing model as a stable demo ECC keypair rather than full PKI, and the overall artefact as study-project scope rather than production-complete maturity.

---

## Best safe summary line per theme
- **Authentication:** "EduSecure protects stored passwords with bcrypt and issues the browser session only after full authentication completes."
- **MFA:** "EduSecure supplements password authentication with optional TOTP MFA, encrypted secret storage, and hashed one-time recovery codes."
- **Browser security:** "EduSecure uses cookie-based browser authentication with CSRF protection, while TLS remains the intended deployment transport control."
- **Submission integrity:** "EduSecure computes a SHA-256 digest and uses ECC-based signing to make submission integrity evidence reviewable."
- **Submission confidentiality:** "EduSecure encrypts stored submission content at rest and separates metadata viewing from plaintext retrieval."
- **Grades:** "EduSecure restricts grading to verified submissions and authorised teaching roles."
- **Audit:** "EduSecure records sensitive actions with HMAC-backed integrity values to make the audit trail tamper-evident within project scope."

## Phrases to prefer live
- "The repository evidences..."
- "Within the study-project scope..."
- "The implemented backend demonstrates..."
- "This materially reduces the risk of..."
- "TLS is the intended deployment-side transport control..."

## Phrases to avoid live
- "fully secure"
- "production-ready"
- "end-to-end encrypted"
- "complete non-repudiation"
- "the whole database is encrypted"
- "the repository proves deployed HTTPS"

## Final pre-PowerPoint capture checklist
- [ ] Capture login screen from `frontend/src/pages/Login/index.vue`
- [ ] Capture MFA challenge flow from `frontend/src/pages/MfaChallenge/index.vue`
- [ ] Capture MFA setup/status screen from `frontend/src/pages/AccountSecurity/index.vue`
- [ ] Capture submission evidence screen from `frontend/src/pages/SubmissionDetail/index.vue`
- [ ] Capture grade panel from `frontend/src/pages/SubmissionDetail/index.vue`
- [ ] Capture browser devtools cookie evidence for `EDUSECURE_AUTH` and `XSRF-TOKEN`
- [ ] Capture terminal screenshot of the successful targeted test run
- [ ] Build the two PowerPoint-native summary visuals: problem-to-control table and implemented-now vs bounded-claims table


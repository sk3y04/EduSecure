# CVSS Risk Register

This register complements the qualitative EduSecure risk material by applying **CVSS v3.1 base scoring** to the most important application-security and deployment-risk scenarios visible in the current repository state.

> Scope note: this is a **scenario-based risk register**, not a claim that every row is a confirmed CVE. Where a control is already present, the row is marked as a **residual / regression-sensitive** risk rather than an open defect.

## Executive summary

The current repository state suggests **three critical priorities** and **three high-priority regression-sensitive risks**.

### Highest priorities

1. **Fallback cryptographic secrets in configuration** — if production keeps repository defaults for JWT, audit HMAC, MFA secret encryption, or submission-storage encryption, the blast radius is severe across confidentiality, integrity, and trust.
2. **No clearly evidenced login throttling** — the login flow appears protected by password hashing and optional MFA, but not by visible account/IP rate limiting against credential stuffing.
3. **TLS and secure-cookie posture still depend on correct deployment** — the repository has good `prod` guardrails for cookie security, but transport protection is still partly an operational assurance problem.

### High-value residual risks

4. **Broken object authorization regression on submissions/grades** — current ownership checks look strong, but this remains one of the most damaging classes of web-app failure in an academic platform.
5. **CSRF regression against cookie-backed endpoints** — current controls are materially better now, but browser-driven auth always deserves repeated hostile-origin testing.
6. **Upload/storage abuse regression** — current upload validation and encrypted-at-rest design reduce risk, but file-handling paths are historically easy to weaken during later expansion.

## Method note

This assessment uses **CVSS v3.1 base scoring** as a prioritisation aid, not as a replacement for the broader qualitative method in `risk-methodology.md`.

The scoring approach used here is:
- identify the abuse scenario or hardening gap
- map it to the most defensible CVSS v3.1 base vector
- interpret the result together with EduSecure's academic confidentiality, integrity, and accountability needs
- distinguish between **open hardening gaps** and **residual risks currently mitigated in code**

## Assessment basis

Primary evidence used:
- `docs/01-governance-risk-traceability/risk-register-refined.md`
- `docs/05-security-review/security-sensitive-modules-inventory.md`
- `docs/05-security-review/security-test-scenarios-matrix.md`
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-prod.properties`
- `frontend/src/services/http.ts`
- `compose.yaml`

## CVSS-prioritised register

| ID | Scenario | Current status | Evidence anchor | CVSS v3.1 vector | Score | Severity | Treatment priority |
|---|---|---|---|---|---:|---|---|
| CVSS-01 | Production deployment accidentally keeps repository fallback secrets for JWT, audit HMAC, MFA encryption, or submission-storage encryption | **Open hardening gap** | `application.properties`, `SubmissionKeyProtectionService`, `MfaSecretCryptoService`, `JwtService`, `security-test-scenarios-matrix.md` (`CFG-03`) | `AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H` | 9.8 | Critical | Immediate |
| CVSS-02 | Login endpoint lacks clear brute-force / credential-stuffing throttling | **Open hardening gap** | `AuthService.login(...)`, `security-sensitive-modules-inventory.md`, `security-test-scenarios-matrix.md` (`AUTH-14`) | `AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:L` | 9.4 | Critical | Immediate |
| CVSS-03 | Deployment runs without TLS or with insecure cookie transport assumptions outside the guarded prod profile | **Environmental / deployment risk** | `application.properties`, `application-prod.properties`, `SecurityConfig`, `compose.yaml` | `AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N` | 9.1 | Critical | Immediate |
| CVSS-04 | Submission or grade object-authorization checks regress, allowing an authenticated but unauthorized user to read or alter another student's academic record | **Residual, currently controlled** | `SubmissionService`, `GradeService`, `security-test-scenarios-matrix.md` (`SUB-03`, `SUB-04`, `GRADE-04`, `GRADE-10`, `GRADE-11`) | `AV:N/AC:L/PR:L/UI:N/S:U/C:H/I:H/A:N` | 8.1 | High | High regression-test priority |
| CVSS-05 | CSRF protection or browser cookie/origin posture regresses, enabling cross-site state-changing requests against cookie-authenticated endpoints | **Residual, currently controlled** | `SecurityConfig.csrf(...)`, `frontend/src/services/http.ts`, `security-test-scenarios-matrix.md` (`AUTH-15`, `CSRF-01`) | `AV:N/AC:L/PR:N/UI:R/S:U/C:L/I:H/A:N` | 7.1 | High | High regression-test priority |
| CVSS-06 | Upload-validation or encrypted-storage controls regress, allowing authenticated upload abuse, storage tampering, or unsafe file handling | **Residual, currently controlled** | `SubmissionService.validateAndReadUpload(...)`, `SubmissionKeyProtectionService`, `security-test-scenarios-matrix.md` (`SUB-06` to `SUB-13`) | `AV:N/AC:L/PR:L/UI:N/S:U/C:L/I:H/A:L` | 7.6 | High | High regression-test priority |

## Scoring rationale notes

- **Attack Vector (AV)** is mostly `N` because the main risks are reachable over normal web deployment paths.
- **Privileges Required (PR)** is `N` for internet-facing configuration and login-hardening gaps, but `L` for broken-object-access and upload-abuse scenarios because an ordinary authenticated student or lecturer would already have a foothold.
- **User Interaction (UI)** is `R` only where browser-mediated abuse is a realistic prerequisite, especially CSRF.
- **Scope (S)** is kept `U` for most scenarios because the harm primarily stays within the same application trust boundary, even when the consequences are severe.
- **Confidentiality / Integrity / Availability** were weighted toward **confidentiality and integrity** because EduSecure handles identities, submissions, grades, and audit evidence where trust and correctness matter more than pure uptime.

## Short interpretation

### Highest-priority open items

1. **CVSS-01: fallback secrets**
   - The repository ships default values for `jwt.secret`, `audit.hmac-secret`, `mfa.secret-encryption-key`, and `submission.storage.master-key` in `application.properties`.
   - The current codebase validates secure cookie posture for `prod`, but it does **not** show an equivalent startup guard that blocks production from using default cryptographic secrets.
   - If these values remain live in a deployed environment, token forgery, audit-integrity compromise, MFA-secret decryption, or submission-content key recovery become materially easier.

2. **CVSS-02: login brute-force protection gap**
   - `AuthService.login(...)` authenticates credentials and handles MFA branching, but the reviewed code and current documentation show no obvious account/IP throttling for password login.
   - The security scenario matrix already marks this as a manual high-priority gap (`AUTH-14`).

3. **CVSS-03: TLS / secure-cookie deployment dependency**
   - Local/default settings allow `auth.cookie.secure=false`, and the developer topology in `compose.yaml` uses HTTP origins.
   - `application-prod.properties` and `AuthCookieConfigurationValidator` reduce the risk for the `prod` profile, but the overall security posture still depends on correct deployment and reverse-proxy TLS termination.

### High-value residual risks to keep testing

4. **CVSS-04: authorization regressions on submissions/grades**
   - Current service-layer ownership checks are strong and already evidenced, but this remains one of the most damaging classes of regression in EduSecure because it directly affects sensitive academic records.

5. **CVSS-05: CSRF regression risk**
   - EduSecure now has server-side CSRF token enforcement plus cookie/origin posture, which materially reduces this risk.
   - It remains worth scoring because cookie-backed browser authentication makes CSRF a natural regression target whenever frontend or deployment topology changes.

6. **CVSS-06: upload/storage abuse regression risk**
   - Current controls reject empty, oversized, traversal-like, fake-PDF, and invalid-UTF-8 uploads and encrypt stored content at rest.
   - This should remain high in the regression backlog because upload paths often re-open through later feature expansion.

## Overall conclusion

Using CVSS as a prioritisation lens, the **most urgent work** for EduSecure is not the already well-evidenced authorization logic, but the **operational hardening edges** around secrets, login abuse resistance, and deployment transport posture.

At the same time, the repository should continue treating **submission access, grade access, CSRF, and upload handling** as top-tier regression areas because these directly affect academic integrity and privacy even if the present implementation appears comparatively strong.

## Recommended next actions

1. Add a production startup validator that rejects repository fallback secrets for JWT, audit HMAC, MFA secret encryption, signing keys, and submission-storage encryption.
2. Add login throttling or account/IP-based rate limiting around `POST /api/auth/login`, then capture evidence in the auth integration suite.
3. Record explicit TLS and reverse-proxy deployment requirements in operations/security docs and keep `prod` startup validation strict.
4. Keep broken-object-level-authorization, CSRF, and upload-abuse scenarios in the highest regression-test tier.
5. Re-score this register after any major auth, cookie, frontend rendering, or submission-storage change.


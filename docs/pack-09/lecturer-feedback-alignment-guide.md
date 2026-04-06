# Lecturer Feedback Alignment Guide

This guide turns the lecturer's message into a report-ready checklist using the EduSecure files that already exist in the repository.

## 1. Marker-facing checklist

- [ ] include **one small use-case diagram** that focuses on the main protected interactions
- [ ] include **sequence diagrams for selected security-critical use cases**
- [ ] include a **risk register** that explicitly names vulnerabilities and mitigations
- [ ] explain **how the code is executed**
- [ ] demonstrate **MFA enforcement**
- [ ] demonstrate the current posture for **SQL injection, XSS, and CSRF**
- [ ] demonstrate controls that reduce attempts to **tamper with uploaded files**
- [ ] explain **browser-to-server confidentiality** honestly, without overstating what the repository proves

## 2. Recommended diagram set

### Use-case diagram

Use the trimmed report-friendly diagram in:
- `docs/pack-09/uml/use-case-security-focused.puml`

If you want a broader system-level alternative, keep using:
- `docs/pack-02/uml/use-case.puml`

### Sequence diagrams to include

Best security-focused pairings already present in the repository:

1. **Login / MFA before and after hardening**
   - before: `docs/pack-02/uml/sequence-login-insecure.puml`
   - after: `docs/pack-02/uml/sequence-login-secure.puml`

2. **Secure submission handling**
   - `docs/pack-04/uml/sequence-submission-secure-pack04.puml`

3. **Grade integrity and audit trail**
   - `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`

### Best report structure for the diagrams

A clean order is:
1. use-case diagram
2. insecure login baseline
3. secure login with MFA
4. secure submission sequence
5. grade-integrity sequence

That order gives the marker a simple progression from actor goals to hardened workflows.

## 3. Risk register to cite

Primary risk-register file:
- `docs/pack-02/risk-register-refined.md`

For the lecturer's message, the most important entries to cite are now:
- `R1` password exposure
- `R2` transport interception / browser-server confidentiality
- `R3` injection against grade data
- `R4` submission tampering / authorship dispute
- `R5` untraceable grade or submission actions
- `R8` CSRF against cookie-backed endpoints
- `R9` XSS in the frontend
- `R10` malicious upload abuse or stored-file tampering

## 4. How to explain code execution

The repository already shows three realistic ways to run the project.

### Option A: run the full stack with Docker Compose

Source of truth:
- `compose.yaml`

Use this in PowerShell:

```powershell
Set-Location "C:\Users\skey\IdeaProjects\EduSecure"
docker compose up --build
```

What this starts:
- PostgreSQL on port `5432`
- Spring Boot backend on port `8080`
- Vite frontend on port `5173`

### Option B: run the backend locally

Source of truth:
- `backend/build.gradle`
- `backend/src/main/resources/application.properties`

Use this in PowerShell:

```powershell
Set-Location "C:\Users\skey\IdeaProjects\EduSecure\backend"
.\gradlew.bat bootRun
```

### Option C: run the frontend locally

Source of truth:
- `frontend/README.md`

Use this in PowerShell:

```powershell
Set-Location "C:\Users\skey\IdeaProjects\EduSecure\frontend"
npm install
npm run dev
```

### Safe wording for the report

> EduSecure can be run either as a full local stack through `compose.yaml` or as separate backend and frontend processes. The backend is a Spring Boot application using PostgreSQL and Liquibase, while the frontend is a Vue 3 / Vite client that sends credentialed requests to the backend.

## 5. What to demonstrate for each lecturer point

### A. MFA enforcement

Use these files as evidence:
- `docs/pack-02/uml/sequence-login-secure.puml`
- `docs/pack-03/api-auth-contract.md`
- `docs/pack-03/implementation-status-and-evidence.md`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`

What to say:
- password login does **not** complete authentication when MFA is enabled
- the backend returns `MFA_REQUIRED` plus a short-lived challenge
- the authenticated cookie is issued **only after** `/api/auth/mfa/verify` succeeds
- recovery codes are one-time use
- MFA secrets are protected at rest

Best demo evidence:
- screenshot or API trace of `POST /api/auth/login` returning `MFA_REQUIRED`
- screenshot or API trace of successful `POST /api/auth/mfa/verify`
- screenshot of MFA status before and after enablement

### B. SQL injection protection

Use these files as evidence:
- `backend/src/main/java/edusecure/edusecure/repository/space/SpaceRepository.java`
- JPA repositories under `backend/src/main/java/edusecure/edusecure/repository/`
- `docs/pack-02/risk-register-refined.md`

What to say:
- the backend uses Spring Data JPA repositories and parameter binding rather than string-built SQL in controller code
- even the explicit `@Query` usage in `SpaceRepository` uses named parameters such as `:createdByUserId` and `:studentUserId`
- validation and RBAC reduce the impact of malformed or hostile input

Honesty boundary:
- this is a strong **secure-coding posture** against SQL injection
- it is not a cryptographic control
- avoid claiming that injection is mathematically impossible

### C. XSS protection

Use these files as evidence:
- `frontend/src/services/http.ts`
- frontend components under `frontend/src/`
- repository review result: no `v-html` usage was found in `frontend/src/`
- `docs/pack-02/risk-register-refined.md`

What to say:
- the frontend relies on Vue's default escaped rendering
- the current codebase does not use `v-html` or similar raw HTML sinks
- auth is in an `HttpOnly` cookie, so the browser script layer cannot directly read the session JWT

Honesty boundary:
- this reduces current XSS exposure, but future raw HTML rendering would weaken the posture
- avoid claiming a complete XSS elimination guarantee

### D. CSRF protection

Use these files as evidence:
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`
- `backend/src/main/java/edusecure/edusecure/security/AuthCookieService.java`
- `backend/src/main/resources/application-prod.properties`
- `docs/pack-11/security-test-scenarios-matrix.md`
- `docs/pack-02/risk-register-refined.md`

What to say honestly:
- the application uses cookie-backed auth, so CSRF must be discussed explicitly
- Spring Security CSRF protection is now enabled for unsafe methods
- the browser client bootstraps a readable `XSRF-TOKEN` cookie and echoes it in the `X-XSRF-TOKEN` header on unsafe requests
- this server-side token check now works alongside cookie settings such as `SameSite`, restricted CORS origins, and secure production deployment
- the remaining honest caveat is that hostile-origin browser testing should still be shown or discussed, especially if deployment topology changes

This honesty is important because it is better to show correct security reasoning than to overclaim.

### E. Attempts to modify uploaded files

Use these files as evidence:
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/FileSystemSubmissionContentStore.java`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`
- `docs/pack-04/uml/sequence-submission-secure-pack04.puml`

What to say:
- upload filenames are cleaned and traversal-style names are rejected
- only bounded file types are accepted in the current scope: UTF-8 `text/plain` and validated `application/pdf`
- PDF uploads must contain a real PDF header
- oversized, empty, and invalid UTF-8 uploads are rejected
- uploaded content is hashed and digitally signed in the backend workflow
- stored file references are randomised and path-validated
- ciphertext, not plaintext, is written to disk
- plaintext retrieval is separated into a protected endpoint and audited

Best demo evidence:
- show the test case that rejects `../secrets.txt`
- show the test case that rejects fake PDF content
- show the successful upload case where stored bytes differ from plaintext because the content is encrypted at rest

### F. Confidentiality and safety between browser and server

Use these files as evidence:
- `frontend/src/services/http.ts`
- `backend/src/main/java/edusecure/edusecure/security/AuthCookieService.java`
- `backend/src/main/resources/application-prod.properties`
- `frontend/README.md`
- `docs/pack-09/final-implementation-evidence-map.md`

What to say:
- browser requests are credentialed and rely on an `HttpOnly` auth cookie
- production cookie settings require `Secure=true`
- unsafe production cookie combinations fail fast at startup
- the report design treats TLS/HTTPS as the transport-layer confidentiality control

Honesty boundary:
- the repository documents TLS as the intended deployment control
- unless you include separate deployment evidence, do **not** claim that HTTPS enforcement is fully proven by the repository alone

## 6. Best files to cite in the final report

If you want a short high-value citation set, use:
- `docs/pack-09/lecturer-feedback-alignment-guide.md`
- `docs/pack-09/final-implementation-evidence-map.md`
- `docs/pack-09/final-doc-alignment-summary.md`
- `docs/pack-02/risk-register-refined.md`
- `docs/pack-02/uml/sequence-login-secure.puml`
- `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- `backend/src/test/java/edusecure/edusecure/MfaAuthIntegrationTests.java`
- `backend/src/test/java/edusecure/edusecure/SubmissionFlowIntegrationTests.java`

## 7. One-paragraph summary you can reuse

> EduSecure already contains report-ready evidence for the lecturer's requested structure. A small use-case diagram can be used to frame the main protected interactions, while sequence diagrams show how login/MFA, secure submission handling, and grade integrity work in the implemented design. The refined risk register now explicitly covers transport interception, injection, CSRF, XSS, and upload tampering. Execution can be demonstrated through either Docker Compose or separate backend/frontend startup flows. In the implementation discussion, MFA enforcement, cookie-backed authentication, JPA-based data access, safe Vue rendering, upload validation, encrypted-at-rest storage, audit logging, and deployment-side TLS can all be explained clearly, while still preserving honest boundaries around CSRF and transport-layer proof.


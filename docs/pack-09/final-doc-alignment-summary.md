# Final Documentation Alignment Summary

This note records the highest-visibility documentation and UML corrections made to keep the final EduSecure report aligned with the implemented repository state.

## 1. Purpose

The main goal of this summary is to reduce marker-facing contradiction risk.

It distinguishes:
- **implemented evidence**: documents that should be cited as proof of what the code now does
- **design abstraction**: UML/design notes that still describe the intended security logic at a higher level

## 2. Highest-value mismatches corrected

| Area | Previous mismatch | Corrected position |
|---|---|---|
| Browser auth transport | older wording in some UML/status notes referred to bearer tokens or token-return-to-JS flows | browser-facing auth is now described consistently as a backend-issued `HttpOnly` cookie session with credentialed frontend requests |
| Login/MFA sequence | secure-login UML previously showed token returned directly in response bodies | UML now shows `Set-Cookie`, cookie clearing on MFA challenge, and session establishment only after successful MFA verification |
| Submission signing boundary | older UML implied browser/client-side hashing/signing | UML now shows the implemented backend boundary: digest generation, signature creation/verification, and verification-state persistence occur in `SubmissionService` |
| Submission signing-key stability | earlier code used a fresh runtime-generated demo RSA keypair | the signing model now uses a stable configured demo RSA keypair loaded from externalisable resource locations, while remaining a bounded study-project simulation |
| Submission confidentiality boundary | older submission design files did not show AES-at-rest storage or separate plaintext retrieval clearly | UML/docs now describe encrypted-at-rest storage, protected key wrapping, metadata/content endpoint split, and `SUBMISSION_CONTENT_ACCESSED` audit events |
| Grade auth wording | older grade UML/status note used bearer-token wording | grade flow now consistently refers to the authenticated cookie-backed browser session |
| Unimplemented endpoints | some API design notes listed audit/re-verification endpoints without strong status distinction | API docs now mark deferred endpoints explicitly and keep the implemented contract clear |
| Submission class model | old class addendum used `storedFilePath`, omitted newer encryption metadata, and included an unimplemented `AuditController` | class addendum now uses `storedFileReference`, includes core encrypted-at-rest metadata fields, and removes the unimplemented controller |

## 3. Implemented evidence documents to cite first in the report

These are the safest report-facing sources when you need to prove what the repository **currently implements**:

- `docs/pack-03/implementation-status-and-evidence.md`
- `docs/pack-06/submission-phase-status-and-evidence.md`
- `docs/pack-06/submission-content-protection-and-retrieval.md`
- `docs/pack-07/grade-phase-status-and-evidence.md`
- `docs/pack-09/current-state-data-flow-diagram.md`
- `docs/pack-09/uml/dfd-level-1-current-state.puml`
- `docs/pack-09/final-implementation-evidence-map.md`
- backend integration tests under `backend/src/test/java/edusecure/edusecure/`

## 4. Design-abstraction documents that remain useful

These files are still valuable in the report, but they should be presented as **design-level** artefacts rather than literal code dumps:

- `docs/pack-02/uml/sequence-login-secure.puml`
- `docs/pack-02/uml/sequence-submission-secure.puml`
- `docs/pack-04/uml/class-diagram-submission-addendum.puml`
- `docs/pack-04/uml/sequence-submission-secure-pack04.puml`
- `docs/pack-05/uml/sequence-grade-integrity-secure-pack05.puml`
- `docs/pack-09/uml/dfd-context-current-state.puml`
- `docs/pack-05/uml/sequence-aes-secure-transmission-demo.puml`

Recommended wording in the report:

> The UML artefacts represent the security design and main interaction logic of EduSecure. Implementation-specific refinements, such as cookie transport details, encrypted-at-rest submission storage, and separate plaintext retrieval, are corroborated by the implementation evidence notes and automated tests.

The Pack 09 DFD files can be used alongside those UML artefacts when you want one marker-friendly diagram that highlights current trust boundaries, data stores, and sensitive-data movement more directly.

## 5. Code symbols the corrected docs now map to

### Auth and MFA
- `backend/src/main/java/edusecure/edusecure/controller/AuthController.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/MfaService.java`
- `backend/src/main/java/edusecure/edusecure/security/AuthCookieService.java`
- `backend/src/main/java/edusecure/edusecure/config/SecurityConfig.java`

### Submission integrity and confidentiality
- `backend/src/main/java/edusecure/edusecure/controller/SubmissionController.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionKeyProtectionService.java`
- `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentStore.java`

### Grade integrity and audit
- `backend/src/main/java/edusecure/edusecure/controller/GradeController.java`
- `backend/src/main/java/edusecure/edusecure/service/grade/GradeService.java`
- `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
- `backend/src/main/java/edusecure/edusecure/service/crypto/AesRsaCryptoService.java`

## 6. Remaining honesty boundaries to preserve in the final report

Do **not** overstate these areas:

- the submission signature flow is a **study-project simulated signing model**, not a user-held PKI deployment
- ~~the AES demo is a **separate artefact demonstration**~~ — the standalone AES demo has been removed; secure transmission is handled by TLS 1.3 via Certbot/Let's Encrypt; AES-GCM appears only in real at-rest business roles (MFA secrets, submission content)
- the PostgreSQL/Liquibase path proves **versioned schema delivery and local verification**, not enterprise production hardening
- default property fallbacks in `application.properties` should be described as **development/demo defaults**, with production secrets expected to be externalised

## 7. Final practical rule

When the report makes a strong implementation claim, prefer citing in this order:

1. implementation-status/evidence note
2. concrete backend class or endpoint
3. integration test proving the behavior
4. UML/design artefact as supporting explanation

That order keeps the final submission technically honest and much harder to challenge during marking.


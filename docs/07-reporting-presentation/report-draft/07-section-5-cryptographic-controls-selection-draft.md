# Section 5 Draft — Cryptographic Controls and Selection Justification

This file is a **report-ready working draft** for Section 5.

Use it together with:
- `docs/02-architecture-crypto/crypto-decision-matrix.md`
- `docs/07-reporting-presentation/final-cryptography-claims-matrix.md`
- `docs/07-reporting-presentation/report-claims-audit-note.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/07-reporting-presentation/high-mark-report-blueprint.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`

## How to use this draft

- keep the prose in the **main report voice**
- keep only the **core comparison table** in the main body unless space remains
- treat the optional control-to-risk table and any code artefact screenshot as **appendix-first material** under word pressure
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-1-to-5.md`

## 5. Cryptographic Controls and Selection Justification

The selected cryptographic controls in EduSecure were chosen by matching the assignment’s risks and named primitives to mechanisms that are both technically appropriate and realistically implementable within a bounded Spring Boot artefact. The key academic point is not merely that certain algorithms appear in the repository, but that each one is selected for a distinct security role and justified against plausible alternatives. This compare-and-select approach is stronger than a descriptive feature list because it explains both **why the chosen controls fit the EduSecure threat model** and **why other options were not used as the primary implementation focus**.

[TABLE PLACEHOLDER — Table X. Algorithm comparison and final control selection
Source: writer-created summary based on `docs/02-architecture-crypto/crypto-decision-matrix.md`
Purpose: compare `bcrypt`, `AES-GCM`, `ECDH P-256`, `HKDF-SHA-256`, ECC signatures, `SHA-256`, `HMAC-SHA-256`, and TLS against the main alternatives discussed in the report
Placement: immediately after the opening compare-and-select paragraph
Priority: core
Status: not inserted yet]

A strong comparison table here can replace repeated explanatory sentences later in the section. If used, the surrounding text should interpret the final choices rather than re-listing every row in full.

## 5.1 Password storage choice

For password storage, `bcrypt` is the correct primary mechanism because the assignment brief identifies plaintext password handling as an existing security failure. A password-storage control must be resistant to offline guessing after database compromise, which is why a dedicated password-hashing function is more appropriate than a fast general-purpose digest. In EduSecure, `bcrypt` therefore addresses the exact risk that the brief makes explicit: direct disclosure of reusable credentials from weak storage practice.

A useful academic comparison is between `bcrypt` and raw `SHA-256`. Although `SHA-256` is cryptographically strong as a digest function, it is not designed to be the primary protection for stored password verifiers. Its speed is a benefit for integrity checking but a weakness for password storage, because fast hashing makes large-scale password guessing easier once an attacker has the verifier set. `bcrypt` was therefore selected because it is purpose-built for password verification, salted by design, and more defensible in the context of credential protection. The report should still state the residual limitation clearly: password hashing improves stored-verifier confidentiality, but it does not by itself prevent phishing, password reuse, or compromise at the user endpoint.

## 5.2 Symmetric encryption choice

For symmetric encryption, the project compares AES as the family-level choice and selects `AES-GCM` as the specific operational mode. This distinction matters because choosing “AES” alone is not yet a complete engineering decision: the mode determines whether the encrypted data also gains ciphertext-integrity protection and how easy the implementation is to misuse. `AES-GCM` is the stronger fit in EduSecure because it provides authenticated encryption, which means it protects confidentiality while also detecting tampering with the encrypted payload.

This choice is justified more strongly when compared with weaker or less suitable alternatives. ECB is unsuitable because it leaks structure, while CBC requires additional integrity protection and is easier to misuse incorrectly. In EduSecure, `AES-GCM` is not treated as an abstract textbook algorithm; it appears in two concrete business roles already evidenced in the repository: protection of recoverable MFA secrets at rest and protection of stored submission content at rest. That gives the control stronger report value because the discussion is anchored in real application assets rather than a retired standalone crypto demonstration.

The wording must remain precise. `AES-GCM` should be described as an application-level confidentiality-at-rest control for specific sensitive assets. It should not be expanded into claims of whole-database encryption, end-to-end encrypted uploads, or enterprise-grade key-management maturity unless separate evidence is added.

## 5.3 Encrypted-chat key-distribution and message-protection choice

The encrypted space-chat workflow needs its own selection argument because it protects a different asset class from the submission and MFA slices. The design problem is not only how to encrypt stored data, but how to let multiple authorised participants recover a shared chat key without handing plaintext room keys to the backend. EduSecure therefore uses browser-generated `ECDH P-256` device keys, derives per-recipient wrap keys with `HKDF-SHA-256`, and uses `AES-GCM` both to wrap per-space room keys and to encrypt the message payloads themselves.

This combination is justified by the current repository constraints. `ECDH P-256` is available through the browser Web Crypto API and is easier to justify in a coursework setting than introducing a larger dependency-heavy messaging stack. The per-space room-key model is also more practical than encrypting every message separately to every recipient because it keeps ciphertext envelopes smaller, fits the existing REST and polling workflow, and makes history retrieval more manageable. A useful academic contrast is with server-side plaintext chat or a naive "encrypt the whole message separately for each participant" approach: the first would fail the confidentiality objective, while the second would add complexity without a clear benefit for the bounded artefact.

The report should still acknowledge the limitations honestly. The implementation remains a browser-served E2EE model rather than a fully zero-trust messaging system, the current room-key retrieval flow is centred on the active key version, and metadata such as sender identity, space membership, and message length remains visible to the backend. Those caveats do not weaken the selection argument; they define the protection boundary accurately.

## 5.4 Asymmetric and signature choice

For digital-signature logic and proof-of-authorship discussion, the strongest compare-and-select frame is RSA versus ECC. RSA remains a valid and academically familiar comparison point, and it should still appear in the report because the assignment brief expects asymmetric-cryptography comparison. However, the implemented EduSecure artefact now uses ECC combined with `SHA-256` in its signing workflow, which is a defensible modern choice for a bounded study-project design.

The main justification for ECC in this report is that it provides a more modern asymmetric-signature narrative with smaller key material while still remaining implementable using standard Java cryptography support. More importantly, the repository evidence now supports that narrative through a stable configured demo signing keypair, which improves repeatability and avoids the weakness of presenting ephemeral runtime-generated signing material as if it were a durable identity system. That improvement makes the signing evidence clearer without changing the scope boundary: the current model still demonstrates digital-signature creation, verification, and tamper-evidence logic within a controlled backend simulation rather than a full enterprise PKI or user-held private-key infrastructure.

The report should make that limitation explicit because it strengthens credibility. A bounded but clearly evidenced ECC signing workflow is academically stronger than a vague or overclaimed PKI story that the repository does not actually prove.

## 5.5 Hashing and MAC choice

EduSecure uses both `SHA-256` and `HMAC-SHA-256`, but it uses them for different reasons and in different trust contexts. `SHA-256` is selected for digest generation because it creates a stable fingerprint of submitted plaintext content and supports integrity checking when content is recomputed later. This makes it well suited to submission hashing and to preparing the digest material used by the signing workflow.

However, a plain digest is not enough for every integrity problem in the system. Audit integrity requires stronger protection because the question is not only whether a value changed, but whether the record has protected integrity within the trusted application boundary. `HMAC-SHA-256` is therefore selected for audit-record protection because it adds a keyed integrity check and is more appropriate than a plain hash when a shared secret is available and controlled by the backend. This distinction is worth emphasising in the report because it shows that multiple integrity-related primitives can coexist without being redundant: the digest supports tamper detection and signature preparation, while HMAC supports tamper-evident internal audit records.

## 5.6 Transport security design choice

Transport security is also part of the cryptographic selection story, but it must be phrased more carefully than the controls directly evidenced in code and tests. In EduSecure, `TLS 1.3` via Certbot/Let’s Encrypt is the intended deployment-side control for protecting credentials, cookies, and API traffic in transit. This directly answers the public-Wi‑Fi token-interception incident described in the brief and provides the correct infrastructure-layer response to network eavesdropping and MITM risk.

The key wording boundary is that TLS should be presented as a secure-design and deployment control unless direct HTTPS deployment evidence is included with the report. This keeps the discussion honest. The main implementation proof for symmetric cryptography remains the repository-evidenced `AES-GCM` at-rest controls, while TLS remains the documented transport control for a secure deployed posture.

## 5.7 Selection synthesis

Overall, the final EduSecure control set is coherent because each selected mechanism corresponds to a clearly different security need. `bcrypt` protects stored password verifiers; TOTP strengthens authentication assurance; `AES-GCM` protects MFA secrets and submission content at rest; browser-side chat E2EE combines `ECDH P-256`, `HKDF-SHA-256`, and `AES-GCM` to protect collaboration messages and room-key distribution; `SHA-256` supports digest-based integrity evidence; ECC-based signatures strengthen submission tamper-evidence and authorship logic; `HMAC-SHA-256` protects audit integrity; and TLS 1.3 is the intended control for transmission security in deployment. This is a stronger report position than presenting a single algorithm as a universal solution, because the case study itself contains multiple distinct risk types.

The report should also state explicitly that the chosen controls were selected not only because they are technically valid, but because they fit the project’s bounded scope. A full production PKI, enterprise-grade key-management stack, or deployment-maturity claim would add complexity without equivalent evidential strength in the repository. By contrast, the selected mix is defensible, evidenced, and appropriately scoped for the EduSecure artefact.

[TABLE PLACEHOLDER — Optional Table X. Control-to-risk mapping
Source: writer-created summary using `crypto-decision-matrix.md`, Section 3 risk discussion, and `final-cryptography-claims-matrix.md`
Purpose: show how each chosen control maps back to a specific EduSecure risk from the brief
Placement: after the final synthesis paragraph
Priority: optional
Status: insert only if it removes repeated prose rather than duplicating the risk section]

[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Optional Figure X. Concise implementation evidence for a selected control
Source: one small, readable capture from `frontend/src/services/chatCrypto.ts`, `backend/src/main/java/edusecure/edusecure/service/auth/MfaSecretCryptoService.java`, `backend/src/main/java/edusecure/edusecure/service/submission/SubmissionContentEncryptionService.java`, or `backend/src/main/java/edusecure/edusecure/audit/AuditService.java`
Purpose: support one high-value implementation statement without turning the section into a code dump
Placement: appendix first; only keep in the main body if it replaces prose effectively
Priority: appendix
Status: capture only if needed]

## Safe wording reminders for this section

Prefer wording such as:
- "EduSecure selects... because..."
- "The repository evidences..."
- "Within the study-project scope..."
- "This control materially reduces the risk of..."
- "The encrypted-chat stack is selected separately because it protects a different workflow..."
- "TLS 1.3 is documented as the intended deployment-side transport control..."

Avoid wording such as:
- "fully secure"
- "production-ready"
- "complete non-repudiation"
- "the chat system guarantees zero-trust secrecy"
- "the database is encrypted"
- "the repository proves deployed HTTPS"
- "enterprise PKI"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. the optional control-to-risk table
2. the optional code artefact screenshot
3. repeated explanation of why TLS is different from AES-GCM after the first clear statement
4. repeated RSA-versus-ECC comparison sentences once one clear paragraph is retained

Keep until the end:
- one opening compare-and-select paragraph
- the main algorithm comparison table placeholder
- the chat E2EE selection paragraph explaining `ECDH`, `HKDF-SHA-256`, and per-space `AES-GCM` room keys
- the bounded wording around ECC and TLS
- the clear distinction between `SHA-256` and `HMAC-SHA-256`
- the explanation that `AES-GCM` appears in multiple real business roles in EduSecure


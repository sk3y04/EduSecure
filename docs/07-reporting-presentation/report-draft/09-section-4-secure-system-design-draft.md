# Section 4 Draft — Secure System Design

This file is a **report-ready working draft** for Section 4.

Use it together with:
- `docs/07-reporting-presentation/report-diagram-figure-map.md`
- `docs/02-architecture-crypto/uml-refresh-assessment.md`
- `docs/07-reporting-presentation/final-doc-alignment-summary.md`
- `docs/02-architecture-crypto/current-state-data-flow-diagram.md`
- `docs/02-architecture-crypto/space-chat-e2ee-design.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/07-reporting-presentation/report-section-to-evidence-map.md`
- `docs/07-reporting-presentation/report-draft/04-report-visual-allocation-plan.md`
- `docs/07-reporting-presentation/report-draft/05-report-visual-placeholder-workpack.md`
- `docs/07-reporting-presentation/report-draft/06-report-core-vs-appendix-evidence-budget.md`

## How to use this draft

- keep the section diagram-led rather than prose-heavy
- keep only the **core workflow figures** in the main body unless space remains
- treat use-case, insecure-baseline, DFD, and class-model additions as optional or appendix-first under word pressure
- interpret each diagram briefly instead of inserting figures without commentary
- merge the strongest final text into `docs/07-reporting-presentation/final-report-draft-sections-1-to-5.md`

## 4. Secure System Design

The secure-system design for EduSecure should be presented as a direct response to the insecure baseline described in the assignment brief rather than as a generic architecture discussion. The brief identifies weak password handling, insecure transmission, tamperable submissions, vulnerable grade handling, and missing accountability for sensitive actions. The secure design therefore strengthens the system at the points where trust is most likely to fail: authentication, submission integrity and confidentiality, grade-sensitive workflows, protected collaboration messaging, and transport assumptions.

[FIGURE PLACEHOLDER — Optional Figure X. Security-focused use-case diagram
Source: `docs/02-architecture-crypto/uml/current-state/use-case-security-focused.puml`
Purpose: give a concise overview of the main protected interactions before the design details begin
Placement: after the opening paragraph of Section 4
Priority: optional
Status: export needed]

If space permits, this overview figure helps the marker see that the design story is centred on authentication, MFA, secure submission, audited content retrieval, and grade handling rather than on a full LMS feature map.

## 4.1 Insecure baseline

The insecure baseline is important because the later controls only make sense when tied back to the original failures. In that baseline, passwords are stored unsafely, client-server traffic is exposed to interception, submissions can be modified before review, grade-sensitive actions can occur without trustworthy accountability, and the wider platform trust model is weakly defined. A strong Section 4 opening therefore describes the insecure state briefly, then shows how the later secure design constrains those failures with clearer trust boundaries and targeted cryptographic controls.

[FIGURE PLACEHOLDER — Optional Figure X. Insecure deployment baseline
Source: `docs/02-architecture-crypto/uml/foundation/deployment-insecure.puml`
Purpose: show the weak transport and trust-boundary assumptions in the brief before the secure controls are applied
Placement: after the insecure-baseline paragraph
Priority: optional
Status: export needed]

[FIGURE PLACEHOLDER — Figure X. Secure deployment comparison
Source: `docs/02-architecture-crypto/uml/foundation/deployment-secure.puml`
Purpose: show improved trust boundaries, protected session handling, and deployment-side transport assumptions in the secure design
Placement: immediately after the transition from insecure baseline to secure design
Priority: core
Status: export needed]

The secure deployment comparison should be interpreted as a design-level explanation of how browser clients, the backend, data stores, and deployment transport controls relate to one another. It should not be described as proof of a live production deployment. The safe wording is that the UML represents bounded design logic, while implementation-specific details are corroborated elsewhere by evidence notes and tests.

## 4.2 Secure authentication design

The authentication design begins by replacing plaintext password handling with `bcrypt`, but it does not stop there. EduSecure now models authentication as a staged process: password verification occurs first, MFA is enforced for accounts that enable it, and the authenticated browser session is established only after the full login flow succeeds. This is stronger than a design that returns a usable session immediately after password verification, because MFA-enabled users do not receive a fully authenticated session until second-factor validation completes.

The design is also clearer about the browser trust boundary. Instead of relying on frontend-managed JWT storage, EduSecure uses a backend-issued `HttpOnly` authentication cookie and credentialed frontend requests. The corrected documentation and UML now align on this point, which is important because older bearer-token wording would misrepresent the actual browser-session model. Within the current design, MFA challenges remain browser-mediated, pending challenge state is limited, and the session boundary is intentionally backend-centred.

[FIGURE PLACEHOLDER — Figure X. Secure login sequence
Source: `docs/02-architecture-crypto/uml/foundation/sequence-login-secure.puml`
Purpose: show password verification, MFA branching, and session establishment only after successful authentication completes
Placement: after the authentication design paragraph
Priority: core
Status: export needed]

A short interpretation sentence should follow the figure in the final report: the diagram shows that authentication is not complete until the secure login sequence finishes, which strengthens the report’s explanation of password protection, MFA, and cookie-backed session establishment.

## 4.3 Secure submission design

The submission design is the strongest integrity-focused part of the current architecture. In the implemented model, uploaded content is processed through a backend-centred workflow in which the plaintext is hashed, signature logic is applied, verification state is stored, and the content is then protected at rest before durable storage. This is a more credible design story than a vague claim that files are simply “secured,” because it distinguishes integrity evidence, authorship logic, metadata visibility, ciphertext storage, and audited plaintext retrieval.

The refreshed UML and design notes also tighten the scope boundary around submission signing. The report should describe the current model as a **bounded study-project signing simulation** using a stable configured demo ECC keypair. That keeps the design technically honest while still allowing the report to explain why digest generation, signature verification, metadata/content separation, and audited retrieval materially improve the original insecure workflow.

[FIGURE PLACEHOLDER — Figure X. Secure submission sequence
Source: `docs/02-architecture-crypto/uml/submissions-audit/sequence-submission-secure-pack04.puml`
Purpose: show digest generation, signature processing, encrypted-at-rest storage, and the separate metadata/content handling path
Placement: after the main submission design paragraph
Priority: core
Status: export needed]

[FIGURE PLACEHOLDER — Optional Figure X. Submission integrity class model
Source: `docs/02-architecture-crypto/uml/submissions-audit/class-diagram-submission-addendum.puml`
Purpose: show how submission metadata, verification state, grade linkage, and audit-relevant fields relate at the domain level
Placement: after the submission sequence discussion if a domain-model visual is needed
Priority: appendix
Status: export needed only if it removes repeated prose]

If a class-model figure is used, it should be presented as an implementation-simplified design view rather than a field-for-field ORM dump. If the section is already heavy with diagrams, this figure should move to the appendix first.

## 4.4 Grade-integrity design

The grade-integrity design builds on the earlier submission logic rather than existing as a separate isolated feature. In EduSecure, grade creation and update are restricted to authorised teaching roles, student grade visibility is bounded by the current space/assignment visibility rules, and only verified submissions may be graded. This means grading is treated as an integrity-sensitive workflow rather than a simple database write.

The design also links grade handling to accountability. Sensitive grade actions produce audit records protected by HMAC-backed integrity values, so the architecture supports not only prevention through access control but also later investigation through tamper-evident internal audit evidence. This is an important marker-facing point because it shows the design thinking beyond permission checks alone.

[FIGURE PLACEHOLDER — Figure X. Grade integrity sequence
Source: `docs/02-architecture-crypto/uml/grades-and-history/sequence-grade-integrity-secure-pack05.puml`
Purpose: show verified-submission gating, owner-scoped lecturer access, admin override, and audit-backed grade handling
Placement: after the grade-integrity paragraph
Priority: core
Status: export needed]

A safe interpretation here is that the diagram represents the implemented grade-integrity logic at a bounded design level. It should not be expanded into claims of a full moderation workflow, appeal process, or enterprise academic-record system unless separate evidence is included.

## 4.5 Encrypted space-chat design

EduSecure also includes a distinct secure-communication workflow in the form of browser-side encrypted space chat. This matters because the earlier submission and grade controls mainly protect stored academic artefacts, whereas the chat design addresses the confidentiality of collaboration content during routine platform use. In the implemented model, user devices generate local `ECDH P-256` key pairs, only the public keys are registered with the backend, a manager-capable client publishes a per-space `AES-GCM` room key wrapped separately for each authorised participant, and message payloads are encrypted in the browser before ciphertext storage in MongoDB.

The main design benefit is that routine backend handling no longer requires plaintext chat bodies when encrypted mode is active. The backend still enforces membership, key-version state, and message pagination, but message decryption is pushed to the client boundary instead of the application server. This gives Section 4 an additional trust-boundary story that is genuinely different from the submission workflow: confidentiality is improved for one collaboration channel, yet the report must still state that metadata remains visible and that a browser-served web application cannot claim a zero-trust messaging model against a server that can alter delivered frontend code.

[FIGURE PLACEHOLDER — Figure X. Space chat E2EE sequence
Source: `docs/02-architecture-crypto/uml/academic-workflows/sequence-space-chat-e2ee-implemented.puml`
Purpose: show browser-side key generation, room-key publication, recipient-specific key wrapping, ciphertext message send, and local decryption after fetch
Placement: after the encrypted-chat design paragraph
Priority: core if the report treats chat as a major differentiator; otherwise appendix
Status: export needed]

The interpretation sentence for this figure should be simple: it shows that the chat design reduces normal backend plaintext exposure by moving key handling and message decryption to the browser, while still retaining honest membership and delivery trust assumptions.

## 4.6 Secure transmission design

Secure transmission in EduSecure is handled at the deployment-design layer through `TLS 1.3` via Certbot/Let’s Encrypt rather than through a standalone application-layer encryption endpoint. This is the correct design response to the brief’s intercepted-token and public-Wi‑Fi scenario, because credentials, session cookies, and application traffic should be protected in transit by the transport boundary itself. The report should therefore keep the transmission story separate from the application-level `AES-GCM` controls, which operate in the current codebase only for MFA secrets and submission content at rest.

This wording boundary matters because the repository no longer uses the retired standalone symmetric-transport slice as active evidence. The strongest direct implementation evidence in this area remains the browser-session model, the operations/deployment notes, and the surrounding auth-cookie configuration posture. If direct HTTPS deployment evidence is not included in the final submission, TLS should be described as the intended transport control rather than as repository-proven runtime enforcement.

[FIGURE PLACEHOLDER — Optional Figure X. Current-state context or level-1 DFD
Source: `docs/02-architecture-crypto/uml/current-state/dfd-context-current-state.puml` or `docs/02-architecture-crypto/uml/current-state/dfd-level-1-current-state.puml`
Purpose: show current trust boundaries and sensitive data movement at a higher level than the sequence diagrams
Placement: after the transport or architecture-bridge paragraph if one extra marker-friendly architecture figure is needed
Priority: appendix
Status: export only if the report needs one DFD to simplify the design story]

If a DFD is used in Section 4, keep it to one figure only. The level-1 DFD is generally stronger than the context DFD if you need to show where authentication data, MFA material, submission plaintext, ciphertext, grades, and audit records move through the system.

## Mini-conclusion

Taken together, the secure-system design for EduSecure is not a loose collection of controls but a coherent response to the original trust failures in the brief. Authentication is strengthened before session establishment, submissions gain integrity/authorship evidence and protected-at-rest storage, grade handling is constrained by verification and role boundaries, encrypted space chat reduces routine backend plaintext exposure for collaboration content, and transport protection is addressed through a clear deployment-side TLS posture. This makes the architecture section strongest when it explains what each figure proves rather than merely listing the components involved.

## Safe wording reminders for this section

Prefer wording such as:
- "The UML artefacts represent the security design and main interaction logic of EduSecure..."
- "The current-state DFD supplements the sequence diagrams by showing trust boundaries and sensitive-data movement..."
- "Within the study-project scope..."
- "Transport security is documented as the intended deployment-side control..."
- "Public audit review remains deferred..."

Avoid wording such as:
- "the diagrams exactly mirror the final codebase"
- "the diagrams prove production deployment"
- "the chat sequence proves a zero-trust messaging model"
- "EduSecure implements a full enterprise PKI"
- "the repository proves deployed HTTPS from local tests alone"
- "the design includes a live public audit-review API"
- "the retired standalone AES transport demo remains active evidence"

## Trim-first notes if the word count becomes tight

Cut in this order:
1. the optional use-case diagram
2. the optional insecure deployment diagram once the secure deployment figure remains
3. the optional DFD
4. the optional class-diagram placeholder
5. repeated explanation of cookie transport once the login-sequence paragraph is clear

Keep until the end:
- the secure deployment comparison figure placeholder
- the secure login sequence figure placeholder
- the secure submission sequence figure placeholder
- the grade-integrity sequence figure placeholder
- the encrypted space-chat sequence placeholder if chat is discussed in the main body
- one clear sentence distinguishing TLS transport design from AES-GCM at-rest controls
- the safe wording boundary that diagrams are implementation-simplified design artefacts rather than exact code mirrors


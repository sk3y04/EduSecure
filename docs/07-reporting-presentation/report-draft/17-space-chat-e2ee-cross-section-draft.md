# Space Chat E2EE Cross-Section Draft Pack

This file is a **report-ready insert pack** for the implemented EduSecure end-to-end encrypted space-chat slice.

Use it together with:
- `docs/02-architecture-crypto/space-chat-e2ee-design.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/02-architecture-crypto/uml/academic-workflows/sequence-space-chat-e2ee-implemented.puml`
- `frontend/src/services/chatCrypto.ts`
- `frontend/src/services/chatKeyStore.ts`
- `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- `backend/src/main/java/edusecure/edusecure/service/spacechatkey/SpaceChatE2eeService.java`
- `backend/src/main/java/edusecure/edusecure/service/spacechat/MongoSpaceChatService.java`

## Why this note exists

Space chat E2EE is too important to leave as a side mention, but it also does **not** need a standalone Section 10 in the final report. The strongest approach is to weave it through the existing report structure:
- **Section 2** explains what the chat cryptography does
- **Section 4** shows the secure chat workflow and trust boundary
- **Section 5** justifies why the chat stack uses `ECDH P-256`, `HKDF-SHA-256`, per-space `AES-GCM` room keys, and `AES-GCM` message encryption
- **Section 6** explains the implementation trade-offs such as browser key storage, nonce generation, and single-device-first scope
- **Section 7** evaluates the confidentiality and integrity gains against the remaining metadata and browser-delivery limits
- **Section 8** proves the feature is implemented with concrete frontend, backend, schema, and test evidence

## Core report claim

EduSecure now includes an implemented browser-side encrypted space-chat workflow in which each user generates a local `ECDH P-256` key pair, only the public key is registered with the backend, a per-space `AES-GCM` room key is created and wrapped separately for each authorised participant using `ECDH`-derived `HKDF-SHA-256` wrap keys, and chat messages are encrypted in the browser before storage as ciphertext envelopes in MongoDB. This gives the report a distinct end-to-end-encrypted communication slice that is separate from the submission and MFA cryptography already discussed elsewhere.

## Best insert points by section

## Section 2 — Role of cryptography

Use a short paragraph after the symmetric-encryption explanation.

Paste-ready wording:

> EduSecure also applies cryptography to protected communication content through its encrypted space-chat workflow. In that slice, browser-generated `ECDH P-256` device keys are used to derive per-recipient wrap keys via `HKDF-SHA-256`, while a per-space `AES-GCM` room key encrypts the actual message payloads. This is an important addition to the report because it shows that the project uses cryptography not only for password protection, signatures, and at-rest secrecy, but also for browser-side ciphertext messaging within a bounded academic collaboration workflow.

## Section 4 — Secure system design

Use one short design paragraph plus one figure placeholder.

Paste-ready wording:

> A further secure-design improvement appears in EduSecure space chat, where confidentiality is moved away from routine backend plaintext handling and into the browser client. The implemented model uses browser-held device keys, per-space wrapped room keys, and ciphertext message envelopes so that the backend can still enforce membership and persist messages without learning plaintext content during normal operation. This design materially reduces database and backend plaintext exposure, while still retaining honest trust-boundary limits because the system is delivered as a browser-served web application.

[FIGURE PLACEHOLDER — Figure X. Space chat E2EE implemented sequence
Source: `docs/02-architecture-crypto/uml/academic-workflows/sequence-space-chat-e2ee-implemented.puml`
Purpose: show browser key setup, room-key publication, ciphertext message send, wrapped-key retrieval, and local decryption
Placement: after the encrypted-chat design paragraph in Section 4
Priority: core if chat is discussed in the main body; otherwise appendix
Status: export needed]

## Section 5 — Cryptographic controls and selection justification

Use a compare-and-select paragraph focused on why this stack fits the current repository.

Paste-ready wording:

> The encrypted-chat slice also required a separate selection argument because direct plaintext chat storage would contradict the project’s confidentiality goals, yet a heavyweight messaging protocol would exceed the scope of a browser-served coursework artefact. EduSecure therefore uses browser-compatible `ECDH P-256` for shared-secret derivation, `HKDF-SHA-256` for wrap-key derivation, and per-space `AES-GCM` room keys for efficient message encryption. This is more suitable than leaving the backend to handle plaintext chat, and more practical for the current Vue/Web Crypto stack than introducing a larger dependency-heavy protocol redesign. The report should still state the remaining limitation clearly: this is meaningful browser-side encrypted messaging, not a zero-trust guarantee against a malicious server that can alter delivered frontend code.

## Section 6 — Implementation plan and considerations

Use a practical engineering paragraph on browser storage and lifecycle trade-offs.

Paste-ready wording:

> The encrypted-chat implementation adds a different class of engineering concern from the backend-only controls. The user’s private `ECDH` key remains on the client device in IndexedDB-backed browser storage, decrypted room keys are cached locally, and only public-key plus wrapped-key metadata is stored server-side. This makes the implementation more defensible from a confidentiality perspective, but it also introduces bounded trade-offs around browser support, single-device-first key lifecycle, nonce generation discipline, and the fact that legacy plaintext chat history may coexist during rollout.

## Section 7 — CIA evaluation

Use this mainly in the confidentiality subsection.

Paste-ready wording:

> The implemented encrypted-chat workflow materially strengthens confidentiality for one important collaboration channel because message content is encrypted in the browser and stored in MongoDB as ciphertext envelopes rather than plaintext bodies. However, the improvement is bounded rather than absolute: the backend still sees metadata such as sender, space, time, and plaintext length, and the browser-served delivery model means frontend integrity remains a trust assumption.

## Section 8 — Technical artefact summary

Use this as a distinct implemented slice.

Paste-ready wording:

> A particularly distinctive implemented slice is EduSecure’s end-to-end encrypted space chat. The repository now evidences browser-side `ECDH P-256` key generation, public-key registration, per-space room-key publication, recipient-specific room-key wrapping with `ECDH + HKDF-SHA-256 + AES-GCM`, ciphertext message storage in MongoDB, local decryption in the Vue chat UI, and rekey enforcement when membership changes. This gives the artefact a stronger technical profile because it proves a multi-step communication-security workflow rather than only at-rest protection of backend-managed assets.

## Best visual candidates

[TABLE PLACEHOLDER — Table X. Space chat E2EE cryptographic stack summary
Source: writer-created summary based on `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
Purpose: compress the roles of `ECDH P-256`, `SHA-256` fingerprints, `HKDF-SHA-256`, `AES-GCM` room-key wrapping, `AES-GCM` message encryption, and audit-integrity chaining
Placement: Section 5 if the general algorithm-comparison table becomes too crowded; otherwise appendix
Priority: optional
Status: not inserted yet]

[FRONTEND SCREENSHOT PLACEHOLDER — Figure X. Encrypted space-chat UI evidence
Source: `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
Purpose: show visible encrypted-chat states such as key setup, rekey-required state, encrypted message send, or decrypt-on-read behavior
Placement: Section 8
Priority: core if chat is one of the chosen artefact screenshots
Status: capture needed]

[CODE ARTEFACT SCREENSHOT PLACEHOLDER — Figure X. Browser cryptography helper evidence for encrypted chat
Source: `frontend/src/services/chatCrypto.ts`
Purpose: support the claim that the E2EE workflow is implemented with Web Crypto rather than only documented in design notes
Placement: appendix first or Section 8 if one compact code capture replaces prose effectively
Priority: appendix
Status: capture only if needed]

## Strongest evidence anchors to cite

- `frontend/src/services/chatCrypto.ts`
- `frontend/src/services/chatKeyStore.ts`
- `frontend/src/services/spaceChat.ts`
- `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- `backend/src/main/java/edusecure/edusecure/service/spacechatkey/SpaceChatE2eeService.java`
- `backend/src/main/java/edusecure/edusecure/service/spacechat/MongoSpaceChatService.java`
- `backend/src/main/java/edusecure/edusecure/entity/spacechatkey/`
- `backend/src/main/resources/db/changelog/changes/011-space-chat-e2ee-metadata.yaml`
- `backend/src/main/resources/db/changelog/changes/012-space-chat-e2ee-publisher-key.yaml`
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/03-features/academic-workflows/space-chat-e2ee-crypto-justification-table.md`
- `docs/02-architecture-crypto/uml/academic-workflows/sequence-space-chat-e2ee-implemented.puml`

## Safe wording reminders

Prefer:
- "browser-side encrypted space chat"
- "backend-blind message bodies during normal operation"
- "bounded end-to-end-encrypted workflow in a browser-served application"
- "metadata confidentiality remains limited"
- "browser delivery remains part of the trust model"

Avoid:
- "perfectly secure messaging"
- "complete metadata secrecy"
- "the server can never influence chat confidentiality"
- "full Signal-style threat model"
- "multi-device encrypted chat is fully solved"


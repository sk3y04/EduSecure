# Space Chat E2EE Cryptography Justification Table

## Status and purpose

This document is a companion to:
- `docs/03-features/academic-workflows/space-chat-e2ee-implementation.md`
- `docs/02-architecture-crypto/space-chat-e2ee-design.md`

Its purpose is to justify the cryptographic mechanisms that are **actually implemented** for EduSecure space chat end-to-end encryption (E2EE).

It is intentionally brief, report-friendly, and implementation-grounded.

## Scope note

This table covers the cryptographic mechanisms currently used in the codebase for:
- device key generation
- public-key fingerprinting
- room-key generation
- room-key wrapping and unwrapping
- message encryption and decryption
- integrity protection of chat-related audit records

It does **not** claim that all messaging-security properties are fully solved. Current limitations such as browser trust, single active device key, and metadata visibility still apply and are documented in `space-chat-e2ee-implementation.md`.

## Cryptography justification table

| Mechanism | Implemented algorithm / primitive | Where used | Security purpose | Why this choice fits EduSecure | Important implementation notes |
|---|---|---|---|---|---|
| user chat key pair | `ECDH` on `P-256` | browser-side user/device key generation | establishes a device-held asymmetric key pair for deriving shared secrets without exposing the private key to the server | supported by the Web Crypto API and standard Java crypto stack; avoids adding a large extra crypto dependency; is explainable in a coursework setting | generated client-side in `frontend/src/services/chatCrypto.ts`; only the public key is registered with the backend |
| public-key fingerprinting | `SHA-256` over canonicalized JWK JSON | user key registration and local/server key-state comparison | provides a stable identifier for the active public key so the client can detect whether the local device key matches the server-registered key | SHA-256 is standard, widely available, and sufficient for fingerprinting; canonical JSON prevents unstable field-order effects | this fingerprint is an identifier, not a secret; it is stored and displayed as metadata |
| room-key generation | `AES-GCM` 256-bit symmetric key | one room key per space key version | provides efficient shared-content encryption for space chat messages | symmetric encryption is much more efficient than encrypting every message separately for every recipient; AES-GCM is already preferred elsewhere in the project | room keys are generated in the browser and stored locally after publication or unwrap |
| shared-secret derivation | `ECDH` shared secret + `HKDF-SHA-256` | deriving a per-recipient wrap key | transforms the raw ECDH shared secret into a purpose-specific wrapping key suitable for AES-GCM | HKDF is the standard way to derive a strong context-bound symmetric key from shared secret material | implementation uses empty salt and the fixed info string `EduSecure Space Chat Room Key Wrap v1`; this is simple and consistent, though more context separation could be added later |
| room-key wrapping | `AES-GCM` | manager wraps the room key separately for each recipient | protects the room key in transit/storage so only the intended recipient can unwrap it with their private key | AES-GCM provides confidentiality and integrity in one primitive and is already supported by Web Crypto | wrap nonces are randomly generated per recipient package; the publisher public key is stored with the key version so recipients can derive the same wrap key |
| room-key unwrapping | `ECDH` + `HKDF-SHA-256` + `AES-GCM` | recipient-side room-key recovery | lets a participant recover the shared room key without the backend learning that key | mirrors the publish-side derivation and keeps room-key plaintext client-side only | implemented in `unwrapRoomKeyFromPublisher()`; requires the publisher public key plus wrapped ciphertext and nonce |
| message encryption | `AES-GCM` 256-bit | browser-side encryption before chat message send | protects message confidentiality and integrity from the backend, database disclosure, and routine storage exposure | strong standard AEAD primitive; available in browser Web Crypto; efficient for short text messages | implemented in `encryptSpaceChatMessage()`; the backend stores only the ciphertext envelope for encrypted messages |
| message integrity binding | AES-GCM authenticated additional data (AAD) over `spaceId`, `keyVersion`, and `contentType` | encryption and decryption of chat messages | binds important message metadata to the ciphertext so tampering causes decryption failure | simple, explainable, and directly supported by AES-GCM; protects contextual correctness without extra protocol complexity | the current implemented AAD set is narrower than the earlier design proposal; documentation should reflect the code rather than the larger planned set |
| nonce generation | `crypto.getRandomValues(...)` with 12-byte nonces | room-key wrapping and message encryption | ensures nonce uniqueness and safe AES-GCM operation | 12-byte nonces are the standard practical choice for GCM and are directly supported by Web Crypto | nonce reuse with the same key would be dangerous, so fresh random nonces are generated for each wrap and each message encryption |
| ciphertext / binary transport encoding | base64url | transport/storage of fingerprints, nonces, ciphertext, and wrapped keys | converts binary crypto outputs into JSON-safe API fields | base64url is compact, URL-safe, and practical for JSON APIs | used consistently for wrapped room keys and encrypted message envelopes |
| audit-log tamper evidence for chat events | HMAC-style integrity chaining through the existing audit service | audit records for key publication and message creation | provides tamper-evident integrity for security-sensitive audit events even though audit logging is separate from message encryption | reuses the existing project audit-integrity mechanism rather than inventing a chat-specific logging scheme | protects audit trail integrity, not message confidentiality; audit details are intentionally metadata-only |

## Why these choices work together

The implemented E2EE model uses a layered approach:

1. `ECDH P-256` gives each device a browser-held key pair.
2. `SHA-256` fingerprints help the client detect whether the current device key matches the registered key.
3. A random `AES-GCM` room key encrypts actual message content efficiently.
4. `ECDH + HKDF-SHA-256` derives a different symmetric wrap key for each recipient.
5. `AES-GCM` wraps the room key for each recipient and encrypts each message payload.
6. AES-GCM AAD binds core chat metadata so modified metadata causes decryption failure.
7. The audit-integrity chain protects evidence of key publication and encrypted message creation without storing secrets in audit details.

This combination is appropriate for EduSecure because it keeps cryptography:
- standard
- explainable
- browser-compatible
- efficient enough for space chat
- separated between key distribution and message encryption

## Summary wording for reports

Safe concise wording:

- "EduSecure space chat uses browser-generated ECDH P-256 device keys and per-space AES-GCM room keys."
- "Room keys are wrapped per recipient using ECDH-derived AES-GCM wrapping keys with HKDF-SHA-256."
- "Chat messages are encrypted client-side with AES-GCM and stored as ciphertext envelopes in MongoDB."
- "Message metadata binding is enforced with AES-GCM authenticated additional data."
- "Audit records for encrypted chat events remain metadata-only and are protected by the project’s existing audit-integrity chain."

Avoid over-claiming with wording such as:
- "perfectly secure messaging"
- "complete metadata secrecy"
- "zero-trust protection against a malicious server-delivered frontend"

## Current limitations relevant to justification

The cryptographic choices are strong enough for the implemented goals, but the current system still has important boundaries:
- it is a browser-served application, so frontend delivery remains a trust assumption
- the backend still sees metadata such as sender, space, time, and plaintext length
- there is currently one active chat key per user rather than seamless multi-device support
- legacy plaintext history may still coexist during the rollout period
- the current room-key retrieval flow is centered on the latest active key version

These limitations do not invalidate the implemented cryptographic choices; they define the scope of what those choices currently protect.


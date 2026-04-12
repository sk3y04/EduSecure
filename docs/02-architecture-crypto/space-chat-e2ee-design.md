# Space Chat End-to-End Encryption Design

## 0. Status and purpose

This document analyses how EduSecure can add end-to-end encryption (E2EE) to the existing per-space chat feature and prepares the repository for implementation.

It is intentionally implementation-oriented:
- it starts from the current verified chat architecture in the repository
- it proposes a concrete protocol and data model that fit the current Vue + Spring Boot + PostgreSQL + MongoDB stack
- it distinguishes clearly between what E2EE can protect and what it cannot protect in a browser-served web application
- it lists the exact backend, frontend, and schema touch points that should be changed next

This is a design and implementation-preparation document, not a claim that E2EE is already implemented.

## 1. Current verified chat baseline

The current space chat implementation is plaintext-at-rest and plaintext-in-backend-memory.

Verified anchors in the repository:
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceChatController.java`
- `backend/src/main/java/edusecure/edusecure/service/spacechat/MongoSpaceChatService.java`
- `backend/src/main/java/edusecure/edusecure/document/spacechat/SpaceChatMessage.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceAccessService.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
- `frontend/src/services/spaceChat.ts`
- `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- `frontend/src/pages/SpaceDetail/components/SpaceChatMessageList.vue`

Current baseline behaviour:
- chat is protected by the existing authenticated session model
- authorization is enforced through `SpaceAccessService`
- messages are stored in MongoDB under `space_chat_messages`
- message bodies are currently stored as plaintext `body`
- the frontend renders messages safely as plain text and avoids raw HTML injection
- archived spaces remain readable but reject new chat writes
- audit logging for chat creation is metadata-only and does not store the message body in the audit trail

This is a sound access-control baseline, but it is not E2EE because the server currently receives and stores plaintext chat bodies.

## 2. Goal and non-goals

### 2.1 Goal

The target design is:
- only authorized chat participants can decrypt message content
- the backend can still authenticate users, enforce space authorization, page messages, and store encrypted payloads
- MongoDB stores ciphertext rather than plaintext chat bodies
- PostgreSQL stores only public-key and wrapped-key metadata needed for access control and key distribution

### 2.2 Explicit non-goals for the first E2EE iteration

The first implementation should not try to solve all secure-messaging problems at once.

Out of scope for the first iteration:
- end-to-end encrypted file attachments
- message editing or deletion with encrypted history rewrite
- cryptographic search over message bodies
- perfect forward secrecy across all historical messages after a device compromise
- secure interoperability with native mobile apps
- zero-trust protection against a fully malicious server that can alter frontend JavaScript at delivery time
- seamless multi-device recovery without some explicit backup or re-registration flow

## 3. Threat model and trust assumptions

### 3.1 Threats the design should reduce

The E2EE design should reduce exposure from:
- accidental database disclosure of MongoDB chat content
- backend operators or logs seeing plaintext chat content during normal storage/retrieval flow
- plaintext message leakage via backup snapshots of the MongoDB chat datastore
- application bugs that would otherwise expose stored plaintext messages

### 3.2 Trust assumptions that remain

The design still depends on several trusted elements:
- the backend remains the authority for authentication and space membership
- the backend remains trusted to decide who may request encrypted chat records
- TLS/HTTPS is still required to protect traffic in transit
- client devices are assumed not to be compromised while encrypting/decrypting messages

### 3.3 Important limitation of browser-served E2EE

EduSecure is currently a browser-served web application. That means a server that can alter the frontend bundle can also alter the cryptographic logic delivered to the browser.

So this design meaningfully protects stored chat content and routine backend handling, but it does **not** provide the same trust model as an independently distributed messaging client with out-of-band key verification.

Safe wording for future documentation:
- "E2EE reduces backend/plaintext exposure for chat content"
- not: "the server can never influence chat confidentiality under any condition"

## 4. Recommended E2EE model

## 4.1 Recommendation summary

Recommended MVP protocol:
- browser-generated long-term user chat key pair for ECDH
- per-space versioned symmetric room key for message encryption
- message bodies encrypted client-side with `AES-GCM`
- room keys wrapped separately for each currently authorized participant
- backend stores only ciphertext messages and wrapped room-key packages
- membership changes trigger room-key rotation for future messages

This keeps the current REST + polling model intact while moving confidentiality to the client.

## 4.2 Why this model fits the current repository

It fits because EduSecure already has:
- stable per-space authorization in PostgreSQL
- a shared space identifier that can act as the chat room identifier
- MongoDB used only for chat payload storage
- a frontend that already loads/sends chat messages through one service layer
- existing cryptography documentation that already prefers `AES-GCM` and standard APIs

This model avoids trying to encrypt each message separately to every member, which would grow payload size quickly and complicate polling.

## 4.3 Algorithm choices

Recommended first-implementation choices:
- asymmetric key agreement: `ECDH` on `P-256`
- key derivation: `HKDF-SHA-256`
- content encryption: `AES-GCM` with a 256-bit room key
- room-key wrapping: derive a symmetric wrap key from ECDH + HKDF, then wrap the room key using `AES-GCM`
- message/room-key metadata integrity: rely on authenticated encryption and bind important metadata through AAD

### Why `P-256` ECDH is recommended here

For this repository, `P-256` ECDH is a practical first choice because:
- it is available through the Web Crypto API in browsers
- it is available through the standard Java cryptography APIs on the backend
- it avoids introducing a large extra frontend cryptography dependency for the first implementation
- it remains consistent with the broader repository preference for standard, explainable cryptography

Possible future refinement:
- move to `X25519` with a vetted library if the project later wants a more modern ECDH primitive and is comfortable adding a new dependency and compatibility checks

## 5. Key hierarchy and lifecycle

## 5.1 Long-term user chat key pair

Each user should have a long-term chat identity key pair generated client-side.

Recommended first implementation:
- generate the key pair on first successful authenticated chat use
- store the private key in browser storage controlled by the frontend, preferably IndexedDB
- upload only the public key and public metadata to the backend

Minimum stored metadata on the backend:
- `userId`
- `algorithm` such as `ECDH_P256`
- `publicKeyJwk` or equivalent encoded public key
- `keyFingerprint`
- `createdAt`
- `revokedAt` if rotation/revocation is later needed

### First-iteration tradeoff

For the first implementation, prefer **single-device or single-browser-profile support** rather than solving encrypted private-key backup immediately.

That means:
- the first browser/device that registers a chat key becomes the user’s active E2EE endpoint
- signing in from a second browser without a migrated private key should show a controlled UX message such as "Encrypted chat is not configured on this device yet"
- a later phase can add password-protected key backup/export or multi-device key registration

This constraint keeps the first implementation much smaller and safer.

## 5.2 Per-space room key

Each space chat should have a versioned symmetric room key.

Properties:
- random 256-bit key
- used only for one space
- rotated when membership changes in a way that affects future access
- identified by a monotonically increasing `keyVersion`

Why a room key is better than per-message recipient encryption here:
- efficient for polling and history pagination
- compact MongoDB message documents
- simpler decryption path in the Vue chat UI
- easier to reuse for future encrypted attachments if needed

## 5.3 Wrapped room keys

The backend must not know plaintext room keys.

For each active member, the client that creates or rotates a room key should:
1. fetch the current participant public keys for the space
2. derive a per-recipient wrap key using ECDH + HKDF
3. encrypt the room key for that recipient
4. upload the wrapped room key package to the backend

Each recipient later downloads only their own wrapped room key package and unwraps it locally with their private key.

## 6. Recommended membership and history rules

## 6.1 New member access rule

Recommended rule for the first encrypted version:
- new members can decrypt only messages encrypted after they joined the space and after the next room-key rotation

This is the safest and simplest rule.

Why:
- retroactively granting a new member access to old history requires old room keys to be re-distributed or history to be re-encrypted
- the current architecture does not keep old plaintext available for safe server-side migration
- requiring clients to rewrap every historical key version would make membership changes much more complex

## 6.2 Removed member rule

Recommended rule:
- removed members cannot fetch new wrapped room keys or new encrypted messages after removal
- they may still retain anything they already decrypted, cached, copied, or exported before removal

This is a standard limitation of E2EE systems and should be stated explicitly.

## 6.3 Archived space rule

The existing archived-space rule should remain:
- archived chat history is readable
- new messages are blocked
- no new room-key creation should happen for archived spaces except for narrowly controlled recovery/administration tooling if ever added later

## 6.4 Rekey trigger rule

The system should mark a space as requiring rekey when:
- a student is added to a space
- a student is removed from a space
- a lecturer/admin manually rotates encrypted chat keys after a security concern

Recommended first implementation:
- set `requiresRekey=true` in relational metadata when membership changes
- block sending encrypted messages until a new room key version has been published for the current member set

This is safer than silently continuing to use an old room key after access changed.

## 7. Message envelope design

The current `SpaceChatMessage.body` plaintext field should be replaced with an encrypted payload envelope.

Recommended MongoDB message shape:

```json
{
  "id": "uuid",
  "spaceId": "uuid",
  "authorUserId": "uuid",
  "authorDisplayName": "Student Example",
  "keyVersion": 3,
  "algorithm": "AES_GCM_256",
  "nonce": "base64url",
  "ciphertext": "base64url",
  "createdAt": "2026-04-12T12:34:56.789Z",
  "plaintextLength": 42,
  "contentType": "text/plain"
}
```

Notes:
- `authorDisplayName` can remain plaintext unless the project wants author identity to be confidential from the backend too
- keeping `authorDisplayName` plaintext preserves current UI and audit ergonomics
- `plaintextLength` is optional but useful for validation and UX limits; if stored, treat it as metadata leakage that is acceptable for MVP
- if stronger metadata minimisation is desired later, `authorDisplayName` and `plaintextLength` can be removed or encrypted too

### Recommended AAD

Bind these values as additional authenticated data (AAD) during AES-GCM encryption:
- `spaceId`
- `messageId`
- `authorUserId`
- `createdAt`
- `keyVersion`
- `contentType`

This ensures the ciphertext is cryptographically bound to the chat metadata expected by the client.

## 8. Relational schema additions

The current MongoDB chat collection is not the right place for authorization truth or key-distribution metadata.

Recommended PostgreSQL additions:

### 8.1 `user_chat_keys`

Purpose:
- store each user’s public E2EE chat key and key metadata

Suggested columns:
- `id UUID primary key`
- `user_id UUID not null references users(id)`
- `algorithm VARCHAR(32) not null`
- `public_key_jwk TEXT not null`
- `fingerprint VARCHAR(128) not null`
- `created_at TIMESTAMP WITH TIME ZONE not null`
- `revoked_at TIMESTAMP WITH TIME ZONE null`
- unique active key per user for the first iteration

### 8.2 `space_chat_key_versions`

Purpose:
- track room-key versions for each space

Suggested columns:
- `id UUID primary key`
- `space_id UUID not null`
- `key_version INTEGER not null`
- `created_by_user_id UUID not null references users(id)`
- `created_at TIMESTAMP WITH TIME ZONE not null`
- `rotation_reason VARCHAR(64) not null`
- `requires_rekey BOOLEAN not null default false`
- unique constraint on `(space_id, key_version)`

### 8.3 `space_chat_key_recipients`

Purpose:
- store one wrapped room key per active recipient for a specific key version

Suggested columns:
- `id UUID primary key`
- `space_chat_key_version_id UUID not null references space_chat_key_versions(id)`
- `recipient_user_id UUID not null references users(id)`
- `wrap_algorithm VARCHAR(32) not null`
- `wrap_nonce VARCHAR(255) not null`
- `wrapped_key_ciphertext TEXT not null`
- `created_at TIMESTAMP WITH TIME ZONE not null`
- unique constraint on `(space_chat_key_version_id, recipient_user_id)`

### 8.4 Why PostgreSQL should hold this metadata

Reasons:
- membership and authorization already live in PostgreSQL
- rekey decisions must be consistent with the space membership truth
- Liquibase is already the schema-management baseline for relational changes
- key-distribution metadata is structured and transactional enough to fit better in PostgreSQL than in the MongoDB chat document store

## 9. API design changes

The current message endpoints can remain, but they need encrypted payload support plus a small E2EE bootstrap surface.

## 9.1 Keep the existing message routes

Keep:
- `GET /api/spaces/{spaceId}/chat/messages`
- `POST /api/spaces/{spaceId}/chat/messages`

Change the POST body from plaintext to ciphertext metadata.

Suggested encrypted message create request:

```json
{
  "keyVersion": 3,
  "algorithm": "AES_GCM_256",
  "nonce": "base64url",
  "ciphertext": "base64url",
  "contentType": "text/plain",
  "plaintextLength": 42
}
```

Suggested message list response item:

```json
{
  "id": "uuid",
  "spaceId": "uuid",
  "authorUserId": "uuid",
  "authorDisplayName": "Student Example",
  "keyVersion": 3,
  "algorithm": "AES_GCM_256",
  "nonce": "base64url",
  "ciphertext": "base64url",
  "contentType": "text/plain",
  "plaintextLength": 42,
  "createdAt": "2026-04-12T12:34:56.789Z"
}
```

## 9.2 Add E2EE bootstrap endpoints

Recommended minimal new endpoints:

### A. Current user key status
- `GET /api/chat/e2ee/me`
- `PUT /api/chat/e2ee/me`

Purpose:
- fetch whether the current user has an active chat public key
- register or rotate that key

### B. Space chat E2EE state
- `GET /api/spaces/{spaceId}/chat/e2ee/state`

Purpose:
- return the active `keyVersion`
- return whether the space requires rekey before sending
- return the current user’s wrapped room key package, if one exists

### C. Rekey bootstrap recipient list
- `GET /api/spaces/{spaceId}/chat/e2ee/recipients`

Purpose:
- return current eligible recipients and their public keys for rekeying
- only authorized users should be able to call it

### D. Publish a new room key version
- `POST /api/spaces/{spaceId}/chat/e2ee/key-versions`

Purpose:
- create a new key version and upload the per-recipient wrapped room keys

Suggested publish request shape:

```json
{
  "keyVersion": 3,
  "rotationReason": "MEMBERSHIP_CHANGED",
  "recipients": [
    {
      "recipientUserId": "uuid",
      "wrapAlgorithm": "ECDH_P256_HKDF_SHA256_AES_GCM",
      "wrapNonce": "base64url",
      "wrappedKeyCiphertext": "base64url"
    }
  ]
}
```

## 9.3 Server validation rules

The backend should validate:
- the caller is authorized for the space
- the caller has an active registered public key before using E2EE endpoints
- encrypted chat send is blocked if the space is flagged `requiresRekey=true`
- `keyVersion` matches the current active version
- ciphertext and nonce decode correctly and remain within size limits
- the backend never attempts to decrypt or log the plaintext

## 10. Frontend implementation design

## 10.1 Keep crypto in the browser

The browser should be responsible for:
- generating the user’s ECDH key pair
- storing the private key locally
- encrypting plaintext before POSTing messages
- unwrapping the room key for the current user
- decrypting fetched message envelopes before rendering

## 10.2 Recommended new frontend modules

Suggested new files:
- `frontend/src/services/chatCrypto.ts`
- `frontend/src/services/chatKeyStore.ts`
- `frontend/src/types/spaceChatCrypto.ts`
- `frontend/src/workers/chatCrypto.worker.ts` later if decryption volume grows

Suggested responsibilities:

### `chatCrypto.ts`
- generate/import/export ECDH public keys
- derive wrap keys with ECDH + HKDF
- generate room keys
- encrypt/decrypt message bodies with AES-GCM
- wrap/unwrap room keys
- calculate fingerprints for UX/debug visibility

### `chatKeyStore.ts`
- persist the private key and decrypted room keys in IndexedDB
- avoid storing long-lived plaintext message caches unnecessarily
- expose helper methods like `loadCurrentPrivateKey()`, `saveCurrentPrivateKey()`, `loadRoomKey(spaceId, keyVersion)`

### `spaceChat.ts`
- extend the existing service with E2EE bootstrap endpoints
- keep transport/auth/CSRF behaviour unchanged

## 10.3 Changes to the chat UI flow

`frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue` should change from a plaintext flow to this sequence:

1. confirm the current user has a local chat key pair
2. if not, offer a one-time setup action
3. fetch E2EE state for the space
4. if the current room key is available, unwrap it locally
5. fetch encrypted messages
6. decrypt each message locally before passing display text to `SpaceChatMessageList.vue`
7. when sending, encrypt locally first, then POST ciphertext

## 10.4 UX states that must be added

The UI needs additional states beyond the current loading/sending flags:
- encrypted chat not set up on this device
- room key unavailable because a rekey is required
- message could not be decrypted
- key registration failed
- this device no longer has access to the active room key

For undecryptable messages, prefer a safe placeholder such as:
- `Unable to decrypt this message on this device.`

## 11. Backend implementation design

## 11.1 New backend packages/classes to add

Suggested additions:
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/e2ee/`
- `backend/src/main/java/edusecure/edusecure/entity/spacechatkey/`
- `backend/src/main/java/edusecure/edusecure/repository/spacechatkey/`
- `backend/src/main/java/edusecure/edusecure/service/spacechatkey/`
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceChatE2eeController.java`

Suggested core services:
- `UserChatKeyService`
- `SpaceChatKeyVersionService`
- `SpaceChatE2eeService`

## 11.2 Existing backend files likely to change

Expected touch points:
- `backend/src/main/java/edusecure/edusecure/document/spacechat/SpaceChatMessage.java`
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/CreateSpaceChatMessageRequest.java`
- `backend/src/main/java/edusecure/edusecure/dto/spacechat/SpaceChatMessageResponse.java`
- `backend/src/main/java/edusecure/edusecure/service/spacechat/MongoSpaceChatService.java`
- `backend/src/main/java/edusecure/edusecure/controller/space/SpaceChatController.java`
- `backend/src/main/java/edusecure/edusecure/service/space/SpaceService.java`
- `backend/src/main/java/edusecure/edusecure/service/auth/AuthService.java`
- `backend/src/main/java/edusecure/edusecure/dto/auth/CurrentUserResponse.java`
- `backend/src/main/resources/db/changelog/db.changelog-master.yaml`

## 11.3 Why `AuthService` and `CurrentUserResponse` are relevant

The frontend needs to know whether encrypted chat is ready for the current account/device.

Minimal first-step enhancement:
- extend `CurrentUserResponse` with chat-E2EE status fields such as `chatE2eeEnabled` and `chatKeyRegistered`

This is optional but helpful because it lets the Vue app surface setup prompts without making extra requests on every route change.

## 12. MongoDB document migration considerations

The current MongoDB documents use plaintext `body`.

Recommended migration strategy:
- do not attempt an in-place cryptographic migration of old plaintext messages into E2EE ciphertext automatically on the backend
- treat existing plaintext messages as legacy history
- add a feature flag so new spaces or opted-in spaces use encrypted messages only
- optionally label legacy plaintext messages in the UI during a transition period

Why this is safer:
- the server cannot create genuine E2EE ciphertext from historical plaintext without seeing plaintext and choosing keys itself
- mixing server-generated and client-generated ciphertext would weaken the E2EE claim
- a clean cutover is easier to explain and test

## 13. Rollout strategy

Recommended staged rollout:

### Phase 0: design freeze
- confirm the scope is single-device-first E2EE
- confirm new members do not receive historical chat by default
- confirm archived spaces stay read-only
- confirm legacy plaintext history is not auto-migrated into E2EE

### Phase 1: relational metadata and backend APIs
- add Liquibase changesets for public keys and wrapped room keys
- add repositories/entities/services for E2EE metadata
- add key-registration and E2EE-state endpoints

### Phase 2: browser key generation and local storage
- add Web Crypto helpers
- add IndexedDB-backed private-key storage
- add chat key setup UX

### Phase 3: encrypted room-key distribution
- add recipient discovery
- add room-key version publishing
- add `requiresRekey` handling tied to membership changes

### Phase 4: encrypted chat payloads
- change message DTOs and Mongo documents to ciphertext envelopes
- update the Vue chat panel to encrypt before send and decrypt after fetch

### Phase 5: migration and user-visible rollout
- introduce feature flag(s) for encrypted chat spaces
- keep legacy plaintext chat readable during transition if needed
- add admin/lecturer guidance for first rekey and user setup

## 14. Testing strategy

E2EE implementation must be tested at both protocol and feature levels.

### 14.1 Frontend tests

Add tests or validation flows for:
- key-pair generation and persistence
- room-key wrapping/unwrapping
- AES-GCM encrypt/decrypt round-trip
- failure on wrong AAD or wrong key version
- controlled handling when the private key is missing

### 14.2 Backend tests

Add tests for:
- only authorized users can register/fetch space key material
- membership changes set `requiresRekey`
- encrypted message send rejects stale `keyVersion`
- archived spaces still reject new posts
- audit records stay metadata-only and never log ciphertext-derived plaintext

### 14.3 End-to-end integration checks

Manual or automated scenarios should cover:
- student A and student B can read the same encrypted space chat
- a non-member cannot fetch room-key material
- a newly added member cannot decrypt old messages before the new key version
- a removed member cannot obtain new room-key versions
- a new browser profile without the saved private key cannot decrypt prior content

## 15. Risks and implementation caveats

Main risks:
- browser key loss if the private key exists only locally
- added complexity in membership changes and rekey UX
- harder debugging because ciphertext replaces visible payloads
- increased support burden when a user changes device/browser
- false expectations if the project overstates what browser-served E2EE guarantees

Important caveat for report wording:
- EduSecure can implement meaningful client-side encrypted chat with backend-blind ciphertext storage
- but it should not be described as a perfect protection against a malicious server delivering altered frontend code

## 16. Concrete file-by-file implementation prep

## 16.1 Backend coding order

1. add Liquibase changesets for E2EE metadata tables
2. include them from `backend/src/main/resources/db/changelog/db.changelog-master.yaml`
3. create new entities/repositories for user chat keys and room-key versions
4. create E2EE DTOs and a dedicated controller/service
5. extend `SpaceService` membership change flows to mark spaces for rekey
6. extend chat message DTOs/documents to support ciphertext envelopes
7. update `MongoSpaceChatService` to store ciphertext only
8. update integration tests for encrypted chat state and rekey rules

## 16.2 Frontend coding order

1. create `chatCrypto.ts` with Web Crypto wrappers
2. create `chatKeyStore.ts` with IndexedDB persistence
3. extend `spaceChat.ts` with key bootstrap calls
4. extend auth/current-user typing for key registration status if desired
5. update `SpaceChatPanel.vue` to load keys, decrypt messages, and encrypt outbound sends
6. update `SpaceChatMessageList.vue` to handle undecryptable placeholders
7. add type checking and manual browser validation for rekey and missing-key scenarios

## 16.3 Feature flags to add before coding further

Recommended flags:
- `app.chat.e2ee.enabled=false`
- `app.chat.e2ee.require-registered-key=false`
- optional per-space flag later if selective rollout is needed

These flags allow safe staged rollout without breaking the existing plaintext chat path immediately.

## 17. Recommended next implementation step

The best first coding milestone is **not** changing message storage yet.

The safest first milestone is:
1. add user public-key registration
2. add space room-key metadata tables and APIs
3. set and test `requiresRekey` on membership changes
4. prove that a client can create, publish, fetch, and unwrap a room key before encrypting actual messages

Once that works, changing the message body from plaintext to ciphertext becomes a controlled second step instead of a risky all-at-once rewrite.

## 18. Acceptance criteria for the first E2EE implementation

The first encrypted-chat implementation can be considered complete when:
- a user can register a chat public key from the browser
- a space can publish a versioned encrypted room key for current participants
- MongoDB chat messages store ciphertext instead of plaintext body text
- the backend cannot decrypt chat bodies in normal operation
- authorized participants can decrypt messages locally in the browser
- membership changes require key rotation before future messages can be sent
- archived spaces remain readable and non-writable
- audit logs continue to avoid chat plaintext
- the documentation clearly states the remaining browser-delivery trust limitation


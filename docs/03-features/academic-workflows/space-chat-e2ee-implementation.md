# Space Chat End-to-End Encryption Implementation

## Status and purpose

This document describes the **implemented** end-to-end encryption (E2EE) path for EduSecure space chat.

It focuses on the cryptographic mechanisms that are present in the codebase now, rather than the earlier design-only proposal.

This document should be read alongside:
- `docs/02-architecture-crypto/space-chat-e2ee-design.md` for the broader design rationale
- `docs/02-architecture-crypto/implementation-plan-and-considerations.md` for the wider cryptography context in the project
- `docs/02-architecture-crypto/uml/academic-workflows/sequence-space-chat-e2ee-implemented.puml` for a sequence-diagram view of the implemented flow

## Evidence anchors in the codebase

Primary implementation anchors:
- frontend cryptography: `frontend/src/services/chatCrypto.ts`
- frontend private/room-key storage: `frontend/src/services/chatKeyStore.ts`
- frontend chat workflow: `frontend/src/pages/SpaceDetail/components/SpaceChatPanel.vue`
- frontend API contract bindings: `frontend/src/services/spaceChat.ts`
- backend E2EE bootstrap/publish logic: `backend/src/main/java/edusecure/edusecure/service/spacechatkey/SpaceChatE2eeService.java`
- backend encrypted message validation/storage: `backend/src/main/java/edusecure/edusecure/service/spacechat/MongoSpaceChatService.java`
- backend room-key metadata entities: `backend/src/main/java/edusecure/edusecure/entity/spacechatkey/`
- MongoDB chat message document: `backend/src/main/java/edusecure/edusecure/document/spacechat/SpaceChatMessage.java`
- relational schema changes: `backend/src/main/resources/db/changelog/changes/011-space-chat-e2ee-metadata.yaml` and `012-space-chat-e2ee-publisher-key.yaml`

## 1. Implemented cryptographic stack

The current implementation uses the following cryptographic mechanisms.

### 1.1 User key agreement key pair

Implemented choice:
- `ECDH` on curve `P-256`

Where:
- `generateChatKeyPairMaterial()` in `frontend/src/services/chatCrypto.ts`

How it is used:
- each user generates a browser-side ECDH key pair
- the public key is exported as JWK JSON
- the private key stays on the client device

### 1.2 Public key fingerprinting

Implemented choice:
- `SHA-256` over canonicalized public JWK JSON

Where:
- `stringifyCanonicalJson()` and `generateChatKeyPairMaterial()` in `frontend/src/services/chatCrypto.ts`

Why it matters:
- gives a stable fingerprint for the currently registered device key
- supports device-state comparison between local key material and backend metadata

### 1.3 Per-space room key

Implemented choice:
- symmetric `AES-GCM` 256-bit room key

Where:
- `generateSpaceRoomKey()` in `frontend/src/services/chatCrypto.ts`

How it is used:
- one room key version is created for a space
- the room key is then wrapped separately for each recipient
- messages are encrypted under that room key

### 1.4 Recipient-specific room-key wrapping

Implemented choice:
- `ECDH` shared secret
- `HKDF-SHA-256` to derive an AES-GCM wrap key
- `AES-GCM` for room-key wrapping

Where:
- `deriveRecipientWrapKey()`
- `wrapRoomKeyForRecipient()`
- `unwrapRoomKeyFromPublisher()`
in `frontend/src/services/chatCrypto.ts`

Implementation details actually used:
- HKDF hash: `SHA-256`
- HKDF salt: empty byte array
- HKDF info string: `EduSecure Space Chat Room Key Wrap v1`
- wrapping algorithm label exposed to the backend: `ECDH_P256_HKDF_SHA256_AES_GCM`

Why the publisher public key is stored:
- the recipient must derive the same shared secret using:
  - their private key
  - the publisher public key used during wrapping
- therefore the backend stores the publisher public key and fingerprint with the published key version

### 1.5 Message encryption

Implemented choice:
- `AES-GCM` 256-bit for message payloads

Where:
- `encryptSpaceChatMessage()`
- `decryptSpaceChatMessage()`
in `frontend/src/services/chatCrypto.ts`

Encrypted message algorithm label used in the API:
- `AES_GCM_256`

### 1.6 Authenticated additional data (AAD)

The current implementation binds the following message metadata as AES-GCM authenticated additional data:
- `spaceId`
- `keyVersion`
- `contentType`

Where:
- `buildMessageAad()` in `frontend/src/services/chatCrypto.ts`

Important note:
- this is narrower than the earlier design proposal, which discussed binding more fields such as message id and author metadata
- the implementation documentation should reflect the code as it exists today

### 1.7 Nonce handling

Implemented behavior:
- 12-byte random nonces generated with `crypto.getRandomValues(...)`
- used for:
  - room-key wrapping
  - message encryption

Where:
- `wrapRoomKeyForRecipient()`
- `encryptSpaceChatMessage()`

### 1.8 Encoding format

Implemented encoding:
- binary outputs are encoded with base64url

Where:
- `base64UrlEncode()`
- `base64UrlDecode()`

This is used for:
- public-key fingerprints
- wrap nonces
- wrapped room-key ciphertext
- message nonces
- message ciphertext

## 2. User chat-key lifecycle

## 2.1 Key generation

The user chat key pair is generated in the browser.

Where:
- `generateChatKeyPairMaterial()`

What happens:
1. browser support is checked using `detectBrowserCryptoSupport()`
2. an ECDH `P-256` key pair is generated
3. the public key is exported to JWK
4. a SHA-256 fingerprint is computed
5. the private key and metadata are stored locally
6. only the public key data is sent to the backend

## 2.2 Local storage of the private key

Private keys are stored client-side in IndexedDB.

Where:
- `frontend/src/services/chatKeyStore.ts`

Stored client-side per user:
- `userId`
- algorithm
- fingerprint
- public JWK JSON
- private `CryptoKey`
- creation timestamp

Important implementation detail:
- the browser stores the private key as a `CryptoKey` object through structured clone support
- the implementation checks browser capability for this before enabling setup

## 2.3 Backend registration of the public key

Public key registration endpoints:
- `GET /api/chat/e2ee/me`
- `PUT /api/chat/e2ee/me`

Handled by:
- `SpaceChatE2eeController`
- `SpaceChatE2eeService`

Relational storage table:
- `user_chat_keys`

Stored server-side:
- `user_id`
- algorithm
- `public_key_jwk`
- fingerprint
- `created_at`
- `revoked_at`

Security property:
- the backend stores only the public key and metadata, not the private key

## 3. Room-key lifecycle

## 3.1 Room-key publication

Room keys are created in the browser by a manager-capable user.

Frontend flow:
- `SpaceChatPanel.vue`
- `generateSpaceRoomKey()`
- `wrapRoomKeyForRecipient()`

Backend endpoint:
- `POST /api/spaces/{spaceId}/chat/e2ee/key-versions`

Backend handler:
- `SpaceChatE2eeService.publishKeyVersion(...)`

## 3.2 Recipient set

Current implemented recipient set:
- the space creator / manager of record
- all current student members in the space membership roster

Recipient discovery endpoint:
- `GET /api/spaces/{spaceId}/chat/e2ee/recipients`

Backend checks:
- the recipient set in the publish request must match the current participant set exactly
- duplicates are rejected
- publication fails if any current participant lacks a registered key

## 3.3 Room-key metadata stored in PostgreSQL

### `space_chat_key_versions`

Stores:
- space id
- key version
- creator user id
- creation time
- rotation reason
- `requires_rekey`
- publisher public key JWK
- publisher key fingerprint

### `space_chat_key_recipients`

Stores per recipient:
- key-version row id
- recipient user id
- wrap algorithm
- wrap nonce
- wrapped room-key ciphertext
- creation time

## 3.4 Rekeying rules implemented

Membership changes trigger rekey-required state.

Where:
- `SpaceService`
- `SpaceChatKeyVersionService.markSpaceRequiresRekey(...)`

Effect:
- when a student is added or removed, the latest room-key version is marked `requiresRekey=true`
- encrypted message posting is blocked until a new room-key version is published

## 4. Message encryption flow

## 4.1 Browser-side encryption before send

When encrypted chat is active and the device has the active room key:
1. the draft plaintext is trimmed
2. `encryptSpaceChatMessage()` encrypts it with AES-GCM
3. the frontend sends the envelope fields to `/api/spaces/{spaceId}/chat/messages`
4. the plaintext body is not sent to the backend

Envelope fields used:
- `keyVersion`
- `algorithm`
- `nonce`
- `ciphertext`
- `contentType`
- `plaintextLength`

## 4.2 Backend validation before storage

Handled by:
- `MongoSpaceChatService.buildEncryptedMessage(...)`

The backend currently validates:
- plaintext body must not be supplied in encrypted mode
- `keyVersion` is present and positive
- algorithm is present and must equal `AES_GCM_256`
- nonce is present
- ciphertext is present
- content type is present and must equal `text/plain`
- plaintext length is present, positive, and within the configured max length
- an active room-key version exists for the space
- `requiresRekey` is false
- the supplied `keyVersion` matches the current active version

## 4.3 MongoDB storage format

Collection:
- `space_chat_messages`

Encrypted messages store:
- `spaceId`
- `authorUserId`
- `authorDisplayName`
- `keyVersion`
- `algorithm`
- `nonce`
- `ciphertext`
- `contentType`
- `plaintextLength`
- `createdAt`

Legacy plaintext messages may still store:
- `body`

## 4.4 Browser-side decryption after fetch

Implemented in:
- `SpaceChatPanel.vue`
- `resolveRoomKeyForVersion(...)`
- `resolveDisplayMessage(...)`
- `decryptSpaceChatMessage()`

Flow:
1. the frontend fetches chat messages
2. if a message has ciphertext fields, it is treated as encrypted
3. the frontend resolves the correct room key from IndexedDB or from wrapped-key state
4. AES-GCM decryption is attempted using the bound AAD
5. if successful, the decrypted plaintext is rendered locally
6. if not, a safe placeholder is shown

## 5. Local and server-side storage boundaries

## 5.1 Stored only on the client device

- user private ECDH chat key
- decrypted room keys
- decrypted message bodies in runtime memory

## 5.2 Stored on the backend relational side

In PostgreSQL:
- user public keys
- room-key version metadata
- wrapped room keys per recipient
- publisher public-key metadata used for unwrap

## 5.3 Stored on the backend document side

In MongoDB:
- encrypted message envelope fields
- author metadata
- legacy plaintext `body` for pre-E2EE transition messages

## 5.4 What the backend can still see

Even with the current E2EE implementation, the backend still sees some metadata:
- sender identity
- space identity
- message creation time
- key version
- content type
- plaintext length
- membership and participant set

So the implementation provides **content confidentiality**, not metadata confidentiality.

## 6. Audit and integrity protections

The implementation specifically avoids logging chat plaintext and wrapped room-key secrets in audit details.

Verified by code and tests:
- encrypted message audit records include metadata such as `bodyLength`
- encrypted message audit records do not include plaintext body or ciphertext payload
- room-key publish audit records include metadata such as `spaceId`, `keyVersion`, and `recipientCount`
- room-key publish audit records do not include wrapped ciphertext or nonce values

Relevant backend pieces:
- `AuditService`
- `AuditActionType.SPACE_CHAT_MESSAGE_CREATED`
- `AuditActionType.SPACE_CHAT_KEY_VERSION_PUBLISHED`
- integration coverage in `SpaceChatE2eeFlowIntegrationTests.java`

Additional integrity detail:
- audit records are chained with an integrity MAC through the existing audit service design
- this is separate from chat message encryption, but it strengthens tamper-evidence around security-sensitive events

## 7. Implemented limitations and current boundaries

## 7.1 Browser trust boundary remains

This is a browser-served application.
If the server were able to deliver altered frontend JavaScript, it could alter client-side cryptographic behavior.

So the current implementation should be described as:
- meaningful client-side encrypted chat with backend-blind message bodies under normal operation
- not a zero-trust guarantee against a malicious server controlling delivered frontend code

## 7.2 Single active key model

The current relational model enforces one active chat key per user.

Implication:
- registering a new device key effectively replaces the active registered device key
- seamless multi-device support is not yet implemented

## 7.3 Legacy plaintext coexistence

The implementation intentionally supports mixed history during rollout.

Implication:
- older plaintext messages may remain visible in MongoDB and in the UI
- not all historical chat content is retroactively encrypted

## 7.4 Current room-key retrieval scope

The frontend resolves the active room key from:
- local room-key storage
- the current wrapped room key returned by `/chat/e2ee/state`

This means the implementation is currently optimized around the **latest active key version**.

## 7.5 `requireRegisteredKey` is not the main enforcement switch for message sending

There is a configuration field for `app.chat.e2ee.require-registered-key`, but the strongest practical send enforcement currently comes from:
- whether E2EE is enabled
- whether the current device has a registered key
- whether the active room key can be resolved locally
- whether `requiresRekey` is false
- whether the active key version matches

That is the behavior visible in the current frontend and backend code.

## 8. Practical summary

In its current implemented form, EduSecure space chat E2EE works as follows:
- user devices generate an ECDH `P-256` key pair locally
- only the public key is registered with the backend
- a manager device generates an AES-256 room key for a space
- that room key is wrapped separately for each participant using ECDH-derived AES-GCM wrap keys
- chat messages are encrypted in the browser using AES-GCM
- encrypted messages are stored in MongoDB as ciphertext envelopes
- recipients unwrap the room key locally and decrypt messages in the browser
- membership changes force a rekey before new encrypted messages can be sent
- audit logs record only metadata, not plaintext or wrapped-key secrets

That is the actual implemented cryptographic solution in the repository at the time of writing.


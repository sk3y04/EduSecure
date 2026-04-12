import { describe, expect, it } from 'vitest'

import {
  decryptSpaceChatMessage,
  encryptSpaceChatMessage,
  generateChatKeyPairMaterial,
  generateSpaceRoomKey,
  unwrapRoomKeyFromPublisher,
  wrapRoomKeyForRecipient,
} from '@/services/chatCrypto'

describe('chatCrypto', () => {
  it('generates a chat key pair with public JWK metadata', async () => {
    const material = await generateChatKeyPairMaterial()

    expect(material.algorithm).toBe('ECDH_P256')
    expect(material.fingerprint.length).toBeGreaterThan(10)
    expect(material.publicKeyJwk.kty).toBe('EC')
    expect(material.publicKeyJwkJson).toContain('"kty":"EC"')
    expect(material.privateKey).toBeInstanceOf(CryptoKey)
    expect(material.publicKey).toBeInstanceOf(CryptoKey)
  })

  it('wraps and unwraps a room key between two participants', async () => {
    const publisher = await generateChatKeyPairMaterial()
    const recipient = await generateChatKeyPairMaterial()
    const roomKey = await generateSpaceRoomKey()

    const wrapped = await wrapRoomKeyForRecipient(
      publisher.privateKey,
      recipient.publicKeyJwkJson,
      roomKey,
    )
    const unwrappedRoomKey = await unwrapRoomKeyFromPublisher(
      recipient.privateKey,
      publisher.publicKeyJwkJson,
      wrapped.wrapNonce,
      wrapped.wrappedKeyCiphertext,
    )

    const encryptedMessage = await encryptSpaceChatMessage(
      roomKey,
      'space-123',
      2,
      'Encrypted hello world',
    )
    const decryptedMessage = await decryptSpaceChatMessage(
      unwrappedRoomKey,
      'space-123',
      2,
      encryptedMessage.nonce,
      encryptedMessage.ciphertext,
      encryptedMessage.contentType,
    )

    expect(wrapped.wrapAlgorithm).toBe('ECDH_P256_HKDF_SHA256_AES_GCM')
    expect(decryptedMessage).toBe('Encrypted hello world')
  })

  it('rejects decryption when authenticated metadata does not match', async () => {
    const publisher = await generateChatKeyPairMaterial()
    const recipient = await generateChatKeyPairMaterial()
    const roomKey = await generateSpaceRoomKey()

    const wrapped = await wrapRoomKeyForRecipient(
      publisher.privateKey,
      recipient.publicKeyJwkJson,
      roomKey,
    )
    const unwrappedRoomKey = await unwrapRoomKeyFromPublisher(
      recipient.privateKey,
      publisher.publicKeyJwkJson,
      wrapped.wrapNonce,
      wrapped.wrappedKeyCiphertext,
    )
    const encryptedMessage = await encryptSpaceChatMessage(
      roomKey,
      'space-123',
      4,
      'AAD mismatch should fail',
    )

    await expect(
      decryptSpaceChatMessage(
        unwrappedRoomKey,
        'space-999',
        4,
        encryptedMessage.nonce,
        encryptedMessage.ciphertext,
        encryptedMessage.contentType,
      ),
    ).rejects.toBeTruthy()
  })
})


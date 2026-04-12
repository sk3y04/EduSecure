const CHAT_KEY_ALGORITHM = 'ECDH_P256' as const
const CHAT_KEY_WRAP_ALGORITHM = 'ECDH_P256_HKDF_SHA256_AES_GCM' as const
const ROOM_KEY_ALGORITHM = 'AES_GCM_256' as const
const NAMED_CURVE = 'P-256'
const ROOM_KEY_WRAP_INFO = 'EduSecure Space Chat Room Key Wrap v1'

export type ChatKeyAlgorithm = typeof CHAT_KEY_ALGORITHM
export type ChatKeyWrapAlgorithm = typeof CHAT_KEY_WRAP_ALGORITHM
export type RoomKeyAlgorithm = typeof ROOM_KEY_ALGORITHM

export interface ChatKeyPairMaterial {
  algorithm: ChatKeyAlgorithm
  fingerprint: string
  publicKeyJwk: JsonWebKey
  publicKeyJwkJson: string
  privateKey: CryptoKey
  publicKey: CryptoKey
  createdAt: string
}

export interface BrowserCryptoSupport {
  cryptoAvailable: boolean
  subtleAvailable: boolean
  indexedDbAvailable: boolean
  cryptoKeyStructuredCloneAvailable: boolean
}

export interface WrappedRoomKeyMaterial {
  wrapAlgorithm: ChatKeyWrapAlgorithm
  wrapNonce: string
  wrappedKeyCiphertext: string
}

export interface EncryptedChatMessagePayload {
  keyVersion: number
  algorithm: RoomKeyAlgorithm
  nonce: string
  ciphertext: string
  contentType: 'text/plain'
  plaintextLength: number
}

function ensureSubtleCrypto(): SubtleCrypto {
  const subtle = globalThis.crypto?.subtle
  if (!subtle) {
    throw new Error('This browser does not support the Web Crypto API required for encrypted chat setup.')
  }
  return subtle
}

function base64UrlEncode(bytes: Uint8Array): string {
  let binary = ''
  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }

  return btoa(binary)
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '')
}

function parseJsonWebKey(json: string): JsonWebKey {
  try {
    return JSON.parse(json) as JsonWebKey
  } catch {
    throw new Error('Encrypted chat recipient key data is malformed.')
  }
}

function base64UrlDecode(value: string): Uint8Array {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padding = normalized.length % 4 === 0 ? '' : '='.repeat(4 - (normalized.length % 4))
  const binary = atob(`${normalized}${padding}`)
  return Uint8Array.from(binary, (character) => character.charCodeAt(0))
}

function toArrayBuffer(bytes: Uint8Array): ArrayBuffer {
  return bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength) as ArrayBuffer
}

function buildMessageAad(spaceId: string, keyVersion: number, contentType: string): ArrayBuffer {
  return toArrayBuffer(new TextEncoder().encode(JSON.stringify({
    spaceId,
    keyVersion,
    contentType,
  })))
}

function canonicalizeJsonValue(value: unknown): unknown {
  if (Array.isArray(value)) {
    return value.map(canonicalizeJsonValue)
  }

  if (value && typeof value === 'object') {
    return Object.keys(value as Record<string, unknown>)
      .sort()
      .reduce<Record<string, unknown>>((accumulator, key) => {
        accumulator[key] = canonicalizeJsonValue((value as Record<string, unknown>)[key])
        return accumulator
      }, {})
  }

  return value
}

function stringifyCanonicalJson(value: unknown): string {
  return JSON.stringify(canonicalizeJsonValue(value))
}

export async function detectBrowserCryptoSupport(): Promise<BrowserCryptoSupport> {
  const cryptoAvailable = Boolean(globalThis.crypto)
  const subtleAvailable = Boolean(globalThis.crypto?.subtle)
  const indexedDbAvailable = Boolean(globalThis.indexedDB)

  let cryptoKeyStructuredCloneAvailable = false
  if (subtleAvailable && indexedDbAvailable) {
    try {
      const probeKey = await ensureSubtleCrypto().generateKey(
        { name: 'AES-GCM', length: 256 },
        false,
        ['encrypt', 'decrypt'],
      )
      cryptoKeyStructuredCloneAvailable = probeKey instanceof CryptoKey
    } catch {
      cryptoKeyStructuredCloneAvailable = false
    }
  }

  return {
    cryptoAvailable,
    subtleAvailable,
    indexedDbAvailable,
    cryptoKeyStructuredCloneAvailable,
  }
}

export async function generateChatKeyPairMaterial(): Promise<ChatKeyPairMaterial> {
  const subtle = ensureSubtleCrypto()
  const keyPair = await subtle.generateKey(
    {
      name: 'ECDH',
      namedCurve: NAMED_CURVE,
    },
    true,
    ['deriveBits', 'deriveKey'],
  )

  if (!(keyPair.privateKey instanceof CryptoKey) || !(keyPair.publicKey instanceof CryptoKey)) {
    throw new Error('Unable to generate a valid encrypted chat key pair in this browser.')
  }

  const publicKeyJwk = await subtle.exportKey('jwk', keyPair.publicKey)
  const publicKeyJwkJson = stringifyCanonicalJson(publicKeyJwk)
  const digest = await subtle.digest('SHA-256', new TextEncoder().encode(publicKeyJwkJson))
  const fingerprint = base64UrlEncode(new Uint8Array(digest))

  return {
    algorithm: CHAT_KEY_ALGORITHM,
    fingerprint,
    publicKeyJwk,
    publicKeyJwkJson,
    privateKey: keyPair.privateKey,
    publicKey: keyPair.publicKey,
    createdAt: new Date().toISOString(),
  }
}

async function importRecipientPublicKey(publicKeyJwkJson: string): Promise<CryptoKey> {
  return ensureSubtleCrypto().importKey(
    'jwk',
    parseJsonWebKey(publicKeyJwkJson),
    {
      name: 'ECDH',
      namedCurve: NAMED_CURVE,
    },
    false,
    [],
  )
}

async function deriveRecipientWrapKey(privateKey: CryptoKey, recipientPublicKey: CryptoKey): Promise<CryptoKey> {
  const subtle = ensureSubtleCrypto()
  const sharedBits = await subtle.deriveBits(
    {
      name: 'ECDH',
      public: recipientPublicKey,
    },
    privateKey,
    256,
  )
  const hkdfBaseKey = await subtle.importKey('raw', sharedBits, 'HKDF', false, ['deriveKey'])

  return subtle.deriveKey(
    {
      name: 'HKDF',
      hash: 'SHA-256',
      salt: new Uint8Array(),
      info: new TextEncoder().encode(ROOM_KEY_WRAP_INFO),
    },
    hkdfBaseKey,
    {
      name: 'AES-GCM',
      length: 256,
    },
    false,
    ['encrypt', 'decrypt'],
  )
}

export async function generateSpaceRoomKey(): Promise<CryptoKey> {
  return ensureSubtleCrypto().generateKey(
    {
      name: 'AES-GCM',
      length: 256,
    },
    true,
    ['encrypt', 'decrypt'],
  )
}

export async function wrapRoomKeyForRecipient(
  privateKey: CryptoKey,
  recipientPublicKeyJwkJson: string,
  roomKey: CryptoKey,
): Promise<WrappedRoomKeyMaterial> {
  const subtle = ensureSubtleCrypto()
  const recipientPublicKey = await importRecipientPublicKey(recipientPublicKeyJwkJson)
  const wrapKey = await deriveRecipientWrapKey(privateKey, recipientPublicKey)
  const roomKeyBytes = new Uint8Array(await subtle.exportKey('raw', roomKey))
  const nonce = globalThis.crypto.getRandomValues(new Uint8Array(12))
  const ciphertext = await subtle.encrypt(
    {
      name: 'AES-GCM',
      iv: nonce,
    },
    wrapKey,
    roomKeyBytes,
  )

  return {
    wrapAlgorithm: CHAT_KEY_WRAP_ALGORITHM,
    wrapNonce: base64UrlEncode(nonce),
    wrappedKeyCiphertext: base64UrlEncode(new Uint8Array(ciphertext)),
  }
}

export async function unwrapRoomKeyFromPublisher(
  privateKey: CryptoKey,
  publisherPublicKeyJwkJson: string,
  wrapNonce: string,
  wrappedKeyCiphertext: string,
): Promise<CryptoKey> {
  const subtle = ensureSubtleCrypto()
  const publisherPublicKey = await importRecipientPublicKey(publisherPublicKeyJwkJson)
  const wrapKey = await deriveRecipientWrapKey(privateKey, publisherPublicKey)
  const rawRoomKey = await subtle.decrypt(
    {
      name: 'AES-GCM',
      iv: toArrayBuffer(base64UrlDecode(wrapNonce)),
    },
    wrapKey,
    toArrayBuffer(base64UrlDecode(wrappedKeyCiphertext)),
  )

  return subtle.importKey(
    'raw',
    rawRoomKey,
    {
      name: 'AES-GCM',
      length: 256,
    },
    false,
    ['encrypt', 'decrypt'],
  )
}

export async function encryptSpaceChatMessage(
  roomKey: CryptoKey,
  spaceId: string,
  keyVersion: number,
  plaintext: string,
): Promise<EncryptedChatMessagePayload> {
  const subtle = ensureSubtleCrypto()
  const contentType = 'text/plain' as const
  const normalizedPlaintext = plaintext.trim()
  const plaintextBytes = new TextEncoder().encode(normalizedPlaintext)
  const nonce = globalThis.crypto.getRandomValues(new Uint8Array(12))
  const ciphertext = await subtle.encrypt(
    {
      name: 'AES-GCM',
      iv: nonce,
      additionalData: buildMessageAad(spaceId, keyVersion, contentType),
    },
    roomKey,
    plaintextBytes,
  )

  return {
    keyVersion,
    algorithm: ROOM_KEY_ALGORITHM,
    nonce: base64UrlEncode(nonce),
    ciphertext: base64UrlEncode(new Uint8Array(ciphertext)),
    contentType,
    plaintextLength: normalizedPlaintext.length,
  }
}

export async function decryptSpaceChatMessage(
  roomKey: CryptoKey,
  spaceId: string,
  keyVersion: number,
  nonce: string,
  ciphertext: string,
  contentType: string,
): Promise<string> {
  const subtle = ensureSubtleCrypto()
  const plaintext = await subtle.decrypt(
    {
      name: 'AES-GCM',
      iv: toArrayBuffer(base64UrlDecode(nonce)),
      additionalData: buildMessageAad(spaceId, keyVersion, contentType),
    },
    roomKey,
    toArrayBuffer(base64UrlDecode(ciphertext)),
  )

  return new TextDecoder().decode(plaintext)
}




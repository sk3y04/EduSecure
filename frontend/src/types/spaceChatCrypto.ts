export interface CurrentUserChatKeyResponse {
  e2eeEnabled: boolean
  keyRegistrationRequired: boolean
  keyRegistered: boolean
  algorithm: string | null
  publicKeyJwk: string | null
  fingerprint: string | null
  createdAt: string | null
}

export interface UpsertCurrentUserChatKeyRequest {
  algorithm: string
  publicKeyJwk: string
  fingerprint: string
}

export interface SpaceChatWrappedKeyResponse {
  publisherUserId: string
  publisherPublicKeyJwk: string
  publisherKeyFingerprint: string
  wrapAlgorithm: string
  wrapNonce: string
  wrappedKeyCiphertext: string
  createdAt: string
}

export interface SpaceChatE2eeStateResponse {
  e2eeEnabled: boolean
  keyRegistrationRequired: boolean
  currentUserKeyRegistered: boolean
  activeKeyVersion: number | null
  requiresRekey: boolean
  currentUserWrappedKey: SpaceChatWrappedKeyResponse | null
}

export interface SpaceChatE2eeRecipientResponse {
  userId: string
  displayName: string
  manager: boolean
  keyRegistered: boolean
  algorithm: string | null
  publicKeyJwk: string | null
  fingerprint: string | null
}

export interface PublishSpaceChatKeyVersionRecipientRequest {
  recipientUserId: string
  wrapAlgorithm: string
  wrapNonce: string
  wrappedKeyCiphertext: string
}

export interface PublishSpaceChatKeyVersionRequest {
  keyVersion: number
  rotationReason: string
  recipients: PublishSpaceChatKeyVersionRecipientRequest[]
}

export interface SpaceChatKeyVersionPublishResponse {
  keyVersion: number
  rotationReason: string
  recipientCount: number
  requiresRekey: boolean
  publisherKeyFingerprint: string
  createdAt: string
}


export interface SpaceChatMessage {
  id: string
  spaceId: string
  authorUserId: string
  authorDisplayName: string
  body: string | null
  keyVersion: number | null
  algorithm: string | null
  nonce: string | null
  ciphertext: string | null
  contentType: string | null
  plaintextLength: number | null
  createdAt: string
  displayBody?: string
  bodyState?: 'legacy-plaintext' | 'decrypted' | 'undecryptable'
  encrypted?: boolean
}

export interface SpaceChatMessagePage {
  items: SpaceChatMessage[]
  hasMore: boolean
  nextCursorBeforeCreatedAt: string | null
  nextCursorBeforeMessageId: string | null
}

export interface CreateSpaceChatMessageRequest {
  body?: string | null
  keyVersion?: number | null
  algorithm?: string | null
  nonce?: string | null
  ciphertext?: string | null
  contentType?: string | null
  plaintextLength?: number | null
}

export interface ListSpaceChatMessagesParams {
  limit?: number
  beforeCreatedAt?: string
  beforeMessageId?: string
}


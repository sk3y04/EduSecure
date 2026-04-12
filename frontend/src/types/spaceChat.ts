export interface SpaceChatMessage {
  id: string
  spaceId: string
  authorUserId: string
  authorDisplayName: string
  body: string
  createdAt: string
}

export interface SpaceChatMessagePage {
  items: SpaceChatMessage[]
  hasMore: boolean
  nextCursorBeforeCreatedAt: string | null
  nextCursorBeforeMessageId: string | null
}

export interface CreateSpaceChatMessageRequest {
  body: string
}

export interface ListSpaceChatMessagesParams {
  limit?: number
  beforeCreatedAt?: string
  beforeMessageId?: string
}


import http from '@/services/http'
import type {
  CreateSpaceChatMessageRequest,
  ListSpaceChatMessagesParams,
  SpaceChatMessage,
  SpaceChatMessagePage,
} from '@/types/spaceChat'
import type {
  CurrentUserChatKeyResponse,
  PublishSpaceChatKeyVersionRequest,
  SpaceChatE2eeRecipientResponse,
  SpaceChatE2eeStateResponse,
  SpaceChatKeyVersionPublishResponse,
  UpsertCurrentUserChatKeyRequest,
} from '@/types/spaceChatCrypto'

export const spaceChatService = {
  async listMessages(spaceId: string, params: ListSpaceChatMessagesParams = {}): Promise<SpaceChatMessagePage> {
    const response = await http.get<SpaceChatMessagePage>(`/spaces/${spaceId}/chat/messages`, {
      params,
    })
    return response.data
  },

  async createMessage(spaceId: string, payload: CreateSpaceChatMessageRequest): Promise<SpaceChatMessage> {
    const response = await http.post<SpaceChatMessage>(`/spaces/${spaceId}/chat/messages`, payload)
    return response.data
  },

  async getCurrentUserChatKey(): Promise<CurrentUserChatKeyResponse> {
    const response = await http.get<CurrentUserChatKeyResponse>('/chat/e2ee/me')
    return response.data
  },

  async upsertCurrentUserChatKey(payload: UpsertCurrentUserChatKeyRequest): Promise<CurrentUserChatKeyResponse> {
    const response = await http.put<CurrentUserChatKeyResponse>('/chat/e2ee/me', payload)
    return response.data
  },

  async getE2eeState(spaceId: string): Promise<SpaceChatE2eeStateResponse> {
    const response = await http.get<SpaceChatE2eeStateResponse>(`/spaces/${spaceId}/chat/e2ee/state`)
    return response.data
  },

  async listE2eeRecipients(spaceId: string): Promise<SpaceChatE2eeRecipientResponse[]> {
    const response = await http.get<SpaceChatE2eeRecipientResponse[]>(`/spaces/${spaceId}/chat/e2ee/recipients`)
    return response.data
  },

  async publishE2eeKeyVersion(
    spaceId: string,
    payload: PublishSpaceChatKeyVersionRequest,
  ): Promise<SpaceChatKeyVersionPublishResponse> {
    const response = await http.post<SpaceChatKeyVersionPublishResponse>(`/spaces/${spaceId}/chat/e2ee/key-versions`, payload)
    return response.data
  },
}


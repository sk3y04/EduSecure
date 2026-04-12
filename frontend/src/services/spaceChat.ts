import http from '@/services/http'
import type {
  CreateSpaceChatMessageRequest,
  ListSpaceChatMessagesParams,
  SpaceChatMessage,
  SpaceChatMessagePage,
} from '@/types/spaceChat'

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
}


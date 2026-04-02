import http from '@/services/http'
import type {
  AddSpaceStudentRequest,
  CreateSpaceRequest,
  SpaceDetail,
  SpaceStudent,
  SpaceSummary,
  UpdateSpaceRequest,
} from '@/types/space'

export const spacesService = {
  async list(): Promise<SpaceSummary[]> {
    const response = await http.get<SpaceSummary[]>('/spaces')
    return response.data
  },

  async getById(spaceId: string): Promise<SpaceDetail> {
    const response = await http.get<SpaceDetail>(`/spaces/${spaceId}`)
    return response.data
  },

  async create(payload: CreateSpaceRequest): Promise<SpaceDetail> {
    const response = await http.post<SpaceDetail>('/spaces', payload)
    return response.data
  },

  async update(spaceId: string, payload: UpdateSpaceRequest): Promise<SpaceDetail> {
    const response = await http.put<SpaceDetail>(`/spaces/${spaceId}`, payload)
    return response.data
  },

  async addStudent(spaceId: string, payload: AddSpaceStudentRequest): Promise<SpaceStudent> {
    const response = await http.post<SpaceStudent>(`/spaces/${spaceId}/students`, payload)
    return response.data
  },

  async removeStudent(spaceId: string, studentUserId: string): Promise<void> {
    await http.delete(`/spaces/${spaceId}/students/${studentUserId}`)
  },
}
import http from '@/services/http'
import type { AssignmentResponse, AssignmentSummary, CreateAssignmentRequest } from '@/types/assignment'

export const assignmentsService = {
  async list(): Promise<AssignmentSummary[]> {
    const response = await http.get<AssignmentSummary[]>('/assignments')
    return response.data
  },

  async create(payload: CreateAssignmentRequest): Promise<AssignmentResponse> {
    const response = await http.post<AssignmentResponse>('/assignments', payload)
    return response.data
  },
}


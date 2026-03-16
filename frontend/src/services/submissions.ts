import http from '@/services/http'
import type { CreateSubmissionRequest, SubmissionContentResponse, SubmissionResponse } from '@/types/submission'

export const submissionsService = {
  async create(assignmentId: string, payload: CreateSubmissionRequest): Promise<SubmissionResponse> {
    const response = await http.post<SubmissionResponse>(`/assignments/${assignmentId}/submissions`, payload)
    return response.data
  },

  async getById(submissionId: string): Promise<SubmissionResponse> {
    const response = await http.get<SubmissionResponse>(`/submissions/${submissionId}`)
    return response.data
  },

  async getContent(submissionId: string): Promise<SubmissionContentResponse> {
    const response = await http.get<SubmissionContentResponse>(`/submissions/${submissionId}/content`)
    return response.data
  },
}


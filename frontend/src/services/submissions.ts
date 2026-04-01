import http from '@/services/http'
import type { SubmissionContentResponse, SubmissionResponse } from '@/types/submission'

export const submissionsService = {
  async create(assignmentId: string, file: File): Promise<SubmissionResponse> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await http.post<SubmissionResponse>(`/assignments/${assignmentId}/submissions`, formData)
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


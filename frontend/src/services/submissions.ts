import http from '@/services/http'
import type { SubmissionDownloadResult, SubmissionResponse } from '@/types/submission'

export const submissionsService = {
  async create(assignmentId: string, file: File): Promise<SubmissionResponse> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await http.post<SubmissionResponse>(`/assignments/${assignmentId}/submissions`, formData)
    return response.data
  },

  async getMyLatestForAssignment(assignmentId: string): Promise<SubmissionResponse> {
    const response = await http.get<SubmissionResponse>(`/assignments/${assignmentId}/submissions/me`)
    return response.data
  },

  async listForAssignment(assignmentId: string): Promise<SubmissionResponse[]> {
    const response = await http.get<SubmissionResponse[]>(`/assignments/${assignmentId}/submissions`)
    return response.data
  },

  async getById(submissionId: string): Promise<SubmissionResponse> {
    const response = await http.get<SubmissionResponse>(`/submissions/${submissionId}`)
    return response.data
  },

  async downloadContent(submissionId: string, fileName: string, contentType: string): Promise<SubmissionDownloadResult> {
    const response = await http.get<Blob>(`/submissions/${submissionId}/content`, {
      responseType: 'blob',
      headers: {
        Accept: `${contentType},application/octet-stream`,
      },
    })

    return {
      fileName,
      contentType: response.headers['content-type'] ?? contentType,
      content: response.data,
    }
  },
}


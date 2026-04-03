import http from '@/services/http'
import type {
  CreateGradeRequest,
  GradeResponse,
  MyGradeResponse,
  UpdateGradeRequest,
} from '@/types/grade'

export const gradesService = {
  async create(submissionId: string, payload: CreateGradeRequest): Promise<GradeResponse> {
    const response = await http.post<GradeResponse>(`/submissions/${submissionId}/grade`, payload)
    return response.data
  },

  async getForSubmission(submissionId: string): Promise<GradeResponse> {
    const response = await http.get<GradeResponse>(`/submissions/${submissionId}/grade`)
    return response.data
  },

  async update(gradeId: string, payload: UpdateGradeRequest): Promise<GradeResponse> {
    const response = await http.put<GradeResponse>(`/grades/${gradeId}`, payload)
    return response.data
  },

  async getById(gradeId: string): Promise<GradeResponse> {
    const response = await http.get<GradeResponse>(`/grades/${gradeId}`)
    return response.data
  },

  async getMyGrade(gradeId: string): Promise<MyGradeResponse> {
    const response = await http.get<MyGradeResponse>(`/my/grades/${gradeId}`)
    return response.data
  },

  async getMyGradeForSubmission(submissionId: string): Promise<MyGradeResponse> {
    const response = await http.get<MyGradeResponse>(`/my/submissions/${submissionId}/grade`)
    return response.data
  },
}


import http from '@/services/http'
import type {
  CreateExamResultRequest,
  ExamResultResponse,
  MyExamResultResponse,
  UpdateExamResultRequest,
} from '@/types/examResult'

export const examResultsService = {
  async listForExam(examId: string): Promise<ExamResultResponse[]> {
    const response = await http.get<ExamResultResponse[]>(`/exams/${examId}/results`)
    return response.data
  },

  async create(examId: string, payload: CreateExamResultRequest): Promise<ExamResultResponse> {
    const response = await http.post<ExamResultResponse>(`/exams/${examId}/results`, payload)
    return response.data
  },

  async update(examResultId: string, payload: UpdateExamResultRequest): Promise<ExamResultResponse> {
    const response = await http.put<ExamResultResponse>(`/exam-results/${examResultId}`, payload)
    return response.data
  },

  async listMine(): Promise<MyExamResultResponse[]> {
    const response = await http.get<MyExamResultResponse[]>('/my/exam-results')
    return response.data
  },
}
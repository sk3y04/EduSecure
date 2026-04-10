import http from '@/services/http'
import type { Exam, ExamPayload } from '@/types/exam'

export const examsService = {
  async list(): Promise<Exam[]> {
    const response = await http.get<Exam[]>('/exams')
    return response.data
  },

  async create(payload: ExamPayload): Promise<Exam> {
    const response = await http.post<Exam>('/exams', payload)
    return response.data
  },

  async update(examId: string, payload: ExamPayload): Promise<Exam> {
    const response = await http.put<Exam>(`/exams/${examId}`, payload)
    return response.data
  },
}
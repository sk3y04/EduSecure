import http from '@/services/http'
import type {
  FeedbackForm,
  FeedbackFormPayload,
  FeedbackFormReview,
  FeedbackFormSubmissionPayload,
  FeedbackFormSubmissionReceipt,
} from '@/types/feedbackForm'

export const feedbackFormsService = {
  async listForExam(examId: string): Promise<FeedbackForm[]> {
    const response = await http.get<FeedbackForm[]>(`/exams/${examId}/feedback-forms`)
    return response.data
  },

  async create(examId: string, payload: FeedbackFormPayload): Promise<FeedbackForm> {
    const response = await http.post<FeedbackForm>(`/exams/${examId}/feedback-forms`, payload)
    return response.data
  },

  async update(formId: string, payload: FeedbackFormPayload): Promise<FeedbackForm> {
    const response = await http.put<FeedbackForm>(`/feedback-forms/${formId}`, payload)
    return response.data
  },

  async getById(formId: string): Promise<FeedbackForm> {
    const response = await http.get<FeedbackForm>(`/feedback-forms/${formId}`)
    return response.data
  },

  async submitResponse(formId: string, payload: FeedbackFormSubmissionPayload): Promise<FeedbackFormSubmissionReceipt> {
    const response = await http.post<FeedbackFormSubmissionReceipt>(`/feedback-forms/${formId}/responses`, payload)
    return response.data
  },

  async listResponses(formId: string): Promise<FeedbackFormReview> {
    const response = await http.get<FeedbackFormReview>(`/feedback-forms/${formId}/responses`)
    return response.data
  },
}
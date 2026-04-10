import http from '@/services/http'
import type {
  CreateRegistrationRequestPayload,
  ReviewRegistrationRequest,
  ReviewRegistrationRequestPayload,
  StudentRegistrationRequest,
} from '@/types/registration'

export const registrationRequestsService = {
  async create(payload: CreateRegistrationRequestPayload): Promise<StudentRegistrationRequest> {
    const response = await http.post<StudentRegistrationRequest>('/space-registration-requests', payload)
    return response.data
  },

  async listMine(): Promise<StudentRegistrationRequest[]> {
    const response = await http.get<StudentRegistrationRequest[]>('/space-registration-requests/mine')
    return response.data
  },

  async cancel(requestId: string): Promise<StudentRegistrationRequest> {
    const response = await http.post<StudentRegistrationRequest>(`/space-registration-requests/${requestId}/cancel`)
    return response.data
  },

  async listReviewQueue(): Promise<ReviewRegistrationRequest[]> {
    const response = await http.get<ReviewRegistrationRequest[]>('/space-registration-requests/review')
    return response.data
  },

  async approve(requestId: string, payload: ReviewRegistrationRequestPayload): Promise<ReviewRegistrationRequest> {
    const response = await http.post<ReviewRegistrationRequest>(`/space-registration-requests/${requestId}/approve`, payload)
    return response.data
  },

  async reject(requestId: string, payload: ReviewRegistrationRequestPayload): Promise<ReviewRegistrationRequest> {
    const response = await http.post<ReviewRegistrationRequest>(`/space-registration-requests/${requestId}/reject`, payload)
    return response.data
  },
}
export type RegistrationRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'CANCELLED'

export interface StudentRegistrationRequest {
  id: string
  spaceId: string
  spaceCode: string
  spaceName: string
  status: RegistrationRequestStatus
  requestMessage: string | null
  requestedAt: string
  reviewedAt: string | null
  reviewNote: string | null
}

export interface ReviewRegistrationRequest extends StudentRegistrationRequest {
  studentUserId: string
  studentEmail: string
  studentFullName: string
  canReview: boolean
}

export interface CreateRegistrationRequestPayload {
  spaceCode: string
  requestMessage: string
}

export interface ReviewRegistrationRequestPayload {
  reviewNote: string
}
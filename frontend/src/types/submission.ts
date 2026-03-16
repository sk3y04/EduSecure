export type SubmissionVerificationStatus = 'PENDING' | 'VERIFIED' | 'FAILED_VERIFICATION' | 'REJECTED'

export interface CreateSubmissionRequest {
  fileName: string
  contentType: string
  content: string
}

export interface SubmissionResponse {
  id: string
  assignmentId: string
  studentUserId: string
  submittedAt: string
  fileName: string
  contentType: string
  hashDigest: string
  digitalSignature: string
  signatureAlgorithm: string
  verificationStatus: SubmissionVerificationStatus
  verificationMessage: string
}

export interface SubmissionContentResponse {
  submissionId: string
  fileName: string
  contentType: string
  content: string
}


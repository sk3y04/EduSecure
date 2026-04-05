export type SubmissionVerificationStatus = 'PENDING' | 'VERIFIED' | 'FAILED_VERIFICATION' | 'REJECTED'


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
  graded: boolean
  gradeId: string | null
}

export interface SubmissionDownloadResult {
  fileName: string
  contentType: string
  content: Blob
}


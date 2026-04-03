export interface GradeResponse {
  id: string
  submissionId: string
  value: string
  feedback: string
  gradedByLecturerId: string
  gradedAt: string
  lastModifiedAt: string | null
}

export interface MyGradeResponse {
  id: string
  submissionId: string
  value: string
  feedback: string
  lastModifiedAt: string | null
}

export interface CreateGradeRequest {
  value: string
  feedback: string
}

export interface UpdateGradeRequest {
  value: string
  feedback: string
}


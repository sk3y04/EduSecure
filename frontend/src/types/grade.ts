export interface GradeResponse {
  id: string
  submissionId: string
  value: number
  feedback: string
  gradedByLecturerId: string
  gradedAt: string
  lastModifiedAt: string | null
}

export interface MyGradeResponse {
  id: string
  submissionId: string
  value: number
  feedback: string
  lastModifiedAt: string | null
}

export interface CreateGradeRequest {
  value: number
  feedback: string
}

export interface UpdateGradeRequest {
  value: number
  feedback: string
}


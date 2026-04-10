export interface ExamResultResponse {
  id: string
  examId: string
  examTitle: string
  spaceId: string
  spaceCode: string
  spaceName: string
  studentUserId: string
  studentEmail: string
  studentFullName: string
  value: number
  feedback: string | null
  published: boolean
  gradedByUserId: string
  gradedAt: string
  lastModifiedAt: string | null
  publishedAt: string | null
}

export interface MyExamResultResponse {
  id: string
  examId: string
  examTitle: string
  spaceCode: string
  spaceName: string
  value: number
  feedback: string | null
  publishedAt: string | null
  lastModifiedAt: string | null
}

export interface CreateExamResultRequest {
  studentEmail: string
  value: number
  feedback: string
  published: boolean
}

export interface UpdateExamResultRequest {
  value: number
  feedback: string
  published: boolean
}
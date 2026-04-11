export type FeedbackQuestionType = 'RATING' | 'TEXT'

export interface FeedbackFormQuestion {
  id: string
  prompt: string
  questionType: FeedbackQuestionType
  required: boolean
  displayOrder: number
}

export interface FeedbackForm {
  id: string
  examId: string
  examTitle: string
  spaceId: string
  spaceCode: string
  spaceName: string
  title: string
  description: string | null
  published: boolean
  createdAt: string
  updatedAt: string
  canManage: boolean
  alreadySubmitted: boolean
  responseCount: number | null
  questions: FeedbackFormQuestion[]
}

export interface FeedbackFormQuestionPayload {
  prompt: string
  questionType: FeedbackQuestionType
  required: boolean
  displayOrder: number
}

export interface FeedbackFormPayload {
  title: string
  description: string
  published: boolean
  questions: FeedbackFormQuestionPayload[]
}

export interface FeedbackFormSubmissionAnswerPayload {
  questionId: string
  ratingValue: number | null
  textValue: string | null
}

export interface FeedbackFormSubmissionPayload {
  answers: FeedbackFormSubmissionAnswerPayload[]
}

export interface FeedbackFormSubmissionReceipt {
  submissionId: string
  formId: string
  submittedAt: string
}

export interface FeedbackFormAnswer {
  questionId: string
  ratingValue: number | null
  textValue: string | null
}

export interface FeedbackFormSubmission {
  id: string
  studentUserId: string
  studentEmail: string
  studentFullName: string
  submittedAt: string
  answers: FeedbackFormAnswer[]
}

export interface FeedbackFormQuestionSummary {
  questionId: string
  prompt: string
  questionType: FeedbackQuestionType
  responseCount: number
  averageRating: number | null
  ratingCounts: Record<string, number>
}

export interface FeedbackFormReview {
  formId: string
  title: string
  examTitle: string
  spaceCode: string
  spaceName: string
  responseCount: number
  questionSummaries: FeedbackFormQuestionSummary[]
  submissions: FeedbackFormSubmission[]
}
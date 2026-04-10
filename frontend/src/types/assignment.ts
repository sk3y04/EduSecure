export interface AssignmentSummary {
  id: string
  title: string
  dueAt: string
  spaceId: string
  open: boolean
  latestSubmissionId: string | null
  latestSubmittedAt: string | null
}

export interface AssignmentResponse extends AssignmentSummary {
  description: string
  createdByLecturerId: string
}

export interface CreateAssignmentRequest {
  title: string
  description: string
  dueAt: string
  spaceId: string
}


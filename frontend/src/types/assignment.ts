export interface AssignmentSummary {
  id: string
  title: string
  dueAt: string
  open: boolean
}

export interface AssignmentResponse extends AssignmentSummary {
  description: string
  createdByLecturerId: string
}

export interface CreateAssignmentRequest {
  title: string
  description: string
  dueAt: string
}


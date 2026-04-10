export interface Exam {
  id: string
  spaceId: string
  spaceCode: string
  spaceName: string
  title: string
  description: string | null
  location: string
  startsAt: string
  endsAt: string
  published: boolean
  createdByUserId: string
  createdAt: string
  updatedAt: string
  canManage: boolean
}

export interface ExamPayload {
  spaceId: string
  title: string
  description: string
  location: string
  startsAt: string
  endsAt: string
  published: boolean
}
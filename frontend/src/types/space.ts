export interface SpaceSummary {
  id: string
  name: string
  code: string
  description: string
  archived: boolean
  memberCount: number
  canManage: boolean
  isMember: boolean
}

export interface SpaceStudent {
  studentUserId: string
  studentEmail: string
  studentFullName: string
  addedByUserId: string
  addedAt: string
}

export interface SpaceDetail extends SpaceSummary {
  createdByUserId: string
  createdAt: string
  updatedAt: string
  memberships: SpaceStudent[]
}

export interface CreateSpaceRequest {
  name: string
  code: string
  description: string
}

export interface UpdateSpaceRequest extends CreateSpaceRequest {
  archived: boolean
}

export interface AddSpaceStudentRequest {
  studentEmail: string
}
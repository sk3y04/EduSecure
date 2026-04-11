export type AttendanceStatus = 'PRESENT' | 'LATE' | 'ABSENT' | 'EXCUSED'

export interface AttendanceSession {
  id: string
  spaceId: string
  spaceCode: string
  spaceName: string
  title: string
  description: string | null
  startsAt: string
  endsAt: string
  createdByUserId: string
  createdAt: string
  updatedAt: string
  canManage: boolean
  myStatus: AttendanceStatus | null
  memberCount: number
  recordedCount: number
  presentCount: number
  lateCount: number
  absentCount: number
  excusedCount: number
}

export interface AttendanceSessionPayload {
  spaceId: string
  title: string
  description: string
  startsAt: string
  endsAt: string
}

export interface UpdateAttendanceSessionPayload {
  title: string
  description: string
  startsAt: string
  endsAt: string
}

export interface AttendanceStudentRecord {
  studentUserId: string
  studentEmail: string
  studentFullName: string
  status: AttendanceStatus | null
  recordedByUserId: string | null
  recordedAt: string | null
}

export interface AttendanceSessionRecordsResponse {
  session: AttendanceSession
  records: AttendanceStudentRecord[]
}

export interface AttendanceRecordUpdateItem {
  studentUserId: string
  status: AttendanceStatus | null
}

export interface UpdateAttendanceRecordsPayload {
  records: AttendanceRecordUpdateItem[]
}


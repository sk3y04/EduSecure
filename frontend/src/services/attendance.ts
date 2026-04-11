import http from '@/services/http'
import type {
  AttendanceSession,
  AttendanceSessionPayload,
  AttendanceSessionRecordsResponse,
  UpdateAttendanceRecordsPayload,
  UpdateAttendanceSessionPayload,
} from '@/types/attendance'

export const attendanceService = {
  async listSessions(): Promise<AttendanceSession[]> {
    const response = await http.get<AttendanceSession[]>('/attendance-sessions')
    return response.data
  },

  async createSession(payload: AttendanceSessionPayload): Promise<AttendanceSession> {
    const response = await http.post<AttendanceSession>('/attendance-sessions', payload)
    return response.data
  },

  async updateSession(sessionId: string, payload: UpdateAttendanceSessionPayload): Promise<AttendanceSession> {
    const response = await http.put<AttendanceSession>(`/attendance-sessions/${sessionId}`, payload)
    return response.data
  },

  async getSessionRecords(sessionId: string): Promise<AttendanceSessionRecordsResponse> {
    const response = await http.get<AttendanceSessionRecordsResponse>(`/attendance-sessions/${sessionId}/records`)
    return response.data
  },

  async updateSessionRecords(sessionId: string, payload: UpdateAttendanceRecordsPayload): Promise<AttendanceSessionRecordsResponse> {
    const response = await http.put<AttendanceSessionRecordsResponse>(`/attendance-sessions/${sessionId}/records`, payload)
    return response.data
  },
}


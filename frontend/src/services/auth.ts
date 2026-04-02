import http from '@/services/http'
import type {
  AuthResponse,
  CreateManagedUserRequest,
  CurrentUserResponse,
  LoginRequest,
  MfaDisableRequest,
  MfaEnableRequest,
  MfaEnableResponse,
  MfaSetupResponse,
  MfaStatusResponse,
  MfaVerifyRequest,
} from '@/types/auth'

export const authService = {
  async createManagedUser(payload: CreateManagedUserRequest): Promise<CurrentUserResponse> {
    const response = await http.post<CurrentUserResponse>('/auth/users', payload)
    return response.data
  },

  async login(payload: LoginRequest): Promise<AuthResponse> {
    const response = await http.post<AuthResponse>('/auth/login', payload)
    return response.data
  },

  async logout(): Promise<void> {
    await http.post('/auth/logout')
  },

  async getCurrentUser(): Promise<CurrentUserResponse> {
    const response = await http.get<CurrentUserResponse>('/auth/me')
    return response.data
  },

  async getMfaStatus(): Promise<MfaStatusResponse> {
    const response = await http.get<MfaStatusResponse>('/auth/mfa/status')
    return response.data
  },

  async setupMfa(): Promise<MfaSetupResponse> {
    const response = await http.post<MfaSetupResponse>('/auth/mfa/setup')
    return response.data
  },

  async enableMfa(payload: MfaEnableRequest): Promise<MfaEnableResponse> {
    const response = await http.post<MfaEnableResponse>('/auth/mfa/enable', payload)
    return response.data
  },

  async verifyMfa(payload: MfaVerifyRequest): Promise<AuthResponse> {
    const response = await http.post<AuthResponse>('/auth/mfa/verify', payload)
    return response.data
  },

  async disableMfa(payload: MfaDisableRequest): Promise<void> {
    await http.post('/auth/mfa/disable', payload)
  },
}


export type RoleName = 'STUDENT' | 'LECTURER' | 'ADMIN'
export type AuthStatus = 'AUTHENTICATED' | 'MFA_REQUIRED'
export type MfaMethod = 'TOTP' | string

export interface LoginRequest {
  email: string
  password: string
}

export interface CreateManagedUserRequest {
  email: string
  password: string
  fullName: string
  role: Exclude<RoleName, 'ADMIN'>
}

export interface AuthResponse {
  authStatus: AuthStatus
  userId: string | null
  email: string | null
  fullName: string | null
  roles: RoleName[] | null
  token: string | null
  mfaEnabled: boolean | null
  amr: string[] | null
  challengeId: string | null
  mfaMethod: MfaMethod | null
  expiresAt: string | null
  remainingAttempts: number | null
}

export interface CurrentUserResponse {
  userId: string
  email: string
  fullName: string
  roles: RoleName[]
}

export interface MfaPendingChallenge {
  challengeId: string
  mfaMethod: MfaMethod
  expiresAt: string | null
  remainingAttempts: number | null
}

export interface MfaStatusResponse {
  mfaEnabled: boolean
  mfaMethod: MfaMethod | null
  recoveryCodesRemaining: number
  enabledAt: string | null
}

export interface MfaSetupResponse {
  mfaMethod: MfaMethod
  manualEntryKey: string
  otpauthUri: string
}

export interface MfaEnableRequest {
  verificationCode: string
}

export interface MfaEnableResponse {
  mfaEnabled: boolean
  mfaMethod: MfaMethod | null
  recoveryCodes: string[]
}

export interface MfaVerifyRequest {
  challengeId: string
  verificationCode: string
}

export interface MfaDisableRequest {
  password: string
  verificationCode: string
}

export function mapAuthResponseToCurrentUser(response: AuthResponse): CurrentUserResponse | null {
  if (!response.userId || !response.email || !response.fullName || !response.roles) {
    return null
  }

  return {
    userId: response.userId,
    email: response.email,
    fullName: response.fullName,
    roles: response.roles,
  }
}


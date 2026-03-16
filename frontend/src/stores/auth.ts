import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { authService } from '@/services/auth'
import { extractErrorMessage, MFA_CHALLENGE_STORAGE_KEY } from '@/services/http'
import {
  mapAuthResponseToCurrentUser,
  type CurrentUserResponse,
  type LoginRequest,
  type MfaPendingChallenge,
  type RoleName,
} from '@/types/auth'

interface LoginResult {
  requiresMfa: boolean
}

function readPendingChallengeFromStorage(): MfaPendingChallenge | null {
  if (typeof window === 'undefined') {
    return null
  }

  const storedValue = window.sessionStorage.getItem(MFA_CHALLENGE_STORAGE_KEY)

  if (!storedValue) {
    return null
  }

  try {
    return JSON.parse(storedValue) as MfaPendingChallenge
  } catch {
    window.sessionStorage.removeItem(MFA_CHALLENGE_STORAGE_KEY)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<CurrentUserResponse | null>(null)
  const pendingChallenge = ref<MfaPendingChallenge | null>(readPendingChallengeFromStorage())
  const initialized = ref(false)
  const isInitializing = ref(false)
  const errorMessage = ref<string | null>(null)

  const isAuthenticated = computed(() => Boolean(user.value))

  function setPendingChallenge(value: MfaPendingChallenge | null) {
    pendingChallenge.value = value

    if (typeof window === 'undefined') {
      return
    }

    if (value) {
      window.sessionStorage.setItem(MFA_CHALLENGE_STORAGE_KEY, JSON.stringify(value))
    } else {
      window.sessionStorage.removeItem(MFA_CHALLENGE_STORAGE_KEY)
    }
  }

  function clearError() {
    errorMessage.value = null
  }

  function clearAuthState() {
    setPendingChallenge(null)
    user.value = null
    clearError()
  }

  async function fetchCurrentUser(): Promise<CurrentUserResponse | null> {
    const currentUser = await authService.getCurrentUser()
    user.value = currentUser
    return currentUser
  }

  async function initialize() {
    if (initialized.value || isInitializing.value) {
      return
    }

    isInitializing.value = true

    try {
      const currentUser = await fetchCurrentUser().catch(() => null)

      if (currentUser) {
        setPendingChallenge(null)
      }
      user.value = currentUser
    } finally {
      initialized.value = true
      isInitializing.value = false
    }
  }

  async function login(credentials: LoginRequest): Promise<LoginResult> {
    clearError()
    setPendingChallenge(null)

    let response

    try {
      response = await authService.login(credentials)
    } catch (error) {
      clearAuthState()
      errorMessage.value = extractErrorMessage(error)
      throw error
    }

    if (response.authStatus === 'MFA_REQUIRED') {
      user.value = null
      setPendingChallenge({
        challengeId: response.challengeId ?? '',
        mfaMethod: response.mfaMethod ?? 'TOTP',
        expiresAt: response.expiresAt,
        remainingAttempts: response.remainingAttempts,
      })
      return { requiresMfa: true }
    }

    if (response.authStatus !== 'AUTHENTICATED') {
      clearAuthState()
      errorMessage.value = 'Authentication did not complete successfully.'
      throw new Error(errorMessage.value)
    }

    user.value = mapAuthResponseToCurrentUser(response)
    setPendingChallenge(null)
    await fetchCurrentUser()
    return { requiresMfa: false }
  }

  async function verifyMfa(verificationCode: string): Promise<void> {
    clearError()

    if (!pendingChallenge.value?.challengeId) {
      errorMessage.value = 'No MFA challenge is available. Please sign in again.'
      throw new Error(errorMessage.value)
    }

    let response

    try {
      response = await authService.verifyMfa({
        challengeId: pendingChallenge.value.challengeId,
        verificationCode,
      })
    } catch (error) {
      errorMessage.value = extractErrorMessage(error)
      throw error
    }

    if (response.authStatus !== 'AUTHENTICATED') {
      errorMessage.value = 'MFA verification did not complete authentication.'
      throw new Error(errorMessage.value)
    }

    user.value = mapAuthResponseToCurrentUser(response)
    setPendingChallenge(null)
    await fetchCurrentUser()
  }

  async function logout() {
    try {
      await authService.logout()
    } finally {
      clearAuthState()
      initialized.value = true
    }
  }

  function hasAnyRole(roles: RoleName[]): boolean {
    return roles.some((role) => user.value?.roles.includes(role))
  }

  return {
    user,
    pendingChallenge,
    initialized,
    isInitializing,
    isAuthenticated,
    errorMessage,
    clearError,
    initialize,
    fetchCurrentUser,
    login,
    verifyMfa,
    logout,
    hasAnyRole,
  }
})


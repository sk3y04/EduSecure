import axios, { type AxiosError } from 'axios'

import type { ApiErrorResponse } from '@/types/api'

export const MFA_CHALLENGE_STORAGE_KEY = 'edusecure.mfa.challenge'

type UnauthorizedHandler = (error: AxiosError<ApiErrorResponse>) => void | Promise<void>

let unauthorizedHandler: UnauthorizedHandler | null = null

const SESSION_EXPIRY_EXEMPT_PATH_SUFFIXES = ['/api/auth/login', '/api/auth/register', '/api/auth/mfa/verify']

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
})

function resolveRequestPath(url?: string, baseURL?: string): string {
  if (!url) {
    return ''
  }

  try {
    return new URL(url, baseURL ?? http.defaults.baseURL).pathname
  } catch {
    return url
  }
}

function shouldIgnoreUnauthorizedError(error: AxiosError<ApiErrorResponse>): boolean {
  const requestPath = resolveRequestPath(error.config?.url, error.config?.baseURL)
  return SESSION_EXPIRY_EXEMPT_PATH_SUFFIXES.some((suffix) => requestPath.endsWith(suffix))
}

http.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    if (
      unauthorizedHandler
      && axios.isAxiosError<ApiErrorResponse>(error)
      && error.response?.status === 401
      && !shouldIgnoreUnauthorizedError(error)
    ) {
      await unauthorizedHandler(error)
    }

    return Promise.reject(error)
  },
)

export function registerUnauthorizedHandler(handler: UnauthorizedHandler | null) {
  unauthorizedHandler = handler
}


export function extractErrorMessages(error: unknown): string[] {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const responseBody = error.response?.data
    const fieldMessages = responseBody?.errors ? Object.values(responseBody.errors).flat() : []

    if (fieldMessages.length > 0) {
      return fieldMessages
    }

    if (responseBody?.message) {
      return [responseBody.message]
    }

    if (error.response?.status === 401) {
      return ['Your session is no longer valid. Please sign in again.']
    }
  }

  if (error instanceof Error && error.message) {
    return [error.message]
  }

  return ['An unexpected error occurred.']
}

export function extractErrorMessage(error: unknown): string {
  return extractErrorMessages(error)[0] ?? 'An unexpected error occurred.'
}

export default http



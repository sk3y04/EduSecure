import axios from 'axios'

import type { ApiErrorResponse } from '@/types/api'

export const MFA_CHALLENGE_STORAGE_KEY = 'edusecure.mfa.challenge'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api',
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})


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



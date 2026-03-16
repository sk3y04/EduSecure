export type FieldErrors = Record<string, string[]>

export interface ApiErrorResponse {
  message: string
  errors?: FieldErrors
}


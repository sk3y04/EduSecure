import 'vue-router'

import type { RoleName } from '@/types/auth'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    guestOnly?: boolean
    roles?: RoleName[]
    requiresPendingChallenge?: boolean
  }
}


import { createRouter, createWebHistory } from 'vue-router'

import AppShell from '@/components/AppShell.vue'
import { useAuthStore } from '@/stores/auth'
import type { RoleName } from '@/types/auth'
import AssignmentListView from '@/views/assignments/AssignmentListView.vue'
import LoginView from '@/views/auth/LoginView.vue'
import MfaChallengeView from '@/views/auth/MfaChallengeView.vue'
import AccountSecurityView from '@/views/security/AccountSecurityView.vue'
import SpaceDetailView from '@/views/spaces/SpaceDetailView.vue'
import SpaceListView from '@/views/spaces/SpaceListView.vue'
import SubmissionCreateView from '@/views/submissions/SubmissionCreateView.vue'
import SubmissionDetailView from '@/views/submissions/SubmissionDetailView.vue'
import UserManagementView from '@/views/users/UserManagementView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      meta: {
        requiresAuth: false,
        guestOnly: true,
      },
    },
    {
      path: '/mfa',
      name: 'mfa',
      component: MfaChallengeView,
      meta: {
        requiresAuth: false,
        guestOnly: true,
        requiresPendingChallenge: true,
      },
    },
    {
      path: '/',
      component: AppShell,
      meta: {
        requiresAuth: true,
      },
      children: [
        {
          path: '',
          redirect: { name: 'assignments' },
        },
        {
          path: 'assignments',
          name: 'assignments',
          component: AssignmentListView,
        },
        {
          path: 'spaces',
          name: 'spaces',
          component: SpaceListView,
        },
        {
          path: 'spaces/:spaceId',
          name: 'space-detail',
          component: SpaceDetailView,
        },
        {
          path: 'assignments/:assignmentId/submit',
          name: 'submission-create',
          component: SubmissionCreateView,
          meta: {
            roles: ['STUDENT'] satisfies RoleName[],
          },
        },
        {
          path: 'submissions/:submissionId',
          name: 'submission-detail',
          component: SubmissionDetailView,
        },
        {
          path: 'security/mfa',
          name: 'account-security',
          component: AccountSecurityView,
        },
        {
          path: 'users',
          name: 'user-management',
          component: UserManagementView,
          meta: {
            roles: ['ADMIN', 'LECTURER'] satisfies RoleName[],
          },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: { name: 'assignments' },
    },
  ],
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  if (!authStore.initialized) {
    await authStore.initialize()
  }

  if (to.name === 'login' && authStore.pendingChallenge && !authStore.isAuthenticated) {
    return { name: 'mfa', query: to.query }
  }

  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return { name: 'assignments' }
  }

  if (to.meta.requiresPendingChallenge && !authStore.pendingChallenge) {
    return { name: 'login' }
  }

  if (to.meta.requiresAuth !== false && !authStore.isAuthenticated) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  if (to.meta.roles && !authStore.hasAnyRole(to.meta.roles)) {
    return { name: 'assignments' }
  }

  return true
})

export default router


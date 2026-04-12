import { createRouter, createWebHistory } from 'vue-router'

import AppShell from '@/components/AppShell.vue'
import { useAuthStore } from '@/stores/auth'
import type { RoleName } from '@/types/auth'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/Login/index.vue'),
      meta: {
        requiresAuth: false,
        guestOnly: true,
      },
    },
    {
      path: '/mfa',
      name: 'mfa',
      component: () => import('@/pages/MfaChallenge/index.vue'),
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
          redirect: { name: 'spaces' },
        },
        {
          path: 'attendance',
          name: 'attendance',
          component: () => import('@/pages/Attendance/index.vue'),
        },
        {
          path: 'assignments',
          name: 'assignments',
          component: () => import('@/pages/AssignmentList/index.vue'),
        },
        {
          path: 'exams',
          name: 'exams',
          component: () => import('@/pages/ExamSchedule/index.vue'),
        },
        {
          path: 'exam-results',
          name: 'exam-results',
          redirect: { name: 'exams', query: { view: 'results' } },
        },
        {
          path: 'feedback-forms',
          name: 'feedback-forms',
          component: () => import('@/pages/FeedbackForms/index.vue'),
        },
        {
          path: 'spaces',
          name: 'spaces',
          component: () => import('@/pages/SpaceList/index.vue'),
        },
        {
          path: 'spaces/:spaceId',
          name: 'space-detail',
          component: () => import('@/pages/SpaceDetail/index.vue'),
        },
        {
          path: 'registration-requests',
          name: 'registration-requests',
          component: () => import('@/pages/RegistrationRequests/index.vue'),
          meta: {
            roles: ['STUDENT'] satisfies RoleName[],
          },
        },
        {
          path: 'registration-review',
          name: 'registration-review',
          component: () => import('@/pages/RegistrationReview/index.vue'),
          meta: {
            roles: ['LECTURER', 'ADMIN'] satisfies RoleName[],
          },
        },
        {
          path: 'assignments/:assignmentId/submit',
          name: 'submission-create',
          component: () => import('@/pages/SubmissionCreate/index.vue'),
          meta: {
            roles: ['STUDENT'] satisfies RoleName[],
          },
        },
        {
          path: 'assignments/:assignmentId/submissions',
          name: 'assignment-submissions',
          component: () => import('@/pages/AssignmentSubmissions/index.vue'),
          meta: {
            roles: ['LECTURER', 'ADMIN'] satisfies RoleName[],
          },
        },
        {
          path: 'submissions/:submissionId',
          name: 'submission-detail',
          component: () => import('@/pages/SubmissionDetail/index.vue'),
        },
        {
          path: 'security/mfa',
          name: 'account-security',
          component: () => import('@/pages/AccountSecurity/index.vue'),
        },
        {
          path: 'users',
          name: 'user-management',
          component: () => import('@/pages/UserManagement/index.vue'),
          meta: {
            roles: ['ADMIN', 'LECTURER'] satisfies RoleName[],
          },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: { name: 'spaces' },
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
    return { name: 'spaces' }
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
    return { name: 'spaces' }
  }

  return true
})

export default router


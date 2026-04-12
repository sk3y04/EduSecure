<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import AppShellContextPanel from '@/components/app-shell/AppShellContextPanel.vue'
import AppShellHeader from '@/components/app-shell/AppShellHeader.vue'
import AppShellSidebar from '@/components/app-shell/AppShellSidebar.vue'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const navigationItems = computed(() => {
  const items = [
    {
      label: 'Spaces',
      to: { name: 'spaces' },
    },
    {
      label: 'Attendance',
      to: { name: 'attendance' },
    },
    {
      label: 'Assessments',
      to: { name: 'exams' },
    },
    {
      label: 'Feedback forms',
      to: { name: 'feedback-forms' },
    },
  ]

  if (authStore.hasAnyRole(['STUDENT'])) {
    items.push({
      label: 'Registration requests',
      to: { name: 'registration-requests' },
    })
  }

  if (authStore.hasAnyRole(['ADMIN', 'LECTURER'])) {
    items.push({
      label: 'Registration review',
      to: { name: 'registration-review' },
    })
    items.push({
      label: 'User management',
      to: { name: 'user-management' },
    })
  }

  items.push({
    label: 'Security',
    to: { name: 'account-security' },
  })

  return items
})

const routeTitles = computed<Record<string, string>>(() => ({
  spaces: 'Spaces',
  'space-detail': 'Space',
  attendance: 'Attendance',
  assignments: 'Assignments',
  exams: 'Assessments',
  'exam-results': 'Assessments',
  'feedback-forms': 'Feedback forms',
  'registration-requests': 'Registration requests',
  'registration-review': 'Registration review',
  'assignment-submissions': 'Submissions',
  'submission-create': 'New submission',
  'submission-detail': 'Submission',
  'account-security': 'Security',
  'user-management': 'User management',
}))

const currentSection = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  return navigationItems.value.find((item) => item.to.name === routeName)?.label
    ?? routeTitles.value[routeName]
    ?? 'Workspace'
})

const dateLabel = computed(() =>
  new Intl.DateTimeFormat('en-US', {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
  }).format(new Date()),
)

const primaryRole = computed(() => {
  const role = authStore.user?.roles[0] ?? 'WORKSPACE USER'
  return role
    .toLowerCase()
    .split('_')
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(' ')
})

async function handleLogout() {
  await authStore.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="desktop-shell">
    <AppShellSidebar :navigation-items="navigationItems" />

    <section class="desktop-shell-content">
      <div class="desktop-shell-topbar">
        <AppShellHeader :title="currentSection" :date-label="dateLabel" />

        <AppShellContextPanel
          :user="authStore.user"
          :primary-role="primaryRole"
          @logout="handleLogout"
        />
      </div>

      <main class="desktop-shell-view">
        <RouterView />
      </main>
    </section>
  </div>
</template>


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
      caption: 'Browse collaboration areas, rostered modules, and managed space access.',
      to: { name: 'spaces' },
    },
    {
      label: 'Attendance',
      caption: 'Track attendance checkpoints and session visibility across teaching spaces.',
      to: { name: 'attendance' },
    },
    {
      label: 'Exams',
      caption: 'Review exam schedules, deadlines, and upcoming academic milestones.',
      to: { name: 'exams' },
    },
    {
      label: 'Exam results',
      caption: 'Inspect marked outcomes and results released to the current user.',
      to: { name: 'exam-results' },
    },
    {
      label: 'Feedback forms',
      caption: 'Open institutional feedback workflows and response summaries.',
      to: { name: 'feedback-forms' },
    },
  ]

  if (authStore.hasAnyRole(['STUDENT'])) {
    items.push({
      label: 'Registration requests',
      caption: 'Request access to staff-managed spaces using approved course codes.',
      to: { name: 'registration-requests' },
    })
  }

  if (authStore.hasAnyRole(['ADMIN', 'LECTURER'])) {
    items.push({
      label: 'Registration review',
      caption: 'Approve or reject pending registration requests for controlled spaces.',
      to: { name: 'registration-review' },
    })
    items.push({
      label: 'User management',
      caption: 'Provision managed accounts and review staff-controlled access roles.',
      to: { name: 'user-management' },
    })
  }

  items.push({
    label: 'Account security',
    caption: 'Configure MFA posture and personal security controls for this account.',
    to: { name: 'account-security' },
  })

  return items
})

const routeDescriptions = computed<Record<string, { title: string; description: string }>>(() => ({
  spaces: {
    title: 'Spaces overview',
    description: 'Review academic spaces, open detailed work areas, and keep related actions within a balanced desktop canvas.',
  },
  'space-detail': {
    title: 'Space detail',
    description: 'Manage a single space with metadata, assignments, chat, and roster operations arranged in one scrollable workspace.',
  },
  attendance: {
    title: 'Attendance tracking',
    description: 'Monitor scheduled attendance workflows with a dedicated workspace for records, filters, and session actions.',
  },
  assignments: {
    title: 'Assignments',
    description: 'Review coursework deadlines and secure submission pathways from a full-width working area.',
  },
  exams: {
    title: 'Exam schedule',
    description: 'Keep important assessment timelines visible inside a focused desktop layout.',
  },
  'exam-results': {
    title: 'Exam results',
    description: 'Surface marked outcomes and supporting result data in a calm, high-contrast workspace.',
  },
  'feedback-forms': {
    title: 'Feedback forms',
    description: 'Work through form availability, response management, and academic follow-up without leaving the shell.',
  },
  'registration-requests': {
    title: 'Registration requests',
    description: 'Submit and track controlled access requests using a structured desktop canvas.',
  },
  'registration-review': {
    title: 'Registration review',
    description: 'Process pending access requests from a dedicated review workspace with persistent account context.',
  },
  'assignment-submissions': {
    title: 'Submission review',
    description: 'Inspect student submissions and audit supporting evidence in a focused review surface.',
  },
  'submission-create': {
    title: 'Submit coursework',
    description: 'Prepare and send coursework inside a clear, distraction-light desktop submission flow.',
  },
  'submission-detail': {
    title: 'Submission detail',
    description: 'Review submission contents, audit evidence, and academic status from a single desktop canvas.',
  },
  'account-security': {
    title: 'Account security',
    description: 'Manage MFA and personal access controls while keeping identity context pinned to the right rail.',
  },
  'user-management': {
    title: 'User management',
    description: 'Provision managed users and review access roles from a structured desktop control panel.',
  },
}))

const currentSection = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  const fromNavigation = navigationItems.value.find((item) => item.to.name === routeName)

  if (fromNavigation) {
    return {
      title: fromNavigation.label,
      description: fromNavigation.caption,
    }
  }

  return routeDescriptions.value[routeName] ?? {
    title: 'Workspace overview',
    description: 'Navigate academic operations from a desktop shell that keeps navigation, content, and identity in view.',
  }
})

const dateLabel = computed(() =>
  new Intl.DateTimeFormat('en-US', {
    weekday: 'long',
    month: 'long',
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

    <AppShellHeader
      :title="currentSection.title"
      :description="currentSection.description"
      :date-label="dateLabel"
    />

    <div class="desktop-shell-main">
      <main class="desktop-shell-view">
        <RouterView />
      </main>
    </div>

    <AppShellContextPanel
      :user="authStore.user"
      :primary-role="primaryRole"
      @logout="handleLogout"
    />
  </div>
</template>


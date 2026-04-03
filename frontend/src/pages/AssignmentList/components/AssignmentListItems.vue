<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { AssignmentSummary } from '@/types/assignment'

const props = defineProps<{
  assignments: AssignmentSummary[]
  isLoading: boolean
  loadError: string | null
  isStudent: boolean
  canReviewSubmissions: boolean
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function hasExistingSubmission(assignment: AssignmentSummary): boolean {
  return Boolean(assignment.latestSubmissionId)
}
</script>

<template>
  <section class="surface-panel p-8">
    <div class="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="text-xl font-semibold text-slate-900">Visible assignments</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          Students can open the secure submission form directly from an active assignment.
        </p>
      </div>
      <button type="button" class="btn-secondary self-start sm:self-auto" @click="emit('refresh')">
        Refresh
      </button>
    </div>

    <div v-if="props.loadError" class="alert-error mb-4">
      {{ props.loadError }}
    </div>

    <div v-if="props.isLoading" class="empty-state">
      Loading assignments…
    </div>

    <div v-else-if="props.assignments.length === 0" class="empty-state">
      No assignments are available yet.
    </div>

    <div v-else class="space-y-4">
      <article
        v-for="assignment in props.assignments"
        :key="assignment.id"
        class="rounded-sm border border-slate-300 bg-white p-5"
      >
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <h4 class="text-lg font-semibold text-slate-900">{{ assignment.title }}</h4>
            <p class="mt-2 text-sm text-slate-600">Due {{ formatDate(assignment.dueAt) }}</p>
            <p v-if="props.isStudent && assignment.latestSubmittedAt" class="mt-2 text-sm text-slate-600">
              Latest submission: {{ formatDate(assignment.latestSubmittedAt) }}
            </p>
          </div>
          <div class="flex flex-wrap gap-2">
            <span
              class="status-pill"
              :class="assignment.open ? 'border-emerald-300 bg-emerald-50 text-emerald-800' : 'border-amber-300 bg-amber-50 text-amber-800'"
            >
              {{ assignment.open ? 'Open' : 'Closed' }}
            </span>
            <span
              v-if="props.isStudent && hasExistingSubmission(assignment)"
              class="status-pill border-sky-300 bg-sky-50 text-sky-800"
            >
              Submitted
            </span>
          </div>
        </div>

        <div class="mt-5 flex flex-wrap items-center gap-3 border-t border-slate-200 pt-4">
          <template v-if="props.isStudent">
            <RouterLink
              :to="{ name: 'submission-create', params: { assignmentId: assignment.id } }"
              class="btn-primary"
            >
              Submit work
            </RouterLink>
            <RouterLink
              v-if="assignment.latestSubmissionId"
              :to="{ name: 'submission-detail', params: { submissionId: assignment.latestSubmissionId } }"
              class="btn-secondary"
            >
              View latest submission
            </RouterLink>
          </template>
          <RouterLink
            v-else-if="props.canReviewSubmissions"
            :to="{ name: 'assignment-submissions', params: { assignmentId: assignment.id } }"
            class="btn-secondary"
          >
            View submissions
          </RouterLink>
          <span v-else class="text-sm text-slate-500">
            Submission action is reserved for authenticated students.
          </span>
        </div>
      </article>
    </div>
  </section>
</template>


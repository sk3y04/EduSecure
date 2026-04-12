<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { AssignmentSummary } from '@/types/assignment'

const props = defineProps<{
  assignments: AssignmentSummary[]
  isLoading: boolean
  loadError: string | null
  isStudent: boolean
  canReviewSubmissions: boolean
  spaceId?: string | null
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
  <section class="page-section desktop-page-panel panel-shell panel-shell-min-24">
    <div class="panel-header-split">
      <div>
        <h3 class="panel-title">Visible assignments</h3>
        <p class="panel-copy">
          Students can open the secure submission form directly from an active assignment.
        </p>
      </div>
      <button type="button" class="btn-secondary self-start sm:self-auto" @click="emit('refresh')">
        Refresh
      </button>
    </div>

    <div class="min-h-0 flex-1">
      <div v-if="props.loadError" class="alert-error mb-4">
        {{ props.loadError }}
      </div>

      <div v-if="props.isLoading" class="empty-state">
        Loading assignments…
      </div>

      <div v-else-if="props.assignments.length === 0" class="empty-state">
        No assignments are available yet.
      </div>

      <div v-else class="panel-scroll-list h-full">
        <article
          v-for="assignment in props.assignments"
          :key="assignment.id"
          class="record-card"
        >
          <div class="record-card-frame">
            <div>
              <h4 class="font-display text-xl font-semibold text-[var(--color-heading)]">{{ assignment.title }}</h4>
              <p class="mt-2 text-base text-[var(--color-text-soft)]">Due {{ formatDate(assignment.dueAt) }}</p>
              <p v-if="props.isStudent && assignment.latestSubmittedAt" class="mt-2 text-base text-[var(--color-text-soft)]">
                Latest submission: {{ formatDate(assignment.latestSubmittedAt) }}
              </p>
            </div>
            <div class="flex flex-wrap gap-2">
              <span
                class="status-pill"
                :class="assignment.open ? 'status-pill-success' : 'status-pill-warning'"
              >
                {{ assignment.open ? 'Open' : 'Closed' }}
              </span>
              <span
                v-if="props.isStudent && hasExistingSubmission(assignment)"
                class="status-pill status-pill-neutral"
              >
                Submitted
              </span>
            </div>
          </div>

          <div class="record-card-footer">
            <template v-if="props.isStudent">
              <RouterLink
                :to="{
                  name: 'submission-create',
                  params: { assignmentId: assignment.id },
                  query: props.spaceId ? { spaceId: props.spaceId } : undefined,
                }"
                class="btn-primary"
              >
                Submit work
              </RouterLink>
              <RouterLink
                v-if="assignment.latestSubmissionId"
                :to="{
                  name: 'submission-detail',
                  params: { submissionId: assignment.latestSubmissionId },
                  query: props.spaceId ? { spaceId: props.spaceId } : undefined,
                }"
                class="btn-secondary"
              >
                View latest submission
              </RouterLink>
            </template>
            <RouterLink
              v-else-if="props.canReviewSubmissions"
              :to="{
                name: 'assignment-submissions',
                params: { assignmentId: assignment.id },
                query: props.spaceId ? { spaceId: props.spaceId } : undefined,
              }"
              class="btn-secondary"
            >
              View submissions
            </RouterLink>
            <span v-else class="text-base text-[var(--color-text-soft)]">
              Submission action is reserved for authenticated students.
            </span>
          </div>
        </article>
      </div>
    </div>
  </section>
</template>


<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { submissionsService } from '@/services/submissions'
import type { SubmissionResponse } from '@/types/submission'

const route = useRoute()

const assignmentId = computed(() => String(route.params.assignmentId ?? ''))
const spaceId = computed(() => {
  const value = route.query.spaceId
  return typeof value === 'string' && value.length > 0 ? value : null
})
const submissions = ref<SubmissionResponse[]>([])
const isLoading = ref(true)
const errorMessage = ref<string | null>(null)

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function verificationTone(status: SubmissionResponse['verificationStatus']): string {
  switch (status) {
    case 'VERIFIED':
      return 'status-pill-success'
    case 'FAILED_VERIFICATION':
    case 'REJECTED':
      return 'status-pill-danger'
    default:
      return 'status-pill-warning'
  }
}

async function loadSubmissions() {
  isLoading.value = true
  errorMessage.value = null

  try {
    submissions.value = await submissionsService.listForAssignment(assignmentId.value)
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void loadSubmissions()
})
</script>

<template>
  <section class="space-y-6">
    <div class="page-hero">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker">Assignment submissions</p>
          <h2 class="section-title">Review submitted work before grading</h2>
          <p class="section-copy">
            Open a submission to inspect its integrity evidence, retrieve content, and assign a grade
            expressed as a percentage from 0 to 100.
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button type="button" class="btn-secondary" @click="loadSubmissions">Refresh</button>
          <RouterLink
            :to="spaceId ? { name: 'space-detail', params: { spaceId } } : { name: 'spaces' }"
            class="btn-secondary"
          >
            {{ spaceId ? 'Back to space' : 'Back to spaces' }}
          </RouterLink>
        </div>
      </div>

      <div class="surface-panel-muted mt-6 px-5 py-4">
        <p class="meta-label">Assignment reference</p>
        <p class="meta-value break-all mono-meta">{{ assignmentId }}</p>
      </div>
    </div>

    <div v-if="errorMessage" class="alert-error">{{ errorMessage }}</div>
    <div v-else-if="isLoading" class="empty-state">Loading assignment submissions…</div>
    <div v-else-if="submissions.length === 0" class="empty-state">No submissions have been uploaded for this assignment yet.</div>

    <section v-else class="page-section">
      <div class="record-list">
        <article
          v-for="submission in submissions"
          :key="submission.id"
          class="record-card"
        >
          <div class="record-card-frame">
            <div class="space-y-2">
              <p class="font-display text-xl font-semibold text-[var(--color-heading)]">{{ submission.fileName }}</p>
              <p class="text-base text-[var(--color-text-soft)]">Submitted {{ formatDate(submission.submittedAt) }}</p>
              <p class="text-base text-[var(--color-text-soft)]">Student ID: <span class="mono-meta">{{ submission.studentUserId }}</span></p>
            </div>
            <div class="flex flex-wrap gap-2">
              <span class="status-pill" :class="verificationTone(submission.verificationStatus)">
                {{ submission.verificationStatus.replaceAll('_', ' ') }}
              </span>
              <span
                class="status-pill"
                :class="submission.graded ? 'status-pill-neutral' : 'status-pill-neutral'"
              >
                {{ submission.graded ? 'Graded' : 'Not graded' }}
              </span>
            </div>
          </div>

          <div class="record-card-footer">
            <RouterLink
              :to="{
                name: 'submission-detail',
                params: { submissionId: submission.id },
                query: spaceId ? { spaceId } : undefined,
              }"
              class="btn-primary"
            >
              Review and grade
            </RouterLink>
            <span class="text-base text-[var(--color-text-soft)]">{{ submission.verificationMessage }}</span>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>


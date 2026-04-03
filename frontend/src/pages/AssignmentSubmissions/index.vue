<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { submissionsService } from '@/services/submissions'
import type { SubmissionResponse } from '@/types/submission'

const route = useRoute()

const assignmentId = computed(() => String(route.params.assignmentId ?? ''))
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
      return 'border-emerald-300 bg-emerald-50 text-emerald-800'
    case 'FAILED_VERIFICATION':
    case 'REJECTED':
      return 'border-rose-300 bg-rose-50 text-rose-800'
    default:
      return 'border-amber-300 bg-amber-50 text-amber-800'
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
    <div class="surface-panel p-8">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker tracking-[0.3em]">Assignment submissions</p>
          <h2 class="section-title">Review submitted work before grading</h2>
          <p class="section-copy">
            Open a submission to inspect its integrity evidence, retrieve content, and assign a grade
            expressed as a percentage from 0 to 100.
          </p>
        </div>
        <div class="flex flex-wrap gap-3">
          <button type="button" class="btn-secondary" @click="loadSubmissions">Refresh</button>
          <RouterLink :to="{ name: 'assignments' }" class="btn-secondary">Back to assignments</RouterLink>
        </div>
      </div>

      <div class="surface-panel-muted mt-6 px-5 py-4 text-sm text-slate-600">
        <p class="font-semibold text-slate-900">Assignment reference</p>
        <p class="mt-2 break-all font-mono text-xs text-slate-700">{{ assignmentId }}</p>
      </div>
    </div>

    <div v-if="errorMessage" class="alert-error">{{ errorMessage }}</div>
    <div v-else-if="isLoading" class="empty-state">Loading assignment submissions…</div>
    <div v-else-if="submissions.length === 0" class="empty-state">No submissions have been uploaded for this assignment yet.</div>

    <section v-else class="surface-panel p-8">
      <div class="space-y-4">
        <article
          v-for="submission in submissions"
          :key="submission.id"
          class="rounded-sm border border-slate-300 bg-white p-5"
        >
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div class="space-y-2">
              <p class="text-lg font-semibold text-slate-900">{{ submission.fileName }}</p>
              <p class="text-sm text-slate-600">Submitted {{ formatDate(submission.submittedAt) }}</p>
              <p class="text-sm text-slate-600">Student ID: <span class="font-mono text-xs text-slate-700">{{ submission.studentUserId }}</span></p>
            </div>
            <div class="flex flex-wrap gap-2">
              <span class="status-pill" :class="verificationTone(submission.verificationStatus)">
                {{ submission.verificationStatus.replaceAll('_', ' ') }}
              </span>
              <span
                class="status-pill"
                :class="submission.graded ? 'border-sky-300 bg-sky-50 text-sky-800' : 'border-slate-300 bg-slate-50 text-slate-700'"
              >
                {{ submission.graded ? 'Graded' : 'Not graded' }}
              </span>
            </div>
          </div>

          <div class="mt-5 flex flex-wrap items-center gap-3 border-t border-slate-200 pt-4">
            <RouterLink
              :to="{ name: 'submission-detail', params: { submissionId: submission.id } }"
              class="btn-primary"
            >
              Review and grade
            </RouterLink>
            <span class="text-sm text-slate-500">{{ submission.verificationMessage }}</span>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>


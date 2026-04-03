<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { SubmissionResponse } from '@/types/submission'

const props = defineProps<{
  assignmentId: string
  submission: SubmissionResponse | null
  isLoading: boolean
  loadError: string | null
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

function statusTone(status: SubmissionResponse['verificationStatus']): string {
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
</script>

<template>
  <section class="surface-panel-muted p-6">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="text-xs font-semibold uppercase tracking-[0.3em] text-slate-500">Latest submission</p>
        <h3 class="mt-2 text-lg font-semibold text-slate-900">Return to your previously submitted work</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          When you revisit this assignment, your latest submission stays visible here so you can open
          the evidence view again without needing to upload another file.
        </p>
      </div>
      <div class="surface-panel bg-white px-4 py-3 text-sm text-slate-600">
        <p class="font-semibold text-slate-900">Assignment reference</p>
        <p class="mt-2 break-all font-mono text-xs text-slate-700">{{ props.assignmentId }}</p>
      </div>
    </div>

    <div v-if="props.loadError" class="alert-error mt-5">{{ props.loadError }}</div>
    <div v-else-if="props.isLoading" class="mt-5 text-sm text-slate-600">Loading your latest submission…</div>
    <div v-else-if="props.submission" class="mt-5 grid gap-4 rounded-sm border border-slate-300 bg-white p-5 md:grid-cols-[1.1fr_0.9fr]">
      <div class="space-y-3">
        <div>
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">File</p>
          <p class="mt-1 break-all text-sm font-semibold text-slate-900">{{ props.submission.fileName }}</p>
        </div>
        <div>
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Submitted</p>
          <p class="mt-1 text-sm text-slate-700">{{ formatDate(props.submission.submittedAt) }}</p>
        </div>
      </div>

      <div class="space-y-3">
        <div>
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Verification</p>
          <div class="mt-2 flex flex-wrap items-center gap-3">
            <span class="status-pill" :class="statusTone(props.submission.verificationStatus)">
              {{ props.submission.verificationStatus.replaceAll('_', ' ') }}
            </span>
            <span class="text-sm text-slate-600">{{ props.submission.verificationMessage }}</span>
          </div>
        </div>
        <div class="pt-1">
          <RouterLink
            :to="{ name: 'submission-detail', params: { submissionId: props.submission.id } }"
            class="btn-secondary"
          >
            View submitted work
          </RouterLink>
        </div>
      </div>
    </div>
    <div v-else class="mt-5 rounded-sm border border-dashed border-slate-300 bg-white p-5 text-sm text-slate-600">
      You have not submitted work for this assignment yet.
    </div>
  </section>
</template>



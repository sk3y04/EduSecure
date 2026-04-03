<script setup lang="ts">
import { computed } from 'vue'

import type { SubmissionResponse } from '@/types/submission'

const props = defineProps<{
  submission: SubmissionResponse
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

const verificationTone = computed(() => {
  switch (props.submission.verificationStatus) {
    case 'VERIFIED':
      return 'border-emerald-300 bg-emerald-50 text-emerald-800'
    case 'FAILED_VERIFICATION':
    case 'REJECTED':
      return 'border-rose-300 bg-rose-50 text-rose-800'
    default:
      return 'border-amber-300 bg-amber-50 text-amber-800'
  }
})
</script>

<template>
  <section class="surface-panel p-8">
    <div class="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="text-xl font-semibold text-slate-900">Submission metadata</h3>
        <p class="mt-2 text-sm text-slate-600">Submitted {{ formatDate(props.submission.submittedAt) }}</p>
      </div>
      <div :class="['status-pill', verificationTone]">
        {{ props.submission.verificationStatus }}
      </div>
    </div>

    <dl class="mt-6 grid gap-4 sm:grid-cols-2">
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Submission ID</dt>
        <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ props.submission.id }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Assignment ID</dt>
        <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ props.submission.assignmentId }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Submitted by (user ID)</dt>
        <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ props.submission.studentUserId }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">File name</dt>
        <dd class="mt-2 text-sm text-slate-900">{{ props.submission.fileName }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Content type</dt>
        <dd class="mt-2 text-sm text-slate-900">{{ props.submission.contentType }}</dd>
      </div>
    </dl>
  </section>
</template>



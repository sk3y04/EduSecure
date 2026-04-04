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
      return 'status-pill-success'
    case 'FAILED_VERIFICATION':
    case 'REJECTED':
      return 'status-pill-danger'
    default:
      return 'status-pill-warning'
  }
})
</script>

<template>
  <section class="page-section">
    <div class="panel-header flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Submission metadata</h3>
        <p class="mt-2 text-base text-[var(--color-text-soft)]">Submitted {{ formatDate(props.submission.submittedAt) }}</p>
      </div>
      <div :class="['status-pill', verificationTone]">
        {{ props.submission.verificationStatus.replaceAll('_', ' ') }}
      </div>
    </div>

    <dl class="stats-grid mt-6">
      <div class="stat-card">
        <dt class="meta-label">Submission ID</dt>
        <dd class="meta-value break-all mono-meta">{{ props.submission.id }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Assignment ID</dt>
        <dd class="meta-value break-all mono-meta">{{ props.submission.assignmentId }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Submitted by user ID</dt>
        <dd class="meta-value break-all mono-meta">{{ props.submission.studentUserId }}</dd>
      </div>
      <div class="stat-card bg-[var(--color-surface-offset)]">
        <dt class="meta-label">File name</dt>
        <dd class="meta-value">{{ props.submission.fileName }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Content type</dt>
        <dd class="meta-value">{{ props.submission.contentType }}</dd>
      </div>
    </dl>
  </section>
</template>



<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { SubmissionResponse } from '@/types/submission'

const props = defineProps<{
  assignmentId: string
  spaceId?: string | null
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
      return 'status-pill-success'
    case 'FAILED_VERIFICATION':
    case 'REJECTED':
      return 'status-pill-danger'
    default:
      return 'status-pill-warning'
  }
}
</script>

<template>
  <section class="surface-panel-muted p-6">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker">Latest submission</p>
        <h3 class="mt-2 font-display text-xl font-semibold text-[var(--color-heading)]">Return to your previously submitted work</h3>
        <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
          When you revisit this assignment, your latest submission stays visible here so you can open
          the evidence view again without needing to upload another file.
        </p>
      </div>
      <div class="surface-panel bg-[var(--color-surface)] px-4 py-3">
        <p class="meta-label">Assignment reference</p>
        <p class="meta-value break-all mono-meta">{{ props.assignmentId }}</p>
      </div>
    </div>

    <div v-if="props.loadError" class="alert-error mt-5">{{ props.loadError }}</div>
    <div v-else-if="props.isLoading" class="mt-5 text-base text-[var(--color-text-soft)]">Loading your latest submission…</div>
    <div v-else-if="props.submission" class="record-card mt-5">
      <div class="record-card-frame">
        <div class="space-y-3">
          <div>
            <p class="meta-label">File</p>
            <p class="meta-value break-all font-medium">{{ props.submission.fileName }}</p>
          </div>
          <div>
            <p class="meta-label">Submitted</p>
            <p class="meta-value">{{ formatDate(props.submission.submittedAt) }}</p>
          </div>
        </div>

        <div class="space-y-3">
          <div>
            <p class="meta-label">Verification</p>
            <div class="mt-2 flex flex-wrap items-center gap-3">
              <span class="status-pill" :class="statusTone(props.submission.verificationStatus)">
                {{ props.submission.verificationStatus.replaceAll('_', ' ') }}
              </span>
              <span class="text-base text-[var(--color-text-soft)]">{{ props.submission.verificationMessage }}</span>
            </div>
          </div>
          <div class="pt-1">
            <RouterLink
              :to="{
                name: 'submission-detail',
                params: { submissionId: props.submission.id },
                query: props.spaceId ? { spaceId: props.spaceId } : undefined,
              }"
              class="btn-secondary"
            >
              View submitted work
            </RouterLink>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-state mt-5">
      You have not submitted work for this assignment yet.
    </div>
  </section>
</template>



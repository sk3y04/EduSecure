<script setup lang="ts">
import type { SubmissionDownloadResult, SubmissionResponse } from '@/types/submission'

const props = defineProps<{
  isLoadingContent: boolean
  contentErrorMessage: string | null
  submission: SubmissionResponse | null
  downloadedSubmission: SubmissionDownloadResult | null
}>()

const emit = defineEmits<{
  (e: 'retrieve'): void
}>()
</script>

<template>
  <section class="page-section">
    <div class="panel-header flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div>
        <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Protected submission content</h3>
      </div>
      <button
        type="button"
        class="btn-primary self-start"
        :disabled="props.isLoadingContent"
        @click="emit('retrieve')"
      >
        {{ props.isLoadingContent ? 'Retrieving…' : props.downloadedSubmission ? 'Download again' : 'Download file' }}
      </button>
    </div>

    <div v-if="props.contentErrorMessage" class="alert-error mt-6">
      {{ props.contentErrorMessage }}
    </div>

    <div v-else-if="props.submission" class="surface-panel-muted mt-6 p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="text-base font-semibold text-[var(--color-heading)]">{{ props.submission.fileName }}</p>
          <p class="mt-1 text-sm text-[var(--color-text-soft)]">
            {{ props.submission.contentType }}
          </p>
        </div>
        <p class="status-pill status-pill-success">Audited retrieval</p>
      </div>


      <p v-if="props.downloadedSubmission" class="mt-3 text-sm font-medium text-[var(--color-heading)]">
        Last retrieved: {{ props.downloadedSubmission.fileName }} ({{ props.downloadedSubmission.contentType }})
      </p>
    </div>

    <div v-else class="empty-state mt-6">
      Content is encrypted at rest and is not included in the standard metadata response.
    </div>
  </section>
</template>


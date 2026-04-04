<script setup lang="ts">
import type { SubmissionContentResponse } from '@/types/submission'

const props = defineProps<{
  isLoadingContent: boolean
  contentErrorMessage: string | null
  submissionContent: SubmissionContentResponse | null
}>()

const emit = defineEmits<{
  (e: 'retrieve'): void
}>()
</script>

<template>
  <section class="page-section">
    <div class="panel-header flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Protected submission content</h3>
        <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
          Metadata remains visible by default. Uploaded text content is only fetched through
          the controlled retrieval endpoint so the backend can authorize and audit access.
        </p>
      </div>
      <button
        type="button"
        class="btn-primary self-start"
        :disabled="props.isLoadingContent"
        @click="emit('retrieve')"
      >
        {{ props.isLoadingContent ? 'Retrieving…' : props.submissionContent ? 'Reload content' : 'Retrieve content' }}
      </button>
    </div>

    <div v-if="props.contentErrorMessage" class="alert-error mt-6">
      {{ props.contentErrorMessage }}
    </div>

    <div v-else-if="props.submissionContent" class="surface-panel-muted mt-6 p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="text-base font-semibold text-[var(--color-heading)]">{{ props.submissionContent.fileName }}</p>
          <p class="mt-1 text-sm text-[var(--color-text-soft)]">
            {{ props.submissionContent.contentType }}
          </p>
        </div>
        <p class="status-pill status-pill-success">Audited retrieval</p>
      </div>
      <pre class="code-block mt-4">{{ props.submissionContent.content }}</pre>
    </div>

    <div v-else class="empty-state mt-6">
      Content is encrypted at rest and is not included in the standard metadata response.
    </div>
  </section>
</template>


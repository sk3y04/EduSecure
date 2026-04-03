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
  <section class="surface-panel p-8">
    <div class="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <h3 class="text-xl font-semibold text-slate-900">Protected submission content</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
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

    <div v-else-if="props.submissionContent" class="mt-6 rounded-sm border border-slate-300 bg-slate-50 p-5">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <p class="text-sm font-semibold text-slate-900">{{ props.submissionContent.fileName }}</p>
          <p class="mt-1 text-xs uppercase tracking-[0.25em] text-slate-500">
            {{ props.submissionContent.contentType }}
          </p>
        </div>
        <p class="text-xs font-semibold uppercase tracking-[0.25em] text-emerald-700">Audited retrieval</p>
      </div>
      <pre class="code-block mt-4">{{ props.submissionContent.content }}</pre>
    </div>

    <div v-else class="empty-state mt-6">
      Content is encrypted at rest and is not included in the standard metadata response.
    </div>
  </section>
</template>


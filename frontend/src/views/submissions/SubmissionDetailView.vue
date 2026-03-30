<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { submissionsService } from '@/services/submissions'
import { extractErrorMessage } from '@/services/http'
import type { SubmissionContentResponse, SubmissionResponse } from '@/types/submission'

const route = useRoute()

const submission = ref<SubmissionResponse | null>(null)
const submissionContent = ref<SubmissionContentResponse | null>(null)
const isLoading = ref(true)
const isLoadingContent = ref(false)
const errorMessage = ref<string | null>(null)
const contentErrorMessage = ref<string | null>(null)

const submissionId = computed(() => String(route.params.submissionId ?? ''))

const verificationTone = computed(() => {
  switch (submission.value?.verificationStatus) {
    case 'VERIFIED':
      return 'border-emerald-300 bg-emerald-50 text-emerald-800'
    case 'FAILED_VERIFICATION':
    case 'REJECTED':
      return 'border-rose-300 bg-rose-50 text-rose-800'
    default:
      return 'border-amber-300 bg-amber-50 text-amber-800'
  }
})

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

async function loadSubmission() {
  isLoading.value = true
  errorMessage.value = null

  try {
    submission.value = await submissionsService.getById(submissionId.value)
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function loadSubmissionContent() {
  isLoadingContent.value = true
  contentErrorMessage.value = null

  try {
    submissionContent.value = await submissionsService.getContent(submissionId.value)
  } catch (error) {
    contentErrorMessage.value = extractErrorMessage(error)
  } finally {
    isLoadingContent.value = false
  }
}

onMounted(() => {
  void loadSubmission()
})
</script>

<template>
  <section class="space-y-6">
    <div class="surface-panel p-8">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker tracking-[0.3em]">Submission evidence</p>
          <h2 class="section-title">Review integrity and authorship metadata</h2>
          <p class="section-copy">
            This screen keeps the cryptographic evidence easy to inspect: digest, signature
            algorithm, and verification result remain prominent, while plaintext retrieval stays a
            separate audited action.
          </p>
        </div>

        <button type="button" class="btn-secondary self-start" @click="loadSubmission">
          Refresh
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="alert-error">
      {{ errorMessage }}
    </div>

    <div v-else-if="isLoading" class="empty-state">
      Loading submission evidence…
    </div>

    <template v-else-if="submission">
      <div class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <section class="surface-panel p-8">
          <div class="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h3 class="text-xl font-semibold text-slate-900">Submission metadata</h3>
              <p class="mt-2 text-sm text-slate-600">Submitted {{ formatDate(submission.submittedAt) }}</p>
            </div>
            <div :class="['status-pill', verificationTone]">
              {{ submission.verificationStatus }}
            </div>
          </div>

          <dl class="mt-6 grid gap-4 sm:grid-cols-2">
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Submission ID</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ submission.id }}</dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Assignment ID</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ submission.assignmentId }}</dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">File name</dt>
              <dd class="mt-2 text-sm text-slate-900">{{ submission.fileName }}</dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Content type</dt>
              <dd class="mt-2 text-sm text-slate-900">{{ submission.contentType }}</dd>
            </div>
          </dl>
        </section>

        <section class="surface-panel p-8">
          <h3 class="text-xl font-semibold text-slate-900">Verification summary</h3>
          <p class="mt-2 text-sm leading-6 text-slate-600">{{ submission.verificationMessage }}</p>

          <dl class="mt-6 space-y-4">
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Hash digest</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ submission.hashDigest }}</dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Signature algorithm</dt>
              <dd class="mt-2 text-sm text-slate-900">{{ submission.signatureAlgorithm }}</dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Digital signature</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ submission.digitalSignature }}</dd>
            </div>
          </dl>
        </section>
      </div>

      <section class="surface-panel p-8">
        <div class="flex flex-col gap-4 border-b border-slate-200 pb-5 lg:flex-row lg:items-start lg:justify-between">
          <div class="max-w-3xl">
            <h3 class="text-xl font-semibold text-slate-900">Protected submission content</h3>
            <p class="mt-2 text-sm leading-6 text-slate-600">
              Metadata remains visible by default. Plaintext content is only fetched through the
              controlled retrieval endpoint so the backend can authorize and audit access.
            </p>
          </div>
          <button type="button" class="btn-primary self-start" :disabled="isLoadingContent" @click="loadSubmissionContent">
            {{ isLoadingContent ? 'Retrieving…' : submissionContent ? 'Reload content' : 'Retrieve content' }}
          </button>
        </div>

        <div v-if="contentErrorMessage" class="alert-error mt-6">
          {{ contentErrorMessage }}
        </div>

        <div v-else-if="submissionContent" class="mt-6 rounded-sm border border-slate-300 bg-slate-50 p-5">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-slate-900">{{ submissionContent.fileName }}</p>
              <p class="mt-1 text-xs uppercase tracking-[0.25em] text-slate-500">
                {{ submissionContent.contentType }}
              </p>
            </div>
            <p class="text-xs font-semibold uppercase tracking-[0.25em] text-emerald-700">Audited retrieval</p>
          </div>

          <pre class="code-block mt-4">{{ submissionContent.content }}</pre>
        </div>

        <div v-else class="empty-state mt-6">
          Content is encrypted at rest and is not included in the standard metadata response.
        </div>
      </section>
    </template>
  </section>
</template>


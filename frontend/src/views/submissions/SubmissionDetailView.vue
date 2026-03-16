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
      return 'border-emerald-500/30 bg-emerald-500/10 text-emerald-100'
    case 'FAILED_VERIFICATION':
    case 'REJECTED':
      return 'border-rose-500/30 bg-rose-500/10 text-rose-100'
    default:
      return 'border-amber-500/30 bg-amber-500/10 text-amber-100'
  }
})

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
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
  <section class="space-y-8">
    <div class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">Submission evidence</p>
          <h2 class="mt-3 text-3xl font-semibold text-white">Review the integrity and authorship metadata</h2>
          <p class="mt-3 text-sm leading-7 text-slate-400">
            This screen exists to make the cryptographic evidence explicit: digest, signature
            algorithm, and verification result are all visible to support the report narrative.
            Stored content is intentionally retrieved through a separate audited endpoint.
          </p>
        </div>

        <button
          type="button"
          class="inline-flex items-center rounded-2xl border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-brand-500 hover:text-white"
          @click="loadSubmission"
        >
          Refresh
        </button>
      </div>
    </div>

    <div
      v-if="errorMessage"
      class="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
    >
      {{ errorMessage }}
    </div>

    <div v-else-if="isLoading" class="rounded-3xl border border-dashed border-slate-700 p-8 text-center text-sm text-slate-400">
      Loading submission evidence…
    </div>

    <template v-else-if="submission">
      <div class="grid gap-8 xl:grid-cols-[1.1fr_0.9fr]">
        <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
          <div class="flex flex-wrap items-center justify-between gap-4">
            <div>
              <h3 class="text-xl font-semibold text-white">Submission metadata</h3>
              <p class="mt-2 text-sm text-slate-400">Submitted {{ formatDate(submission.submittedAt) }}</p>
            </div>
            <div :class="['rounded-full border px-4 py-2 text-xs font-semibold uppercase tracking-wide', verificationTone]">
              {{ submission.verificationStatus }}
            </div>
          </div>

          <dl class="mt-8 grid gap-5 sm:grid-cols-2">
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Submission ID</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-200">{{ submission.id }}</dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Assignment ID</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-200">{{ submission.assignmentId }}</dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">File name</dt>
              <dd class="mt-2 text-sm text-slate-200">{{ submission.fileName }}</dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Content type</dt>
              <dd class="mt-2 text-sm text-slate-200">{{ submission.contentType }}</dd>
            </div>
          </dl>
        </section>

        <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
          <h3 class="text-xl font-semibold text-white">Verification summary</h3>
          <p class="mt-2 text-sm leading-6 text-slate-400">{{ submission.verificationMessage }}</p>

          <dl class="mt-6 space-y-4">
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Hash digest</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-200">{{ submission.hashDigest }}</dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Signature algorithm</dt>
              <dd class="mt-2 text-sm text-slate-200">{{ submission.signatureAlgorithm }}</dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Digital signature</dt>
              <dd class="mt-2 break-all font-mono text-sm text-slate-200">{{ submission.digitalSignature }}</dd>
            </div>
          </dl>
        </section>
      </div>

      <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div class="max-w-3xl">
            <h3 class="text-xl font-semibold text-white">Protected submission content</h3>
            <p class="mt-2 text-sm leading-6 text-slate-400">
              Metadata stays visible by default. Plaintext content is retrieved only through a
              separate controlled endpoint so the backend can authorize and audit content access.
            </p>
          </div>
          <button
            type="button"
            class="inline-flex items-center rounded-2xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isLoadingContent"
            @click="loadSubmissionContent"
          >
            {{ isLoadingContent ? 'Retrieving…' : submissionContent ? 'Reload content' : 'Retrieve content' }}
          </button>
        </div>

        <div
          v-if="contentErrorMessage"
          class="mt-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
        >
          {{ contentErrorMessage }}
        </div>

        <div
          v-else-if="submissionContent"
          class="mt-6 rounded-2xl border border-slate-800 bg-slate-950/70 p-5"
        >
          <div class="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p class="text-sm font-semibold text-white">{{ submissionContent.fileName }}</p>
              <p class="mt-1 text-xs uppercase tracking-[0.25em] text-slate-500">
                {{ submissionContent.contentType }}
              </p>
            </div>
            <p class="text-xs uppercase tracking-[0.25em] text-emerald-300">Audited retrieval</p>
          </div>

          <pre class="mt-4 overflow-x-auto rounded-2xl border border-slate-800 bg-slate-950 p-4 text-sm leading-6 text-slate-200 whitespace-pre-wrap">{{ submissionContent.content }}</pre>
        </div>

        <div
          v-else
          class="mt-6 rounded-2xl border border-dashed border-slate-700 p-6 text-sm text-slate-400"
        >
          Content is encrypted at rest and is not included in the standard metadata response.
        </div>
      </section>
    </template>
  </section>
</template>


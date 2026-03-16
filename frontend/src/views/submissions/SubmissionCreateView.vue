<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'

import { submissionsService } from '@/services/submissions'
import { extractErrorMessage } from '@/services/http'

const route = useRoute()
const router = useRouter()

const assignmentId = computed(() => String(route.params.assignmentId ?? ''))
const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)

const form = reactive({
  fileName: '',
  contentType: 'text/plain',
  content: '',
})

async function handleSubmit() {
  isSubmitting.value = true
  errorMessage.value = null

  try {
    const response = await submissionsService.create(assignmentId.value, {
      fileName: form.fileName,
      contentType: form.contentType,
      content: form.content,
    })

    await router.push({
      name: 'submission-detail',
      params: { submissionId: response.id },
    })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">Secure submission</p>
        <h2 class="mt-3 text-3xl font-semibold text-white">Submit simulated content for integrity verification</h2>
        <p class="mt-3 text-sm leading-7 text-slate-400">
          This phase uses the backend JSON submission model rather than multipart upload. The goal is
          to make the digest, signature, and verification result visible immediately after submission,
          while the stored content is encrypted at rest and retrieved later through a separate
          controlled endpoint.
        </p>
      </div>

      <div class="rounded-2xl border border-slate-800 bg-slate-950/70 px-5 py-4 text-sm text-slate-300">
        <p class="font-semibold text-white">Assignment reference</p>
        <p class="mt-2 break-all font-mono text-xs text-slate-400">{{ assignmentId }}</p>
      </div>
    </div>

    <div
      v-if="errorMessage"
      class="mt-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
    >
      {{ errorMessage }}
    </div>

    <form class="mt-8 grid gap-5" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="mb-2 block text-sm font-medium text-slate-200">File name</span>
        <input
          v-model="form.fileName"
          type="text"
          required
          class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
          placeholder="coursework.txt"
        />
      </label>

      <label class="block">
        <span class="mb-2 block text-sm font-medium text-slate-200">Content type</span>
        <input
          v-model="form.contentType"
          type="text"
          required
          class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
          placeholder="text/plain"
        />
      </label>

      <label class="block">
        <span class="mb-2 block text-sm font-medium text-slate-200">Submission content</span>
        <textarea
          v-model="form.content"
          rows="12"
          required
          class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
          placeholder="Paste the simulated coursework content that will be hashed and signed by the backend."
        />
      </label>

      <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4 text-sm text-slate-400">
        <p class="font-semibold text-slate-200">What happens next</p>
        <ul class="mt-3 list-disc space-y-2 pl-5">
          <li>The backend computes a SHA-256 digest.</li>
          <li>A digital signature is created and immediately verified.</li>
          <li>The submission content is encrypted at rest before durable storage.</li>
          <li>The detail page exposes metadata first, and content only through a separate audited retrieval.</li>
        </ul>
      </div>

      <div class="flex flex-wrap gap-3">
        <button
          type="submit"
          class="inline-flex items-center rounded-2xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="isSubmitting"
        >
          {{ isSubmitting ? 'Submitting…' : 'Create secure submission' }}
        </button>
        <RouterLink
          :to="{ name: 'assignments' }"
          class="inline-flex items-center rounded-2xl border border-slate-700 px-5 py-3 text-sm font-medium text-slate-200 transition hover:border-brand-500 hover:text-white"
        >
          Back to assignments
        </RouterLink>
      </div>
    </form>
  </section>
</template>



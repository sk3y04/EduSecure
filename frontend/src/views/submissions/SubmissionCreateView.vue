<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useRoute, useRouter } from 'vue-router'

import { submissionsService } from '@/services/submissions'
import { extractErrorMessage } from '@/services/http'

const route = useRoute()
const router = useRouter()

const assignmentId = computed(() => String(route.params.assignmentId ?? ''))
const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)

const selectedFile = ref<File | null>(null)

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  errorMessage.value = null
}

function formatFileSize(size: number): string {
  if (size < 1024) {
    return `${size} B`
  }

  return `${(size / 1024).toFixed(1)} KB`
}

async function handleSubmit() {
  if (!selectedFile.value) {
    errorMessage.value = 'Select a UTF-8 text/plain file before submitting.'
    return
  }

  isSubmitting.value = true
  errorMessage.value = null

  try {
    const response = await submissionsService.create(assignmentId.value, selectedFile.value)

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
  <section class="surface-panel p-8">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker tracking-[0.3em]">Secure submission</p>
        <h2 class="section-title">Submit content for integrity verification</h2>
        <p class="section-copy">
          The form stays intentionally direct: send the simulated content, let the backend hash and
          sign it, and move straight to the evidence view without decorative distractions.
        </p>
      </div>

      <div class="surface-panel-muted px-5 py-4 text-sm text-slate-600">
        <p class="font-semibold text-slate-900">Assignment reference</p>
        <p class="mt-2 break-all font-mono text-xs text-slate-700">{{ assignmentId }}</p>
      </div>
    </div>

    <div v-if="errorMessage" class="alert-error mt-6">
      {{ errorMessage }}
    </div>

    <form class="mt-8 grid gap-5" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="field-label">Submission file</span>
        <input
          type="file"
          required
          class="form-input"
          accept=".txt,text/plain"
          @change="handleFileChange"
        />
      </label>

      <div v-if="selectedFile" class="surface-panel-muted grid gap-4 p-4 text-sm text-slate-600 sm:grid-cols-3">
        <div>
          <p class="font-semibold text-slate-900">Selected file</p>
          <p class="mt-2 break-all text-slate-700">{{ selectedFile.name }}</p>
        </div>
        <div>
          <p class="font-semibold text-slate-900">Detected content type</p>
          <p class="mt-2 text-slate-700">{{ selectedFile.type || 'Browser did not provide a type' }}</p>
        </div>
        <div>
          <p class="font-semibold text-slate-900">Size</p>
          <p class="mt-2 text-slate-700">{{ formatFileSize(selectedFile.size) }}</p>
        </div>
      </div>

      <div class="surface-panel-muted p-4 text-sm text-slate-600">
        <p class="font-semibold text-slate-900">What happens next</p>
        <ul class="mt-3 list-disc space-y-2 pl-5">
          <li>The current upload flow is intentionally narrow: UTF-8 <code>text/plain</code> files only.</li>
          <li>The backend computes a SHA-256 digest over the uploaded file bytes.</li>
          <li>A digital signature is created and immediately verified.</li>
          <li>The submission content is encrypted at rest before durable storage.</li>
          <li>The detail page exposes metadata first and content through a separate audited retrieval.</li>
        </ul>
      </div>

      <div class="flex flex-wrap gap-3">
        <button type="submit" class="btn-primary" :disabled="isSubmitting">
          {{ isSubmitting ? 'Submitting…' : 'Create secure submission' }}
        </button>
        <RouterLink :to="{ name: 'assignments' }" class="btn-secondary">
          Back to assignments
        </RouterLink>
      </div>
    </form>
  </section>
</template>


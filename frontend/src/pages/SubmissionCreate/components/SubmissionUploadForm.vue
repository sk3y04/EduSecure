<script setup lang="ts">
import { RouterLink } from 'vue-router'

const props = defineProps<{
  errorMessage: string | null
  isSubmitting: boolean
  selectedFile: File | null
}>()

const emit = defineEmits<{
  (e: 'submit'): void
  (e: 'fileChange', file: File | null): void
}>()

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  emit('fileChange', input.files?.[0] ?? null)
}

function formatFileSize(size: number): string {
  if (size < 1024) return `${size} B`
  return `${(size / 1024).toFixed(1)} KB`
}
</script>

<template>
  <form class="mt-8 grid gap-5" @submit.prevent="emit('submit')">
    <div v-if="props.errorMessage" class="alert-error">{{ props.errorMessage }}</div>

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

    <div v-if="props.selectedFile" class="surface-panel-muted grid gap-4 p-4 text-sm text-slate-600 sm:grid-cols-3">
      <div>
        <p class="font-semibold text-slate-900">Selected file</p>
        <p class="mt-2 break-all text-slate-700">{{ props.selectedFile.name }}</p>
      </div>
      <div>
        <p class="font-semibold text-slate-900">Detected content type</p>
        <p class="mt-2 text-slate-700">{{ props.selectedFile.type || 'Browser did not provide a type' }}</p>
      </div>
      <div>
        <p class="font-semibold text-slate-900">Size</p>
        <p class="mt-2 text-slate-700">{{ formatFileSize(props.selectedFile.size) }}</p>
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
      <button type="submit" class="btn-primary" :disabled="props.isSubmitting">
        {{ props.isSubmitting ? 'Submitting…' : 'Create secure submission' }}
      </button>
      <RouterLink :to="{ name: 'assignments' }" class="btn-secondary">
        Back to assignments
      </RouterLink>
    </div>
  </form>
</template>


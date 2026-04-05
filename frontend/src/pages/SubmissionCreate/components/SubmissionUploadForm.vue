<script setup lang="ts">
import { RouterLink } from 'vue-router'

const props = defineProps<{
  errorMessage: string | null
  isSubmitting: boolean
  selectedFile: File | null
  spaceId?: string | null
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
  <form class="page-section grid gap-5" @submit.prevent="emit('submit')">
    <div v-if="props.errorMessage" class="alert-error">{{ props.errorMessage }}</div>

    <label class="block">
      <span class="field-label">Submission file</span>
      <input
        type="file"
        required
        class="form-input"
        accept=".txt,text/plain,.pdf,application/pdf"
        @change="handleFileChange"
      />
      <p class="mt-2 text-sm text-[var(--color-text-soft)]">
        Accepted formats: TXT or PDF. Maximum file size: 5 MB.
      </p>
    </label>

    <dl v-if="props.selectedFile" class="stats-grid">
      <div class="stat-card bg-[var(--color-surface-offset)]">
        <dt class="meta-label">Selected file</dt>
        <dd class="meta-value break-all">{{ props.selectedFile.name }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Detected content type</dt>
        <dd class="meta-value">{{ props.selectedFile.type || 'Browser did not provide a type' }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Size</dt>
        <dd class="meta-value">{{ formatFileSize(props.selectedFile.size) }}</dd>
      </div>
    </dl>

    <div class="surface-panel-muted p-4">
      <p class="text-base font-semibold text-[var(--color-heading)]">What happens next</p>
      <ul class="quiet-list">
        <li>The current upload flow supports UTF-8 <code>text/plain</code> files and validated <code>application/pdf</code> uploads up to 5 MB.</li>
        <li>The backend computes a SHA-256 digest over the uploaded file bytes.</li>
        <li>A digital signature is created and immediately verified.</li>
        <li>The submission content is encrypted at rest before durable storage.</li>
        <li>The detail page exposes metadata first and retrieves the protected file through a separate audited download.</li>
      </ul>
    </div>

    <div class="flex flex-wrap gap-3">
      <button type="submit" class="btn-primary" :disabled="props.isSubmitting">
        {{ props.isSubmitting ? 'Submitting…' : 'Create secure submission' }}
      </button>
      <RouterLink
        :to="props.spaceId ? { name: 'space-detail', params: { spaceId: props.spaceId } } : { name: 'spaces' }"
        class="btn-secondary"
      >
        {{ props.spaceId ? 'Back to space' : 'Back to spaces' }}
      </RouterLink>
    </div>
  </form>
</template>


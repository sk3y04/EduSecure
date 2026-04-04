<script setup lang="ts">
import type { MyGradeResponse } from '@/types/grade'

const props = defineProps<{
  grade: MyGradeResponse | null
  isLoading: boolean
  errorMessage: string | null
}>()

function formatDate(value: string | null): string {
  if (!value) return '—'
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
</script>

<template>
  <section class="page-section">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Your grade</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        If your submission has been marked, the awarded percentage and feedback appear here.
      </p>
    </div>

    <div v-if="props.errorMessage" class="alert-error mb-4">{{ props.errorMessage }}</div>
    <div v-else-if="props.isLoading" class="empty-state">Loading your grade…</div>
    <div v-else-if="!props.grade" class="empty-state">Your submission has not been graded yet.</div>
    <template v-else>
      <div class="stats-grid mb-6">
        <div class="stat-card bg-[var(--color-surface-offset)]">
          <dt class="meta-label">Grade</dt>
          <dd class="meta-value text-lg font-semibold">{{ props.grade.value }}%</dd>
        </div>
        <div class="stat-card">
          <dt class="meta-label">Grade ID</dt>
          <dd class="meta-value break-all mono-meta">{{ props.grade.id }}</dd>
        </div>
        <div class="stat-card">
          <dt class="meta-label">Feedback updated</dt>
          <dd class="meta-value">{{ formatDate(props.grade.lastModifiedAt) }}</dd>
        </div>
      </div>

      <div class="surface-panel-muted p-4">
        <p class="meta-label">Feedback</p>
        <p class="mt-3 whitespace-pre-wrap text-base leading-7 text-[var(--color-text)]">{{ props.grade.feedback }}</p>
      </div>
    </template>
  </section>
</template>


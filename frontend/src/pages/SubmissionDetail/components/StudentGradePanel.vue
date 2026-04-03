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
  <section class="surface-panel p-8">
    <div class="mb-6 border-b border-slate-200 pb-5">
      <h3 class="text-xl font-semibold text-slate-900">Your grade</h3>
      <p class="mt-2 text-sm leading-6 text-slate-600">
        If your submission has been marked, the awarded percentage and feedback appear here.
      </p>
    </div>

    <div v-if="props.errorMessage" class="alert-error mb-4">{{ props.errorMessage }}</div>
    <div v-else-if="props.isLoading" class="empty-state">Loading your grade…</div>
    <div v-else-if="!props.grade" class="empty-state">Your submission has not been graded yet.</div>
    <template v-else>
      <div class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Grade</dt>
          <dd class="mt-2 text-lg font-semibold text-slate-900">{{ props.grade.value }}%</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Grade ID</dt>
          <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ props.grade.id }}</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Feedback updated</dt>
          <dd class="mt-2 text-sm text-slate-900">{{ formatDate(props.grade.lastModifiedAt) }}</dd>
        </div>
      </div>

      <div class="surface-panel-muted p-4">
        <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Feedback</p>
        <p class="mt-3 whitespace-pre-wrap text-sm leading-6 text-slate-700">{{ props.grade.feedback }}</p>
      </div>
    </template>
  </section>
</template>


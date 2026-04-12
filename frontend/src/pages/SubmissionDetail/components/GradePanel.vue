<script setup lang="ts">
import { reactive, watch } from 'vue'

import type { GradeResponse } from '@/types/grade'

const props = defineProps<{
  submissionVerified: boolean
  isCreating: boolean
  isUpdating: boolean
  errorMessage: string | null
  successMessage: string | null
  existingGrade: GradeResponse | null
}>()

const emit = defineEmits<{
  (e: 'create', payload: { value: number; feedback: string }): void
  (e: 'update', payload: { value: number; feedback: string }): void
}>()

const form = reactive({
  value: props.existingGrade?.value ?? null as number | null,
  feedback: props.existingGrade?.feedback ?? '',
})

// Sync form when existingGrade arrives after initial mount (i.e. after create response)
watch(
  () => props.existingGrade,
  (grade) => {
    if (grade) {
      form.value = grade.value
      form.feedback = grade.feedback
    } else {
      form.value = null
      form.feedback = ''
    }
  },
)

function handleSubmit() {
  if (form.value === null) {
    return
  }

  const gradeValue = Number(form.value)

  if (props.existingGrade) {
    emit('update', { value: gradeValue, feedback: form.feedback })
  } else {
    emit('create', { value: gradeValue, feedback: form.feedback })
  }
}

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
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">
        {{ props.existingGrade ? 'Update grade' : 'Grade submission' }}
      </h3>
    </div>

    <div v-if="!props.submissionVerified" class="empty-state">
      Grading is only available once the submission has a
      <span class="font-semibold text-[var(--color-heading)]">verified</span> status.
    </div>

    <template v-else>
      <div v-if="props.errorMessage" class="alert-error mb-4">{{ props.errorMessage }}</div>
      <div v-if="props.successMessage" class="alert-success mb-4">{{ props.successMessage }}</div>

      <div v-if="props.existingGrade" class="stats-grid mb-6">
        <div class="stat-card">
          <dt class="meta-label">Grade ID</dt>
          <dd class="meta-value break-all mono-meta">{{ props.existingGrade.id }}</dd>
        </div>
        <div class="stat-card bg-[var(--color-surface-offset)]">
          <dt class="meta-label">Current percentage</dt>
          <dd class="meta-value font-medium">{{ props.existingGrade.value }}%</dd>
        </div>
        <div class="stat-card">
          <dt class="meta-label">Graded at</dt>
          <dd class="meta-value">{{ formatDate(props.existingGrade.gradedAt) }}</dd>
        </div>
        <div class="stat-card">
          <dt class="meta-label">Last modified</dt>
          <dd class="meta-value">{{ formatDate(props.existingGrade.lastModifiedAt) }}</dd>
        </div>
      </div>

      <form class="grid gap-5 lg:grid-cols-2" @submit.prevent="handleSubmit">
        <label class="block lg:col-span-1">
          <span class="field-label">Grade percentage</span>
          <input
            v-model.number="form.value"
            type="number"
            required
            min="0"
            max="100"
            step="1"
            class="form-input"
            placeholder="0 - 100"
          />
        </label>

        <label class="block lg:col-span-2">
          <span class="field-label">Feedback</span>
          <textarea
            v-model="form.feedback"
            required
            rows="4"
            class="form-input"
            placeholder="Provide constructive feedback on the submission."
          />
        </label>

        <div class="lg:col-span-2">
          <button
            type="submit"
            class="btn-primary"
            :disabled="props.isCreating || props.isUpdating"
          >
            <template v-if="props.existingGrade">
              {{ props.isUpdating ? 'Updating…' : 'Update grade' }}
            </template>
            <template v-else>
              {{ props.isCreating ? 'Grading…' : 'Submit grade' }}
            </template>
          </button>
        </div>
      </form>
    </template>
  </section>
</template>



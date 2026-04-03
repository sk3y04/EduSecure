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
  <section class="surface-panel p-8">
    <div class="mb-6 border-b border-slate-200 pb-5">
      <h3 class="text-xl font-semibold text-slate-900">
        {{ props.existingGrade ? 'Update grade' : 'Grade submission' }}
      </h3>
      <p class="mt-2 text-sm leading-6 text-slate-600">
        Only verified submissions can be graded. Grades are stored as whole-number percentages
        from 0 to 100. One grade per submission — use the update form once a grade already exists.
      </p>
    </div>

    <div v-if="!props.submissionVerified" class="empty-state">
      Grading is only available once the submission has a
      <span class="font-semibold text-slate-900">VERIFIED</span> status.
    </div>

    <template v-else>
      <div v-if="props.errorMessage" class="alert-error mb-4">{{ props.errorMessage }}</div>
      <div v-if="props.successMessage" class="alert-success mb-4">{{ props.successMessage }}</div>

      <div v-if="props.existingGrade" class="mb-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Grade ID</dt>
          <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ props.existingGrade.id }}</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Current percentage</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ props.existingGrade.value }}%</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Graded at</dt>
          <dd class="mt-2 text-sm text-slate-900">{{ formatDate(props.existingGrade.gradedAt) }}</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Last modified</dt>
          <dd class="mt-2 text-sm text-slate-900">{{ formatDate(props.existingGrade.lastModifiedAt) }}</dd>
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
          <span class="mt-2 block text-xs text-slate-500">Enter a whole-number percentage between 0 and 100.</span>
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



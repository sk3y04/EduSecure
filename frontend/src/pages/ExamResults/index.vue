<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { examsService } from '@/services/exams'
import { examResultsService } from '@/services/examResults'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { ExamResultsHeader, ExamResultsMetricsPanel } from './components'
import type { Exam } from '@/types/exam'
import type {
  CreateExamResultRequest,
  ExamResultResponse,
  MyExamResultResponse,
  UpdateExamResultRequest,
} from '@/types/examResult'

const authStore = useAuthStore()

const isStaffView = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))

const exams = ref<Exam[]>([])
const selectedExamId = ref('')
const staffResults = ref<ExamResultResponse[]>([])
const myResults = ref<MyExamResultResponse[]>([])

const isLoading = ref(true)
const isLoadingResults = ref(false)
const isSubmitting = ref(false)
const editingResultId = ref<string | null>(null)

const loadError = ref<string | null>(null)
const resultError = ref<string | null>(null)
const formError = ref<string | null>(null)
const formSuccess = ref<string | null>(null)

const createForm = ref<CreateExamResultRequest>(emptyCreateForm())
const editForm = ref<UpdateExamResultRequest>(emptyUpdateForm())

const selectedExam = computed(() => exams.value.find((exam) => exam.id === selectedExamId.value) ?? null)
const hasStaffResults = computed(() => staffResults.value.length > 0)
const hasMyResults = computed(() => myResults.value.length > 0)
const publishedResultsCount = computed(() =>
  isStaffView.value
    ? staffResults.value.filter((result) => result.published).length
    : myResults.value.filter((result) => Boolean(result.publishedAt)).length,
)
const draftResultsCount = computed(() =>
  isStaffView.value ? staffResults.value.filter((result) => !result.published).length : 0,
)

function emptyCreateForm(): CreateExamResultRequest {
  return {
    studentEmail: '',
    value: 0,
    feedback: '',
    published: false,
  }
}

function emptyUpdateForm(): UpdateExamResultRequest {
  return {
    value: 0,
    feedback: '',
    published: false,
  }
}

function applyUpsert(updatedResult: ExamResultResponse) {
  const existingIndex = staffResults.value.findIndex((result) => result.id === updatedResult.id)

  if (existingIndex >= 0) {
    staffResults.value.splice(existingIndex, 1, updatedResult)
  } else {
    staffResults.value = [...staffResults.value, updatedResult]
  }
}

function formatDate(value: string | null) {
  if (!value) {
    return '—'
  }

  return new Date(value).toLocaleString()
}

async function loadStaffExams() {
  exams.value = await examsService.list()
  if (!selectedExamId.value && exams.value.length > 0) {
    selectedExamId.value = exams.value[0].id
  }
}

async function loadStaffResults() {
  if (!selectedExamId.value) {
    staffResults.value = []
    return
  }

  isLoadingResults.value = true
  resultError.value = null

  try {
    staffResults.value = await examResultsService.listForExam(selectedExamId.value)
  } catch (error) {
    resultError.value = extractErrorMessage(error)
  } finally {
    isLoadingResults.value = false
  }
}

async function loadMyResults() {
  isLoadingResults.value = true
  resultError.value = null

  try {
    myResults.value = await examResultsService.listMine()
  } catch (error) {
    resultError.value = extractErrorMessage(error)
  } finally {
    isLoadingResults.value = false
  }
}

async function loadPage() {
  isLoading.value = true
  loadError.value = null

  try {
    if (isStaffView.value) {
      await loadStaffExams()
      await loadStaffResults()
    } else {
      await loadMyResults()
    }
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function handleCreate() {
  if (!selectedExamId.value) {
    return
  }

  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null

  try {
    const created = await examResultsService.create(selectedExamId.value, {
      studentEmail: createForm.value.studentEmail.trim(),
      value: Number(createForm.value.value),
      feedback: createForm.value.feedback.trim(),
      published: createForm.value.published,
    })
    applyUpsert(created)
    createForm.value = emptyCreateForm()
    formSuccess.value = 'Exam result created.'
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function startEdit(result: ExamResultResponse) {
  editingResultId.value = result.id
  formError.value = null
  formSuccess.value = null
  editForm.value = {
    value: result.value,
    feedback: result.feedback ?? '',
    published: result.published,
  }
}

function cancelEdit() {
  editingResultId.value = null
  editForm.value = emptyUpdateForm()
}

async function handleUpdate() {
  if (!editingResultId.value) {
    return
  }

  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null

  try {
    const updated = await examResultsService.update(editingResultId.value, {
      value: Number(editForm.value.value),
      feedback: editForm.value.feedback.trim(),
      published: editForm.value.published,
    })
    applyUpsert(updated)
    cancelEdit()
    formSuccess.value = 'Exam result updated.'
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

watch(selectedExamId, async (newExamId, oldExamId) => {
  if (!isStaffView.value || newExamId === oldExamId) {
    return
  }

  editingResultId.value = null
  formError.value = null
  formSuccess.value = null
  await loadStaffResults()
})

onMounted(() => {
  void loadPage()
})
</script>

<template>
  <section class="desktop-page-grid">
    <ExamResultsHeader class="xl:col-span-8 xl:row-span-2" />

    <ExamResultsMetricsPanel
      class="xl:col-span-4"
      :total-exams="isStaffView ? exams.length : myResults.length"
      :selected-exam-results="isStaffView ? staffResults.length : myResults.length"
      :published-results="publishedResultsCount"
      :draft-results="draftResultsCount"
    />

    <div v-if="loadError" class="alert-error xl:col-span-12">{{ loadError }}</div>
    <div v-else-if="isLoading" class="empty-state xl:col-span-12">Loading exam results…</div>

    <template v-else-if="isStaffView">
      <section class="page-section desktop-page-panel flex h-full flex-col xl:col-span-4 xl:row-span-3">
        <div class="panel-header">
          <h3 class="panel-title">Manage exam results</h3>
          <p class="panel-copy">
            Select a managed exam, create one result per student, and choose when that result becomes visible.
          </p>
        </div>

        <label class="space-y-2">
          <span class="field-label">Exam</span>
          <select v-model="selectedExamId" class="form-input" :disabled="isSubmitting || !exams.length">
            <option value="" disabled>Select an exam</option>
            <option v-for="exam in exams" :key="exam.id" :value="exam.id">
              {{ exam.spaceCode }} · {{ exam.title }}
            </option>
          </select>
        </label>

        <div v-if="selectedExam" class="surface-panel-muted mt-4 p-4">
          <p class="meta-label">Selected exam</p>
          <p class="mt-2 text-base font-medium text-[var(--color-heading)]">
            {{ selectedExam.spaceCode }} · {{ selectedExam.title }}
          </p>
          <p class="mt-1 text-sm text-[var(--color-text-soft)]">
            {{ selectedExam.spaceName }} · {{ selectedExam.location }}
          </p>
        </div>

        <div v-if="!exams.length" class="empty-state mt-4 flex-1">
          No manageable exams are available yet. Create an exam schedule entry first.
        </div>

        <template v-else>
          <div class="mt-4 grid gap-4 lg:grid-cols-2">
            <label class="space-y-2">
              <span class="field-label">Student email</span>
              <input v-model="createForm.studentEmail" type="email" class="form-input" placeholder="student@example.com">
            </label>

            <label class="space-y-2">
              <span class="field-label">Result value</span>
              <input v-model.number="createForm.value" type="number" min="0" max="100" class="form-input">
            </label>

            <label class="space-y-2 lg:col-span-2">
              <span class="field-label">Feedback</span>
              <textarea v-model="createForm.feedback" class="form-input min-h-28" maxlength="2000"></textarea>
            </label>
          </div>

          <label class="mt-4 inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)]">
            <input v-model="createForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
            Publish immediately
          </label>

          <div class="mt-4 flex flex-wrap items-center gap-3">
            <button type="button" class="btn-primary" :disabled="isSubmitting || !selectedExamId" @click="handleCreate">
              {{ isSubmitting ? 'Saving…' : 'Create exam result' }}
            </button>
            <button type="button" class="btn-secondary" :disabled="!selectedExamId" @click="loadStaffResults">
              Refresh results
            </button>
          </div>
        </template>

        <p v-if="formError" class="alert-error mt-4">{{ formError }}</p>
        <p v-else-if="formSuccess" class="alert-success mt-4">{{ formSuccess }}</p>
      </section>

      <section class="page-section desktop-page-panel flex min-h-[34rem] flex-col xl:col-span-8 xl:row-span-4">
        <div class="panel-header">
          <h3 class="panel-title">Exam result records</h3>
          <p class="panel-copy">
            One result may exist per student per exam. Publication is controlled per result.
          </p>
        </div>

        <div v-if="resultError" class="alert-error">{{ resultError }}</div>
        <div v-else-if="isLoadingResults" class="empty-state">Loading exam result records…</div>
        <div v-else-if="!selectedExamId" class="empty-state">Select an exam to view its results.</div>
        <div v-else-if="!hasStaffResults" class="empty-state">No exam results recorded for this exam yet.</div>

        <div v-else class="panel-scroll-list">
          <article v-for="result in staffResults" :key="result.id" class="record-card">
            <template v-if="editingResultId === result.id">
              <div class="grid gap-4 lg:grid-cols-2">
                <label class="space-y-2">
                  <span class="field-label">Result value</span>
                  <input v-model.number="editForm.value" type="number" min="0" max="100" class="form-input">
                </label>

                <label class="mt-8 inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)] lg:mt-10">
                  <input v-model="editForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
                  Published
                </label>

                <label class="space-y-2 lg:col-span-2">
                  <span class="field-label">Feedback</span>
                  <textarea v-model="editForm.feedback" class="form-input min-h-28" maxlength="2000"></textarea>
                </label>
              </div>

              <div class="mt-4 flex flex-wrap items-center gap-3">
                <button type="button" class="btn-primary" :disabled="isSubmitting" @click="handleUpdate">
                  {{ isSubmitting ? 'Saving…' : 'Save changes' }}
                </button>
                <button type="button" class="btn-secondary" :disabled="isSubmitting" @click="cancelEdit">
                  Cancel
                </button>
              </div>
            </template>

            <template v-else>
              <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div class="space-y-3">
                  <div>
                    <p class="text-sm font-medium uppercase tracking-[0.2em] text-[var(--color-text-soft)]">
                      {{ result.studentFullName }} · {{ result.studentEmail }}
                    </p>
                    <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                      {{ result.value }}%
                    </h4>
                  </div>

                  <div class="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                    <span :class="result.published ? 'status-pill status-pill-success' : 'status-pill status-pill-neutral'">
                      {{ result.published ? 'Published' : 'Draft' }}
                    </span>
                    <span>Published {{ formatDate(result.publishedAt) }}</span>
                    <span>Updated {{ formatDate(result.lastModifiedAt ?? result.gradedAt) }}</span>
                  </div>

                  <p v-if="result.feedback" class="text-base leading-7 text-[var(--color-text)]">
                    {{ result.feedback }}
                  </p>
                </div>

                <button type="button" class="btn-secondary self-start" @click="startEdit(result)">
                  Edit
                </button>
              </div>
            </template>
          </article>
        </div>
      </section>
    </template>

    <template v-else>
      <section class="page-section desktop-page-panel flex min-h-[34rem] flex-col xl:col-span-12">
        <div class="panel-header">
          <h3 class="panel-title">Your published exam results</h3>
          <p class="panel-copy">
            Only published results remain visible here, and access still depends on your current membership in the linked academic space.
          </p>
        </div>

        <div v-if="resultError" class="alert-error">{{ resultError }}</div>
        <div v-else-if="isLoadingResults" class="empty-state">Loading your exam results…</div>
        <div v-else-if="!hasMyResults" class="empty-state">No published exam results are visible yet.</div>

        <div v-else class="panel-scroll-list">
          <article v-for="result in myResults" :key="result.id" class="record-card">
            <div class="space-y-3">
              <div>
                <p class="text-sm font-medium uppercase tracking-[0.2em] text-[var(--color-text-soft)]">
                  {{ result.spaceCode }} · {{ result.spaceName }}
                </p>
                <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                  {{ result.examTitle }}
                </h4>
              </div>

              <div class="stats-grid">
                <div class="stat-card bg-[var(--color-surface-offset)]">
                  <dt class="meta-label">Result</dt>
                  <dd class="meta-value text-lg font-semibold">{{ result.value }}%</dd>
                </div>
                <div class="stat-card">
                  <dt class="meta-label">Published</dt>
                  <dd class="meta-value">{{ formatDate(result.publishedAt) }}</dd>
                </div>
                <div class="stat-card">
                  <dt class="meta-label">Feedback updated</dt>
                  <dd class="meta-value">{{ formatDate(result.lastModifiedAt) }}</dd>
                </div>
              </div>

              <div class="surface-panel-muted p-4">
                <p class="meta-label">Feedback</p>
                <p class="mt-3 whitespace-pre-wrap text-base leading-7 text-[var(--color-text)]">
                  {{ result.feedback || 'No feedback was recorded for this result.' }}
                </p>
              </div>
            </div>
          </article>
        </div>
      </section>
    </template>
  </section>
</template>
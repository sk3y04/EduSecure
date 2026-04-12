<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { examsService } from '@/services/exams'
import { examResultsService } from '@/services/examResults'
import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import { useAuthStore } from '@/stores/auth'
import type { Exam, ExamPayload } from '@/types/exam'
import type {
  CreateExamResultRequest,
  ExamResultResponse,
  MyExamResultResponse,
  UpdateExamResultRequest,
} from '@/types/examResult'
import type { SpaceSummary } from '@/types/space'

type AssessmentView = 'schedule' | 'results'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const isStaffView = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))

const exams = ref<Exam[]>([])
const manageableSpaces = ref<SpaceSummary[]>([])

const isLoadingPage = ref(true)
const isInitializing = ref(false)
const isLoadingSpaces = ref(false)
const isSavingExam = ref(false)
const isSavingResult = ref(false)
const editingExamId = ref<string | null>(null)
const editingResultId = ref<string | null>(null)

const pageError = ref<string | null>(null)
const examFormError = ref<string | null>(null)
const examFormSuccess = ref<string | null>(null)
const resultLoadError = ref<string | null>(null)
const resultFormError = ref<string | null>(null)
const resultFormSuccess = ref<string | null>(null)

const selectedExamId = ref('')
const staffResults = ref<ExamResultResponse[]>([])
const myResults = ref<MyExamResultResponse[]>([])
const isLoadingResults = ref(false)

const createExamForm = ref<ExamPayload>(emptyExamForm())
const editExamForm = ref<ExamPayload>(emptyExamForm())
const createResultForm = ref<CreateExamResultRequest>(emptyCreateResultForm())
const editResultForm = ref<UpdateExamResultRequest>(emptyUpdateResultForm())

const currentView = computed<AssessmentView>(() => route.query.view === 'results' ? 'results' : 'schedule')
const hasExams = computed(() => exams.value.length > 0)
const publishedExams = computed(() => exams.value.filter((exam) => exam.published).length)
const draftExams = computed(() => exams.value.filter((exam) => !exam.published).length)
const manageableExams = computed(() => exams.value.filter((exam) => exam.canManage))
const selectedExam = computed(() => manageableExams.value.find((exam) => exam.id === selectedExamId.value) ?? null)
const visibleResults = computed(() => isStaffView.value ? staffResults.value.length : myResults.value.length)
const publishedResults = computed(() =>
  isStaffView.value
    ? staffResults.value.filter((result) => result.published).length
    : myResults.value.filter((result) => Boolean(result.publishedAt)).length,
)
const draftResults = computed(() =>
  isStaffView.value ? staffResults.value.filter((result) => !result.published).length : 0,
)

function emptyExamForm(): ExamPayload {
  return {
    spaceId: '',
    title: '',
    description: '',
    location: '',
    startsAt: '',
    endsAt: '',
    published: false,
  }
}

function emptyCreateResultForm(): CreateExamResultRequest {
  return {
    studentEmail: '',
    value: 0,
    feedback: '',
    published: false,
  }
}

function emptyUpdateResultForm(): UpdateExamResultRequest {
  return {
    value: 0,
    feedback: '',
    published: false,
  }
}

function syncSelectedExam() {
  if (!isStaffView.value) {
    selectedExamId.value = ''
    return
  }

  const nextExams = manageableExams.value

  if (!nextExams.length) {
    selectedExamId.value = ''
    return
  }

  if (!nextExams.some((exam) => exam.id === selectedExamId.value)) {
    selectedExamId.value = nextExams[0].id
  }
}

function toLocalDateTimeInput(value: string) {
  const date = new Date(value)
  const offsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

function toApiExamPayload(payload: ExamPayload): ExamPayload {
  return {
    ...payload,
    title: payload.title.trim(),
    description: payload.description.trim(),
    location: payload.location.trim(),
    startsAt: new Date(payload.startsAt).toISOString(),
    endsAt: new Date(payload.endsAt).toISOString(),
  }
}

function applyExamUpsert(updatedExam: Exam) {
  const existingIndex = exams.value.findIndex((exam) => exam.id === updatedExam.id)

  if (existingIndex >= 0) {
    exams.value.splice(existingIndex, 1, updatedExam)
  } else {
    exams.value = [...exams.value, updatedExam]
  }

  exams.value = [...exams.value].sort(
    (left, right) => new Date(left.startsAt).getTime() - new Date(right.startsAt).getTime(),
  )
  syncSelectedExam()
}

function applyResultUpsert(updatedResult: ExamResultResponse) {
  const existingIndex = staffResults.value.findIndex((result) => result.id === updatedResult.id)

  if (existingIndex >= 0) {
    staffResults.value.splice(existingIndex, 1, updatedResult)
  } else {
    staffResults.value = [...staffResults.value, updatedResult]
  }
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '—'
  }

  return new Intl.DateTimeFormat('en-US', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(value))
}

async function setView(view: AssessmentView) {
  const query = { ...route.query }

  if (view === 'results') {
    query.view = 'results'
  } else {
    delete query.view
  }

  await router.replace({
    name: 'exams',
    query,
  })
}

async function loadExams() {
  exams.value = await examsService.list()
  syncSelectedExam()
}

async function loadManageableSpaces() {
  if (!isStaffView.value) {
    manageableSpaces.value = []
    return
  }

  isLoadingSpaces.value = true

  try {
    manageableSpaces.value = (await spacesService.list()).filter((space) => space.canManage && !space.archived)

    if (!createExamForm.value.spaceId && manageableSpaces.value.length > 0) {
      createExamForm.value.spaceId = manageableSpaces.value[0].id
    }
  } finally {
    isLoadingSpaces.value = false
  }
}

async function loadStaffResults() {
  if (!selectedExamId.value) {
    staffResults.value = []
    return
  }

  isLoadingResults.value = true
  resultLoadError.value = null

  try {
    staffResults.value = await examResultsService.listForExam(selectedExamId.value)
  } catch (error) {
    resultLoadError.value = extractErrorMessage(error)
  } finally {
    isLoadingResults.value = false
  }
}

async function loadMyResults() {
  isLoadingResults.value = true
  resultLoadError.value = null

  try {
    myResults.value = await examResultsService.listMine()
  } catch (error) {
    resultLoadError.value = extractErrorMessage(error)
  } finally {
    isLoadingResults.value = false
  }
}

async function loadPage() {
  isLoadingPage.value = true
  isInitializing.value = true
  pageError.value = null

  try {
    await loadExams()

    if (isStaffView.value) {
      await Promise.all([loadManageableSpaces(), loadStaffResults()])
    } else {
      await loadMyResults()
    }
  } catch (error) {
    pageError.value = extractErrorMessage(error)
  } finally {
    isInitializing.value = false
    isLoadingPage.value = false
  }
}

async function refreshSchedule() {
  pageError.value = null

  try {
    await loadExams()
  } catch (error) {
    pageError.value = extractErrorMessage(error)
  }
}

async function refreshResults() {
  if (isStaffView.value) {
    await loadStaffResults()
    return
  }

  await loadMyResults()
}

async function handleCreateExam() {
  isSavingExam.value = true
  examFormError.value = null
  examFormSuccess.value = null

  try {
    const created = await examsService.create(toApiExamPayload(createExamForm.value))
    applyExamUpsert(created)
    createExamForm.value = {
      ...emptyExamForm(),
      spaceId: createExamForm.value.spaceId,
    }
    examFormSuccess.value = 'Exam saved.'
  } catch (error) {
    examFormError.value = extractErrorMessage(error)
  } finally {
    isSavingExam.value = false
  }
}

function startEditExam(exam: Exam) {
  editingExamId.value = exam.id
  examFormError.value = null
  examFormSuccess.value = null
  editExamForm.value = {
    spaceId: exam.spaceId,
    title: exam.title,
    description: exam.description ?? '',
    location: exam.location,
    startsAt: toLocalDateTimeInput(exam.startsAt),
    endsAt: toLocalDateTimeInput(exam.endsAt),
    published: exam.published,
  }
}

function cancelEditExam() {
  editingExamId.value = null
  editExamForm.value = emptyExamForm()
}

async function handleUpdateExam() {
  if (!editingExamId.value) {
    return
  }

  isSavingExam.value = true
  examFormError.value = null
  examFormSuccess.value = null

  try {
    const updated = await examsService.update(editingExamId.value, toApiExamPayload(editExamForm.value))
    applyExamUpsert(updated)
    cancelEditExam()
    examFormSuccess.value = 'Changes saved.'
  } catch (error) {
    examFormError.value = extractErrorMessage(error)
  } finally {
    isSavingExam.value = false
  }
}

async function handleCreateResult() {
  if (!selectedExamId.value) {
    return
  }

  isSavingResult.value = true
  resultFormError.value = null
  resultFormSuccess.value = null

  try {
    const created = await examResultsService.create(selectedExamId.value, {
      studentEmail: createResultForm.value.studentEmail.trim(),
      value: Number(createResultForm.value.value),
      feedback: createResultForm.value.feedback.trim(),
      published: createResultForm.value.published,
    })
    applyResultUpsert(created)
    createResultForm.value = emptyCreateResultForm()
    resultFormSuccess.value = 'Result saved.'
  } catch (error) {
    resultFormError.value = extractErrorMessage(error)
  } finally {
    isSavingResult.value = false
  }
}

function startEditResult(result: ExamResultResponse) {
  editingResultId.value = result.id
  resultFormError.value = null
  resultFormSuccess.value = null
  editResultForm.value = {
    value: result.value,
    feedback: result.feedback ?? '',
    published: result.published,
  }
}

function cancelEditResult() {
  editingResultId.value = null
  editResultForm.value = emptyUpdateResultForm()
}

async function handleUpdateResult() {
  if (!editingResultId.value) {
    return
  }

  isSavingResult.value = true
  resultFormError.value = null
  resultFormSuccess.value = null

  try {
    const updated = await examResultsService.update(editingResultId.value, {
      value: Number(editResultForm.value.value),
      feedback: editResultForm.value.feedback.trim(),
      published: editResultForm.value.published,
    })
    applyResultUpsert(updated)
    cancelEditResult()
    resultFormSuccess.value = 'Changes saved.'
  } catch (error) {
    resultFormError.value = extractErrorMessage(error)
  } finally {
    isSavingResult.value = false
  }
}

watch(selectedExamId, async (newExamId, oldExamId) => {
  if (!isStaffView.value || isInitializing.value || newExamId === oldExamId) {
    return
  }

  editingResultId.value = null
  resultFormError.value = null
  resultFormSuccess.value = null
  await loadStaffResults()
})

onMounted(() => {
  void loadPage()
})
</script>

<template>
  <section class="space-y-6">
    <section class="page-section">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="meta-label">Assessments</p>
          <h2 class="section-title">Exams and results</h2>
        </div>

        <div class="segmented-control">
          <button
            type="button"
            class="segmented-control-button"
            :class="{ 'segmented-control-button-active': currentView === 'schedule' }"
            @click="setView('schedule')"
          >
            Exams
          </button>
          <button
            type="button"
            class="segmented-control-button"
            :class="{ 'segmented-control-button-active': currentView === 'results' }"
            @click="setView('results')"
          >
            {{ isStaffView ? 'Results' : 'My results' }}
          </button>
        </div>
      </div>

      <div class="stats-grid mt-6">
        <article class="stat-card">
          <p class="meta-label">Exams</p>
          <p class="mt-2 text-3xl font-semibold text-[var(--color-heading)]">{{ exams.length }}</p>
        </article>
        <article class="stat-card">
          <p class="meta-label">Published exams</p>
          <p class="mt-2 text-3xl font-semibold text-[var(--color-heading)]">{{ publishedExams }}</p>
        </article>
        <article class="stat-card">
          <p class="meta-label">Results</p>
          <p class="mt-2 text-3xl font-semibold text-[var(--color-heading)]">{{ visibleResults }}</p>
        </article>
        <article class="stat-card">
          <p class="meta-label">Published results</p>
          <p class="mt-2 text-3xl font-semibold text-[var(--color-heading)]">{{ publishedResults }}</p>
        </article>
        <article class="stat-card">
          <p class="meta-label">Draft items</p>
          <p class="mt-2 text-3xl font-semibold text-[var(--color-heading)]">
            {{ currentView === 'schedule' ? draftExams : draftResults }}
          </p>
        </article>
      </div>
    </section>

    <div v-if="pageError" class="alert-error">{{ pageError }}</div>
    <div v-else-if="isLoadingPage" class="empty-state">Loading…</div>

    <template v-else-if="currentView === 'schedule'">
      <div v-if="isStaffView" class="content-grid-4-8">
        <section class="page-section flex flex-col gap-4">
          <div class="panel-header">
            <h3 class="panel-title">New exam</h3>
          </div>

          <div class="grid gap-4 md:grid-cols-2 xl:grid-cols-1">
            <label class="space-y-2">
              <span class="field-label">Space</span>
              <select v-model="createExamForm.spaceId" class="form-input" :disabled="isLoadingSpaces || isSavingExam">
                <option value="" disabled>Select a space</option>
                <option v-for="space in manageableSpaces" :key="space.id" :value="space.id">
                  {{ space.code }} · {{ space.name }}
                </option>
              </select>
            </label>

            <label class="space-y-2">
              <span class="field-label">Location</span>
              <input v-model="createExamForm.location" type="text" class="form-input" maxlength="160">
            </label>

            <label class="space-y-2 md:col-span-2 xl:col-span-1">
              <span class="field-label">Title</span>
              <input v-model="createExamForm.title" type="text" class="form-input" maxlength="160">
            </label>

            <label class="space-y-2 md:col-span-2 xl:col-span-1">
              <span class="field-label">Description</span>
              <textarea v-model="createExamForm.description" class="form-input min-h-28" maxlength="2000"></textarea>
            </label>

            <label class="space-y-2">
              <span class="field-label">Starts</span>
              <input v-model="createExamForm.startsAt" type="datetime-local" class="form-input">
            </label>

            <label class="space-y-2">
              <span class="field-label">Ends</span>
              <input v-model="createExamForm.endsAt" type="datetime-local" class="form-input">
            </label>
          </div>

          <label class="inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)]">
            <input v-model="createExamForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
            Publish now
          </label>

          <div class="flex flex-wrap items-center gap-3">
            <button type="button" class="btn-primary" :disabled="isSavingExam || !manageableSpaces.length" @click="handleCreateExam">
              {{ isSavingExam ? 'Saving…' : 'Save exam' }}
            </button>
            <button type="button" class="btn-secondary" @click="refreshSchedule">Refresh</button>
          </div>

          <p v-if="examFormError" class="alert-error">{{ examFormError }}</p>
          <p v-else-if="examFormSuccess" class="alert-success">{{ examFormSuccess }}</p>
        </section>

        <section class="page-section flex min-h-[32rem] flex-col">
          <div class="panel-header-split">
            <h3 class="panel-title">Exam list</h3>
            <button type="button" class="btn-secondary" @click="refreshSchedule">Refresh</button>
          </div>

          <div v-if="!hasExams" class="empty-state">No exams yet.</div>

          <div v-else class="panel-scroll-list">
            <article v-for="exam in exams" :key="exam.id" class="record-card">
              <template v-if="editingExamId === exam.id">
                <div class="grid gap-4 lg:grid-cols-2">
                  <label class="space-y-2">
                    <span class="field-label">Space</span>
                    <select v-model="editExamForm.spaceId" class="form-input">
                      <option v-for="space in manageableSpaces" :key="space.id" :value="space.id">
                        {{ space.code }} · {{ space.name }}
                      </option>
                    </select>
                  </label>

                  <label class="space-y-2">
                    <span class="field-label">Location</span>
                    <input v-model="editExamForm.location" type="text" class="form-input" maxlength="160">
                  </label>

                  <label class="space-y-2 lg:col-span-2">
                    <span class="field-label">Title</span>
                    <input v-model="editExamForm.title" type="text" class="form-input" maxlength="160">
                  </label>

                  <label class="space-y-2 lg:col-span-2">
                    <span class="field-label">Description</span>
                    <textarea v-model="editExamForm.description" class="form-input min-h-28" maxlength="2000"></textarea>
                  </label>

                  <label class="space-y-2">
                    <span class="field-label">Starts</span>
                    <input v-model="editExamForm.startsAt" type="datetime-local" class="form-input">
                  </label>

                  <label class="space-y-2">
                    <span class="field-label">Ends</span>
                    <input v-model="editExamForm.endsAt" type="datetime-local" class="form-input">
                  </label>
                </div>

                <label class="mt-4 inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)]">
                  <input v-model="editExamForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
                  Published
                </label>

                <div class="mt-4 flex flex-wrap items-center gap-3">
                  <button type="button" class="btn-primary" :disabled="isSavingExam" @click="handleUpdateExam">
                    {{ isSavingExam ? 'Saving…' : 'Save changes' }}
                  </button>
                  <button type="button" class="btn-secondary" :disabled="isSavingExam" @click="cancelEditExam">
                    Cancel
                  </button>
                </div>
              </template>

              <template v-else>
                <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div class="space-y-3">
                    <div>
                      <p class="text-sm font-medium uppercase tracking-[0.16em] text-[var(--color-text-soft)]">
                        {{ exam.spaceCode }} · {{ exam.spaceName }}
                      </p>
                      <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                        {{ exam.title }}
                      </h4>
                    </div>

                    <div class="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                      <span :class="exam.published ? 'status-pill status-pill-success' : 'status-pill status-pill-neutral'">
                        {{ exam.published ? 'Published' : 'Draft' }}
                      </span>
                      <span>{{ exam.location }}</span>
                      <span>{{ formatDateTime(exam.startsAt) }} – {{ formatDateTime(exam.endsAt) }}</span>
                    </div>

                    <p v-if="exam.description" class="text-sm leading-6 text-[var(--color-text)]">
                      {{ exam.description }}
                    </p>
                  </div>

                  <button
                    v-if="exam.canManage"
                    type="button"
                    class="btn-secondary self-start"
                    @click="startEditExam(exam)"
                  >
                    Edit
                  </button>
                </div>
              </template>
            </article>
          </div>
        </section>
      </div>

      <section v-else class="page-section flex min-h-[32rem] flex-col">
        <div class="panel-header-split">
          <h3 class="panel-title">Exam list</h3>
          <button type="button" class="btn-secondary" @click="refreshSchedule">Refresh</button>
        </div>

        <div v-if="!hasExams" class="empty-state">No exams yet.</div>

        <div v-else class="panel-scroll-list">
          <article v-for="exam in exams" :key="exam.id" class="record-card">
            <div class="space-y-3">
              <div>
                <p class="text-sm font-medium uppercase tracking-[0.16em] text-[var(--color-text-soft)]">
                  {{ exam.spaceCode }} · {{ exam.spaceName }}
                </p>
                <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                  {{ exam.title }}
                </h4>
              </div>

              <div class="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                <span>{{ exam.location }}</span>
                <span>{{ formatDateTime(exam.startsAt) }} – {{ formatDateTime(exam.endsAt) }}</span>
              </div>

              <p v-if="exam.description" class="text-sm leading-6 text-[var(--color-text)]">
                {{ exam.description }}
              </p>
            </div>
          </article>
        </div>
      </section>
    </template>

    <template v-else>
      <div v-if="isStaffView" class="content-grid-4-8">
        <section class="page-section flex flex-col gap-4">
          <div class="panel-header">
            <h3 class="panel-title">New result</h3>
          </div>

          <label class="space-y-2">
            <span class="field-label">Exam</span>
            <select v-model="selectedExamId" class="form-input" :disabled="isSavingResult || !manageableExams.length">
              <option value="" disabled>Select an exam</option>
              <option v-for="exam in manageableExams" :key="exam.id" :value="exam.id">
                {{ exam.spaceCode }} · {{ exam.title }}
              </option>
            </select>
          </label>

          <div v-if="selectedExam" class="surface-panel-muted p-4">
            <p class="text-sm font-semibold text-[var(--color-heading)]">{{ selectedExam.title }}</p>
            <p class="mt-1 text-sm text-[var(--color-text-soft)]">
              {{ selectedExam.spaceCode }} · {{ selectedExam.location }}
            </p>
          </div>

          <div v-if="!manageableExams.length" class="empty-state">Create an exam first.</div>

          <template v-else>
            <div class="grid gap-4">
              <label class="space-y-2">
                <span class="field-label">Student email</span>
                <input v-model="createResultForm.studentEmail" type="email" class="form-input" placeholder="student@example.com">
              </label>

              <label class="space-y-2">
                <span class="field-label">Score</span>
                <input v-model.number="createResultForm.value" type="number" min="0" max="100" class="form-input">
              </label>

              <label class="space-y-2">
                <span class="field-label">Feedback</span>
                <textarea v-model="createResultForm.feedback" class="form-input min-h-28" maxlength="2000"></textarea>
              </label>
            </div>

            <label class="inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)]">
              <input v-model="createResultForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
              Publish now
            </label>

            <div class="flex flex-wrap items-center gap-3">
              <button type="button" class="btn-primary" :disabled="isSavingResult || !selectedExamId" @click="handleCreateResult">
                {{ isSavingResult ? 'Saving…' : 'Save result' }}
              </button>
              <button type="button" class="btn-secondary" :disabled="!selectedExamId" @click="refreshResults">
                Refresh
              </button>
            </div>
          </template>

          <p v-if="resultFormError" class="alert-error">{{ resultFormError }}</p>
          <p v-else-if="resultFormSuccess" class="alert-success">{{ resultFormSuccess }}</p>
        </section>

        <section class="page-section flex min-h-[32rem] flex-col">
          <div class="panel-header-split">
            <h3 class="panel-title">Results</h3>
            <button type="button" class="btn-secondary" :disabled="!selectedExamId" @click="refreshResults">Refresh</button>
          </div>

          <div v-if="resultLoadError" class="alert-error">{{ resultLoadError }}</div>
          <div v-else-if="isLoadingResults" class="empty-state">Loading…</div>
          <div v-else-if="!selectedExamId" class="empty-state">Select an exam.</div>
          <div v-else-if="!staffResults.length" class="empty-state">No results yet.</div>

          <div v-else class="panel-scroll-list">
            <article v-for="result in staffResults" :key="result.id" class="record-card">
              <template v-if="editingResultId === result.id">
                <div class="grid gap-4 lg:grid-cols-2">
                  <label class="space-y-2">
                    <span class="field-label">Score</span>
                    <input v-model.number="editResultForm.value" type="number" min="0" max="100" class="form-input">
                  </label>

                  <label class="mt-8 inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)] lg:mt-10">
                    <input v-model="editResultForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
                    Published
                  </label>

                  <label class="space-y-2 lg:col-span-2">
                    <span class="field-label">Feedback</span>
                    <textarea v-model="editResultForm.feedback" class="form-input min-h-28" maxlength="2000"></textarea>
                  </label>
                </div>

                <div class="mt-4 flex flex-wrap items-center gap-3">
                  <button type="button" class="btn-primary" :disabled="isSavingResult" @click="handleUpdateResult">
                    {{ isSavingResult ? 'Saving…' : 'Save changes' }}
                  </button>
                  <button type="button" class="btn-secondary" :disabled="isSavingResult" @click="cancelEditResult">
                    Cancel
                  </button>
                </div>
              </template>

              <template v-else>
                <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                  <div class="space-y-3">
                    <div>
                      <p class="text-sm font-medium uppercase tracking-[0.16em] text-[var(--color-text-soft)]">
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
                      <span>Published {{ formatDateTime(result.publishedAt) }}</span>
                      <span>Updated {{ formatDateTime(result.lastModifiedAt ?? result.gradedAt) }}</span>
                    </div>

                    <p v-if="result.feedback" class="text-sm leading-6 text-[var(--color-text)]">
                      {{ result.feedback }}
                    </p>
                  </div>

                  <button type="button" class="btn-secondary self-start" @click="startEditResult(result)">
                    Edit
                  </button>
                </div>
              </template>
            </article>
          </div>
        </section>
      </div>

      <section v-else class="page-section flex min-h-[32rem] flex-col">
        <div class="panel-header-split">
          <h3 class="panel-title">My results</h3>
          <button type="button" class="btn-secondary" @click="refreshResults">Refresh</button>
        </div>

        <div v-if="resultLoadError" class="alert-error">{{ resultLoadError }}</div>
        <div v-else-if="isLoadingResults" class="empty-state">Loading…</div>
        <div v-else-if="!myResults.length" class="empty-state">No results yet.</div>

        <div v-else class="panel-scroll-list">
          <article v-for="result in myResults" :key="result.id" class="record-card">
            <div class="space-y-4">
              <div>
                <p class="text-sm font-medium uppercase tracking-[0.16em] text-[var(--color-text-soft)]">
                  {{ result.spaceCode }} · {{ result.spaceName }}
                </p>
                <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                  {{ result.examTitle }}
                </h4>
              </div>

              <div class="stats-grid">
                <div class="stat-card bg-[var(--color-surface-offset)]">
                  <dt class="meta-label">Score</dt>
                  <dd class="meta-value text-lg font-semibold">{{ result.value }}%</dd>
                </div>
                <div class="stat-card">
                  <dt class="meta-label">Published</dt>
                  <dd class="meta-value">{{ formatDateTime(result.publishedAt) }}</dd>
                </div>
                <div class="stat-card">
                  <dt class="meta-label">Updated</dt>
                  <dd class="meta-value">{{ formatDateTime(result.lastModifiedAt) }}</dd>
                </div>
              </div>

              <div class="surface-panel-muted p-4">
                <p class="meta-label">Feedback</p>
                <p class="mt-2 whitespace-pre-wrap text-sm leading-6 text-[var(--color-text)]">
                  {{ result.feedback || 'No feedback.' }}
                </p>
              </div>
            </div>
          </article>
        </div>
      </section>
    </template>
  </section>
</template>
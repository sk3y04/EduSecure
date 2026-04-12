<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { examsService } from '@/services/exams'
import { feedbackFormsService } from '@/services/feedbackForms'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import { FeedbackFormsHeader, FeedbackFormsMetricsPanel } from './components'
import type { Exam } from '@/types/exam'
import type {
  FeedbackForm,
  FeedbackFormPayload,
  FeedbackFormQuestion,
  FeedbackFormQuestionPayload,
  FeedbackFormReview,
} from '@/types/feedbackForm'

const authStore = useAuthStore()

const isStaffView = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))

const exams = ref<Exam[]>([])
const forms = ref<FeedbackForm[]>([])
const selectedExamId = ref('')
const selectedFormId = ref('')
const selectedForm = ref<FeedbackForm | null>(null)
const review = ref<FeedbackFormReview | null>(null)

const isLoading = ref(true)
const isLoadingForms = ref(false)
const isLoadingDetail = ref(false)
const isLoadingReview = ref(false)
const isSubmitting = ref(false)

const loadError = ref<string | null>(null)
const formError = ref<string | null>(null)
const formSuccess = ref<string | null>(null)
const reviewError = ref<string | null>(null)
const submitError = ref<string | null>(null)
const submitSuccess = ref<string | null>(null)

const editingFormId = ref<string | null>(null)
const formDraft = ref<FeedbackFormPayload>(emptyFormDraft())
const studentAnswers = ref<Record<string, string>>({})

const hasForms = computed(() => forms.value.length > 0)
const selectedExam = computed(() => exams.value.find((exam) => exam.id === selectedExamId.value) ?? null)
const selectedQuestionCount = computed(() => selectedForm.value?.questions.length ?? 0)
const responseVolume = computed(() =>
  isStaffView.value
    ? review.value?.responseCount ?? forms.value.reduce((sum, form) => sum + (form.responseCount ?? 0), 0)
    : forms.value.filter((form) => form.alreadySubmitted).length,
)

function emptyQuestion(order: number): FeedbackFormQuestionPayload {
  return {
    prompt: '',
    questionType: 'RATING',
    required: true,
    displayOrder: order,
  }
}

function emptyFormDraft(): FeedbackFormPayload {
  return {
    title: '',
    description: '',
    published: false,
    questions: [emptyQuestion(1)],
  }
}

function formatDate(value: string | null) {
  if (!value) {
    return '—'
  }

  return new Date(value).toLocaleString()
}

function reorderQuestions() {
  formDraft.value.questions = formDraft.value.questions.map((question, index) => ({
    ...question,
    displayOrder: index + 1,
  }))
}

function resetDraft(clearMessages = true) {
  editingFormId.value = null
  formDraft.value = emptyFormDraft()
  if (clearMessages) {
    formError.value = null
    formSuccess.value = null
  }
}

function handleResetDraft() {
  resetDraft()
}

function populateDraft(form: FeedbackForm) {
  editingFormId.value = form.id
  formDraft.value = {
    title: form.title,
    description: form.description ?? '',
    published: form.published,
    questions: form.questions.map((question) => ({
      prompt: question.prompt,
      questionType: question.questionType,
      required: question.required,
      displayOrder: question.displayOrder,
    })),
  }
  formError.value = null
  formSuccess.value = null
}

function prepareStudentAnswers(form: FeedbackForm | null) {
  if (!form) {
    studentAnswers.value = {}
    return
  }

  studentAnswers.value = Object.fromEntries(form.questions.map((question) => [question.id, '']))
}

async function loadExams() {
  exams.value = await examsService.list()
  if (!selectedExamId.value && exams.value.length > 0) {
    selectedExamId.value = exams.value[0].id
  }
}

async function loadForms() {
  if (!selectedExamId.value) {
    forms.value = []
    selectedFormId.value = ''
    selectedForm.value = null
    review.value = null
    return
  }

  isLoadingForms.value = true
  formError.value = null
  submitError.value = null
  reviewError.value = null

  try {
    forms.value = await feedbackFormsService.listForExam(selectedExamId.value)

    const currentFormStillExists = forms.value.some((form) => form.id === selectedFormId.value)
    if (!currentFormStillExists) {
      selectedFormId.value = forms.value[0]?.id ?? ''
    }
  } catch (error) {
    formError.value = extractErrorMessage(error)
    forms.value = []
    selectedFormId.value = ''
  } finally {
    isLoadingForms.value = false
  }
}

async function loadSelectedForm() {
  if (!selectedFormId.value) {
    selectedForm.value = null
    review.value = null
    prepareStudentAnswers(null)
    return
  }

  isLoadingDetail.value = true
  submitError.value = null

  try {
    selectedForm.value = await feedbackFormsService.getById(selectedFormId.value)
    prepareStudentAnswers(selectedForm.value)
  } catch (error) {
    submitError.value = extractErrorMessage(error)
    selectedForm.value = null
    review.value = null
  } finally {
    isLoadingDetail.value = false
  }
}

async function loadReview() {
  if (!isStaffView.value || !selectedFormId.value) {
    review.value = null
    return
  }

  isLoadingReview.value = true
  reviewError.value = null

  try {
    review.value = await feedbackFormsService.listResponses(selectedFormId.value)
  } catch (error) {
    reviewError.value = extractErrorMessage(error)
    review.value = null
  } finally {
    isLoadingReview.value = false
  }
}

async function loadPage() {
  isLoading.value = true
  loadError.value = null

  try {
    await loadExams()
    await loadForms()
    await loadSelectedForm()
    if (isStaffView.value) {
      await loadReview()
    }
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

function addQuestion() {
  if (formDraft.value.questions.length >= 10) {
    return
  }

  formDraft.value.questions = [...formDraft.value.questions, emptyQuestion(formDraft.value.questions.length + 1)]
}

function removeQuestion(index: number) {
  if (formDraft.value.questions.length <= 1) {
    return
  }

  formDraft.value.questions.splice(index, 1)
  reorderQuestions()
}

function normalizeDraft(): FeedbackFormPayload {
  return {
    title: formDraft.value.title.trim(),
    description: formDraft.value.description.trim(),
    published: formDraft.value.published,
    questions: formDraft.value.questions.map((question, index) => ({
      prompt: question.prompt.trim(),
      questionType: question.questionType,
      required: question.required,
      displayOrder: index + 1,
    })),
  }
}

async function handleSave() {
  if (!selectedExamId.value) {
    return
  }

  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null

  try {
    const payload = normalizeDraft()
    const saved = editingFormId.value
      ? await feedbackFormsService.update(editingFormId.value, payload)
      : await feedbackFormsService.create(selectedExamId.value, payload)

    await loadForms()
    selectedFormId.value = saved.id
    await loadSelectedForm()
    await loadReview()
    resetDraft(false)
    formSuccess.value = editingFormId.value ? 'Feedback form updated.' : 'Feedback form created.'
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

async function handleStudentSubmit() {
  if (!selectedForm.value) {
    return
  }

  isSubmitting.value = true
  submitError.value = null
  submitSuccess.value = null

  try {
    await feedbackFormsService.submitResponse(selectedForm.value.id, {
      answers: selectedForm.value.questions.map((question) => {
        const rawValue = studentAnswers.value[question.id] ?? ''
        return {
          questionId: question.id,
          ratingValue: question.questionType === 'RATING' && rawValue !== '' ? Number(rawValue) : null,
          textValue: question.questionType === 'TEXT' && rawValue.trim() !== '' ? rawValue.trim() : null,
        }
      }),
    })
    await loadForms()
    await loadSelectedForm()
    submitSuccess.value = 'Feedback submitted.'
  } catch (error) {
    submitError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function findSubmissionAnswer(question: FeedbackFormQuestion, submission: FeedbackFormReview['submissions'][number]) {
  return submission.answers.find((answer) => answer.questionId === question.id)
}

watch(selectedExamId, async (newExamId, oldExamId) => {
  if (!newExamId || newExamId === oldExamId) {
    return
  }

  selectedFormId.value = ''
  selectedForm.value = null
  review.value = null
  submitSuccess.value = null
  formSuccess.value = null
  resetDraft()
  await loadForms()
  await loadSelectedForm()
  await loadReview()
})

watch(selectedFormId, async (newFormId, oldFormId) => {
  if (!newFormId || newFormId === oldFormId) {
    if (!newFormId) {
      selectedForm.value = null
      review.value = null
      prepareStudentAnswers(null)
    }
    return
  }

  submitSuccess.value = null
  await loadSelectedForm()
  await loadReview()
})

onMounted(() => {
  void loadPage()
})
</script>

<template>
  <section class="desktop-page-grid">
    <FeedbackFormsHeader class="xl:col-span-8 xl:row-span-2" />

    <FeedbackFormsMetricsPanel
      class="xl:col-span-4"
      :accessible-exams="exams.length"
      :visible-forms="forms.length"
      :selected-questions="selectedQuestionCount"
      :response-volume="responseVolume"
    />

    <div v-if="loadError" class="alert-error xl:col-span-12">{{ loadError }}</div>
    <div v-else-if="isLoading" class="empty-state xl:col-span-12">Loading feedback forms…</div>

    <template v-else>
      <section class="page-section desktop-page-panel flex h-full flex-col xl:col-span-4 xl:row-span-2">
        <div class="panel-header">
          <h3 class="panel-title">Select exam</h3>
          <p class="panel-copy">
            Feedback forms stay linked to a published exam and reuse the same access boundary as that exam.
          </p>
        </div>

        <label class="space-y-2">
          <span class="field-label">Exam</span>
          <select v-model="selectedExamId" class="form-input" :disabled="!exams.length || isSubmitting">
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
            {{ selectedExam.spaceName }} · {{ formatDate(selectedExam.startsAt) }}
          </p>
        </div>

        <div v-if="!exams.length" class="empty-state mt-4 flex-1">
          No accessible exams are available yet.
        </div>
      </section>

      <template v-if="isStaffView">
        <section class="page-section desktop-page-panel flex min-h-[36rem] flex-col xl:col-span-8 xl:row-span-4">
          <div class="panel-header flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <h3 class="panel-title">Manage forms</h3>
              <p class="panel-copy">
                Create ordered rating and text prompts, publish them when ready, and update metadata later.
              </p>
            </div>

            <button type="button" class="btn-secondary self-start" @click="handleResetDraft">
              New form
            </button>
          </div>

          <div class="grid gap-4 lg:grid-cols-2">
            <label class="space-y-2">
              <span class="field-label">Form title</span>
              <input v-model="formDraft.title" type="text" class="form-input" maxlength="160">
            </label>

            <label class="mt-8 inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)] lg:mt-10">
              <input v-model="formDraft.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
              Published
            </label>

            <label class="space-y-2 lg:col-span-2">
              <span class="field-label">Description</span>
              <textarea v-model="formDraft.description" class="form-input min-h-28" maxlength="2000"></textarea>
            </label>
          </div>

          <div class="panel-scroll-stack mt-5 flex flex-col gap-4">
            <div class="flex items-center justify-between gap-4">
              <h4 class="font-display text-lg font-semibold text-[var(--color-heading)]">Questions</h4>
              <button
                type="button"
                class="btn-secondary"
                :disabled="formDraft.questions.length >= 10"
                @click="addQuestion"
              >
                Add question
              </button>
            </div>

            <article
              v-for="(question, index) in formDraft.questions"
              :key="`draft-question-${index}`"
              class="surface-panel-muted p-4"
            >
              <div class="grid gap-4 lg:grid-cols-[minmax(0,1.6fr),minmax(0,0.8fr),auto]">
                <label class="space-y-2">
                  <span class="field-label">Prompt {{ index + 1 }}</span>
                  <input v-model="question.prompt" type="text" class="form-input" maxlength="300">
                </label>

                <label class="space-y-2">
                  <span class="field-label">Type</span>
                  <select v-model="question.questionType" class="form-input">
                    <option value="RATING">Rating 1-5</option>
                    <option value="TEXT">Text</option>
                  </select>
                </label>

                <button
                  type="button"
                  class="btn-secondary self-end"
                  :disabled="formDraft.questions.length <= 1"
                  @click="removeQuestion(index)"
                >
                  Remove
                </button>
              </div>

              <label class="mt-3 inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)]">
                <input v-model="question.required" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
                Required question
              </label>
            </article>
          </div>

          <div class="mt-5 flex flex-wrap items-center gap-3">
            <button
              type="button"
              class="btn-primary"
              :disabled="isSubmitting || !selectedExamId"
              @click="handleSave"
            >
              {{ isSubmitting ? 'Saving…' : editingFormId ? 'Update form' : 'Create form' }}
            </button>
            <button type="button" class="btn-secondary" :disabled="isSubmitting" @click="handleResetDraft">
              Cancel
            </button>
          </div>

          <p v-if="formError" class="alert-error mt-4">{{ formError }}</p>
          <p v-else-if="formSuccess" class="alert-success mt-4">{{ formSuccess }}</p>
        </section>

        <section class="page-section desktop-page-panel flex min-h-[30rem] flex-col xl:col-span-4 xl:row-span-4">
          <div class="panel-header">
            <h3 class="panel-title">Existing forms</h3>
            <p class="panel-copy">
              Review submissions and reopen a form in the editor to change metadata or publish status.
            </p>
          </div>

          <div v-if="isLoadingForms" class="empty-state">Loading forms…</div>
          <div v-else-if="!selectedExamId" class="empty-state">Select an exam to manage its forms.</div>
          <div v-else-if="!hasForms" class="empty-state">No feedback forms exist for this exam yet.</div>

          <div v-else class="panel-scroll-list">
            <article v-for="form in forms" :key="form.id" class="record-card">
              <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div class="space-y-3">
                  <div>
                    <p class="text-sm font-medium uppercase tracking-[0.2em] text-[var(--color-text-soft)]">
                      {{ form.spaceCode }} · {{ form.examTitle }}
                    </p>
                    <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                      {{ form.title }}
                    </h4>
                  </div>

                  <div class="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                    <span :class="form.published ? 'status-pill status-pill-success' : 'status-pill status-pill-neutral'">
                      {{ form.published ? 'Published' : 'Draft' }}
                    </span>
                    <span>{{ form.questions.length }} questions</span>
                    <span>{{ form.responseCount ?? 0 }} responses</span>
                    <span>Updated {{ formatDate(form.updatedAt) }}</span>
                  </div>

                  <p v-if="form.description" class="text-base leading-7 text-[var(--color-text)]">
                    {{ form.description }}
                  </p>
                </div>

                <div class="flex flex-wrap gap-3">
                  <button type="button" class="btn-secondary" @click="selectedFormId = form.id">
                    Review
                  </button>
                  <button type="button" class="btn-secondary" @click="populateDraft(form)">
                    Edit
                  </button>
                </div>
              </div>
            </article>
          </div>
        </section>

        <section class="page-section desktop-page-panel flex min-h-[34rem] flex-col xl:col-span-12">
          <div class="panel-header">
            <h3 class="panel-title">Response review</h3>
            <p class="panel-copy">
              Review simple rating aggregates and read submitted text feedback for the selected form.
            </p>
          </div>

          <div v-if="reviewError" class="alert-error">{{ reviewError }}</div>
          <div v-else-if="isLoadingDetail || isLoadingReview" class="empty-state">Loading response review…</div>
          <div v-else-if="!selectedForm || !review" class="empty-state">Select a form to review its responses.</div>
          <template v-else>
            <div class="stats-grid">
              <div class="stat-card bg-[var(--color-surface-offset)]">
                <dt class="meta-label">Responses</dt>
                <dd class="meta-value text-lg font-semibold">{{ review.responseCount }}</dd>
              </div>
              <div class="stat-card">
                <dt class="meta-label">Exam</dt>
                <dd class="meta-value">{{ review.spaceCode }} · {{ review.examTitle }}</dd>
              </div>
              <div class="stat-card">
                <dt class="meta-label">Form</dt>
                <dd class="meta-value">{{ review.title }}</dd>
              </div>
            </div>

            <div class="mt-5 grid min-h-0 flex-1 gap-6 xl:grid-cols-[0.9fr_1.1fr]">
              <div class="panel-scroll-stack">
                <article v-for="summary in review.questionSummaries" :key="summary.questionId" class="surface-panel-muted p-4">
                  <p class="meta-label">{{ summary.questionType === 'RATING' ? 'Rating question' : 'Text question' }}</p>
                  <h4 class="mt-2 font-display text-lg font-semibold text-[var(--color-heading)]">
                    {{ summary.prompt }}
                  </h4>
                  <div class="mt-3 flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                    <span>{{ summary.responseCount }} responses</span>
                    <span v-if="summary.averageRating !== null">Average {{ summary.averageRating.toFixed(2) }}/5</span>
                  </div>
                  <div v-if="summary.questionType === 'RATING'" class="mt-3 flex flex-wrap gap-2 text-sm text-[var(--color-text-soft)]">
                    <span
                      v-for="(count, rating) in summary.ratingCounts"
                      :key="`${summary.questionId}-${rating}`"
                      class="status-pill status-pill-neutral"
                    >
                      {{ rating }}★: {{ count }}
                    </span>
                  </div>
                </article>
              </div>

              <div class="panel-scroll-list">
                <article v-for="submission in review.submissions" :key="submission.id" class="record-card">
                  <div class="space-y-3">
                    <div>
                      <p class="text-sm font-medium uppercase tracking-[0.2em] text-[var(--color-text-soft)]">
                        {{ submission.studentFullName }} · {{ submission.studentEmail }}
                      </p>
                      <p class="mt-1 text-sm text-[var(--color-text-soft)]">
                        Submitted {{ formatDate(submission.submittedAt) }}
                      </p>
                    </div>

                    <div class="space-y-3">
                      <div v-for="question in selectedForm.questions" :key="`${submission.id}-${question.id}`" class="surface-panel-muted p-4">
                        <p class="meta-label">{{ question.questionType === 'RATING' ? 'Rating' : 'Text' }}</p>
                        <p class="mt-2 text-base font-medium text-[var(--color-heading)]">{{ question.prompt }}</p>
                        <p class="mt-2 text-base leading-7 text-[var(--color-text)]">
                          <template v-if="question.questionType === 'RATING'">
                            {{ findSubmissionAnswer(question, submission)?.ratingValue ?? 'No answer' }}/5
                          </template>
                          <template v-else>
                            {{ findSubmissionAnswer(question, submission)?.textValue ?? 'No answer provided.' }}
                          </template>
                        </p>
                      </div>
                    </div>
                  </div>
                </article>
              </div>
            </div>
          </template>
        </section>
      </template>

      <template v-else>
        <section class="page-section desktop-page-panel flex min-h-[30rem] flex-col xl:col-span-4 xl:row-span-4">
          <div class="panel-header">
            <h3 class="panel-title">Available forms</h3>
            <p class="panel-copy">
              You can submit one response for each published form in your accessible exam spaces.
            </p>
          </div>

          <div v-if="isLoadingForms" class="empty-state">Loading forms…</div>
          <div v-else-if="!selectedExamId" class="empty-state">Select an exam to view feedback forms.</div>
          <div v-else-if="!hasForms" class="empty-state">No published feedback forms are visible for this exam.</div>

          <div v-else class="panel-scroll-list">
            <article v-for="form in forms" :key="form.id" class="record-card">
              <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
                <div class="space-y-3">
                  <div>
                    <p class="text-sm font-medium uppercase tracking-[0.2em] text-[var(--color-text-soft)]">
                      {{ form.spaceCode }} · {{ form.examTitle }}
                    </p>
                    <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                      {{ form.title }}
                    </h4>
                  </div>

                  <div class="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                    <span :class="form.alreadySubmitted ? 'status-pill status-pill-success' : 'status-pill status-pill-warning'">
                      {{ form.alreadySubmitted ? 'Submitted' : 'Open' }}
                    </span>
                    <span>{{ form.questions.length }} questions</span>
                    <span>Updated {{ formatDate(form.updatedAt) }}</span>
                  </div>

                  <p v-if="form.description" class="text-base leading-7 text-[var(--color-text)]">
                    {{ form.description }}
                  </p>
                </div>

                <button type="button" class="btn-secondary" @click="selectedFormId = form.id">
                  {{ selectedFormId === form.id ? 'Selected' : 'Open form' }}
                </button>
              </div>
            </article>
          </div>
        </section>

        <section class="page-section desktop-page-panel flex min-h-[34rem] flex-col xl:col-span-8 xl:row-span-4">
          <div class="panel-header">
            <h3 class="panel-title">Submit feedback</h3>
            <p class="panel-copy">
              Choose one form and complete each prompt once. Submitted forms cannot be re-opened.
            </p>
          </div>

          <div v-if="submitError" class="alert-error">{{ submitError }}</div>
          <div v-else-if="submitSuccess" class="alert-success">{{ submitSuccess }}</div>
          <div v-else-if="isLoadingDetail" class="empty-state">Loading selected form…</div>
          <div v-else-if="!selectedForm" class="empty-state flex-1">Select a form to begin.</div>
          <div v-else-if="selectedForm.alreadySubmitted" class="empty-state flex-1">
            You have already submitted feedback for this form.
          </div>
          <template v-else>
            <div class="surface-panel-muted p-4">
              <p class="meta-label">Selected form</p>
              <h4 class="mt-2 font-display text-xl font-semibold text-[var(--color-heading)]">
                {{ selectedForm.title }}
              </h4>
              <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
                {{ selectedForm.description || 'No description provided.' }}
              </p>
            </div>

            <div class="panel-scroll-stack mt-5">
              <article v-for="question in selectedForm.questions" :key="question.id" class="surface-panel-muted p-4">
                <div class="flex flex-wrap items-center gap-3">
                  <p class="text-base font-medium text-[var(--color-heading)]">{{ question.prompt }}</p>
                  <span :class="question.required ? 'status-pill status-pill-warning' : 'status-pill status-pill-neutral'">
                    {{ question.required ? 'Required' : 'Optional' }}
                  </span>
                </div>

                <label v-if="question.questionType === 'RATING'" class="mt-4 space-y-2">
                  <span class="field-label">Rating</span>
                  <select v-model="studentAnswers[question.id]" class="form-input">
                    <option value="">Select a rating</option>
                    <option v-for="value in [1, 2, 3, 4, 5]" :key="value" :value="String(value)">
                      {{ value }} / 5
                    </option>
                  </select>
                </label>

                <label v-else class="mt-4 space-y-2">
                  <span class="field-label">Response</span>
                  <textarea v-model="studentAnswers[question.id]" class="form-input min-h-28" maxlength="2000"></textarea>
                </label>
              </article>
            </div>

            <button type="button" class="btn-primary mt-5 self-start" :disabled="isSubmitting" @click="handleStudentSubmit">
              {{ isSubmitting ? 'Submitting…' : 'Submit feedback' }}
            </button>
          </template>
        </section>
      </template>
    </template>
  </section>
</template>
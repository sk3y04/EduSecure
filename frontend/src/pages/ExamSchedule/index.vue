<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { examsService } from '@/services/exams'
import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import { useAuthStore } from '@/stores/auth'
import type { Exam, ExamPayload } from '@/types/exam'
import type { SpaceSummary } from '@/types/space'

const authStore = useAuthStore()

const exams = ref<Exam[]>([])
const manageableSpaces = ref<SpaceSummary[]>([])
const isLoading = ref(true)
const isLoadingSpaces = ref(false)
const isSubmitting = ref(false)
const editingExamId = ref<string | null>(null)
const loadError = ref<string | null>(null)
const formError = ref<string | null>(null)
const formSuccess = ref<string | null>(null)

const canManageExams = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const hasExams = computed(() => exams.value.length > 0)

const createForm = ref<ExamPayload>(emptyForm())
const editForm = ref<ExamPayload>(emptyForm())

function emptyForm(): ExamPayload {
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

function toLocalDateTimeInput(value: string) {
  const date = new Date(value)
  const offsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

function toApiPayload(payload: ExamPayload): ExamPayload {
  return {
    ...payload,
    title: payload.title.trim(),
    description: payload.description.trim(),
    location: payload.location.trim(),
    startsAt: new Date(payload.startsAt).toISOString(),
    endsAt: new Date(payload.endsAt).toISOString(),
  }
}

function applyUpsert(updatedExam: Exam) {
  const existingIndex = exams.value.findIndex((exam) => exam.id === updatedExam.id)

  if (existingIndex >= 0) {
    exams.value.splice(existingIndex, 1, updatedExam)
  } else {
    exams.value = [...exams.value, updatedExam].sort(
      (left, right) => new Date(left.startsAt).getTime() - new Date(right.startsAt).getTime(),
    )
  }
}

async function loadExams() {
  isLoading.value = true
  loadError.value = null

  try {
    exams.value = await examsService.list()
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function loadManageableSpaces() {
  if (!canManageExams.value) {
    return
  }

  isLoadingSpaces.value = true

  try {
    manageableSpaces.value = (await spacesService.list()).filter((space) => space.canManage && !space.archived)
    if (!createForm.value.spaceId && manageableSpaces.value.length > 0) {
      createForm.value.spaceId = manageableSpaces.value[0].id
    }
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoadingSpaces.value = false
  }
}

async function handleCreate() {
  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null

  try {
    const created = await examsService.create(toApiPayload(createForm.value))
    applyUpsert(created)
    createForm.value = {
      ...emptyForm(),
      spaceId: createForm.value.spaceId,
    }
    formSuccess.value = 'Exam schedule entry created.'
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function startEdit(exam: Exam) {
  editingExamId.value = exam.id
  formError.value = null
  formSuccess.value = null
  editForm.value = {
    spaceId: exam.spaceId,
    title: exam.title,
    description: exam.description ?? '',
    location: exam.location,
    startsAt: toLocalDateTimeInput(exam.startsAt),
    endsAt: toLocalDateTimeInput(exam.endsAt),
    published: exam.published,
  }
}

function cancelEdit() {
  editingExamId.value = null
  editForm.value = emptyForm()
}

async function handleUpdate() {
  if (!editingExamId.value) {
    return
  }

  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null

  try {
    const updated = await examsService.update(editingExamId.value, toApiPayload(editForm.value))
    applyUpsert(updated)
    cancelEdit()
    formSuccess.value = 'Exam schedule entry updated.'
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString()
}

onMounted(async () => {
  await Promise.all([loadExams(), loadManageableSpaces()])
})
</script>

<template>
  <section class="space-y-6">
    <div class="page-hero">
      <div class="max-w-3xl">
        <p class="section-kicker">Exam scheduling</p>
        <h2 class="section-title">Assessment timetable</h2>
        <p class="section-copy">
          View the current exam timetable for your visible spaces. Staff can create draft or published
          exam entries, while students only see published records for enrolled spaces.
        </p>
      </div>
    </div>

    <section v-if="canManageExams" class="page-section space-y-4">
      <div>
        <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Create exam entry</h3>
        <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
          Exam entries are space-scoped and checked for same-space overlap conflicts.
        </p>
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <label class="space-y-2">
          <span class="field-label">Space</span>
          <select v-model="createForm.spaceId" class="form-input" :disabled="isLoadingSpaces || isSubmitting">
            <option value="" disabled>Select a space</option>
            <option v-for="space in manageableSpaces" :key="space.id" :value="space.id">
              {{ space.code }} · {{ space.name }}
            </option>
          </select>
        </label>

        <label class="space-y-2">
          <span class="field-label">Location</span>
          <input v-model="createForm.location" type="text" class="form-input" maxlength="160">
        </label>

        <label class="space-y-2 lg:col-span-2">
          <span class="field-label">Title</span>
          <input v-model="createForm.title" type="text" class="form-input" maxlength="160">
        </label>

        <label class="space-y-2 lg:col-span-2">
          <span class="field-label">Description</span>
          <textarea v-model="createForm.description" class="form-input min-h-28" maxlength="2000"></textarea>
        </label>

        <label class="space-y-2">
          <span class="field-label">Starts at</span>
          <input v-model="createForm.startsAt" type="datetime-local" class="form-input">
        </label>

        <label class="space-y-2">
          <span class="field-label">Ends at</span>
          <input v-model="createForm.endsAt" type="datetime-local" class="form-input">
        </label>
      </div>

      <label class="inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)]">
        <input v-model="createForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
        Publish immediately
      </label>

      <div class="flex flex-wrap items-center gap-3">
        <button type="button" class="btn-primary" :disabled="isSubmitting || !manageableSpaces.length" @click="handleCreate">
          {{ isSubmitting ? 'Saving…' : 'Create exam entry' }}
        </button>
        <button type="button" class="btn-secondary" @click="loadExams">Refresh timetable</button>
      </div>

      <p v-if="formError" class="alert-error">{{ formError }}</p>
      <p v-else-if="formSuccess" class="alert-success">{{ formSuccess }}</p>
    </section>

    <section class="page-section space-y-4">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Visible exams</h3>
          <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
            Entries are ordered by start time. Draft entries are visible only to staff who can manage them.
          </p>
        </div>
        <button type="button" class="btn-secondary" @click="loadExams">Refresh</button>
      </div>

      <div v-if="loadError" class="alert-error">{{ loadError }}</div>
      <div v-else-if="isLoading" class="empty-state">Loading exam timetable…</div>
      <div v-else-if="!hasExams" class="empty-state">No exam schedule entries are visible yet.</div>

      <div v-else class="space-y-4">
        <article v-for="exam in exams" :key="exam.id" class="surface-panel px-5 py-5">
          <template v-if="editingExamId === exam.id">
            <div class="grid gap-4 lg:grid-cols-2">
              <label class="space-y-2">
                <span class="field-label">Space</span>
                <select v-model="editForm.spaceId" class="form-input">
                  <option v-for="space in manageableSpaces" :key="space.id" :value="space.id">
                    {{ space.code }} · {{ space.name }}
                  </option>
                </select>
              </label>

              <label class="space-y-2">
                <span class="field-label">Location</span>
                <input v-model="editForm.location" type="text" class="form-input" maxlength="160">
              </label>

              <label class="space-y-2 lg:col-span-2">
                <span class="field-label">Title</span>
                <input v-model="editForm.title" type="text" class="form-input" maxlength="160">
              </label>

              <label class="space-y-2 lg:col-span-2">
                <span class="field-label">Description</span>
                <textarea v-model="editForm.description" class="form-input min-h-28" maxlength="2000"></textarea>
              </label>

              <label class="space-y-2">
                <span class="field-label">Starts at</span>
                <input v-model="editForm.startsAt" type="datetime-local" class="form-input">
              </label>

              <label class="space-y-2">
                <span class="field-label">Ends at</span>
                <input v-model="editForm.endsAt" type="datetime-local" class="form-input">
              </label>
            </div>

            <label class="mt-4 inline-flex items-center gap-3 text-sm font-medium text-[var(--color-text)]">
              <input v-model="editForm.published" type="checkbox" class="h-4 w-4 rounded border-[var(--color-border)]">
              Published
            </label>

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
                  <span>{{ formatDateTime(exam.startsAt) }} to {{ formatDateTime(exam.endsAt) }}</span>
                </div>

                <p v-if="exam.description" class="text-base leading-7 text-[var(--color-text)]">
                  {{ exam.description }}
                </p>
              </div>

              <button
                v-if="exam.canManage"
                type="button"
                class="btn-secondary self-start"
                @click="startEdit(exam)"
              >
                Edit
              </button>
            </div>
          </template>
        </article>
      </div>
    </section>
  </section>
</template>
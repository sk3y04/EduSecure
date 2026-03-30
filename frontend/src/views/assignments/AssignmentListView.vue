<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { assignmentsService } from '@/services/assignments'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { AssignmentSummary } from '@/types/assignment'

const authStore = useAuthStore()

const assignments = ref<AssignmentSummary[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const createSuccess = ref<string | null>(null)
const isCreating = ref(false)

const createForm = reactive({
  title: '',
  description: '',
  dueAt: '',
})

const canCreateAssignments = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const isStudent = computed(() => authStore.hasAnyRole(['STUDENT']))
const sortedAssignments = computed(() =>
  [...assignments.value].sort((left, right) => new Date(left.dueAt).getTime() - new Date(right.dueAt).getTime()),
)

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

async function loadAssignments() {
  isLoading.value = true
  loadError.value = null

  try {
    assignments.value = await assignmentsService.list()
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function handleCreateAssignment() {
  isCreating.value = true
  createError.value = null
  createSuccess.value = null

  try {
    const createdAssignment = await assignmentsService.create({
      title: createForm.title,
      description: createForm.description,
      dueAt: new Date(createForm.dueAt).toISOString(),
    })

    assignments.value = [
      {
        id: createdAssignment.id,
        title: createdAssignment.title,
        dueAt: createdAssignment.dueAt,
        open: createdAssignment.open,
      },
      ...assignments.value,
    ]

    createForm.title = ''
    createForm.description = ''
    createForm.dueAt = ''
    createSuccess.value = 'Assignment created successfully.'
  } catch (error) {
    createError.value = extractErrorMessage(error)
  } finally {
    isCreating.value = false
  }
}

onMounted(() => {
  void loadAssignments()
})
</script>

<template>
  <div class="space-y-6">
    <section class="surface-panel p-8">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker tracking-[0.3em]">Assignments</p>
          <h2 class="section-title">Role-aware coursework workspace</h2>
          <p class="section-copy">
            Students and privileged staff use the same route, but the backend still enforces who can
            create assignments and who can submit work.
          </p>
        </div>

        <div class="surface-panel-muted max-w-sm px-5 py-4 text-sm text-slate-600">
          <p class="font-semibold text-slate-900">Policy alignment</p>
          <p class="mt-2 leading-6">
            Assignment creation remains limited to <span class="font-semibold text-slate-900">LECTURER</span>
            and <span class="font-semibold text-slate-900">ADMIN</span> roles.
          </p>
        </div>
      </div>
    </section>

    <section v-if="canCreateAssignments" class="surface-panel p-8">
      <div class="mb-6 border-b border-slate-200 pb-5">
        <h3 class="text-xl font-semibold text-slate-900">Create assignment</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          A compact form for creating coursework entries without distracting styling.
        </p>
      </div>

      <div v-if="createError" class="alert-error mb-4">
        {{ createError }}
      </div>
      <div v-if="createSuccess" class="alert-success mb-4">
        {{ createSuccess }}
      </div>

      <form class="grid gap-5 lg:grid-cols-2" @submit.prevent="handleCreateAssignment">
        <label class="block lg:col-span-1">
          <span class="field-label">Title</span>
          <input
            v-model="createForm.title"
            type="text"
            required
            class="form-input"
            placeholder="Cryptography Coursework 1"
          />
        </label>

        <label class="block lg:col-span-1">
          <span class="field-label">Due date</span>
          <input v-model="createForm.dueAt" type="datetime-local" required class="form-input" />
        </label>

        <label class="block lg:col-span-2">
          <span class="field-label">Description</span>
          <textarea
            v-model="createForm.description"
            required
            rows="4"
            class="form-input"
            placeholder="Submit your signed artefact or simulated content."
          />
        </label>

        <div class="lg:col-span-2">
          <button type="submit" class="btn-primary" :disabled="isCreating">
            {{ isCreating ? 'Creating…' : 'Create assignment' }}
          </button>
        </div>
      </form>
    </section>

    <section class="surface-panel p-8">
      <div class="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 class="text-xl font-semibold text-slate-900">Visible assignments</h3>
          <p class="mt-2 text-sm leading-6 text-slate-600">
            Students can open the secure submission form directly from an active assignment.
          </p>
        </div>
        <button type="button" class="btn-secondary self-start sm:self-auto" @click="loadAssignments">
          Refresh
        </button>
      </div>

      <div v-if="loadError" class="alert-error mb-4">
        {{ loadError }}
      </div>

      <div v-if="isLoading" class="empty-state">
        Loading assignments…
      </div>

      <div v-else-if="sortedAssignments.length === 0" class="empty-state">
        No assignments are available yet.
      </div>

      <div v-else class="space-y-4">
        <article
          v-for="assignment in sortedAssignments"
          :key="assignment.id"
          class="rounded-sm border border-slate-300 bg-white p-5"
        >
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div>
              <h4 class="text-lg font-semibold text-slate-900">{{ assignment.title }}</h4>
              <p class="mt-2 text-sm text-slate-600">Due {{ formatDate(assignment.dueAt) }}</p>
            </div>
            <span
              class="status-pill"
              :class="assignment.open ? 'border-emerald-300 bg-emerald-50 text-emerald-800' : 'border-amber-300 bg-amber-50 text-amber-800'"
            >
              {{ assignment.open ? 'Open' : 'Closed' }}
            </span>
          </div>

          <div class="mt-5 flex flex-wrap items-center gap-3 border-t border-slate-200 pt-4">
            <RouterLink
              v-if="isStudent"
              :to="{ name: 'submission-create', params: { assignmentId: assignment.id } }"
              class="btn-primary"
            >
              Submit work
            </RouterLink>
            <span v-else class="text-sm text-slate-500">
              Submission action is reserved for authenticated students.
            </span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>


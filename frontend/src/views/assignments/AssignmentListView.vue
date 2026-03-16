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
    dateStyle: 'medium',
    timeStyle: 'short',
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
  <div class="space-y-8">
    <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">Assignments</p>
          <h2 class="mt-3 text-3xl font-semibold text-white">One workspace for role-aware coursework flow</h2>
          <p class="mt-3 text-sm leading-7 text-slate-400">
            The same page serves students and lecturer/admin users. Creation is visible only to
            privileged roles, while students get direct access to the secure submission action.
          </p>
        </div>

        <div class="rounded-2xl border border-slate-800 bg-slate-950/70 px-5 py-4 text-sm text-slate-300">
          <p class="font-semibold text-white">Security evidence</p>
          <p class="mt-2 leading-6 text-slate-400">
            The UI mirrors backend enforcement: only <span class="font-semibold text-slate-200">LECTURER</span>
            and <span class="font-semibold text-slate-200">ADMIN</span> can create assignments.
          </p>
        </div>
      </div>
    </section>

    <section
      v-if="canCreateAssignments"
      class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel"
    >
      <div class="mb-6">
        <h3 class="text-xl font-semibold text-white">Create assignment</h3>
        <p class="mt-2 text-sm leading-6 text-slate-400">
          Keep this form intentionally small. It exists to demonstrate role checks and the secure
          submission entry point.
        </p>
      </div>

      <div
        v-if="createError"
        class="mb-4 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
      >
        {{ createError }}
      </div>
      <div
        v-if="createSuccess"
        class="mb-4 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100"
      >
        {{ createSuccess }}
      </div>

      <form class="grid gap-5 lg:grid-cols-2" @submit.prevent="handleCreateAssignment">
        <label class="block lg:col-span-1">
          <span class="mb-2 block text-sm font-medium text-slate-200">Title</span>
          <input
            v-model="createForm.title"
            type="text"
            required
            class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
            placeholder="Cryptography Coursework 1"
          />
        </label>

        <label class="block lg:col-span-1">
          <span class="mb-2 block text-sm font-medium text-slate-200">Due date</span>
          <input
            v-model="createForm.dueAt"
            type="datetime-local"
            required
            class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
          />
        </label>

        <label class="block lg:col-span-2">
          <span class="mb-2 block text-sm font-medium text-slate-200">Description</span>
          <textarea
            v-model="createForm.description"
            required
            rows="4"
            class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
            placeholder="Submit your signed artefact or simulated content."
          />
        </label>

        <div class="lg:col-span-2">
          <button
            type="submit"
            class="inline-flex items-center rounded-2xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isCreating"
          >
            {{ isCreating ? 'Creating…' : 'Create assignment' }}
          </button>
        </div>
      </form>
    </section>

    <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
      <div class="mb-6 flex items-center justify-between gap-4">
        <div>
          <h3 class="text-xl font-semibold text-white">Visible assignments</h3>
          <p class="mt-2 text-sm leading-6 text-slate-400">
            Students can launch the secure submission form directly from the relevant assignment.
          </p>
        </div>
        <button
          type="button"
          class="inline-flex items-center rounded-2xl border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-brand-500 hover:text-white"
          @click="loadAssignments"
        >
          Refresh
        </button>
      </div>

      <div
        v-if="loadError"
        class="mb-4 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
      >
        {{ loadError }}
      </div>

      <div v-if="isLoading" class="rounded-2xl border border-dashed border-slate-700 p-8 text-center text-sm text-slate-400">
        Loading assignments…
      </div>

      <div
        v-else-if="sortedAssignments.length === 0"
        class="rounded-2xl border border-dashed border-slate-700 p-8 text-center text-sm text-slate-400"
      >
        No assignments are available yet.
      </div>

      <div v-else class="grid gap-4 xl:grid-cols-2">
        <article
          v-for="assignment in sortedAssignments"
          :key="assignment.id"
          class="rounded-3xl border border-slate-800 bg-slate-950/70 p-6"
        >
          <div class="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h4 class="text-lg font-semibold text-white">{{ assignment.title }}</h4>
              <p class="mt-2 text-sm text-slate-400">Due {{ formatDate(assignment.dueAt) }}</p>
            </div>
            <span
              class="rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide"
              :class="assignment.open ? 'bg-emerald-500/15 text-emerald-100 ring-1 ring-inset ring-emerald-500/30' : 'bg-amber-500/15 text-amber-100 ring-1 ring-inset ring-amber-500/30'"
            >
              {{ assignment.open ? 'Open' : 'Closed' }}
            </span>
          </div>

          <div class="mt-6 flex flex-wrap items-center gap-3">
            <RouterLink
              v-if="isStudent"
              :to="{ name: 'submission-create', params: { assignmentId: assignment.id } }"
              class="inline-flex items-center rounded-2xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-brand-500"
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


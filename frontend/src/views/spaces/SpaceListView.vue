<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import { useAuthStore } from '@/stores/auth'
import type { SpaceSummary } from '@/types/space'

const authStore = useAuthStore()

const spaces = ref<SpaceSummary[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const createSuccess = ref<string | null>(null)
const isCreating = ref(false)

const createForm = reactive({
  name: '',
  code: '',
  description: '',
})

const canManageSpaces = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))

function statusClass(archived: boolean): string {
  return archived
    ? 'border-amber-300 bg-amber-50 text-amber-800'
    : 'border-emerald-300 bg-emerald-50 text-emerald-800'
}

async function loadSpaces() {
  isLoading.value = true
  loadError.value = null

  try {
    spaces.value = await spacesService.list()
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function handleCreateSpace() {
  isCreating.value = true
  createError.value = null
  createSuccess.value = null

  try {
    const createdSpace = await spacesService.create({
      name: createForm.name,
      code: createForm.code,
      description: createForm.description,
    })

    spaces.value = [
      {
        id: createdSpace.id,
        name: createdSpace.name,
        code: createdSpace.code,
        description: createdSpace.description,
        archived: createdSpace.archived,
        memberCount: createdSpace.memberCount,
        canManage: createdSpace.canManage,
        isMember: createdSpace.isMember,
      },
      ...spaces.value,
    ]

    createForm.name = ''
    createForm.code = ''
    createForm.description = ''
    createSuccess.value = 'Space created successfully.'
  } catch (error) {
    createError.value = extractErrorMessage(error)
  } finally {
    isCreating.value = false
  }
}

onMounted(() => {
  void loadSpaces()
})
</script>

<template>
  <div class="space-y-6">
    <section class="surface-panel p-8">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker tracking-[0.3em]">Spaces</p>
          <h2 class="section-title">Academic collaboration areas</h2>
          <p class="section-copy">
            Spaces group students into managed academic areas for resources, activities, and future
            coursework coordination. The backend still enforces ownership, roster access, and
            student-only membership rules.
          </p>
        </div>

        <div class="surface-panel-muted max-w-sm px-5 py-4 text-sm text-slate-600">
          <p class="font-semibold text-slate-900">Access posture</p>
          <p class="mt-2 leading-6">
            Lecturers manage the spaces they create, admins manage every space, and students see
            only the spaces they are assigned to.
          </p>
        </div>
      </div>
    </section>

    <section v-if="canManageSpaces" class="surface-panel p-8">
      <div class="mb-6 border-b border-slate-200 pb-5">
        <h3 class="text-xl font-semibold text-slate-900">Create space</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          Create a collaboration area with a stable code that can later anchor roster management and
          related academic workflows.
        </p>
      </div>

      <div v-if="createError" class="alert-error mb-4">
        {{ createError }}
      </div>
      <div v-if="createSuccess" class="alert-success mb-4">
        {{ createSuccess }}
      </div>

      <form class="grid gap-5 lg:grid-cols-2" @submit.prevent="handleCreateSpace">
        <label class="block lg:col-span-1">
          <span class="field-label">Name</span>
          <input v-model="createForm.name" type="text" required class="form-input" placeholder="Applied Cryptography Group A" />
        </label>

        <label class="block lg:col-span-1">
          <span class="field-label">Code</span>
          <input v-model="createForm.code" type="text" required class="form-input" placeholder="CRYPTO-A" />
        </label>

        <label class="block lg:col-span-2">
          <span class="field-label">Description</span>
          <textarea
            v-model="createForm.description"
            required
            rows="4"
            class="form-input"
            placeholder="Shared space for lectures, secure resources, and student coordination."
          />
        </label>

        <div class="lg:col-span-2">
          <button type="submit" class="btn-primary" :disabled="isCreating">
            {{ isCreating ? 'Creating…' : 'Create space' }}
          </button>
        </div>
      </form>
    </section>

    <section class="surface-panel p-8">
      <div class="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h3 class="text-xl font-semibold text-slate-900">Visible spaces</h3>
          <p class="mt-2 text-sm leading-6 text-slate-600">
            The list is filtered server-side to match the current user’s role and ownership rules.
          </p>
        </div>
        <button type="button" class="btn-secondary self-start sm:self-auto" @click="loadSpaces">
          Refresh
        </button>
      </div>

      <div v-if="loadError" class="alert-error mb-4">
        {{ loadError }}
      </div>

      <div v-if="isLoading" class="empty-state">
        Loading spaces…
      </div>

      <div v-else-if="spaces.length === 0" class="empty-state">
        {{ canManageSpaces ? 'No spaces exist yet.' : 'You have not been assigned to any spaces yet.' }}
      </div>

      <div v-else class="space-y-4">
        <article
          v-for="space in spaces"
          :key="space.id"
          class="rounded-sm border border-slate-300 bg-white p-5"
        >
          <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div class="max-w-3xl">
              <div class="flex flex-wrap items-center gap-3">
                <h4 class="text-lg font-semibold text-slate-900">{{ space.name }}</h4>
                <span class="rounded-sm border border-slate-300 bg-slate-50 px-2 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700">
                  {{ space.code }}
                </span>
              </div>
              <p class="mt-3 text-sm leading-6 text-slate-600">{{ space.description }}</p>
            </div>

            <div class="flex flex-col items-start gap-3 lg:items-end">
              <span class="status-pill" :class="statusClass(space.archived)">
                {{ space.archived ? 'Archived' : 'Active' }}
              </span>
              <p class="text-sm text-slate-500">{{ space.memberCount }} student{{ space.memberCount === 1 ? '' : 's' }}</p>
            </div>
          </div>

          <div class="mt-5 flex flex-wrap items-center gap-3 border-t border-slate-200 pt-4">
            <RouterLink :to="{ name: 'space-detail', params: { spaceId: space.id } }" class="btn-primary">
              {{ space.canManage ? 'Manage space' : 'Open space' }}
            </RouterLink>
            <span v-if="space.isMember && !space.canManage" class="text-sm text-slate-500">
              You are enrolled in this space.
            </span>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>
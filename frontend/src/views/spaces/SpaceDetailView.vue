<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import type { SpaceDetail, SpaceStudent } from '@/types/space'

const route = useRoute()

const space = ref<SpaceDetail | null>(null)
const isLoading = ref(true)
const loadError = ref<string | null>(null)

const updateForm = reactive({
  name: '',
  code: '',
  description: '',
  archived: false,
})

const membershipForm = reactive({
  studentEmail: '',
})

const updateError = ref<string | null>(null)
const updateSuccess = ref<string | null>(null)
const membershipError = ref<string | null>(null)
const membershipSuccess = ref<string | null>(null)
const isUpdating = ref(false)
const isAddingStudent = ref(false)
const removingStudentId = ref<string | null>(null)

const spaceId = computed(() => String(route.params.spaceId ?? ''))

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function syncFormState(currentSpace: SpaceDetail) {
  updateForm.name = currentSpace.name
  updateForm.code = currentSpace.code
  updateForm.description = currentSpace.description
  updateForm.archived = currentSpace.archived
}

function upsertMembership(membership: SpaceStudent) {
  if (!space.value) {
    return
  }

  space.value = {
    ...space.value,
    memberCount: space.value.memberCount + 1,
    memberships: [...space.value.memberships, membership],
  }
}

async function loadSpace() {
  isLoading.value = true
  loadError.value = null

  try {
    const response = await spacesService.getById(spaceId.value)
    space.value = response
    syncFormState(response)
  } catch (error) {
    loadError.value = extractErrorMessage(error)
    space.value = null
  } finally {
    isLoading.value = false
  }
}

async function handleUpdateSpace() {
  isUpdating.value = true
  updateError.value = null
  updateSuccess.value = null

  try {
    const updatedSpace = await spacesService.update(spaceId.value, {
      name: updateForm.name,
      code: updateForm.code,
      description: updateForm.description,
      archived: updateForm.archived,
    })

    space.value = updatedSpace
    syncFormState(updatedSpace)
    updateSuccess.value = 'Space updated successfully.'
  } catch (error) {
    updateError.value = extractErrorMessage(error)
  } finally {
    isUpdating.value = false
  }
}

async function handleAddStudent() {
  isAddingStudent.value = true
  membershipError.value = null
  membershipSuccess.value = null

  try {
    const membership = await spacesService.addStudent(spaceId.value, {
      studentEmail: membershipForm.studentEmail,
    })

    upsertMembership(membership)
    membershipForm.studentEmail = ''
    membershipSuccess.value = 'Student added to the space.'
  } catch (error) {
    membershipError.value = extractErrorMessage(error)
  } finally {
    isAddingStudent.value = false
  }
}

async function handleRemoveStudent(studentUserId: string) {
  removingStudentId.value = studentUserId
  membershipError.value = null
  membershipSuccess.value = null

  try {
    await spacesService.removeStudent(spaceId.value, studentUserId)

    if (space.value) {
      space.value = {
        ...space.value,
        memberCount: Math.max(0, space.value.memberCount - 1),
        memberships: space.value.memberships.filter((membership) => membership.studentUserId !== studentUserId),
      }
    }

    membershipSuccess.value = 'Student removed from the space.'
  } catch (error) {
    membershipError.value = extractErrorMessage(error)
  } finally {
    removingStudentId.value = null
  }
}

watch(
  () => route.params.spaceId,
  () => {
    void loadSpace()
  },
  { immediate: true },
)
</script>

<template>
  <section class="space-y-6">
    <div class="surface-panel p-8">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker tracking-[0.3em]">Space detail</p>
          <h2 class="section-title">View metadata and manage membership</h2>
          <p class="section-copy">
            Staff can update metadata and maintain the student roster here. Student viewers remain
            read-only and do not receive roster visibility.
          </p>
        </div>

        <button type="button" class="btn-secondary self-start" @click="loadSpace">
          Refresh
        </button>
      </div>
    </div>

    <div v-if="loadError" class="alert-error">
      {{ loadError }}
    </div>

    <div v-else-if="isLoading" class="empty-state">
      Loading space details…
    </div>

    <template v-else-if="space">
      <section class="surface-panel p-8">
        <div class="flex flex-col gap-5 border-b border-slate-200 pb-5 lg:flex-row lg:items-start lg:justify-between">
          <div class="max-w-3xl">
            <div class="flex flex-wrap items-center gap-3">
              <h3 class="text-2xl font-semibold text-slate-900">{{ space.name }}</h3>
              <span class="rounded-sm border border-slate-300 bg-slate-50 px-2 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700">
                {{ space.code }}
              </span>
            </div>
            <p class="mt-3 text-sm leading-6 text-slate-600">{{ space.description }}</p>
          </div>

          <span
            class="status-pill"
            :class="space.archived ? 'border-amber-300 bg-amber-50 text-amber-800' : 'border-emerald-300 bg-emerald-50 text-emerald-800'"
          >
            {{ space.archived ? 'Archived' : 'Active' }}
          </span>
        </div>

        <dl class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          <div class="data-card">
            <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Students</dt>
            <dd class="mt-2 text-sm font-semibold text-slate-900">{{ space.memberCount }}</dd>
          </div>
          <div class="data-card">
            <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Created</dt>
            <dd class="mt-2 text-sm text-slate-900">{{ formatDate(space.createdAt) }}</dd>
          </div>
          <div class="data-card">
            <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Updated</dt>
            <dd class="mt-2 text-sm text-slate-900">{{ formatDate(space.updatedAt) }}</dd>
          </div>
          <div class="data-card">
            <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Access</dt>
            <dd class="mt-2 text-sm text-slate-900">
              {{ space.canManage ? 'Management access' : space.isMember ? 'Enrolled member' : 'Read only' }}
            </dd>
          </div>
        </dl>
      </section>

      <section v-if="space.canManage" class="grid gap-6 xl:grid-cols-[1fr_0.95fr]">
        <div class="surface-panel p-8">
          <div class="mb-6 border-b border-slate-200 pb-5">
            <h3 class="text-xl font-semibold text-slate-900">Update space</h3>
            <p class="mt-2 text-sm leading-6 text-slate-600">
              Ownership stays enforced on the backend. This form only appears when the API has
              already confirmed management access.
            </p>
          </div>

          <div v-if="updateError" class="alert-error mb-4">
            {{ updateError }}
          </div>
          <div v-if="updateSuccess" class="alert-success mb-4">
            {{ updateSuccess }}
          </div>

          <form class="grid gap-5" @submit.prevent="handleUpdateSpace">
            <label class="block">
              <span class="field-label">Name</span>
              <input v-model="updateForm.name" type="text" required class="form-input" />
            </label>

            <label class="block">
              <span class="field-label">Code</span>
              <input v-model="updateForm.code" type="text" required class="form-input" />
            </label>

            <label class="block">
              <span class="field-label">Description</span>
              <textarea v-model="updateForm.description" rows="5" required class="form-input" />
            </label>

            <label class="flex items-center gap-3 rounded-sm border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-700">
              <input v-model="updateForm.archived" type="checkbox" class="h-4 w-4" />
              Archive this space
            </label>

            <div>
              <button type="submit" class="btn-primary" :disabled="isUpdating">
                {{ isUpdating ? 'Saving…' : 'Save changes' }}
              </button>
            </div>
          </form>
        </div>

        <div class="space-y-6">
          <section class="surface-panel p-8">
            <div class="mb-6 border-b border-slate-200 pb-5">
              <h3 class="text-xl font-semibold text-slate-900">Add student</h3>
              <p class="mt-2 text-sm leading-6 text-slate-600">
                Membership is assigned by student email. The backend validates existence, role, and
                duplicate enrollment.
              </p>
            </div>

            <div v-if="membershipError" class="alert-error mb-4">
              {{ membershipError }}
            </div>
            <div v-if="membershipSuccess" class="alert-success mb-4">
              {{ membershipSuccess }}
            </div>

            <form class="space-y-4" @submit.prevent="handleAddStudent">
              <label class="block">
                <span class="field-label">Student email</span>
                <input
                  v-model="membershipForm.studentEmail"
                  type="email"
                  required
                  class="form-input"
                  placeholder="student@example.com"
                />
              </label>

              <div>
                <button type="submit" class="btn-primary" :disabled="isAddingStudent">
                  {{ isAddingStudent ? 'Adding…' : 'Add student' }}
                </button>
              </div>
            </form>
          </section>

          <section class="surface-panel p-8">
            <div class="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h3 class="text-xl font-semibold text-slate-900">Student roster</h3>
                <p class="mt-2 text-sm leading-6 text-slate-600">
                  Students are listed only for users with management permission on this space.
                </p>
              </div>
            </div>

            <div v-if="space.memberships.length === 0" class="empty-state">
              No students are assigned to this space yet.
            </div>

            <div v-else class="space-y-4">
              <article
                v-for="membership in space.memberships"
                :key="membership.studentUserId"
                class="rounded-sm border border-slate-300 bg-slate-50 p-4"
              >
                <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
                  <div>
                    <p class="text-sm font-semibold text-slate-900">{{ membership.studentFullName }}</p>
                    <p class="mt-1 text-sm text-slate-600">{{ membership.studentEmail }}</p>
                    <p class="mt-2 text-xs uppercase tracking-[0.25em] text-slate-500">
                      Added {{ formatDate(membership.addedAt) }}
                    </p>
                  </div>

                  <button
                    type="button"
                    class="btn-danger self-start sm:self-auto"
                    :disabled="removingStudentId === membership.studentUserId"
                    @click="handleRemoveStudent(membership.studentUserId)"
                  >
                    {{ removingStudentId === membership.studentUserId ? 'Removing…' : 'Remove' }}
                  </button>
                </div>
              </article>
            </div>
          </section>
        </div>
      </section>

      <section v-else class="surface-panel p-8">
        <h3 class="text-xl font-semibold text-slate-900">Read-only access</h3>
        <p class="mt-3 text-sm leading-6 text-slate-600">
          You can view this space because you are enrolled in it. Roster management and metadata
          changes remain restricted to lecturers who own the space and administrators.
        </p>
      </section>
    </template>
  </section>
</template>
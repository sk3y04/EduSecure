<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import type { SpaceDetail, SpaceStudent } from '@/types/space'
import {
  SpaceMembershipForm,
  SpaceMetaPanel,
  SpaceRosterPanel,
  SpaceUpdateForm,
} from './components'

const route = useRoute()

const space = ref<SpaceDetail | null>(null)
const isLoading = ref(true)
const loadError = ref<string | null>(null)

const updateError = ref<string | null>(null)
const updateSuccess = ref<string | null>(null)
const membershipError = ref<string | null>(null)
const membershipSuccess = ref<string | null>(null)
const isUpdating = ref(false)
const isAddingStudent = ref(false)
const removingStudentId = ref<string | null>(null)

const spaceId = computed(() => String(route.params.spaceId ?? ''))

function upsertMembership(membership: SpaceStudent) {
  if (!space.value) return
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
    space.value = await spacesService.getById(spaceId.value)
  } catch (error) {
    loadError.value = extractErrorMessage(error)
    space.value = null
  } finally {
    isLoading.value = false
  }
}

async function handleUpdate(payload: {
  name: string
  code: string
  description: string
  archived: boolean
}) {
  isUpdating.value = true
  updateError.value = null
  updateSuccess.value = null

  try {
    space.value = await spacesService.update(spaceId.value, payload)
    updateSuccess.value = 'Space updated successfully.'
  } catch (error) {
    updateError.value = extractErrorMessage(error)
  } finally {
    isUpdating.value = false
  }
}

async function handleAddStudent(email: string) {
  isAddingStudent.value = true
  membershipError.value = null
  membershipSuccess.value = null

  try {
    const membership = await spacesService.addStudent(spaceId.value, { studentEmail: email })
    upsertMembership(membership)
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
        memberships: space.value.memberships.filter((m) => m.studentUserId !== studentUserId),
      }
    }

    membershipSuccess.value = 'Student removed from the space.'
  } catch (error) {
    membershipError.value = extractErrorMessage(error)
  } finally {
    removingStudentId.value = null
  }
}

watch(() => route.params.spaceId, () => { void loadSpace() }, { immediate: true })
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
        <button type="button" class="btn-secondary self-start" @click="loadSpace">Refresh</button>
      </div>
    </div>

    <div v-if="loadError" class="alert-error">{{ loadError }}</div>
    <div v-else-if="isLoading" class="empty-state">Loading space details…</div>

    <template v-else-if="space">
      <SpaceMetaPanel :space="space" />

      <section v-if="space.canManage" class="grid gap-6 xl:grid-cols-[1fr_0.95fr]">
        <SpaceUpdateForm
          :initial-name="space.name"
          :initial-code="space.code"
          :initial-description="space.description"
          :initial-archived="space.archived"
          :update-error="updateError"
          :update-success="updateSuccess"
          :is-updating="isUpdating"
          @submit="handleUpdate"
        />

        <div class="space-y-6">
          <SpaceMembershipForm
            :membership-error="membershipError"
            :membership-success="membershipSuccess"
            :is-adding-student="isAddingStudent"
            @add-student="handleAddStudent"
          />
          <SpaceRosterPanel
            :memberships="space.memberships"
            :removing-student-id="removingStudentId"
            @remove-student="handleRemoveStudent"
          />
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



<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import AssignmentWorkspace from '@/pages/AssignmentList/components/AssignmentWorkspace.vue'
import { spacesService } from '@/services/spaces'
import type { SpaceDetail, SpaceStudent } from '@/types/space'
import {
  SpaceChatPanel,
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
  <section class="desktop-page-grid">
    <div class="page-hero desktop-page-panel hero-shell xl:col-span-12">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-4xl">
          <p class="section-kicker">Space detail</p>
          <h2 class="section-title">View metadata and manage membership</h2>
          <p class="section-copy max-w-none">
            Metadata, chat, assignments, and roster management now live in a single desktop workspace.
            Wide content panels handle day-to-day work, while narrower utility panels keep staff actions
            grouped and easy to scan.
          </p>
        </div>

        <div class="flex flex-col gap-3 lg:items-end">
          <button type="button" class="btn-secondary self-start lg:self-auto" @click="loadSpace">Refresh</button>
          <div class="surface-panel-muted px-5 py-4">
            <p class="meta-label">View mode</p>
            <p class="meta-value mt-1">Desktop detail canvas</p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loadError" class="alert-error xl:col-span-12">{{ loadError }}</div>
    <div v-else-if="isLoading" class="empty-state xl:col-span-12">Loading space details…</div>

    <template v-else-if="space">
      <SpaceMetaPanel class="xl:col-span-8 xl:row-span-2" :space="space" />

      <SpaceUpdateForm
        v-if="space.canManage"
        class="xl:col-span-4 xl:row-span-2"
        :initial-name="space.name"
        :initial-code="space.code"
        :initial-description="space.description"
        :initial-archived="space.archived"
        :update-error="updateError"
        :update-success="updateSuccess"
        :is-updating="isUpdating"
        @submit="handleUpdate"
      />

      <section
        v-else
        class="page-section desktop-page-panel panel-shell-spread xl:col-span-4 xl:row-span-2"
      >
        <div>
          <p class="section-kicker">Access mode</p>
          <h3 class="mt-2 font-display text-2xl font-semibold text-[var(--color-heading)]">Read-only access</h3>
          <p class="mt-3 text-base leading-7 text-[var(--color-text-soft)]">
            You can view this space because staff added you to its membership roster. Roster
            management and metadata changes remain restricted to lecturers who own the space and
            administrators.
          </p>
        </div>

        <div class="surface-panel-muted mt-6 px-5 py-4">
          <p class="meta-label">What remains available</p>
          <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
            Metadata, assignment visibility, and chat history remain readable inside the same desktop
            grid even when management controls are hidden.
          </p>
        </div>
      </section>

      <AssignmentWorkspace
        class="xl:col-span-8 xl:row-span-3"
        embedded
        :space-id="space.id"
        :space-name="space.name"
      />

      <SpaceChatPanel
        :key="space.id"
        class="xl:col-span-4 xl:row-span-3"
        :space-id="space.id"
        :archived="space.archived"
      />


      <template v-if="space.canManage">
        <SpaceMembershipForm
          class="xl:col-span-4"
          :membership-error="membershipError"
          :membership-success="membershipSuccess"
          :is-adding-student="isAddingStudent"
          @add-student="handleAddStudent"
        />

        <SpaceRosterPanel
          class="xl:col-span-8 xl:row-span-2"
          :memberships="space.memberships"
          :removing-student-id="removingStudentId"
          @remove-student="handleRemoveStudent"
        />
      </template>
    </template>
  </section>
</template>



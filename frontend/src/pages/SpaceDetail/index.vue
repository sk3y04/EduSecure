<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import ExpandablePanel from '@/components/ui/ExpandablePanel.vue'
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
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p class="section-kicker">Space detail</p>
          <h2 class="section-title">{{ space?.name ?? 'Space' }}</h2>
        </div>

        <div class="flex flex-col gap-3 lg:items-end">
          <button type="button" class="btn-secondary self-start lg:self-auto" @click="loadSpace">Refresh</button>
          <span
            v-if="space"
            class="status-pill"
            :class="space.archived ? 'status-pill-warning' : 'status-pill-success'"
          >
            {{ space.archived ? 'Archived' : 'Active' }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="loadError" class="alert-error xl:col-span-12">{{ loadError }}</div>
    <div v-else-if="isLoading" class="empty-state xl:col-span-12">Loading space details…</div>

    <template v-else-if="space">
      <AssignmentWorkspace
        class="xl:col-span-8"
        embedded
        :space-id="space.id"
        :space-name="space.name"
      />

      <SpaceChatPanel
        :key="space.id"
        class="xl:col-span-4"
        :space-id="space.id"
        :archived="space.archived"
        :can-manage="space.canManage"
      />

      <div class="xl:col-span-12">
        <ExpandablePanel title="Space details" :summary="`${space.code} · ${space.memberCount} members`">
          <div class="desktop-page-grid">
            <SpaceMetaPanel class="xl:col-span-8" :space="space" />

            <SpaceUpdateForm
              v-if="space.canManage"
              class="xl:col-span-4"
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
              class="page-section desktop-page-panel xl:col-span-4"
            >
              <h3 class="panel-title">Access</h3>
              <p class="panel-copy">Read-only member access.</p>
            </section>
          </div>
        </ExpandablePanel>
      </div>

      <template v-if="space.canManage">
        <div class="xl:col-span-12">
          <ExpandablePanel title="Roster" summary="Add and manage students">
            <div class="desktop-page-grid">
              <SpaceMembershipForm
                class="xl:col-span-4"
                :membership-error="membershipError"
                :membership-success="membershipSuccess"
                :is-adding-student="isAddingStudent"
                @add-student="handleAddStudent"
              />

              <SpaceRosterPanel
                class="xl:col-span-8"
                :memberships="space.memberships"
                :removing-student-id="removingStudentId"
                @remove-student="handleRemoveStudent"
              />
            </div>
          </ExpandablePanel>
        </div>
      </template>
    </template>
  </section>
</template>



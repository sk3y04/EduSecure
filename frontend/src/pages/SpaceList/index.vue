<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import { useAuthStore } from '@/stores/auth'
import type { SpaceSummary } from '@/types/space'
import {
  SpaceCreateForm,
  SpaceListAccessPanel,
  SpaceListHeader,
  SpaceListItems,
  SpaceListMetricsPanel,
} from './components'

const authStore = useAuthStore()
const router = useRouter()

const spaces = ref<SpaceSummary[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const createSuccess = ref<string | null>(null)
const isCreating = ref(false)

const canManageSpaces = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const canRequestRegistration = computed(() => authStore.hasAnyRole(['STUDENT']))
const canReviewRegistrations = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const activeSpaces = computed(() => spaces.value.filter((space) => !space.archived).length)
const archivedSpaces = computed(() => spaces.value.filter((space) => space.archived).length)
const totalMembers = computed(() => spaces.value.reduce((sum, space) => sum + space.memberCount, 0))

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

async function handleCreate(payload: { name: string; code: string; description: string }) {
  isCreating.value = true
  createError.value = null
  createSuccess.value = null

  try {
    const created = await spacesService.create(payload)

    spaces.value = [
      {
        id: created.id,
        name: created.name,
        code: created.code,
        description: created.description,
        archived: created.archived,
        memberCount: created.memberCount,
        canManage: created.canManage,
        isMember: created.isMember,
      },
      ...spaces.value,
    ]

    createSuccess.value = 'Space created successfully.'
    await router.push({ name: 'space-detail', params: { spaceId: created.id } })
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
  <div class="desktop-page-grid">
    <SpaceListHeader
      class="xl:col-span-8 xl:row-span-2"
      :can-request-registration="canRequestRegistration"
      :can-review-registrations="canReviewRegistrations"
    />

    <SpaceListMetricsPanel
      class="xl:col-span-4"
      :total-spaces="spaces.length"
      :active-spaces="activeSpaces"
      :archived-spaces="archivedSpaces"
      :total-members="totalMembers"
    />

    <SpaceCreateForm
      v-if="canManageSpaces"
      class="xl:col-span-4 xl:row-span-2"
      :create-error="createError"
      :create-success="createSuccess"
      :is-creating="isCreating"
      @submit="handleCreate"
    />

    <SpaceListAccessPanel
      v-else
      class="xl:col-span-4 xl:row-span-2"
      :can-manage-spaces="canManageSpaces"
      :can-request-registration="canRequestRegistration"
      :can-review-registrations="canReviewRegistrations"
    />

    <SpaceListItems
      class="xl:col-span-8 xl:row-span-3"
      :spaces="spaces"
      :is-loading="isLoading"
      :load-error="loadError"
      :can-manage-spaces="canManageSpaces"
      :can-request-registration="canRequestRegistration"
      :can-review-registrations="canReviewRegistrations"
      @refresh="loadSpaces"
    />

    <SpaceListAccessPanel
      v-if="canManageSpaces"
      class="xl:col-span-4"
      :can-manage-spaces="canManageSpaces"
      :can-request-registration="canRequestRegistration"
      :can-review-registrations="canReviewRegistrations"
    />
  </div>
</template>


<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import { useAuthStore } from '@/stores/auth'
import type { SpaceSummary } from '@/types/space'
import { SpaceCreateForm, SpaceListHeader, SpaceListItems } from './components'

const authStore = useAuthStore()

const spaces = ref<SpaceSummary[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const createSuccess = ref<string | null>(null)
const isCreating = ref(false)

const canManageSpaces = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))

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
    <SpaceListHeader />

    <SpaceCreateForm
      v-if="canManageSpaces"
      :create-error="createError"
      :create-success="createSuccess"
      :is-creating="isCreating"
      @submit="handleCreate"
    />

    <SpaceListItems
      :spaces="spaces"
      :is-loading="isLoading"
      :load-error="loadError"
      :can-manage-spaces="canManageSpaces"
      @refresh="loadSpaces"
    />
  </div>
</template>


<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { assignmentsService } from '@/services/assignments'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { AssignmentSummary } from '@/types/assignment'
import { AssignmentCreateForm, AssignmentListHeader, AssignmentListItems } from './components'

const authStore = useAuthStore()

const assignments = ref<AssignmentSummary[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const createSuccess = ref<string | null>(null)
const isCreating = ref(false)

const canCreateAssignments = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const isStudent = computed(() => authStore.hasAnyRole(['STUDENT']))
const sortedAssignments = computed(() =>
  [...assignments.value].sort(
    (left, right) => new Date(left.dueAt).getTime() - new Date(right.dueAt).getTime(),
  ),
)

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

async function handleCreate(payload: { title: string; description: string; dueAt: string }) {
  isCreating.value = true
  createError.value = null
  createSuccess.value = null

  try {
    const created = await assignmentsService.create({
      title: payload.title,
      description: payload.description,
      dueAt: new Date(payload.dueAt).toISOString(),
    })

    assignments.value = [
      { id: created.id, title: created.title, dueAt: created.dueAt, open: created.open },
      ...assignments.value,
    ]

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
    <AssignmentListHeader />

    <AssignmentCreateForm
      v-if="canCreateAssignments"
      :create-error="createError"
      :create-success="createSuccess"
      :is-creating="isCreating"
      @submit="handleCreate"
    />

    <AssignmentListItems
      :assignments="sortedAssignments"
      :is-loading="isLoading"
      :load-error="loadError"
      :is-student="isStudent"
      @refresh="loadAssignments"
    />
  </div>
</template>


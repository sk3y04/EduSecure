<script setup lang="ts">
import { computed, ref } from 'vue'

import { authService } from '@/services/auth'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { CurrentUserResponse, RoleName } from '@/types/auth'
import { UserCreateForm, UserCreatedInfo, UserManagementHeader } from './components'

type ManagedRole = Exclude<RoleName, 'ADMIN'>

const authStore = useAuthStore()

const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const createdUser = ref<CurrentUserResponse | null>(null)

const isAdmin = computed(() => authStore.hasAnyRole(['ADMIN']))
const availableRoleLabels = computed(() => (isAdmin.value ? 'Student, Lecturer' : 'Student'))

async function handleSubmit(payload: {
  fullName: string
  email: string
  password: string
  role: ManagedRole
}) {
  isSubmitting.value = true
  errorMessage.value = null
  successMessage.value = null
  createdUser.value = null

  try {
    const response = await authService.createManagedUser(payload)
    createdUser.value = response
    successMessage.value = `${response.fullName} was created successfully.`
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <UserManagementHeader :available-role-labels="availableRoleLabels" />

    <UserCreateForm
      :is-admin="isAdmin"
      :is-submitting="isSubmitting"
      :error-message="errorMessage"
      :success-message="successMessage"
      @submit="handleSubmit"
    />

    <UserCreatedInfo v-if="createdUser" :user="createdUser" />
  </div>
</template>


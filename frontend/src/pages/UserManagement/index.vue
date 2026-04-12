<script setup lang="ts">
import { computed, ref } from 'vue'

import ExpandablePanel from '@/components/ui/ExpandablePanel.vue'
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
  <div class="desktop-page-grid">
    <UserManagementHeader class="xl:col-span-12" />

    <UserCreateForm
      class="xl:col-span-12"
      :is-admin="isAdmin"
      :is-submitting="isSubmitting"
      :error-message="errorMessage"
      :success-message="successMessage"
      @submit="handleSubmit"
    />

    <div class="xl:col-span-12">
      <ExpandablePanel title="Provisioning details" :summary="`Available roles: ${availableRoleLabels}`">
        <template v-if="createdUser">
          <UserCreatedInfo :user="createdUser" />
        </template>
        <div v-else class="empty-state">
          No account created yet.
        </div>
      </ExpandablePanel>
    </div>
  </div>
</template>


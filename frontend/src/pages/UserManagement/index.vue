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
  <div class="desktop-page-grid">
    <UserManagementHeader
      class="xl:col-span-8 xl:row-span-2"
      :available-role-labels="availableRoleLabels"
    />

    <UserCreateForm
      class="xl:col-span-8 xl:row-span-3"
      :is-admin="isAdmin"
      :is-submitting="isSubmitting"
      :error-message="errorMessage"
      :success-message="successMessage"
      @submit="handleSubmit"
    />

    <UserCreatedInfo
      v-if="createdUser"
      class="xl:col-span-4 xl:row-span-3"
      :user="createdUser"
    />

    <section
      v-else
      class="page-section desktop-page-panel panel-shell-spread xl:col-span-4 xl:row-span-3"
    >
      <div>
        <div class="panel-header">
          <h3 class="panel-title">Account output</h3>
          <p class="panel-copy">
            Review the latest created account here, along with a stable summary of who can be
            provisioned from this workspace.
          </p>
        </div>

        <div class="empty-state">
          No account has been created in this session yet. Submit the form to populate this panel with
          the new managed user details.
        </div>
      </div>

      <div class="surface-panel-muted mt-6 px-5 py-4">
        <p class="meta-label">Provisioning scope</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
          Available role targets in this session: {{ availableRoleLabels }}.
        </p>
      </div>
    </section>
  </div>
</template>


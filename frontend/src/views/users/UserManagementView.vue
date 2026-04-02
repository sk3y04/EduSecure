<script setup lang="ts">
import { computed, reactive, ref } from 'vue'

import { authService } from '@/services/auth'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { CurrentUserResponse, RoleName } from '@/types/auth'

const authStore = useAuthStore()

type ManagedRole = Exclude<RoleName, 'ADMIN'>

const form = reactive({
  fullName: '',
  email: '',
  password: '',
  role: 'STUDENT' as ManagedRole,
})

const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)
const createdUser = ref<CurrentUserResponse | null>(null)

const isAdmin = computed(() => authStore.hasAnyRole(['ADMIN']))
const availableRoleLabels = computed(() => (isAdmin.value ? 'Student, Lecturer' : 'Student'))

async function handleSubmit() {
  isSubmitting.value = true
  errorMessage.value = null
  successMessage.value = null
  createdUser.value = null

  try {
    const response = await authService.createManagedUser({
      fullName: form.fullName,
      email: form.email,
      password: form.password,
      role: form.role,
    })

    createdUser.value = response
    successMessage.value = `${response.fullName} was created successfully.`
    form.fullName = ''
    form.email = ''
    form.password = ''
    form.role = 'STUDENT'
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="space-y-6">
    <section class="surface-panel p-8">
      <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker tracking-[0.3em]">User management</p>
          <h2 class="section-title">Create staff-approved accounts</h2>
          <p class="section-copy">
            Admins can create lecturer and student accounts. Lecturers are limited to creating
            student accounts only. The backend enforces those role boundaries even if the client is
            bypassed.
          </p>
        </div>

        <div class="surface-panel-muted max-w-sm px-5 py-4 text-sm text-slate-600">
          <p class="font-semibold text-slate-900">Current privileges</p>
          <p class="mt-2 leading-6">
            Available role targets: {{ availableRoleLabels }}
          </p>
        </div>
      </div>
    </section>

    <section class="surface-panel p-8">
      <div class="mb-6 border-b border-slate-200 pb-5">
        <h3 class="text-xl font-semibold text-slate-900">Create account</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          New accounts are created with one explicit role. Password policy matches the existing
          registration rules.
        </p>
      </div>

      <div v-if="errorMessage" class="alert-error mb-4">
        {{ errorMessage }}
      </div>
      <div v-if="successMessage" class="alert-success mb-4">
        {{ successMessage }}
      </div>

      <form class="grid gap-5 lg:grid-cols-2" @submit.prevent="handleSubmit">
        <label class="block">
          <span class="field-label">Full name</span>
          <input v-model="form.fullName" type="text" required class="form-input" placeholder="Student Example" />
        </label>

        <label class="block">
          <span class="field-label">Email</span>
          <input v-model="form.email" type="email" required class="form-input" placeholder="student@example.com" />
        </label>

        <label class="block">
          <span class="field-label">Initial password</span>
          <input
            v-model="form.password"
            type="password"
            required
            class="form-input"
            placeholder="StrongPass123!"
          />
        </label>

        <label class="block">
          <span class="field-label">Role</span>
          <select v-model="form.role" class="form-input">
            <option value="STUDENT">Student</option>
            <option v-if="isAdmin" value="LECTURER">Lecturer</option>
          </select>
        </label>

        <div class="lg:col-span-2">
          <button type="submit" class="btn-primary" :disabled="isSubmitting">
            {{ isSubmitting ? 'Creating…' : 'Create account' }}
          </button>
        </div>
      </form>
    </section>

    <section v-if="createdUser" class="surface-panel p-8">
      <h3 class="text-xl font-semibold text-slate-900">Last created account</h3>
      <div class="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Name</dt>
          <dd class="mt-2 text-sm font-semibold text-slate-900">{{ createdUser.fullName }}</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Email</dt>
          <dd class="mt-2 text-sm text-slate-900">{{ createdUser.email }}</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Role</dt>
          <dd class="mt-2 text-sm text-slate-900">{{ createdUser.roles.join(', ') }}</dd>
        </div>
        <div class="data-card">
          <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">User ID</dt>
          <dd class="mt-2 break-all text-sm text-slate-900">{{ createdUser.userId }}</dd>
        </div>
      </div>
    </section>
  </div>
</template>
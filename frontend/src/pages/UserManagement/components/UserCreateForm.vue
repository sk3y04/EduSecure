<script setup lang="ts">
import { reactive } from 'vue'

import type { RoleName } from '@/types/auth'

type ManagedRole = Exclude<RoleName, 'ADMIN'>

const props = defineProps<{
  isAdmin: boolean
  isSubmitting: boolean
  errorMessage: string | null
  successMessage: string | null
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { fullName: string; email: string; password: string; role: ManagedRole }): void
}>()

const form = reactive({
  fullName: '',
  email: '',
  password: '',
  role: 'STUDENT' as ManagedRole,
})

function handleSubmit() {
  emit('submit', { ...form })
  form.fullName = ''
  form.email = ''
  form.password = ''
  form.role = 'STUDENT'
}
</script>

<template>
  <section class="page-section">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Create account</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        New accounts are created with one explicit role. Password policy matches the existing
        registration rules.
      </p>
    </div>

    <div v-if="props.errorMessage" class="alert-error mb-4">{{ props.errorMessage }}</div>
    <div v-if="props.successMessage" class="alert-success mb-4">{{ props.successMessage }}</div>

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
        <input v-model="form.password" type="password" required class="form-input" placeholder="StrongPass123!" />
      </label>

      <label class="block">
        <span class="field-label">Role</span>
        <select v-model="form.role" class="form-input">
          <option value="STUDENT">Student</option>
          <option v-if="props.isAdmin" value="LECTURER">Lecturer</option>
        </select>
      </label>

      <div class="lg:col-span-2">
        <button type="submit" class="btn-primary" :disabled="props.isSubmitting">
          {{ props.isSubmitting ? 'Creating…' : 'Create account' }}
        </button>
      </div>
    </form>
  </section>
</template>


<script setup lang="ts">
import { reactive } from 'vue'

const props = defineProps<{
  errorMessage: string | null
  isSubmitting: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { email: string; password: string }): void
}>()

const form = reactive({ email: '', password: '' })

function handleSubmit() {
  emit('submit', { ...form })
}
</script>

<template>
  <section class="surface-panel p-8 lg:p-10">
    <div class="mb-6 border-b border-slate-200 pb-5">
      <p class="text-xs font-semibold uppercase tracking-[0.3em] text-slate-500">Sign in</p>
      <h2 class="mt-3 text-2xl font-semibold text-slate-900">Authenticate against the API</h2>
      <p class="mt-2 text-sm leading-6 text-slate-600">
        Use an existing backend account. If MFA is enabled, the next step will request a TOTP
        verification code.
      </p>
    </div>

    <div v-if="props.errorMessage" class="alert-error mb-6">
      {{ props.errorMessage }}
    </div>

    <form class="space-y-5" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="field-label">Email</span>
        <input
          v-model="form.email"
          type="email"
          required
          autocomplete="username"
          class="form-input"
          placeholder="student@example.com"
        />
      </label>

      <label class="block">
        <span class="field-label">Password</span>
        <input
          v-model="form.password"
          type="password"
          required
          autocomplete="current-password"
          class="form-input"
          placeholder="Enter your password"
        />
      </label>

      <button type="submit" class="btn-primary w-full" :disabled="props.isSubmitting">
        {{ props.isSubmitting ? 'Signing in…' : 'Continue' }}
      </button>
    </form>
  </section>
</template>


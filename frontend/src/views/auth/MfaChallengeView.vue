<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const verificationCode = ref('')
const isSubmitting = ref(false)

const redirectTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.length > 0 ? redirect : '/assignments'
})

const expiresAtLabel = computed(() => {
  if (!authStore.pendingChallenge?.expiresAt) {
    return 'Short-lived challenge'
  }

  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(authStore.pendingChallenge.expiresAt))
})

onMounted(() => {
  authStore.clearError()
})

async function handleVerify() {
  isSubmitting.value = true

  try {
    await authStore.verifyMfa(verificationCode.value)
    await router.push(redirectTarget.value)
  } catch {
    // Store state already contains the user-facing error message.
  } finally {
    isSubmitting.value = false
  }
}

async function handleStartOver() {
  await authStore.logout()
  await router.push({ name: 'login', query: { redirect: redirectTarget.value } })
}
</script>

<template>
  <div class="app-page px-6 py-10">
    <div class="mx-auto max-w-3xl surface-panel p-8 lg:p-10">
      <p class="section-kicker tracking-[0.3em]">MFA challenge</p>
      <h1 class="section-title">Complete sign-in with your authenticator code</h1>
      <p class="section-copy">
        The authenticated session is only issued after the second factor is verified. This keeps the
        frontend aligned with the backend MFA contract and makes the sign-in process explicit.
      </p>

      <div class="mt-6 grid gap-4 md:grid-cols-3">
        <div class="data-card">
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Method</p>
          <p class="mt-2 text-sm font-semibold text-slate-900">{{ authStore.pendingChallenge?.mfaMethod }}</p>
        </div>
        <div class="data-card">
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Attempts remaining</p>
          <p class="mt-2 text-sm font-semibold text-slate-900">
            {{ authStore.pendingChallenge?.remainingAttempts ?? 'Unknown' }}
          </p>
        </div>
        <div class="data-card">
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Expires</p>
          <p class="mt-2 text-sm font-semibold text-slate-900">{{ expiresAtLabel }}</p>
        </div>
      </div>

      <div v-if="authStore.errorMessage" class="alert-error mt-6">
        {{ authStore.errorMessage }}
      </div>

      <form class="mt-6 space-y-5" @submit.prevent="handleVerify">
        <label class="block">
          <span class="field-label">Verification code</span>
          <input
            v-model="verificationCode"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            required
            class="form-input tracking-[0.35em]"
            placeholder="123456"
          />
        </label>

        <div class="flex flex-col gap-3 sm:flex-row">
          <button type="submit" class="btn-primary flex-1" :disabled="isSubmitting">
            {{ isSubmitting ? 'Verifying…' : 'Verify and continue' }}
          </button>
          <button type="button" class="btn-secondary" @click="handleStartOver">
            Start over
          </button>
        </div>
      </form>
    </div>
  </div>
</template>


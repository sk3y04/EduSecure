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
    dateStyle: 'medium',
    timeStyle: 'short',
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
  <div class="min-h-screen bg-slate-950 px-6 py-10 text-slate-100">
    <div class="mx-auto max-w-3xl rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
      <p class="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">MFA challenge</p>
      <h1 class="mt-4 text-3xl font-semibold text-white">Complete sign-in with your authenticator code</h1>
      <p class="mt-3 text-sm leading-6 text-slate-400">
        The backend only establishes the authenticated HttpOnly cookie after the second factor is
        validated. This keeps the auth flow aligned with the documented MFA contract.
      </p>

      <div class="mt-6 grid gap-4 md:grid-cols-3">
        <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Method</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ authStore.pendingChallenge?.mfaMethod }}</p>
        </div>
        <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Attempts remaining</p>
          <p class="mt-2 text-sm font-semibold text-white">
            {{ authStore.pendingChallenge?.remainingAttempts ?? 'Unknown' }}
          </p>
        </div>
        <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
          <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Expires</p>
          <p class="mt-2 text-sm font-semibold text-white">{{ expiresAtLabel }}</p>
        </div>
      </div>

      <div
        v-if="authStore.errorMessage"
        class="mt-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
      >
        {{ authStore.errorMessage }}
      </div>

      <form class="mt-6 space-y-5" @submit.prevent="handleVerify">
        <label class="block">
          <span class="mb-2 block text-sm font-medium text-slate-200">Verification code</span>
          <input
            v-model="verificationCode"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            required
            class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm tracking-[0.35em] text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
            placeholder="123456"
          />
        </label>

        <div class="flex flex-col gap-3 sm:flex-row">
          <button
            type="submit"
            class="inline-flex flex-1 items-center justify-center rounded-2xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? 'Verifying…' : 'Verify and continue' }}
          </button>
          <button
            type="button"
            class="inline-flex items-center justify-center rounded-2xl border border-slate-700 px-4 py-3 text-sm font-medium text-slate-200 transition hover:border-slate-500 hover:text-white"
            @click="handleStartOver"
          >
            Start over
          </button>
        </div>
      </form>
    </div>
  </div>
</template>


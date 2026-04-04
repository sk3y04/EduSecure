<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { MfaChallengeForm, MfaChallengeInfoCards } from './components'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const isSubmitting = ref(false)

const redirectTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.length > 0 ? redirect : '/spaces'
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

async function handleVerify(code: string) {
  isSubmitting.value = true

  try {
    await authStore.verifyMfa(code)
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
  <div class="app-page px-6 py-[clamp(2rem,7vw,6rem)]">
    <div class="mx-auto max-w-4xl page-hero lg:p-[clamp(2.5rem,6vw,5rem)]">
      <p class="section-kicker">MFA challenge</p>
      <h1 class="hero-title mt-4 max-w-3xl">Complete sign-in with your authenticator code.</h1>
      <p class="section-copy">
        The authenticated session is only issued after the second factor is verified. This keeps the
        frontend aligned with the backend MFA contract and makes the sign-in process explicit.
      </p>

      <MfaChallengeInfoCards
        :challenge="authStore.pendingChallenge"
        :expires-at-label="expiresAtLabel"
      />

      <MfaChallengeForm
        :error-message="authStore.errorMessage"
        :is-submitting="isSubmitting"
        @verify="handleVerify"
        @start-over="handleStartOver"
      />
    </div>
  </div>
</template>


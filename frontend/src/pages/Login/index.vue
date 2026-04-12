<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { LoginForm } from './components'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const isSubmitting = ref(false)

const shouldPreserveErrorMessage = computed(() => route.query.reason === 'session-expired')

const redirectTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.length > 0 ? redirect : '/spaces'
})

watch(
  shouldPreserveErrorMessage,
  (preserveErrorMessage) => {
    if (!preserveErrorMessage) {
      authStore.clearError()
    }
  },
  { immediate: true },
)

async function handleSubmit(payload: { email: string; password: string }) {
  isSubmitting.value = true

  try {
    const result = await authStore.login(payload)

    if (result.requiresMfa) {
      await router.push({
        name: 'mfa',
        query: { redirect: redirectTarget.value },
      })
      return
    }

    await router.push(redirectTarget.value)
  } catch {
    // Store state already contains the user-facing error message.
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="app-page px-4 py-[clamp(1.5rem,5vw,4rem)]">
    <div class="mx-auto max-w-lg">
      <LoginForm
        :error-message="authStore.errorMessage"
        :is-submitting="isSubmitting"
        @submit="handleSubmit"
      />
    </div>
  </div>
</template>


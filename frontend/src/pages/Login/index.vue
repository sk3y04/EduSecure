<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'
import { LoginForm, LoginInfoPanel } from './components'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const isSubmitting = ref(false)

const redirectTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.length > 0 ? redirect : '/assignments'
})

onMounted(() => {
  authStore.clearError()
})

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
  <div class="app-page px-6 py-10">
    <div class="mx-auto grid max-w-6xl gap-8 lg:grid-cols-[1.1fr_0.9fr]">
      <LoginInfoPanel />
      <LoginForm
        :error-message="authStore.errorMessage"
        :is-submitting="isSubmitting"
        @submit="handleSubmit"
      />
    </div>
  </div>
</template>


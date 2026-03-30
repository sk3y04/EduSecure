<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({
  email: '',
  password: '',
})
const isSubmitting = ref(false)

const redirectTarget = computed(() => {
  const redirect = route.query.redirect
  return typeof redirect === 'string' && redirect.length > 0 ? redirect : '/assignments'
})

onMounted(() => {
  authStore.clearError()
})

async function handleSubmit() {
  isSubmitting.value = true

  try {
    const result = await authStore.login({ ...form })

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
      <section class="surface-panel p-8 lg:p-10">
        <p class="section-kicker tracking-[0.3em]">EduSecure portal</p>
        <h1 class="section-title text-4xl">Security-focused access for coursework operations</h1>
        <p class="section-copy max-w-2xl text-base leading-7">
          Sign in to reach the protected assignment and submission workspace. The interface keeps the
          security journey direct: authenticate, complete MFA if required, and review integrity data
          without decorative visual noise.
        </p>

        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <article class="info-card">
            <p class="text-sm font-semibold text-slate-900">Protected session</p>
            <p class="mt-2 text-sm leading-6 text-slate-600">
              Successful authentication establishes the secure cookie used for the protected app.
            </p>
          </article>
          <article class="info-card">
            <p class="text-sm font-semibold text-slate-900">MFA-aware sign-in</p>
            <p class="mt-2 text-sm leading-6 text-slate-600">
              Accounts with TOTP enabled are challenged before the authenticated session is created.
            </p>
          </article>
          <article class="info-card">
            <p class="text-sm font-semibold text-slate-900">Role-based access</p>
            <p class="mt-2 text-sm leading-6 text-slate-600">
              Student and lecturer flows stay separated by the backend role policy already in place.
            </p>
          </article>
        </div>
      </section>

      <section class="surface-panel p-8 lg:p-10">
        <div class="mb-6 border-b border-slate-200 pb-5">
          <p class="text-xs font-semibold uppercase tracking-[0.3em] text-slate-500">Sign in</p>
          <h2 class="mt-3 text-2xl font-semibold text-slate-900">Authenticate against the API</h2>
          <p class="mt-2 text-sm leading-6 text-slate-600">
            Use an existing backend account. If MFA is enabled, the next step will request a TOTP
            verification code.
          </p>
        </div>

        <div v-if="authStore.errorMessage" class="alert-error mb-6">
          {{ authStore.errorMessage }}
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

          <button type="submit" class="btn-primary w-full" :disabled="isSubmitting">
            {{ isSubmitting ? 'Signing in…' : 'Continue' }}
          </button>
        </form>
      </section>
    </div>
  </div>
</template>


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
  <div class="min-h-screen bg-slate-950 px-6 py-10 text-slate-100">
    <div class="mx-auto grid max-w-6xl gap-8 lg:grid-cols-[1.1fr_0.9fr]">
      <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
        <p class="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">Frontend MVP</p>
        <h1 class="mt-4 text-4xl font-semibold text-white">Start with the security-critical flow</h1>
        <p class="mt-4 max-w-2xl text-base leading-7 text-slate-300">
          This frontend is intentionally narrow: sign in, complete MFA when required, submit work,
          and inspect the integrity evidence returned by the backend.
        </p>

        <div class="mt-8 grid gap-4 md:grid-cols-3">
          <article class="rounded-2xl border border-slate-800 bg-slate-950/70 p-5">
            <p class="text-sm font-semibold text-white">Password + secure cookie</p>
            <p class="mt-2 text-sm leading-6 text-slate-400">
              Successful password-only logins establish an HttpOnly authentication cookie for the protected app.
            </p>
          </article>
          <article class="rounded-2xl border border-slate-800 bg-slate-950/70 p-5">
            <p class="text-sm font-semibold text-white">MFA branch</p>
            <p class="mt-2 text-sm leading-6 text-slate-400">
              MFA-enabled accounts are challenged before the authenticated cookie is established.
            </p>
          </article>
          <article class="rounded-2xl border border-slate-800 bg-slate-950/70 p-5">
            <p class="text-sm font-semibold text-white">Role-aware routes</p>
            <p class="mt-2 text-sm leading-6 text-slate-400">
              Students can submit work, while lecturer/admin roles can create assignments.
            </p>
          </article>
        </div>
      </section>

      <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
        <div class="mb-6">
          <p class="text-sm font-semibold uppercase tracking-[0.3em] text-slate-500">Sign in</p>
          <h2 class="mt-3 text-2xl font-semibold text-white">Authenticate against the Spring API</h2>
          <p class="mt-2 text-sm leading-6 text-slate-400">
            Use an existing backend account. If the account has MFA enabled, the next screen will
            request the TOTP code.
          </p>
        </div>

        <div
          v-if="authStore.errorMessage"
          class="mb-6 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
        >
          {{ authStore.errorMessage }}
        </div>

        <form class="space-y-5" @submit.prevent="handleSubmit">
          <label class="block">
            <span class="mb-2 block text-sm font-medium text-slate-200">Email</span>
            <input
              v-model="form.email"
              type="email"
              required
              autocomplete="username"
              class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              placeholder="student@example.com"
            />
          </label>

          <label class="block">
            <span class="mb-2 block text-sm font-medium text-slate-200">Password</span>
            <input
              v-model="form.password"
              type="password"
              required
              autocomplete="current-password"
              class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
              placeholder="Enter your password"
            />
          </label>

          <button
            type="submit"
            class="inline-flex w-full items-center justify-center rounded-2xl bg-brand-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isSubmitting"
          >
            {{ isSubmitting ? 'Signing in…' : 'Continue' }}
          </button>
        </form>
      </section>
    </div>
  </div>
</template>


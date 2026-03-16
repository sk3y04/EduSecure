<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const navigationItems = computed(() => [
  { label: 'Assignments', to: { name: 'assignments' } },
  { label: 'Account security', to: { name: 'account-security' } },
])

async function handleLogout() {
  await authStore.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="min-h-screen bg-slate-950 text-slate-100">
    <header class="border-b border-slate-800 bg-slate-950/95 backdrop-blur">
      <div class="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-6 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">EduSecure</p>
          <h1 class="mt-2 text-3xl font-semibold text-white">Cryptography demo console</h1>
          <p class="mt-2 max-w-2xl text-sm text-slate-400">
            Role-aware frontend for the security artefact: login, MFA status, assignment submission,
            and visible integrity evidence.
          </p>
        </div>

        <div class="rounded-2xl border border-slate-800 bg-slate-900/80 px-5 py-4 shadow-panel">
          <div class="flex flex-wrap items-center gap-2 text-sm text-slate-300">
            <span class="font-semibold text-white">{{ authStore.user?.fullName }}</span>
            <span class="text-slate-500">•</span>
            <span>{{ authStore.user?.email }}</span>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <span
              v-for="role in authStore.user?.roles ?? []"
              :key="role"
              class="rounded-full border border-brand-500/40 bg-brand-500/10 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-brand-100"
            >
              {{ role }}
            </span>
          </div>
          <button
            type="button"
            class="mt-4 inline-flex items-center rounded-lg border border-slate-700 px-4 py-2 text-sm font-medium text-slate-200 transition hover:border-brand-500 hover:text-white"
            @click="handleLogout"
          >
            Sign out
          </button>
        </div>
      </div>
    </header>

    <div class="mx-auto grid max-w-7xl gap-8 px-6 py-8 lg:grid-cols-[240px_minmax(0,1fr)]">
      <aside class="rounded-3xl border border-slate-800 bg-slate-900/80 p-4 shadow-panel">
        <p class="px-3 text-xs font-semibold uppercase tracking-[0.3em] text-slate-500">Navigation</p>
        <nav class="mt-4 space-y-2">
          <RouterLink
            v-for="item in navigationItems"
            :key="item.label"
            :to="item.to"
            class="block rounded-2xl px-4 py-3 text-sm font-medium text-slate-300 transition hover:bg-slate-800 hover:text-white"
            active-class="bg-brand-500/15 text-white ring-1 ring-inset ring-brand-500/40"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="mt-6 rounded-2xl border border-slate-800 bg-slate-950/70 p-4 text-sm text-slate-400">
          <p class="font-semibold text-slate-200">Security evidence focus</p>
          <ul class="mt-3 list-disc space-y-2 pl-5">
            <li>HttpOnly cookie-backed navigation after login</li>
            <li>MFA setup and verification states</li>
            <li>Submission digest and signature visibility</li>
          </ul>
        </div>
      </aside>

      <main>
        <RouterView />
      </main>
    </div>
  </div>
</template>


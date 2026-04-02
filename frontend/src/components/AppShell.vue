<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const navigationItems = computed(() => {
  const items = [
    { label: 'Assignments', to: { name: 'assignments' } },
    { label: 'Spaces', to: { name: 'spaces' } },
  ]

  if (authStore.hasAnyRole(['ADMIN', 'LECTURER'])) {
    items.push({ label: 'User management', to: { name: 'user-management' } })
  }

  items.push({ label: 'Account security', to: { name: 'account-security' } })
  return items
})

async function handleLogout() {
  await authStore.logout()
  await router.push({ name: 'login' })
}
</script>

<template>
  <div class="app-page">
    <header class="border-b border-slate-800 bg-slate-900 text-slate-100">
      <div class="mx-auto flex max-w-7xl flex-col gap-6 px-6 py-5 lg:flex-row lg:items-center lg:justify-between">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.3em] text-brand-100">EduSecure</p>
          <h1 class="mt-2 text-3xl font-semibold text-white">Security operations console</h1>
          <p class="mt-2 max-w-2xl text-sm text-slate-300">
            Frontend access to authentication, MFA state, assignment management, and submission
            integrity evidence.
          </p>
        </div>

        <div class="rounded-sm border border-slate-700 bg-slate-800 px-5 py-4 shadow-panel">
          <div class="flex flex-wrap items-center gap-2 text-sm text-slate-300">
            <span class="font-semibold text-white">{{ authStore.user?.fullName }}</span>
            <span class="text-slate-500">•</span>
            <span>{{ authStore.user?.email }}</span>
          </div>
          <div class="mt-3 flex flex-wrap gap-2">
            <span
              v-for="role in authStore.user?.roles ?? []"
              :key="role"
              class="rounded-sm border border-slate-600 bg-slate-700 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-slate-100"
            >
              {{ role }}
            </span>
          </div>
          <button
            type="button"
            class="mt-4 inline-flex items-center rounded-sm border border-slate-500 bg-slate-100 px-4 py-2 text-sm font-medium text-slate-900 transition hover:bg-white"
            @click="handleLogout"
          >
            Sign out
          </button>
        </div>
      </div>
    </header>

    <div class="mx-auto grid max-w-7xl gap-8 px-6 py-8 lg:grid-cols-[240px_minmax(0,1fr)]">
      <aside class="surface-panel p-4">
        <p class="px-3 text-xs font-semibold uppercase tracking-[0.3em] text-slate-500">Navigation</p>
        <nav class="mt-4 space-y-2">
          <RouterLink
            v-for="item in navigationItems"
            :key="item.label"
            :to="item.to"
            class="block border-l-4 border-transparent px-4 py-3 text-sm font-medium text-slate-700 transition hover:border-slate-300 hover:bg-slate-50 hover:text-slate-900"
            active-class="border-brand-700 bg-slate-100 text-slate-900"
          >
            {{ item.label }}
          </RouterLink>
        </nav>

        <div class="surface-panel-muted mt-6 p-4 text-sm text-slate-600">
          <p class="font-semibold text-slate-900">Security evidence focus</p>
          <ul class="mt-3 list-disc space-y-2 pl-5">
            <li>Cookie-backed navigation after login</li>
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


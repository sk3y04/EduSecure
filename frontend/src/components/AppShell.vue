<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'

import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const router = useRouter()

const navigationItems = computed(() => {
  const items = [
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
    <header class="mx-auto max-w-7xl px-6 pt-6">
      <div class="surface-panel px-5 py-4">
        <div class="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:gap-8">
            <div>
              <p class="text-lg font-semibold text-[var(--color-heading)]">EduSecure</p>
              <p class="text-sm text-[var(--color-text-soft)]">Coursework and submission workspace</p>
            </div>

            <nav class="flex flex-wrap gap-2">
              <RouterLink
                v-for="item in navigationItems"
                :key="item.label"
                :to="item.to"
                class="inline-flex rounded-full px-4 py-2 text-base font-medium text-[var(--color-text)] transition hover:bg-black/5 hover:text-[var(--color-heading)]"
                active-class="bg-[var(--color-surface-offset)] text-[var(--color-heading)] shadow-panel"
              >
                {{ item.label }}
              </RouterLink>
            </nav>
          </div>

          <div class="flex flex-col gap-3 lg:flex-row lg:items-center lg:gap-4">
            <div class="text-left">
              <p class="text-base font-semibold text-[var(--color-heading)]">{{ authStore.user?.fullName }}</p>
              <p class="text-sm text-[var(--color-text-soft)]">{{ authStore.user?.email }}</p>
            </div>

            <div class="flex flex-wrap items-center gap-2">
              <span
                v-for="role in authStore.user?.roles ?? []"
                :key="role"
                class="status-pill status-pill-neutral"
              >
                {{ role.replaceAll('_', ' ') }}
              </span>
              <button type="button" class="btn-secondary" @click="handleLogout">
                Sign out
              </button>
            </div>
          </div>
        </div>
      </div>
    </header>

    <div class="mx-auto max-w-7xl px-6 py-[clamp(2rem,5vw,4rem)]">
      <main>
        <RouterView />
      </main>
    </div>
  </div>
</template>


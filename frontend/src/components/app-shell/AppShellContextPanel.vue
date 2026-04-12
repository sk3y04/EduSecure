<script setup lang="ts">
import type { CurrentUserResponse } from '@/types/auth'

const props = defineProps<{
  user: CurrentUserResponse | null
  primaryRole?: string
}>()

const emit = defineEmits<{
  (e: 'logout'): void
}>()
</script>

<template>
  <aside class="desktop-shell-context">
    <section class="page-section !p-5">
      <p class="section-kicker">Signed in as</p>
      <h3 class="mt-2 font-display text-2xl font-semibold text-[var(--color-heading)]">
        {{ props.user?.fullName ?? 'Workspace user' }}
      </h3>
      <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">{{ props.user?.email ?? 'No email available' }}</p>

      <div class="mt-5 flex flex-wrap gap-2">
        <span
          v-for="role in props.user?.roles ?? []"
          :key="role"
          class="status-pill status-pill-neutral"
        >
          {{ role.replaceAll('_', ' ') }}
        </span>
      </div>
    </section>

    <section class="surface-panel-muted px-5 py-5">
      <p class="meta-label">Session control</p>
      <button type="button" class="btn-secondary mt-4 w-full" @click="emit('logout')">
        Sign out
      </button>
    </section>
  </aside>
</template>


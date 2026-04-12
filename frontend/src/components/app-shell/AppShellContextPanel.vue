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
  <aside class="account-panel">
    <div class="min-w-0">
      <p class="meta-label">Signed in as</p>
      <h3 class="mt-1 truncate text-base font-semibold text-[var(--color-heading)]">
        {{ props.user?.fullName ?? 'Workspace user' }}
      </h3>
      <p class="truncate text-sm text-[var(--color-text-soft)]">{{ props.user?.email ?? 'No email available' }}</p>
      <p v-if="props.primaryRole" class="mt-1 text-xs font-medium uppercase tracking-[0.16em] text-[var(--color-text-soft)]">
        {{ props.primaryRole }}
      </p>
    </div>

    <div class="account-panel-actions">
      <div class="flex flex-wrap justify-end gap-2">
        <span
          v-for="role in props.user?.roles ?? []"
          :key="role"
          class="status-pill status-pill-neutral"
        >
          {{ role.replaceAll('_', ' ') }}
        </span>
      </div>

      <button type="button" class="btn-secondary account-signout" @click="emit('logout')">
        Sign out
      </button>
    </div>
  </aside>
</template>


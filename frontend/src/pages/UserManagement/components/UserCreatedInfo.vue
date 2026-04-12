<script setup lang="ts">
import type { CurrentUserResponse } from '@/types/auth'

const props = defineProps<{
  user: CurrentUserResponse
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Account output</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Review the latest created account here, along with a stable summary of who can be provisioned
        from this workspace.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <div class="stat-card bg-[var(--color-surface-offset)]">
        <dt class="meta-label">Name</dt>
        <dd class="meta-value font-medium">{{ props.user.fullName }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Email</dt>
        <dd class="meta-value">{{ props.user.email }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Role</dt>
        <dd class="meta-value">{{ props.user.roles.join(', ') }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">User ID</dt>
        <dd class="meta-value break-all mono-meta">{{ props.user.userId }}</dd>
      </div>
    </div>
  </section>
</template>


<script setup lang="ts">
import type { CurrentUserResponse } from '@/types/auth'

const props = defineProps<{
  user: CurrentUserResponse
}>()
</script>

<template>
  <section class="page-section">
    <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Last created account</h3>
    <div class="stats-grid mt-5">
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


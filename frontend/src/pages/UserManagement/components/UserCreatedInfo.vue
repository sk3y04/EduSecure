<script setup lang="ts">
import type { CurrentUserResponse } from '@/types/auth'

const props = defineProps<{
  user: CurrentUserResponse
}>()
</script>

<template>
  <section class="surface-panel p-8">
    <h3 class="text-xl font-semibold text-slate-900">Last created account</h3>
    <div class="mt-5 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Name</dt>
        <dd class="mt-2 text-sm font-semibold text-slate-900">{{ props.user.fullName }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Email</dt>
        <dd class="mt-2 text-sm text-slate-900">{{ props.user.email }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Role</dt>
        <dd class="mt-2 text-sm text-slate-900">{{ props.user.roles.join(', ') }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">User ID</dt>
        <dd class="mt-2 break-all text-sm text-slate-900">{{ props.user.userId }}</dd>
      </div>
    </div>
  </section>
</template>


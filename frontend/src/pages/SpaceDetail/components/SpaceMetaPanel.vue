<script setup lang="ts">
import type { SpaceDetail } from '@/types/space'

const props = defineProps<{
  space: SpaceDetail | null
}>()

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
</script>

<template>
  <section class="surface-panel p-8">
    <div class="flex flex-col gap-5 border-b border-slate-200 pb-5 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <div class="flex flex-wrap items-center gap-3">
          <h3 class="text-2xl font-semibold text-slate-900">{{ props.space?.name }}</h3>
          <span class="rounded-sm border border-slate-300 bg-slate-50 px-2 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700">
            {{ props.space?.code }}
          </span>
        </div>
        <p class="mt-3 text-sm leading-6 text-slate-600">{{ props.space?.description }}</p>
      </div>

      <span
        v-if="props.space"
        class="status-pill"
        :class="props.space.archived ? 'border-amber-300 bg-amber-50 text-amber-800' : 'border-emerald-300 bg-emerald-50 text-emerald-800'"
      >
        {{ props.space.archived ? 'Archived' : 'Active' }}
      </span>
    </div>

    <dl v-if="props.space" class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Students</dt>
        <dd class="mt-2 text-sm font-semibold text-slate-900">{{ props.space.memberCount }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Created</dt>
        <dd class="mt-2 text-sm text-slate-900">{{ formatDate(props.space.createdAt) }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Updated</dt>
        <dd class="mt-2 text-sm text-slate-900">{{ formatDate(props.space.updatedAt) }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Access</dt>
        <dd class="mt-2 text-sm text-slate-900">
          {{ props.space.canManage ? 'Management access' : props.space.isMember ? 'Enrolled member' : 'Read only' }}
        </dd>
      </div>
    </dl>
  </section>
</template>



<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { SpaceSummary } from '@/types/space'

const props = defineProps<{
  spaces: SpaceSummary[]
  isLoading: boolean
  loadError: string | null
  canManageSpaces: boolean
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

function statusClass(archived: boolean): string {
  return archived
    ? 'border-amber-300 bg-amber-50 text-amber-800'
    : 'border-emerald-300 bg-emerald-50 text-emerald-800'
}
</script>

<template>
  <section class="surface-panel p-8">
    <div class="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="text-xl font-semibold text-slate-900">Visible spaces</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          The list is filtered server-side to match the current user's role and ownership rules.
        </p>
      </div>
      <button type="button" class="btn-secondary self-start sm:self-auto" @click="emit('refresh')">
        Refresh
      </button>
    </div>

    <div v-if="props.loadError" class="alert-error mb-4">{{ props.loadError }}</div>

    <div v-if="props.isLoading" class="empty-state">Loading spaces…</div>

    <div v-else-if="props.spaces.length === 0" class="empty-state">
      {{ props.canManageSpaces ? 'No spaces exist yet.' : 'You have not been assigned to any spaces yet.' }}
    </div>

    <div v-else class="space-y-4">
      <article
        v-for="space in props.spaces"
        :key="space.id"
        class="rounded-sm border border-slate-300 bg-white p-5"
      >
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div class="max-w-3xl">
            <div class="flex flex-wrap items-center gap-3">
              <h4 class="text-lg font-semibold text-slate-900">{{ space.name }}</h4>
              <span class="rounded-sm border border-slate-300 bg-slate-50 px-2 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700">
                {{ space.code }}
              </span>
            </div>
            <p class="mt-3 text-sm leading-6 text-slate-600">{{ space.description }}</p>
          </div>

          <div class="flex flex-col items-start gap-3 lg:items-end">
            <span class="status-pill" :class="statusClass(space.archived)">
              {{ space.archived ? 'Archived' : 'Active' }}
            </span>
            <p class="text-sm text-slate-500">
              {{ space.memberCount }} student{{ space.memberCount === 1 ? '' : 's' }}
            </p>
          </div>
        </div>

        <div class="mt-5 flex flex-wrap items-center gap-3 border-t border-slate-200 pt-4">
          <RouterLink :to="{ name: 'space-detail', params: { spaceId: space.id } }" class="btn-primary">
            {{ space.canManage ? 'Manage space' : 'Open space' }}
          </RouterLink>
          <span v-if="space.isMember && !space.canManage" class="text-sm text-slate-500">
            You are enrolled in this space.
          </span>
        </div>
      </article>
    </div>
  </section>
</template>


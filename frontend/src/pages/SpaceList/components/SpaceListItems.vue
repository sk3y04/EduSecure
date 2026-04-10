<script setup lang="ts">
import { RouterLink } from 'vue-router'

import type { SpaceSummary } from '@/types/space'

const props = defineProps<{
  spaces: SpaceSummary[]
  isLoading: boolean
  loadError: string | null
  canManageSpaces: boolean
  canRequestRegistration: boolean
  canReviewRegistrations: boolean
}>()

const emit = defineEmits<{
  (e: 'refresh'): void
}>()

function statusClass(archived: boolean): string {
  return archived
    ? 'status-pill-warning'
    : 'status-pill-success'
}
</script>

<template>
  <section class="page-section">
    <div class="panel-header flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Visible spaces</h3>
        <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
          The list is filtered server-side to match the current user's role and ownership rules.
        </p>
      </div>
      <button type="button" class="btn-secondary self-start sm:self-auto" @click="emit('refresh')">
        Refresh
      </button>
    </div>

    <div v-if="props.loadError" class="alert-error mb-4">{{ props.loadError }}</div>

    <div v-if="props.isLoading" class="empty-state">Loading spaces…</div>

    <div v-else-if="props.spaces.length === 0" class="empty-state space-y-3">
      <p>
        {{ props.canManageSpaces ? 'No spaces exist yet.' : 'You have not been assigned to any spaces yet.' }}
      </p>
      <div class="flex flex-wrap gap-3">
        <RouterLink
          v-if="props.canRequestRegistration"
          :to="{ name: 'registration-requests' }"
          class="btn-primary"
        >
          Request access by space code
        </RouterLink>
        <RouterLink
          v-if="props.canReviewRegistrations"
          :to="{ name: 'registration-review' }"
          class="btn-secondary"
        >
          Open review queue
        </RouterLink>
      </div>
    </div>

    <div v-else class="record-list">
      <article
        v-for="space in props.spaces"
        :key="space.id"
        class="record-card"
      >
        <div class="record-card-frame">
          <div class="max-w-3xl">
            <div class="flex flex-wrap items-center gap-3">
              <h4 class="font-display text-xl font-semibold text-[var(--color-heading)]">{{ space.name }}</h4>
              <span class="status-pill status-pill-neutral">
                {{ space.code }}
              </span>
            </div>
            <p class="mt-3 text-base leading-7 text-[var(--color-text-soft)]">{{ space.description }}</p>
          </div>

          <div class="flex flex-col items-start gap-3 lg:items-end">
            <span class="status-pill" :class="statusClass(space.archived)">
              {{ space.archived ? 'Archived' : 'Active' }}
            </span>
            <p class="text-base text-[var(--color-text-soft)]">
              {{ space.memberCount }} student{{ space.memberCount === 1 ? '' : 's' }}
            </p>
          </div>
        </div>

        <div class="record-card-footer">
          <RouterLink :to="{ name: 'space-detail', params: { spaceId: space.id } }" class="btn-primary">
            {{ space.canManage ? 'Manage space' : 'Open space' }}
          </RouterLink>
          <span v-if="space.isMember && !space.canManage" class="text-base text-[var(--color-text-soft)]">
            You currently have staff-managed membership in this space.
          </span>
        </div>
      </article>
    </div>
  </section>
</template>


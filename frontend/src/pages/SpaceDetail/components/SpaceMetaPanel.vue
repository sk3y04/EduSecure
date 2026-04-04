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
  <section class="page-section">
    <div class="panel-header flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <div class="flex flex-wrap items-center gap-3">
          <h3 class="font-display text-2xl font-semibold text-[var(--color-heading)]">{{ props.space?.name }}</h3>
          <span class="status-pill status-pill-neutral">
            {{ props.space?.code }}
          </span>
        </div>
        <p class="mt-3 text-base leading-7 text-[var(--color-text-soft)]">{{ props.space?.description }}</p>
      </div>

      <span
        v-if="props.space"
        class="status-pill"
        :class="props.space.archived ? 'status-pill-warning' : 'status-pill-success'"
      >
        {{ props.space.archived ? 'Archived' : 'Active' }}
      </span>
    </div>

    <dl v-if="props.space" class="stats-grid mt-6">
      <div class="stat-card">
        <dt class="meta-label">Students</dt>
        <dd class="meta-value font-medium">{{ props.space.memberCount }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Created</dt>
        <dd class="meta-value">{{ formatDate(props.space.createdAt) }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Updated</dt>
        <dd class="meta-value">{{ formatDate(props.space.updatedAt) }}</dd>
      </div>
      <div class="stat-card bg-[var(--color-surface-offset)]">
        <dt class="meta-label">Access</dt>
        <dd class="meta-value">
          {{ props.space.canManage ? 'Management access' : props.space.isMember ? 'Enrolled member' : 'Read only' }}
        </dd>
      </div>
    </dl>
  </section>
</template>



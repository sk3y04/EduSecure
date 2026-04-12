<script setup lang="ts">
import type { SpaceStudent } from '@/types/space'

const props = defineProps<{
  memberships: SpaceStudent[]
  removingStudentId: string | null
}>()

const emit = defineEmits<{
  (e: 'removeStudent', studentUserId: string): void
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
  <section class="page-section desktop-page-panel panel-shell panel-shell-min-24">
    <div class="panel-header-split">
      <div>
        <h3 class="panel-title">Student roster</h3>
        <p class="panel-copy">
          Students are listed only for users with management permission on this space.
        </p>
      </div>
    </div>

    <div v-if="props.memberships.length === 0" class="empty-state">
      No students are assigned to this space yet.
    </div>

    <div v-else class="panel-scroll-list">
      <article
        v-for="membership in props.memberships"
        :key="membership.studentUserId"
        class="record-card"
      >
        <div class="record-card-frame">
          <div>
            <p class="text-base font-semibold text-[var(--color-heading)]">{{ membership.studentFullName }}</p>
            <p class="mt-1 text-base text-[var(--color-text-soft)]">{{ membership.studentEmail }}</p>
            <p class="mt-2 text-sm text-[var(--color-text-soft)]">
              Added {{ formatDate(membership.addedAt) }}
            </p>
          </div>

          <button
            type="button"
            class="btn-danger self-start sm:self-auto"
            :disabled="props.removingStudentId === membership.studentUserId"
            @click="emit('removeStudent', membership.studentUserId)"
          >
            {{ props.removingStudentId === membership.studentUserId ? 'Removing…' : 'Remove' }}
          </button>
        </div>
      </article>
    </div>
  </section>
</template>


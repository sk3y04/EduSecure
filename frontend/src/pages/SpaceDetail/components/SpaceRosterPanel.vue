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
  <section class="surface-panel p-8">
    <div class="mb-6 flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <h3 class="text-xl font-semibold text-slate-900">Student roster</h3>
        <p class="mt-2 text-sm leading-6 text-slate-600">
          Students are listed only for users with management permission on this space.
        </p>
      </div>
    </div>

    <div v-if="props.memberships.length === 0" class="empty-state">
      No students are assigned to this space yet.
    </div>

    <div v-else class="space-y-4">
      <article
        v-for="membership in props.memberships"
        :key="membership.studentUserId"
        class="rounded-sm border border-slate-300 bg-slate-50 p-4"
      >
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p class="text-sm font-semibold text-slate-900">{{ membership.studentFullName }}</p>
            <p class="mt-1 text-sm text-slate-600">{{ membership.studentEmail }}</p>
            <p class="mt-2 text-xs uppercase tracking-[0.25em] text-slate-500">
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


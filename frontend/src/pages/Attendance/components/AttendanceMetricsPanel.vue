<script setup lang="ts">
defineProps<{
  totalSessions: number
  filteredSessions: number
  manageableSessions: number
  unrecordedSlots: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Attendance metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A quick desktop summary of reporting volume, manageable scope, and remaining unrecorded attendance.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">Visible sessions</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalSessions }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">All attendance sessions visible to the current user.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Matching filters</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ filteredSessions }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Sessions remaining after current filter rules are applied.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Manageable</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ manageableSessions }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Sessions where roster editing is available in this view.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Unrecorded slots</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ unrecordedSlots }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Attendance entries still missing across visible sessions.</p>
      </article>
    </div>
  </section>
</template>


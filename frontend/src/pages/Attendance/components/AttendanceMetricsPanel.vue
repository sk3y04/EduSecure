<script setup lang="ts">
defineProps<{
  totalSessions: number
  filteredSessions: number
  manageableSessions: number
  unrecordedSlots: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex flex-col">
    <div class="stats-grid">
      <article class="stat-card">
        <p class="meta-label">Visible sessions</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalSessions }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Matching filters</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ filteredSessions }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Manageable</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ manageableSessions }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Unrecorded slots</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ unrecordedSlots }}</p>
      </article>
    </div>
  </section>
</template>


<script setup lang="ts">
defineProps<{
  totalAssignments: number
  openAssignments: number
  closedAssignments: number
  submittedAssignments: number
}>()
</script>

<template>
  <section class="flex flex-col gap-4">
    <div class="stats-grid">
      <article class="stat-card">
        <p class="meta-label">Visible assignments</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalAssignments }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Open</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ openAssignments }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Closed</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ closedAssignments }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Submitted</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ submittedAssignments }}</p>
      </article>
    </div>
  </section>
</template>


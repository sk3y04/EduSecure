<script setup lang="ts">
defineProps<{
  totalRequests: number
  pendingRequests: number
  reviewedRequests: number
  requestsWithNotes: number
}>()
</script>

<template>
  <section class="flex flex-col gap-4">
    <div class="stats-grid">
      <article class="stat-card">
        <p class="meta-label">All requests</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalRequests }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Pending</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ pendingRequests }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Reviewed</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ reviewedRequests }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">With review notes</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ requestsWithNotes }}</p>
      </article>
    </div>
  </section>
</template>


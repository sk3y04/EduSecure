<script setup lang="ts">
defineProps<{
  totalRequests: number
  pendingRequests: number
  reviewedRequests: number
  requestsWithNotes: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Request metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A desktop summary of current access demand, pending work, and completed review outcomes.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">All requests</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalRequests }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Visible request records in your personal history.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Pending</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ pendingRequests }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Requests still waiting for staff review.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Reviewed</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ reviewedRequests }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Requests with a recorded approval or rejection outcome.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">With review notes</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ requestsWithNotes }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Requests that include reviewer follow-up context.</p>
      </article>
    </div>
  </section>
</template>


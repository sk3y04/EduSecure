<script setup lang="ts">
defineProps<{
  totalRequests: number
  uniqueSpaces: number
  uniqueStudents: number
  requestsWithMessages: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Queue metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A desktop summary of review volume, space coverage, and student demand across the current queue.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">Pending requests</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalRequests }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Requests currently awaiting an approval decision.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Spaces involved</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ uniqueSpaces }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Distinct academic spaces represented in the queue.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Students waiting</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ uniqueStudents }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Unique student identities currently requesting access.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Requests with messages</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ requestsWithMessages }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Requests that include reviewer context from the student.</p>
      </article>
    </div>
  </section>
</template>


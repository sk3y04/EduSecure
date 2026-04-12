<script setup lang="ts">
defineProps<{
  totalRequests: number
  uniqueSpaces: number
  uniqueStudents: number
  requestsWithMessages: number
}>()
</script>

<template>
  <section class="flex flex-col gap-4">
    <div class="stats-grid">
      <article class="stat-card">
        <p class="meta-label">Pending requests</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalRequests }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Spaces involved</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ uniqueSpaces }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Students waiting</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ uniqueStudents }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Requests with messages</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ requestsWithMessages }}</p>
      </article>
    </div>
  </section>
</template>


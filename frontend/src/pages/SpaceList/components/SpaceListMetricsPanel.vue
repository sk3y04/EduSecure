<script setup lang="ts">
defineProps<{
  totalSpaces: number
  activeSpaces: number
  archivedSpaces: number
  totalMembers: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Overview</h3>
    </div>

    <div class="stats-grid">
      <article class="stat-card">
        <p class="meta-label">Spaces</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalSpaces }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Active</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ activeSpaces }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Archived</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ archivedSpaces }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Members</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalMembers }}</p>
      </article>
    </div>
  </section>
</template>


<script setup lang="ts">
defineProps<{
  totalSpaces: number
  activeSpaces: number
  archivedSpaces: number
  totalMembers: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Workspace metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A quick operational snapshot to balance discovery, access, and lifecycle status at a glance.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">Visible spaces</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalSpaces }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Server-filtered to your current role.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Active</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ activeSpaces }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Spaces currently available for ongoing coursework.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Archived</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ archivedSpaces }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Historical or paused spaces kept for reference.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Rostered students</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalMembers }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Combined membership visible across listed spaces.</p>
      </article>
    </div>
  </section>
</template>


<script setup lang="ts">
defineProps<{
  totalExams: number
  publishedExams: number
  draftExams: number
  manageableSpaces: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Timetable metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A desktop summary of visible exams, publication state, and currently manageable space scope.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">Visible exams</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalExams }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">All exam entries visible to the current user.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Published</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ publishedExams }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Entries currently visible to enrolled students.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Drafts</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ draftExams }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Staff-only timetable entries awaiting publication.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Manageable spaces</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ manageableSpaces }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Spaces available for exam scheduling in this session.</p>
      </article>
    </div>
  </section>
</template>


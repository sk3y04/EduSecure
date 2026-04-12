<script setup lang="ts">
defineProps<{
  totalExams: number
  selectedExamResults: number
  publishedResults: number
  draftResults: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Result metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A desktop summary of exam coverage, selected result volume, and current publication state.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">Visible exams</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalExams }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Exam options available in the current role-scoped view.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Selected results</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ selectedExamResults }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Result records currently loaded in the active context.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Published</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ publishedResults }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Results visible to students right now.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Draft</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ draftResults }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Unpublished result records still under staff control.</p>
      </article>
    </div>
  </section>
</template>


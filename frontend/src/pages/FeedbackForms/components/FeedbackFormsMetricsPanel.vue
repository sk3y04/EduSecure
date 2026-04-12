<script setup lang="ts">
defineProps<{
  accessibleExams: number
  visibleForms: number
  selectedQuestions: number
  responseVolume: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Feedback metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A quick summary of exam coverage, visible forms, selected prompt count, and recorded response volume.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">Accessible exams</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ accessibleExams }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Exams available in the current role-scoped view.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Visible forms</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ visibleForms }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Forms listed for the selected exam.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Selected questions</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ selectedQuestions }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Prompt count for the currently opened form.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Response volume</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ responseVolume }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Recorded responses in the active context.</p>
      </article>
    </div>
  </section>
</template>


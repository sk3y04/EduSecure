<script setup lang="ts">
defineProps<{
  accessibleExams: number
  visibleForms: number
  selectedQuestions: number
  responseVolume: number
}>()
</script>

<template>
  <section class="flex flex-col gap-4">
    <div class="stats-grid">
      <article class="stat-card">
        <p class="meta-label">Accessible exams</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ accessibleExams }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Visible forms</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ visibleForms }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Selected questions</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ selectedQuestions }}</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Response volume</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ responseVolume }}</p>
      </article>
    </div>
  </section>
</template>


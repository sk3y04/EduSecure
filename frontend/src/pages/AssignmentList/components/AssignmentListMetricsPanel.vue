<script setup lang="ts">
defineProps<{
  totalAssignments: number
  openAssignments: number
  closedAssignments: number
  submittedAssignments: number
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Coursework metrics</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        A quick desktop snapshot of assignment availability, completion signals, and overall workload.
      </p>
    </div>

    <div class="stats-grid flex-1">
      <article class="stat-card">
        <p class="meta-label">Visible assignments</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ totalAssignments }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Filtered to the current authenticated view.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Open</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ openAssignments }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Assignments that still accept student activity.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Closed</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ closedAssignments }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Past or inactive coursework entries kept for reference.</p>
      </article>

      <article class="stat-card">
        <p class="meta-label">Submitted</p>
        <p class="mt-3 text-3xl font-semibold text-[var(--color-heading)]">{{ submittedAssignments }}</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">Assignments with a latest student submission in view.</p>
      </article>
    </div>
  </section>
</template>


<script setup lang="ts">
defineProps<{
  title: string
  description: string
  dateLabel: string
}>()
</script>

<template>
  <header class="desktop-shell-header">
    <div class="flex items-end justify-between gap-6">
      <div class="max-w-3xl">
        <p class="section-kicker">Current workspace</p>
        <h2 class="mt-2 font-display text-[clamp(2rem,1.4rem+1.2vw,3rem)] font-semibold leading-tight tracking-tight text-[var(--color-heading)]">
          {{ title }}
        </h2>
        <p class="mt-3 text-base leading-7 text-[var(--color-text-soft)]">
          {{ description }}
        </p>
      </div>

      <div class="grid gap-3 text-right">
        <div class="surface-panel-muted px-4 py-3">
          <p class="meta-label">Today</p>
          <p class="mt-1 text-base font-semibold text-[var(--color-heading)]">{{ dateLabel }}</p>
        </div>
      </div>
    </div>
  </header>
</template>


<script setup lang="ts">
defineProps<{
  title: string
  dateLabel: string
}>()
</script>

<template>
  <header class="desktop-shell-header">
    <p class="meta-label">{{ dateLabel }}</p>
    <h2 class="mt-2 font-display text-[clamp(1.8rem,1.35rem+1vw,2.6rem)] font-semibold leading-tight tracking-tight text-[var(--color-heading)]">
      {{ title }}
    </h2>
  </header>
</template>


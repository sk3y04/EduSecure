<script setup lang="ts">
defineProps<{
  title: string
  summary?: string
  defaultOpen?: boolean
}>()
</script>

<template>
  <details class="disclosure-panel" :open="defaultOpen ? true : undefined">
    <summary class="disclosure-panel-summary">
      <span class="block">
        <span class="block text-sm font-semibold text-[var(--color-heading)]">{{ title }}</span>
        <span v-if="summary" class="mt-1 block text-sm text-[var(--color-text-soft)]">{{ summary }}</span>
      </span>
      <span class="disclosure-panel-indicator">Details</span>
    </summary>

    <div class="disclosure-panel-content">
      <slot />
    </div>
  </details>
</template>



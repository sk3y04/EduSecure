<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  navigationItems: Array<{
    label: string
    caption: string
    to: { name: string }
  }>
}>()
</script>

<template>
  <aside class="desktop-shell-sidebar">
    <div class="space-y-4">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.28em] text-[var(--color-text-soft)]">EduSecure</p>
        <h1 class="mt-3 font-display text-3xl font-semibold leading-tight text-[var(--color-heading)]">
          Desktop workspace
        </h1>
        <p class="mt-3 text-sm leading-6 text-[var(--color-text-soft)]">
          A full-page academic console with dedicated navigation, content, and context rails.
        </p>
      </div>
    </div>

    <nav class="grid gap-2">
      <RouterLink
        v-for="item in navigationItems"
        :key="item.label"
        :to="item.to"
        class="group rounded-2xl border border-transparent px-4 py-3 transition hover:border-black/10 hover:bg-[var(--color-surface-2)]"
        active-class="border-black/10 bg-[var(--color-surface-offset)] text-[var(--color-heading)]"
      >
        <p class="text-sm font-semibold text-[var(--color-heading)]">{{ item.label }}</p>
        <p class="mt-1 text-sm leading-6 text-[var(--color-text-soft)] transition group-hover:text-[var(--color-text)]">
          {{ item.caption }}
        </p>
      </RouterLink>
    </nav>
  </aside>
</template>


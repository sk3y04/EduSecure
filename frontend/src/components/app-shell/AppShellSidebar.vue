<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  navigationItems: Array<{
    label: string
    to: { name: string }
  }>
}>()
</script>

<template>
  <aside class="desktop-shell-sidebar">
    <div class="space-y-3">
      <div>
        <p class="text-xs font-semibold uppercase tracking-[0.24em] text-[var(--color-text-soft)]">EduSecure</p>
        <h1 class="mt-2 font-display text-2xl font-semibold leading-tight text-[var(--color-heading)]">
          Workspace
        </h1>
      </div>
    </div>

    <nav class="sidebar-nav">
      <RouterLink
        v-for="item in navigationItems"
        :key="item.label"
        :to="item.to"
        class="sidebar-nav-link"
        active-class="sidebar-nav-link-active"
      >
        <p class="text-sm font-semibold text-[var(--color-heading)]">{{ item.label }}</p>
      </RouterLink>
    </nav>
  </aside>
</template>


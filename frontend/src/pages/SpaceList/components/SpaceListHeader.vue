<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  canRequestRegistration: boolean
  canReviewRegistrations: boolean
}>()
</script>

<template>
  <section class="page-hero desktop-page-panel hero-shell">
    <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker">Spaces</p>
        <h2 class="section-title">Academic collaboration areas</h2>
        <p class="section-copy">
          Spaces group students into managed academic areas for resources, activities, and future
          coursework coordination. The backend still enforces ownership, roster access, and
          student-only membership rules.
        </p>

        <div class="mt-5 flex flex-wrap gap-3">
          <RouterLink
            v-if="canRequestRegistration"
            :to="{ name: 'registration-requests' }"
            class="btn-primary"
          >
            Request space access
          </RouterLink>
          <RouterLink
            v-if="canReviewRegistrations"
            :to="{ name: 'registration-review' }"
            class="btn-secondary"
          >
            Review registration queue
          </RouterLink>
        </div>
      </div>
    </div>

    <div class="mt-8 grid gap-4 xl:grid-cols-3">
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Large primary panel</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Discovery and action live together</p>
      </div>
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Balanced hierarchy</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Primary content spans wider columns</p>
      </div>
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Modular rhythm</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Support panels stack cleanly beside the list</p>
      </div>
    </div>
  </section>
</template>


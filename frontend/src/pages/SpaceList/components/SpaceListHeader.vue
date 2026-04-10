<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  canRequestRegistration: boolean
  canReviewRegistrations: boolean
}>()
</script>

<template>
  <section class="page-hero">
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

      <div class="surface-panel-muted max-w-sm px-5 py-4">
        <p class="text-base font-semibold text-[var(--color-heading)]">Access posture</p>
        <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
          Lecturers manage the spaces they create, admins manage every space, and students see
          only the spaces they are assigned to.
        </p>
      </div>
    </div>
  </section>
</template>


<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  canRequestRegistration: boolean
  canReviewRegistrations: boolean
}>()
</script>

<template>
  <section class="page-hero desktop-page-panel hero-shell">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker">Spaces</p>
        <h2 class="section-title">Academic spaces</h2>

        <div class="mt-4 flex flex-wrap gap-3">
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
  </section>
</template>


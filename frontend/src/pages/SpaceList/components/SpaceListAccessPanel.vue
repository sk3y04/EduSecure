<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  canManageSpaces: boolean
  canRequestRegistration: boolean
  canReviewRegistrations: boolean
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Actions</h3>
    </div>

    <div class="space-y-4">
      <div class="surface-panel-muted px-4 py-4">
        <div class="flex flex-col gap-3">
          <RouterLink
            v-if="canRequestRegistration"
            :to="{ name: 'registration-requests' }"
            class="btn-primary w-full"
          >
            Request space access
          </RouterLink>
          <RouterLink
            v-if="canReviewRegistrations"
            :to="{ name: 'registration-review' }"
            class="btn-secondary w-full"
          >
            Review registration queue
          </RouterLink>
          <p v-if="canManageSpaces" class="inline-note">Create spaces and open them from the list.</p>
        </div>
      </div>
    </div>
  </section>
</template>


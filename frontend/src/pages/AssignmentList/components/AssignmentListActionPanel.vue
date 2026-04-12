<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  isStudent: boolean
  canCreateAssignments: boolean
  canReviewSubmissions: boolean
}>()
</script>

<template>
  <section class="flex flex-col gap-4">
    <div class="surface-panel-muted px-4 py-4">
      <div class="flex flex-col gap-3">
          <RouterLink
            v-if="canCreateAssignments"
            :to="{ name: 'spaces' }"
            class="btn-primary w-full"
          >
            Open spaces
          </RouterLink>
          <RouterLink
            v-if="canReviewSubmissions"
            :to="{ name: 'spaces' }"
            class="btn-secondary w-full"
          >
            Review submissions
          </RouterLink>
          <p v-if="isStudent" class="inline-note">Open an assignment below to submit work.</p>
      </div>
      </div>
  </section>
</template>


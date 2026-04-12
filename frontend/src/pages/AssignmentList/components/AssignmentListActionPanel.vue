<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  isStudent: boolean
  canCreateAssignments: boolean
  canReviewSubmissions: boolean
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Workflow guidance</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Keep role-aware next steps in a narrower support column while the assignment list remains the
        main working surface.
      </p>
    </div>

    <div class="space-y-4">
      <div class="surface-panel-muted px-4 py-4">
        <p class="meta-label">Creation policy</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
          New assignments are created from inside a specific space so coursework remains tied to the
          correct staff-managed roster.
        </p>
      </div>

      <div class="surface-panel-muted px-4 py-4">
        <p class="meta-label">Recommended actions</p>
        <div class="mt-4 flex flex-col gap-3">
          <RouterLink
            v-if="canCreateAssignments"
            :to="{ name: 'spaces' }"
            class="btn-primary w-full"
          >
            Open spaces to create coursework
          </RouterLink>
          <RouterLink
            v-if="canReviewSubmissions"
            :to="{ name: 'spaces' }"
            class="btn-secondary w-full"
          >
            Open a space to review submissions
          </RouterLink>
          <p v-if="isStudent" class="text-sm leading-6 text-[var(--color-text-soft)]">
            Students can open active assignments below, submit work, and revisit their latest
            submissions directly from each card.
          </p>
        </div>
      </div>
    </div>
  </section>
</template>


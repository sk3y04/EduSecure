<script setup lang="ts">
import { RouterLink } from 'vue-router'

defineProps<{
  canManageSpaces: boolean
  canRequestRegistration: boolean
  canReviewRegistrations: boolean
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Access posture</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Keep role-aware actions grouped in one side panel so discovery and approval flows stay close to
        the main list.
      </p>
    </div>

    <div class="space-y-4">
      <div class="surface-panel-muted px-4 py-4">
        <p class="meta-label">Role-aware visibility</p>
        <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
          Students see assigned spaces only, while lecturers and admins can manage ownership-driven
          academic areas.
        </p>
      </div>

      <div class="surface-panel-muted px-4 py-4">
        <p class="meta-label">Available actions</p>
        <div class="mt-4 flex flex-col gap-3">
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
          <p v-if="canManageSpaces" class="text-sm leading-6 text-[var(--color-text-soft)]">
            Staff-managed users can create new spaces in the adjacent panel and open detailed
            management views from the list.
          </p>
        </div>
      </div>
    </div>
  </section>
</template>


<script setup lang="ts">
const props = defineProps<{
  availableRoleLabels: string
}>()
</script>

<template>
  <section class="page-hero">
    <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker">User management</p>
        <h2 class="section-title">Create staff-approved accounts</h2>
        <p class="section-copy">
          Admins can create lecturer and student accounts. Lecturers are limited to creating
          student accounts only. The backend enforces those role boundaries even if the client is
          bypassed.
        </p>
      </div>

      <div class="surface-panel-muted max-w-sm px-5 py-4">
        <p class="text-base font-semibold text-[var(--color-heading)]">Current privileges</p>
        <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
          Available role targets: {{ props.availableRoleLabels }}
        </p>
      </div>
    </div>
  </section>
</template>


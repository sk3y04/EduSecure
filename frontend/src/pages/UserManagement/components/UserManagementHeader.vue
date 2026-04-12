<script setup lang="ts">
const props = defineProps<{
  availableRoleLabels: string
}>()
</script>

<template>
  <section class="page-hero desktop-page-panel hero-shell">
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

    </div>

    <div class="mt-8 grid gap-4 xl:grid-cols-3">
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Wide primary workflow</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Account creation owns the main lane</p>
      </div>
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Current privileges</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">
          Available role targets: {{ props.availableRoleLabels }}
        </p>
      </div>
      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Backend enforcement</p>
        <p class="mt-2 text-base font-semibold text-[var(--color-heading)]">Role boundaries remain server-validated</p>
      </div>
    </div>
  </section>
</template>


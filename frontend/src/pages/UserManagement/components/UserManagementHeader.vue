<script setup lang="ts">
const props = defineProps<{
  availableRoleLabels: string
}>()
</script>

<template>
  <section class="surface-panel p-8">
    <div class="flex flex-col gap-6 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <p class="section-kicker tracking-[0.3em]">User management</p>
        <h2 class="section-title">Create staff-approved accounts</h2>
        <p class="section-copy">
          Admins can create lecturer and student accounts. Lecturers are limited to creating
          student accounts only. The backend enforces those role boundaries even if the client is
          bypassed.
        </p>
      </div>

      <div class="surface-panel-muted max-w-sm px-5 py-4 text-sm text-slate-600">
        <p class="font-semibold text-slate-900">Current privileges</p>
        <p class="mt-2 leading-6">Available role targets: {{ props.availableRoleLabels }}</p>
      </div>
    </div>
  </section>
</template>


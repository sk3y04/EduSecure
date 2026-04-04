<script setup lang="ts">
const props = defineProps<{
  assignmentId: string
}>()
</script>

<template>
  <section class="page-hero">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
    <div class="max-w-3xl">
        <p class="section-kicker">Secure submission</p>
        <h2 class="section-title">Submit content for integrity verification</h2>
        <p class="section-copy">
          The form stays intentionally direct: send the simulated content, let the backend hash and
          sign it, and move straight to the evidence view without decorative distractions.
        </p>
      </div>

      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Assignment reference</p>
        <p class="meta-value break-all mono-meta">{{ props.assignmentId }}</p>
      </div>
    </div>
  </section>
</template>


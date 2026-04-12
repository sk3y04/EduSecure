<script setup lang="ts">
const props = defineProps<{
  assignmentId: string
}>()
</script>

<template>
  <section class="page-hero">
    <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div>
        <p class="section-kicker">Secure submission</p>
        <h2 class="section-title">Submit work</h2>
      </div>

      <div class="surface-panel-muted px-5 py-4">
        <p class="meta-label">Assignment reference</p>
        <p class="meta-value break-all mono-meta">{{ props.assignmentId }}</p>
      </div>
    </div>
  </section>
</template>


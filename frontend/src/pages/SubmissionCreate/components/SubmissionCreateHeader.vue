<script setup lang="ts">
const props = defineProps<{
  assignmentId: string
}>()
</script>

<template>
  <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
    <div class="max-w-3xl">
      <p class="section-kicker tracking-[0.3em]">Secure submission</p>
      <h2 class="section-title">Submit content for integrity verification</h2>
      <p class="section-copy">
        The form stays intentionally direct: send the simulated content, let the backend hash and
        sign it, and move straight to the evidence view without decorative distractions.
      </p>
    </div>

    <div class="surface-panel-muted px-5 py-4 text-sm text-slate-600">
      <p class="font-semibold text-slate-900">Assignment reference</p>
      <p class="mt-2 break-all font-mono text-xs text-slate-700">{{ props.assignmentId }}</p>
    </div>
  </div>
</template>


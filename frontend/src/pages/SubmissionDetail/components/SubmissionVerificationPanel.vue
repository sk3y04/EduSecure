<script setup lang="ts">
import type { SubmissionResponse } from '@/types/submission'

const props = defineProps<{
  submission: SubmissionResponse
}>()
</script>

<template>
  <section class="surface-panel p-8">
    <h3 class="text-xl font-semibold text-slate-900">Verification summary</h3>
    <p class="mt-2 text-sm leading-6 text-slate-600">{{ props.submission.verificationMessage }}</p>

    <dl class="mt-6 space-y-4">
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Hash digest</dt>
        <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ props.submission.hashDigest }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Signature algorithm</dt>
        <dd class="mt-2 text-sm text-slate-900">{{ props.submission.signatureAlgorithm }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Digital signature</dt>
        <dd class="mt-2 break-all font-mono text-sm text-slate-900">{{ props.submission.digitalSignature }}</dd>
      </div>
    </dl>
  </section>
</template>


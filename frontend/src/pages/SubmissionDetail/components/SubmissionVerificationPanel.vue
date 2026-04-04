<script setup lang="ts">
import type { SubmissionResponse } from '@/types/submission'

const props = defineProps<{
  submission: SubmissionResponse
}>()
</script>

<template>
  <section class="page-section">
    <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Verification summary</h3>
    <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">{{ props.submission.verificationMessage }}</p>

    <dl class="stats-grid mt-6">
      <div class="stat-card bg-[var(--color-surface-offset)]">
        <dt class="meta-label">Hash digest</dt>
        <dd class="meta-value break-all mono-meta">{{ props.submission.hashDigest }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Signature algorithm</dt>
        <dd class="meta-value">{{ props.submission.signatureAlgorithm }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Digital signature</dt>
        <dd class="meta-value break-all mono-meta">{{ props.submission.digitalSignature }}</dd>
      </div>
    </dl>
  </section>
</template>


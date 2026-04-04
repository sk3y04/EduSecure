<script setup lang="ts">
import type { MfaPendingChallenge } from '@/types/auth'

const props = defineProps<{
  challenge: MfaPendingChallenge | null
  expiresAtLabel: string
}>()
</script>

<template>
  <dl class="stats-grid mt-8">
    <div class="stat-card">
      <dt class="meta-label">Method</dt>
      <dd class="meta-value font-medium">{{ props.challenge?.mfaMethod }}</dd>
    </div>
    <div class="stat-card">
      <dt class="meta-label">Attempts remaining</dt>
      <dd class="meta-value font-medium">{{ props.challenge?.remainingAttempts ?? 'Unknown' }}</dd>
    </div>
    <div class="stat-card bg-[var(--color-surface-offset)]">
      <dt class="meta-label">Expires</dt>
      <dd class="meta-value font-medium">{{ props.expiresAtLabel }}</dd>
    </div>
  </dl>
</template>



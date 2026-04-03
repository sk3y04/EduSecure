<script setup lang="ts">
import type { MfaPendingChallenge } from '@/types/auth'

const props = defineProps<{
  challenge: MfaPendingChallenge | null
  expiresAtLabel: string
}>()
</script>

<template>
  <div class="mt-6 grid gap-4 md:grid-cols-3">
    <div class="data-card">
      <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Method</p>
      <p class="mt-2 text-sm font-semibold text-slate-900">{{ props.challenge?.mfaMethod }}</p>
    </div>
    <div class="data-card">
      <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Attempts remaining</p>
      <p class="mt-2 text-sm font-semibold text-slate-900">
        {{ props.challenge?.remainingAttempts ?? 'Unknown' }}
      </p>
    </div>
    <div class="data-card">
      <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Expires</p>
      <p class="mt-2 text-sm font-semibold text-slate-900">{{ props.expiresAtLabel }}</p>
    </div>
  </div>
</template>



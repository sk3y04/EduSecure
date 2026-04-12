<script setup lang="ts">
import type { MfaStatusResponse } from '@/types/auth'

const props = defineProps<{
  status: MfaStatusResponse | null
  enabledAtLabel: string
  isSettingUp: boolean
}>()

const emit = defineEmits<{
  (e: 'setup'): void
}>()
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Current MFA state</h3>

    <dl class="stats-grid mt-6 flex-1">
      <div class="stat-card bg-[var(--color-surface-offset)]">
        <dt class="meta-label">Enabled</dt>
        <dd class="meta-value font-medium">
          {{ props.status?.mfaEnabled ? 'Yes' : 'No' }}
        </dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Method</dt>
        <dd class="meta-value font-medium">
          {{ props.status?.mfaMethod ?? 'Not configured' }}
        </dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Enabled at</dt>
        <dd class="meta-value font-medium">{{ props.enabledAtLabel }}</dd>
      </div>
      <div class="stat-card">
        <dt class="meta-label">Recovery codes remaining</dt>
        <dd class="meta-value font-medium">
          {{ props.status?.recoveryCodesRemaining ?? 0 }}
        </dd>
      </div>
    </dl>

    <button
      v-if="!props.status?.mfaEnabled"
      type="button"
      class="btn-primary mt-6"
      :disabled="props.isSettingUp"
      @click="emit('setup')"
    >
      {{ props.isSettingUp ? 'Generating setup…' : 'Generate MFA setup' }}
    </button>
  </section>
</template>


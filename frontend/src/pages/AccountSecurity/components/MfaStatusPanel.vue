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
  <section class="surface-panel p-8">
    <h3 class="text-xl font-semibold text-slate-900">Current MFA state</h3>

    <dl class="mt-6 space-y-4">
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Enabled</dt>
        <dd class="mt-2 text-sm font-semibold text-slate-900">
          {{ props.status?.mfaEnabled ? 'Yes' : 'No' }}
        </dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Method</dt>
        <dd class="mt-2 text-sm font-semibold text-slate-900">
          {{ props.status?.mfaMethod ?? 'Not configured' }}
        </dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Enabled at</dt>
        <dd class="mt-2 text-sm font-semibold text-slate-900">{{ props.enabledAtLabel }}</dd>
      </div>
      <div class="data-card">
        <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Recovery codes remaining</dt>
        <dd class="mt-2 text-sm font-semibold text-slate-900">
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


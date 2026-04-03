<script setup lang="ts">
import { ref } from 'vue'

import type { MfaEnableResponse, MfaSetupResponse } from '@/types/auth'

const props = defineProps<{
  setupData: MfaSetupResponse | null
  enableResult: MfaEnableResponse | null
  isEnabling: boolean
}>()

const emit = defineEmits<{
  (e: 'enable', code: string): void
}>()

const verificationCode = ref('')

function handleEnable() {
  emit('enable', verificationCode.value)
  verificationCode.value = ''
}
</script>

<template>
  <section class="surface-panel p-8">
    <div class="border-b border-slate-200 pb-5">
      <h3 class="text-xl font-semibold text-slate-900">Enable MFA</h3>
      <p class="mt-2 text-sm leading-6 text-slate-600">
        Generate the TOTP secret, register it in an authenticator app, then verify the first
        code to complete enrollment.
      </p>
    </div>

    <div v-if="props.setupData" class="mt-6 space-y-4">
      <div class="data-card">
        <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Manual entry key</p>
        <p class="mt-2 break-all font-mono text-sm text-slate-900">
          {{ props.setupData.manualEntryKey }}
        </p>
      </div>
      <div class="data-card">
        <p class="text-xs uppercase tracking-[0.25em] text-slate-500">otpauth URI</p>
        <p class="mt-2 break-all font-mono text-sm text-slate-900">
          {{ props.setupData.otpauthUri }}
        </p>
      </div>

      <form class="space-y-4" @submit.prevent="handleEnable">
        <label class="block">
          <span class="field-label">First verification code</span>
          <input
            v-model="verificationCode"
            type="text"
            inputmode="numeric"
            required
            class="form-input tracking-[0.35em]"
            placeholder="123456"
          />
        </label>
        <button type="submit" class="btn-primary" :disabled="props.isEnabling">
          {{ props.isEnabling ? 'Enabling…' : 'Enable MFA' }}
        </button>
      </form>
    </div>

    <div v-else class="empty-state mt-6">
      Generate setup material to begin TOTP enrollment.
    </div>

    <div
      v-if="props.enableResult?.recoveryCodes?.length"
      class="mt-6 rounded-sm border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
    >
      <p class="font-semibold">Recovery codes</p>
      <ul class="mt-3 space-y-2 font-mono text-xs">
        <li v-for="code in props.enableResult.recoveryCodes" :key="code">{{ code }}</li>
      </ul>
    </div>
  </section>
</template>


<script setup lang="ts">
import * as QRCode from 'qrcode'
import { computed, ref, watch } from 'vue'

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
const qrCodeDataUrl = ref('')
const qrCodeRenderFailed = ref(false)

const isEnrollmentComplete = computed(() => Boolean(props.enableResult?.mfaEnabled))
const setupOtpauthUri = computed(() => props.setupData?.otpauthUri?.trim() ?? '')
const qrFallbackMessage = computed(() => {
  if (!props.setupData) return null
  if (!setupOtpauthUri.value) {
    return 'QR setup is unavailable right now. Use the manual entry key below to finish enrollment.'
  }
  if (qrCodeRenderFailed.value) {
    return 'We could not render the QR code in this browser. Use the manual entry key below to finish enrollment.'
  }
  return null
})

let qrRenderRequestId = 0

watch(
  () => props.setupData?.otpauthUri?.trim() ?? '',
  async (otpauthUri) => {
    const requestId = ++qrRenderRequestId

    qrCodeDataUrl.value = ''
    qrCodeRenderFailed.value = false

    if (!otpauthUri || isEnrollmentComplete.value) {
      return
    }

    try {
      const dataUrl = await QRCode.toDataURL(otpauthUri, {
        errorCorrectionLevel: 'M',
        margin: 2,
        width: 240,
      })

      if (requestId !== qrRenderRequestId) {
        return
      }

      qrCodeDataUrl.value = dataUrl
    } catch {
      if (requestId !== qrRenderRequestId) {
        return
      }

      qrCodeRenderFailed.value = true
    }
  },
  { immediate: true },
)

function handleEnable() {
  emit('enable', verificationCode.value.trim())
  verificationCode.value = ''
}
</script>

<template>
  <section class="page-section desktop-page-panel flex min-h-[32rem] flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Enable MFA</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Generate the TOTP secret, register it in an authenticator app, then verify the first
        code to complete enrollment.
      </p>
    </div>

    <div v-if="props.setupData && !isEnrollmentComplete" class="mt-6 min-h-0 flex-1 space-y-4 overflow-y-auto pr-1">
      <div class="data-card">
        <div class="grid gap-5 lg:grid-cols-[minmax(0,16rem)_1fr] lg:items-start">
          <div class="rounded-xl border border-black/10 bg-white p-4 shadow-sm">
            <img
              v-if="qrCodeDataUrl"
              :src="qrCodeDataUrl"
              alt="Scan this QR code with your authenticator app"
              class="mx-auto block h-auto w-full max-w-60"
            />
            <div
              v-else
              class="flex min-h-60 items-center justify-center rounded-lg border border-dashed border-black/10 bg-[var(--color-surface)] px-4 py-6 text-center text-sm text-[var(--color-text-soft)]"
            >
              {{ qrFallbackMessage ?? 'Preparing your authenticator QR code…' }}
            </div>
          </div>

          <div class="space-y-4">
            <div>
              <p class="meta-label">Authenticator app setup</p>
              <p class="meta-value mt-1 text-sm leading-6 text-[var(--color-text-soft)]">
                Scan this QR code with your authenticator app, then enter the first 6-digit code
                it generates to finish enrollment.
              </p>
            </div>

            <div v-if="qrFallbackMessage" class="rounded-lg border border-black/10 bg-[var(--color-surface)] p-4 text-sm text-[var(--color-text-soft)]">
              {{ qrFallbackMessage }}
            </div>

            <div class="rounded-lg border border-black/10 bg-[var(--color-surface)] p-4">
              <p class="meta-label">Manual entry key</p>
              <p class="meta-value break-all mono-meta">
                {{ props.setupData.manualEntryKey }}
              </p>
              <p class="mt-3 text-sm leading-6 text-[var(--color-text-soft)]">
                If your app cannot scan a QR code, choose the manual entry option and paste this
                key instead.
              </p>
            </div>

            <details v-if="setupOtpauthUri" class="rounded-lg border border-black/10 bg-[var(--color-surface)] p-4">
              <summary class="cursor-pointer text-sm font-medium text-[var(--color-heading)]">
                Advanced setup details
              </summary>
              <p class="mt-3 text-sm leading-6 text-[var(--color-text-soft)]">
                Need the raw enrollment URI for troubleshooting or advanced import?
              </p>
              <p class="meta-value break-all mono-meta">
                {{ setupOtpauthUri }}
              </p>
            </details>
          </div>
        </div>
      </div>

      <form class="data-card space-y-4" @submit.prevent="handleEnable">
        <label class="block">
          <span class="field-label">First verification code</span>
          <input
            v-model="verificationCode"
            type="text"
            inputmode="numeric"
            autocomplete="one-time-code"
            maxlength="6"
            pattern="[0-9]{6}"
            required
            class="form-input font-mono text-lg"
            placeholder="123456"
          />
        </label>
        <button type="submit" class="btn-primary" :disabled="props.isEnabling">
          {{ props.isEnabling ? 'Enabling…' : 'Enable MFA' }}
        </button>
      </form>
    </div>

    <div v-else-if="props.enableResult?.recoveryCodes?.length" class="empty-state mt-6 flex-1">
      MFA is enabled. Save the recovery codes below before leaving this page.
    </div>

    <div v-else class="empty-state mt-6 flex-1">
      Generate setup material to begin TOTP enrollment.
    </div>

    <div
      v-if="props.enableResult?.recoveryCodes?.length"
      class="mt-6 rounded-xl border border-black/10 bg-[var(--color-success-soft)] p-4 text-base text-[var(--color-heading)]"
    >
      <p class="font-semibold">Recovery codes</p>
      <ul class="mt-3 space-y-2 font-mono text-sm">
        <li v-for="code in props.enableResult.recoveryCodes" :key="code">{{ code }}</li>
      </ul>
    </div>
  </section>
</template>


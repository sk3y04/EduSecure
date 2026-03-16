<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'

import { authService } from '@/services/auth'
import { extractErrorMessage } from '@/services/http'
import type { MfaEnableResponse, MfaSetupResponse, MfaStatusResponse } from '@/types/auth'

const isLoading = ref(true)
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const status = ref<MfaStatusResponse | null>(null)
const setupData = ref<MfaSetupResponse | null>(null)
const enableResult = ref<MfaEnableResponse | null>(null)

const enableForm = reactive({
  verificationCode: '',
})

const disableForm = reactive({
  password: '',
  verificationCode: '',
})

const isSettingUp = ref(false)
const isEnabling = ref(false)
const isDisabling = ref(false)

const enabledAtLabel = computed(() => {
  if (!status.value?.enabledAt) {
    return 'Not enabled'
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(new Date(status.value.enabledAt))
})

function resetFeedback() {
  errorMessage.value = null
  successMessage.value = null
}

async function loadStatus() {
  isLoading.value = true
  resetFeedback()

  try {
    status.value = await authService.getMfaStatus()
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function handleSetup() {
  isSettingUp.value = true
  resetFeedback()

  try {
    setupData.value = await authService.setupMfa()
    enableResult.value = null
    successMessage.value = 'TOTP enrollment material generated. Verify the first code to enable MFA.'
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isSettingUp.value = false
  }
}

async function handleEnable() {
  isEnabling.value = true
  resetFeedback()

  try {
    enableResult.value = await authService.enableMfa({
      verificationCode: enableForm.verificationCode,
    })
    enableForm.verificationCode = ''
    await loadStatus()
    successMessage.value = 'MFA is now enabled. Store the recovery codes securely.'
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isEnabling.value = false
  }
}

async function handleDisable() {
  isDisabling.value = true
  resetFeedback()

  try {
    await authService.disableMfa({
      password: disableForm.password,
      verificationCode: disableForm.verificationCode,
    })
    disableForm.password = ''
    disableForm.verificationCode = ''
    setupData.value = null
    enableResult.value = null
    await loadStatus()
    successMessage.value = 'MFA disabled successfully.'
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isDisabling.value = false
  }
}

onMounted(() => {
  void loadStatus()
})
</script>

<template>
  <div class="space-y-8">
    <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
      <p class="text-sm font-semibold uppercase tracking-[0.3em] text-brand-500">Account security</p>
      <h2 class="mt-3 text-3xl font-semibold text-white">Manage optional TOTP MFA</h2>
      <p class="mt-3 max-w-3xl text-sm leading-7 text-slate-400">
        This page surfaces the MFA slice already implemented in the backend: status lookup, setup,
        first-code verification, and secure disable flow that requires password plus second factor.
      </p>
    </section>

    <div
      v-if="errorMessage"
      class="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-100"
    >
      {{ errorMessage }}
    </div>
    <div
      v-if="successMessage"
      class="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 px-4 py-3 text-sm text-emerald-100"
    >
      {{ successMessage }}
    </div>

    <div v-if="isLoading" class="rounded-3xl border border-dashed border-slate-700 p-8 text-center text-sm text-slate-400">
      Loading MFA status…
    </div>

    <template v-else>
      <div class="grid gap-8 xl:grid-cols-[0.95fr_1.05fr]">
        <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
          <h3 class="text-xl font-semibold text-white">Current MFA state</h3>

          <dl class="mt-6 space-y-4">
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Enabled</dt>
              <dd class="mt-2 text-sm font-semibold text-white">
                {{ status?.mfaEnabled ? 'Yes' : 'No' }}
              </dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Method</dt>
              <dd class="mt-2 text-sm font-semibold text-white">{{ status?.mfaMethod ?? 'Not configured' }}</dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Enabled at</dt>
              <dd class="mt-2 text-sm font-semibold text-white">{{ enabledAtLabel }}</dd>
            </div>
            <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Recovery codes remaining</dt>
              <dd class="mt-2 text-sm font-semibold text-white">{{ status?.recoveryCodesRemaining ?? 0 }}</dd>
            </div>
          </dl>

          <button
            v-if="!status?.mfaEnabled"
            type="button"
            class="mt-6 inline-flex items-center rounded-2xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="isSettingUp"
            @click="handleSetup"
          >
            {{ isSettingUp ? 'Generating setup…' : 'Generate MFA setup' }}
          </button>
        </section>

        <section class="rounded-3xl border border-slate-800 bg-slate-900/80 p-8 shadow-panel">
          <template v-if="!status?.mfaEnabled">
            <h3 class="text-xl font-semibold text-white">Enable MFA</h3>
            <p class="mt-2 text-sm leading-6 text-slate-400">
              Generate the TOTP secret, scan it into an authenticator app, then submit the first code
              to complete enrollment.
            </p>

            <div v-if="setupData" class="mt-6 space-y-4">
              <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
                <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Manual entry key</p>
                <p class="mt-2 break-all font-mono text-sm text-slate-200">{{ setupData.manualEntryKey }}</p>
              </div>
              <div class="rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
                <p class="text-xs uppercase tracking-[0.25em] text-slate-500">otpauth URI</p>
                <p class="mt-2 break-all font-mono text-sm text-slate-200">{{ setupData.otpauthUri }}</p>
              </div>

              <form class="space-y-4" @submit.prevent="handleEnable">
                <label class="block">
                  <span class="mb-2 block text-sm font-medium text-slate-200">First verification code</span>
                  <input
                    v-model="enableForm.verificationCode"
                    type="text"
                    inputmode="numeric"
                    required
                    class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm tracking-[0.35em] text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                    placeholder="123456"
                  />
                </label>
                <button
                  type="submit"
                  class="inline-flex items-center rounded-2xl bg-brand-600 px-5 py-3 text-sm font-semibold text-white transition hover:bg-brand-500 disabled:cursor-not-allowed disabled:opacity-60"
                  :disabled="isEnabling"
                >
                  {{ isEnabling ? 'Enabling…' : 'Enable MFA' }}
                </button>
              </form>
            </div>

            <div v-else class="mt-6 rounded-2xl border border-dashed border-slate-700 p-6 text-sm text-slate-400">
              Generate setup material to begin TOTP enrollment.
            </div>

            <div v-if="enableResult?.recoveryCodes?.length" class="mt-6 rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-sm text-emerald-100">
              <p class="font-semibold">Recovery codes</p>
              <ul class="mt-3 space-y-2 font-mono text-xs">
                <li v-for="code in enableResult.recoveryCodes" :key="code">{{ code }}</li>
              </ul>
            </div>
          </template>

          <template v-else>
            <h3 class="text-xl font-semibold text-white">Disable MFA</h3>
            <p class="mt-2 text-sm leading-6 text-slate-400">
              The backend requires both the current password and a valid TOTP or recovery code to
              disable MFA. This protects the account from casual downgrade attacks.
            </p>

            <form class="mt-6 space-y-4" @submit.prevent="handleDisable">
              <label class="block">
                <span class="mb-2 block text-sm font-medium text-slate-200">Current password</span>
                <input
                  v-model="disableForm.password"
                  type="password"
                  required
                  class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                />
              </label>
              <label class="block">
                <span class="mb-2 block text-sm font-medium text-slate-200">TOTP or recovery code</span>
                <input
                  v-model="disableForm.verificationCode"
                  type="text"
                  required
                  class="w-full rounded-2xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-white outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-500/30"
                />
              </label>
              <button
                type="submit"
                class="inline-flex items-center rounded-2xl border border-rose-500/40 bg-rose-500/10 px-5 py-3 text-sm font-semibold text-rose-100 transition hover:bg-rose-500/20 disabled:cursor-not-allowed disabled:opacity-60"
                :disabled="isDisabling"
              >
                {{ isDisabling ? 'Disabling…' : 'Disable MFA' }}
              </button>
            </form>
          </template>
        </section>
      </div>
    </template>
  </div>
</template>


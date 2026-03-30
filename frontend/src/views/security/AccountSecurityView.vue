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
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
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
  <div class="space-y-6">
    <section class="surface-panel p-8">
      <p class="section-kicker tracking-[0.3em]">Account security</p>
      <h2 class="section-title">Manage optional TOTP MFA</h2>
      <p class="section-copy max-w-3xl">
        Review MFA status, generate enrollment material, complete the first-code verification flow,
        and disable MFA only through the stronger password-plus-code requirement.
      </p>
    </section>

    <div v-if="errorMessage" class="alert-error">
      {{ errorMessage }}
    </div>
    <div v-if="successMessage" class="alert-success">
      {{ successMessage }}
    </div>

    <div v-if="isLoading" class="empty-state">
      Loading MFA status…
    </div>

    <template v-else>
      <div class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <section class="surface-panel p-8">
          <h3 class="text-xl font-semibold text-slate-900">Current MFA state</h3>

          <dl class="mt-6 space-y-4">
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Enabled</dt>
              <dd class="mt-2 text-sm font-semibold text-slate-900">
                {{ status?.mfaEnabled ? 'Yes' : 'No' }}
              </dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Method</dt>
              <dd class="mt-2 text-sm font-semibold text-slate-900">{{ status?.mfaMethod ?? 'Not configured' }}</dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Enabled at</dt>
              <dd class="mt-2 text-sm font-semibold text-slate-900">{{ enabledAtLabel }}</dd>
            </div>
            <div class="data-card">
              <dt class="text-xs uppercase tracking-[0.25em] text-slate-500">Recovery codes remaining</dt>
              <dd class="mt-2 text-sm font-semibold text-slate-900">{{ status?.recoveryCodesRemaining ?? 0 }}</dd>
            </div>
          </dl>

          <button
            v-if="!status?.mfaEnabled"
            type="button"
            class="btn-primary mt-6"
            :disabled="isSettingUp"
            @click="handleSetup"
          >
            {{ isSettingUp ? 'Generating setup…' : 'Generate MFA setup' }}
          </button>
        </section>

        <section class="surface-panel p-8">
          <template v-if="!status?.mfaEnabled">
            <div class="border-b border-slate-200 pb-5">
              <h3 class="text-xl font-semibold text-slate-900">Enable MFA</h3>
              <p class="mt-2 text-sm leading-6 text-slate-600">
                Generate the TOTP secret, register it in an authenticator app, then verify the first
                code to complete enrollment.
              </p>
            </div>

            <div v-if="setupData" class="mt-6 space-y-4">
              <div class="data-card">
                <p class="text-xs uppercase tracking-[0.25em] text-slate-500">Manual entry key</p>
                <p class="mt-2 break-all font-mono text-sm text-slate-900">{{ setupData.manualEntryKey }}</p>
              </div>
              <div class="data-card">
                <p class="text-xs uppercase tracking-[0.25em] text-slate-500">otpauth URI</p>
                <p class="mt-2 break-all font-mono text-sm text-slate-900">{{ setupData.otpauthUri }}</p>
              </div>

              <form class="space-y-4" @submit.prevent="handleEnable">
                <label class="block">
                  <span class="field-label">First verification code</span>
                  <input
                    v-model="enableForm.verificationCode"
                    type="text"
                    inputmode="numeric"
                    required
                    class="form-input tracking-[0.35em]"
                    placeholder="123456"
                  />
                </label>
                <button type="submit" class="btn-primary" :disabled="isEnabling">
                  {{ isEnabling ? 'Enabling…' : 'Enable MFA' }}
                </button>
              </form>
            </div>

            <div v-else class="empty-state mt-6">
              Generate setup material to begin TOTP enrollment.
            </div>

            <div
              v-if="enableResult?.recoveryCodes?.length"
              class="mt-6 rounded-sm border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-800"
            >
              <p class="font-semibold">Recovery codes</p>
              <ul class="mt-3 space-y-2 font-mono text-xs">
                <li v-for="code in enableResult.recoveryCodes" :key="code">{{ code }}</li>
              </ul>
            </div>
          </template>

          <template v-else>
            <div class="border-b border-slate-200 pb-5">
              <h3 class="text-xl font-semibold text-slate-900">Disable MFA</h3>
              <p class="mt-2 text-sm leading-6 text-slate-600">
                The backend requires both the current password and a valid TOTP or recovery code to
                disable MFA, which helps prevent easy downgrade attacks.
              </p>
            </div>

            <form class="mt-6 space-y-4" @submit.prevent="handleDisable">
              <label class="block">
                <span class="field-label">Current password</span>
                <input v-model="disableForm.password" type="password" required class="form-input" />
              </label>
              <label class="block">
                <span class="field-label">TOTP or recovery code</span>
                <input v-model="disableForm.verificationCode" type="text" required class="form-input" />
              </label>
              <button type="submit" class="btn-danger" :disabled="isDisabling">
                {{ isDisabling ? 'Disabling…' : 'Disable MFA' }}
              </button>
            </form>
          </template>
        </section>
      </div>
    </template>
  </div>
</template>


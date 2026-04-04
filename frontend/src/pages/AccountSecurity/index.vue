<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { authService } from '@/services/auth'
import { extractErrorMessage } from '@/services/http'
import type { MfaEnableResponse, MfaSetupResponse, MfaStatusResponse } from '@/types/auth'
import { MfaDisablePanel, MfaEnablePanel, MfaStatusPanel } from './components'

const isLoading = ref(true)
const errorMessage = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const status = ref<MfaStatusResponse | null>(null)
const setupData = ref<MfaSetupResponse | null>(null)
const enableResult = ref<MfaEnableResponse | null>(null)

const isSettingUp = ref(false)
const isEnabling = ref(false)
const isDisabling = ref(false)

const enabledAtLabel = computed(() => {
  if (!status.value?.enabledAt) return 'Not enabled'
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

async function handleEnable(code: string) {
  isEnabling.value = true
  resetFeedback()

  try {
    enableResult.value = await authService.enableMfa({ verificationCode: code })
    await loadStatus()
    successMessage.value = 'MFA is now enabled. Store the recovery codes securely.'
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isEnabling.value = false
  }
}

async function handleDisable(payload: { password: string; verificationCode: string }) {
  isDisabling.value = true
  resetFeedback()

  try {
    await authService.disableMfa(payload)
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
    <section class="page-hero">
      <p class="section-kicker">Account security</p>
      <h2 class="section-title">Manage optional TOTP MFA</h2>
      <p class="section-copy max-w-3xl">
        Review MFA status, generate enrollment material, complete the first-code verification flow,
        and disable MFA only through the stronger password-plus-code requirement.
      </p>
    </section>

    <div v-if="errorMessage" class="alert-error">{{ errorMessage }}</div>
    <div v-if="successMessage" class="alert-success">{{ successMessage }}</div>

    <div v-if="isLoading" class="empty-state">Loading MFA status…</div>

    <template v-else>
      <div class="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
        <MfaStatusPanel
          :status="status"
          :enabled-at-label="enabledAtLabel"
          :is-setting-up="isSettingUp"
          @setup="handleSetup"
        />

        <div class="space-y-6">
          <MfaEnablePanel
            v-if="!status?.mfaEnabled || enableResult?.recoveryCodes?.length"
            :setup-data="setupData"
            :enable-result="enableResult"
            :is-enabling="isEnabling"
            @enable="handleEnable"
          />

          <MfaDisablePanel
            v-if="status?.mfaEnabled"
            :is-disabling="isDisabling"
            @disable="handleDisable"
          />
        </div>
      </div>
    </template>
  </div>
</template>


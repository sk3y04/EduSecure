<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import ExpandablePanel from '@/components/ui/ExpandablePanel.vue'
import { authService } from '@/services/auth'
import { extractErrorMessage } from '@/services/http'
import type { MfaEnableResponse, MfaSetupResponse, MfaStatusResponse } from '@/types/auth'
import { AccountSecurityHeader, MfaDisablePanel, MfaEnablePanel, MfaStatusPanel } from './components'

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
  <div class="desktop-page-grid">
    <AccountSecurityHeader class="xl:col-span-12" />

    <div v-if="errorMessage" class="alert-error xl:col-span-12">{{ errorMessage }}</div>
    <div v-if="successMessage" class="alert-success xl:col-span-12">{{ successMessage }}</div>

    <div v-if="isLoading" class="empty-state xl:col-span-12">Loading MFA status…</div>

    <template v-else>
      <MfaEnablePanel
        v-if="!status?.mfaEnabled || enableResult?.recoveryCodes?.length"
        class="xl:col-span-8"
        :setup-data="setupData"
        :enable-result="enableResult"
        :is-enabling="isEnabling"
        @enable="handleEnable"
      />

      <section
        v-else
        class="page-section desktop-page-panel panel-shell-spread xl:col-span-8"
      >
        <div>
          <div class="panel-header">
            <h3 class="panel-title">Enrollment complete</h3>
          </div>

          <div class="empty-state">
            No additional enrollment steps are required right now.
          </div>
        </div>

      </section>

      <MfaDisablePanel
        v-if="status?.mfaEnabled"
        class="xl:col-span-4"
        :is-disabling="isDisabling"
        @disable="handleDisable"
      />

      <div class="xl:col-span-12">
        <ExpandablePanel title="MFA details" summary="Current status and recovery posture">
          <MfaStatusPanel
            :status="status"
            :enabled-at-label="enabledAtLabel"
            :is-setting-up="isSettingUp"
            @setup="handleSetup"
          />
        </ExpandablePanel>
      </div>
    </template>
  </div>
</template>


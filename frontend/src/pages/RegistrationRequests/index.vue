<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { extractErrorMessage } from '@/services/http'
import { registrationRequestsService } from '@/services/registrationRequests'
import type { StudentRegistrationRequest } from '@/types/registration'
import {
  RegistrationRequestsHeader,
  RegistrationRequestsMetricsPanel,
} from './components'

const requests = ref<StudentRegistrationRequest[]>([])
const isLoading = ref(true)
const isSubmitting = ref(false)
const cancellingRequestId = ref<string | null>(null)
const loadError = ref<string | null>(null)
const formError = ref<string | null>(null)
const formSuccess = ref<string | null>(null)
const actionError = ref<string | null>(null)

const spaceCode = ref('')
const requestMessage = ref('')

const hasRequests = computed(() => requests.value.length > 0)
const pendingRequests = computed(() => requests.value.filter((request) => request.status === 'PENDING').length)
const reviewedRequests = computed(() => requests.value.filter((request) => request.reviewedAt).length)
const requestsWithNotes = computed(() => requests.value.filter((request) => Boolean(request.reviewNote?.trim())).length)

async function loadRequests() {
  isLoading.value = true
  loadError.value = null

  try {
    requests.value = await registrationRequestsService.listMine()
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function handleSubmit() {
  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null
  actionError.value = null

  try {
    const created = await registrationRequestsService.create({
      spaceCode: spaceCode.value,
      requestMessage: requestMessage.value,
    })

    requests.value = [created, ...requests.value]
    formSuccess.value = `Request submitted for ${created.spaceCode}.`
    spaceCode.value = ''
    requestMessage.value = ''
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

async function handleCancel(requestId: string) {
  cancellingRequestId.value = requestId
  actionError.value = null
  formSuccess.value = null

  try {
    const updated = await registrationRequestsService.cancel(requestId)
    requests.value = requests.value.map((request) => (request.id === requestId ? updated : request))
  } catch (error) {
    actionError.value = extractErrorMessage(error)
  } finally {
    cancellingRequestId.value = null
  }
}

function formatDate(value: string | null) {
  if (!value) {
    return 'Not reviewed yet'
  }

  return new Date(value).toLocaleString()
}

onMounted(() => {
  void loadRequests()
})
</script>

<template>
  <section class="desktop-page-grid">
    <RegistrationRequestsHeader class="xl:col-span-8 xl:row-span-2" />

    <RegistrationRequestsMetricsPanel
      class="xl:col-span-4"
      :total-requests="requests.length"
      :pending-requests="pendingRequests"
      :reviewed-requests="reviewedRequests"
      :requests-with-notes="requestsWithNotes"
    />

    <section class="page-section desktop-page-panel panel-shell xl:col-span-4 xl:row-span-3">
      <div class="panel-header">
        <h3 class="panel-title">Submit request</h3>
        <p class="panel-copy">
          Use a valid space code and optional reviewer context to request membership from staff.
        </p>
      </div>

      <div class="grid gap-4 lg:grid-cols-[0.7fr_1fr]">
        <label class="space-y-2">
          <span class="field-label">Space code</span>
          <input
            v-model="spaceCode"
            type="text"
            class="form-input uppercase"
            maxlength="32"
            placeholder="CRYPTO-A"
          >
        </label>

        <label class="space-y-2">
          <span class="field-label">Message for reviewer</span>
          <textarea
            v-model="requestMessage"
            class="form-input min-h-28"
            maxlength="500"
            placeholder="Optional context for the lecturer or admin reviewing your request."
          />
        </label>
      </div>

      <div class="mt-4 flex flex-wrap items-center gap-3">
        <button type="button" class="btn-primary" :disabled="isSubmitting" @click="handleSubmit">
          {{ isSubmitting ? 'Submitting…' : 'Submit request' }}
        </button>
        <button type="button" class="btn-secondary" @click="loadRequests">Refresh history</button>
      </div>

      <p v-if="formError" class="alert-error mt-4">{{ formError }}</p>
      <p v-else-if="formSuccess" class="alert-success mt-4">{{ formSuccess }}</p>
    </section>

    <section class="page-section desktop-page-panel panel-shell panel-shell-min-34 xl:col-span-8 xl:row-span-4">
      <div class="panel-header-split !mb-4 !pb-0 !border-b-0 items-start">
        <div>
          <h3 class="panel-title">Your request history</h3>
          <p class="panel-copy">
            Requests are listed newest first. Only pending requests can be cancelled.
          </p>
        </div>
      </div>

      <p v-if="actionError" class="alert-error mb-4 mt-4">{{ actionError }}</p>

      <div class="panel-body mt-4">
        <div v-if="loadError" class="alert-error">{{ loadError }}</div>
        <div v-else-if="isLoading" class="empty-state">Loading request history…</div>
        <div v-else-if="!hasRequests" class="empty-state">No registration requests yet.</div>

        <div v-else class="panel-scroll-list h-full">
          <article
            v-for="request in requests"
            :key="request.id"
            class="record-card"
          >
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div class="space-y-3">
                <div>
                  <p class="text-sm font-medium uppercase tracking-[0.2em] text-[var(--color-text-soft)]">
                    {{ request.spaceCode }}
                  </p>
                  <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                    {{ request.spaceName }}
                  </h4>
                </div>

                <div class="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                  <span class="status-pill status-pill-neutral">{{ request.status }}</span>
                  <span>Submitted {{ formatDate(request.requestedAt) }}</span>
                  <span>Reviewed {{ formatDate(request.reviewedAt) }}</span>
                </div>

                <p v-if="request.requestMessage" class="text-base leading-7 text-[var(--color-text)]">
                  {{ request.requestMessage }}
                </p>

                <p v-if="request.reviewNote" class="text-sm leading-6 text-[var(--color-text-soft)]">
                  Review note: {{ request.reviewNote }}
                </p>
              </div>

              <button
                v-if="request.status === 'PENDING'"
                type="button"
                class="btn-secondary self-start"
                :disabled="cancellingRequestId === request.id"
                @click="handleCancel(request.id)"
              >
                {{ cancellingRequestId === request.id ? 'Cancelling…' : 'Cancel request' }}
              </button>
            </div>
          </article>
        </div>
      </div>
    </section>
  </section>
</template>
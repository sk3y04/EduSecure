<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'

import { extractErrorMessage } from '@/services/http'
import { registrationRequestsService } from '@/services/registrationRequests'
import type { StudentRegistrationRequest } from '@/types/registration'

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
  <section class="space-y-6">
    <div class="page-hero">
      <div class="max-w-3xl">
        <p class="section-kicker">Registration requests</p>
        <h2 class="section-title">Request access to a space</h2>
        <p class="section-copy">
          Enter a space code to request membership. Staff still control approval, and pending requests
          remain visible here until they are reviewed or cancelled.
        </p>
      </div>
    </div>

    <section class="page-section">
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

    <section class="page-section">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Your request history</h3>
          <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
            Requests are listed newest first. Only pending requests can be cancelled.
          </p>
        </div>
      </div>

      <p v-if="actionError" class="alert-error mt-4">{{ actionError }}</p>
      <div v-if="loadError" class="alert-error mt-4">{{ loadError }}</div>
      <div v-else-if="isLoading" class="empty-state mt-4">Loading request history…</div>
      <div v-else-if="!hasRequests" class="empty-state mt-4">No registration requests yet.</div>

      <div v-else class="mt-6 space-y-4">
        <article
          v-for="request in requests"
          :key="request.id"
          class="surface-panel px-5 py-5"
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
    </section>
  </section>
</template>
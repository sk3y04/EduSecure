<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { extractErrorMessage } from '@/services/http'
import { registrationRequestsService } from '@/services/registrationRequests'
import type { ReviewRegistrationRequest } from '@/types/registration'

const requests = ref<ReviewRegistrationRequest[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const actionError = ref<string | null>(null)
const actionSuccess = ref<string | null>(null)
const activeRequestId = ref<string | null>(null)
const reviewNotes = ref<Record<string, string>>({})

async function loadRequests() {
  isLoading.value = true
  loadError.value = null

  try {
    requests.value = await registrationRequestsService.listReviewQueue()
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function handleDecision(requestId: string, decision: 'approve' | 'reject') {
  activeRequestId.value = requestId
  actionError.value = null
  actionSuccess.value = null

  try {
    const payload = { reviewNote: reviewNotes.value[requestId] ?? '' }

    if (decision === 'approve') {
      await registrationRequestsService.approve(requestId, payload)
      actionSuccess.value = 'Request approved and membership created.'
    } else {
      await registrationRequestsService.reject(requestId, payload)
      actionSuccess.value = 'Request rejected.'
    }

    requests.value = requests.value.filter((request) => request.id !== requestId)
    delete reviewNotes.value[requestId]
  } catch (error) {
    actionError.value = extractErrorMessage(error)
  } finally {
    activeRequestId.value = null
  }
}

function formatDate(value: string) {
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
        <p class="section-kicker">Registration review</p>
        <h2 class="section-title">Review pending student access requests</h2>
        <p class="section-copy">
          Approving a request adds the student to the existing space membership roster. Rejecting it
          preserves an auditable decision without granting access.
        </p>
      </div>
    </div>

    <section class="page-section">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Pending queue</h3>
          <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
            Lecturers see owned spaces only. Admins can review all pending requests.
          </p>
        </div>

        <button type="button" class="btn-secondary" @click="loadRequests">Refresh queue</button>
      </div>

      <p v-if="actionError" class="alert-error mt-4">{{ actionError }}</p>
      <p v-else-if="actionSuccess" class="alert-success mt-4">{{ actionSuccess }}</p>

      <div v-if="loadError" class="alert-error mt-4">{{ loadError }}</div>
      <div v-else-if="isLoading" class="empty-state mt-4">Loading pending requests…</div>
      <div v-else-if="requests.length === 0" class="empty-state mt-4">No pending registration requests.</div>

      <div v-else class="mt-6 space-y-4">
        <article
          v-for="request in requests"
          :key="request.id"
          class="surface-panel px-5 py-5"
        >
          <div class="grid gap-5 xl:grid-cols-[1.1fr_0.9fr]">
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
                <span>{{ request.studentFullName }}</span>
                <span>{{ request.studentEmail }}</span>
                <span>Submitted {{ formatDate(request.requestedAt) }}</span>
              </div>

              <p v-if="request.requestMessage" class="text-base leading-7 text-[var(--color-text)]">
                {{ request.requestMessage }}
              </p>
              <p v-else class="text-sm leading-6 text-[var(--color-text-soft)]">No request message provided.</p>
            </div>

            <div class="space-y-4">
              <label class="space-y-2">
                <span class="field-label">Review note</span>
                <textarea
                  v-model="reviewNotes[request.id]"
                  class="form-input min-h-28"
                  maxlength="500"
                  placeholder="Optional note recorded with the review outcome."
                />
              </label>

              <div class="flex flex-wrap gap-3">
                <button
                  type="button"
                  class="btn-primary"
                  :disabled="activeRequestId === request.id"
                  @click="handleDecision(request.id, 'approve')"
                >
                  {{ activeRequestId === request.id ? 'Saving…' : 'Approve' }}
                </button>
                <button
                  type="button"
                  class="btn-secondary"
                  :disabled="activeRequestId === request.id"
                  @click="handleDecision(request.id, 'reject')"
                >
                  {{ activeRequestId === request.id ? 'Saving…' : 'Reject' }}
                </button>
              </div>
            </div>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>
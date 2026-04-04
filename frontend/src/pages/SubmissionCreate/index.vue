<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { submissionsService } from '@/services/submissions'
import type { SubmissionResponse } from '@/types/submission'
import { ExistingSubmissionPanel, SubmissionCreateHeader, SubmissionUploadForm } from './components'

const route = useRoute()
const router = useRouter()

const assignmentId = computed(() => String(route.params.assignmentId ?? ''))
const spaceId = computed(() => {
  const value = route.query.spaceId
  return typeof value === 'string' && value.length > 0 ? value : null
})
const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)
const selectedFile = ref<File | null>(null)
const existingSubmission = ref<SubmissionResponse | null>(null)
const isLoadingExistingSubmission = ref(true)
const existingSubmissionErrorMessage = ref<string | null>(null)

async function loadExistingSubmission() {
  isLoadingExistingSubmission.value = true
  existingSubmissionErrorMessage.value = null

  try {
    existingSubmission.value = await submissionsService.getMyLatestForAssignment(assignmentId.value)
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      existingSubmission.value = null
      return
    }

    existingSubmissionErrorMessage.value = extractErrorMessage(error)
  } finally {
    isLoadingExistingSubmission.value = false
  }
}

function handleFileChange(file: File | null) {
  selectedFile.value = file
  errorMessage.value = null
}

async function handleSubmit() {
  if (!selectedFile.value) {
    errorMessage.value = 'Select a UTF-8 text/plain file before submitting.'
    return
  }

  isSubmitting.value = true
  errorMessage.value = null

  try {
    const response = await submissionsService.create(assignmentId.value, selectedFile.value)
    await router.push({ name: 'submission-detail', params: { submissionId: response.id } })
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

onMounted(() => {
  void loadExistingSubmission()
})
</script>

<template>
  <section class="space-y-6">
    <SubmissionCreateHeader :assignment-id="assignmentId" />
    <ExistingSubmissionPanel
      :assignment-id="assignmentId"
      :space-id="spaceId"
      :submission="existingSubmission"
      :is-loading="isLoadingExistingSubmission"
      :load-error="existingSubmissionErrorMessage"
    />

    <SubmissionUploadForm
      :error-message="errorMessage"
      :is-submitting="isSubmitting"
      :selected-file="selectedFile"
      :space-id="spaceId"
      @submit="handleSubmit"
      @file-change="handleFileChange"
    />
  </section>
</template>


<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { submissionsService } from '@/services/submissions'
import { SubmissionCreateHeader, SubmissionUploadForm } from './components'

const route = useRoute()
const router = useRouter()

const assignmentId = computed(() => String(route.params.assignmentId ?? ''))
const isSubmitting = ref(false)
const errorMessage = ref<string | null>(null)
const selectedFile = ref<File | null>(null)

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
</script>

<template>
  <section class="surface-panel p-8">
    <SubmissionCreateHeader :assignment-id="assignmentId" />
    <SubmissionUploadForm
      :error-message="errorMessage"
      :is-submitting="isSubmitting"
      :selected-file="selectedFile"
      @submit="handleSubmit"
      @file-change="handleFileChange"
    />
  </section>
</template>


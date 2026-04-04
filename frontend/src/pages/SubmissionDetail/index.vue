<script setup lang="ts">
import axios from 'axios'
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'

import { extractErrorMessage } from '@/services/http'
import { gradesService } from '@/services/grades'
import { submissionsService } from '@/services/submissions'
import { useAuthStore } from '@/stores/auth'
import type { GradeResponse, MyGradeResponse } from '@/types/grade'
import type { SubmissionContentResponse, SubmissionResponse } from '@/types/submission'
import {
  GradePanel,
  StudentGradePanel,
  SubmissionContentPanel,
  SubmissionMetaPanel,
  SubmissionVerificationPanel,
} from './components'

const route = useRoute()
const authStore = useAuthStore()

const submission = ref<SubmissionResponse | null>(null)
const submissionContent = ref<SubmissionContentResponse | null>(null)
const isLoading = ref(true)
const isLoadingContent = ref(false)
const errorMessage = ref<string | null>(null)
const contentErrorMessage = ref<string | null>(null)

// Grade state (gradeId only known after POST — not in SubmissionResponse)
const grade = ref<GradeResponse | null>(null)
const isCreatingGrade = ref(false)
const isUpdatingGrade = ref(false)
const gradeErrorMessage = ref<string | null>(null)
const gradeSuccessMessage = ref<string | null>(null)
const studentGrade = ref<MyGradeResponse | null>(null)
const isLoadingStudentGrade = ref(false)
const studentGradeErrorMessage = ref<string | null>(null)

const submissionId = computed(() => String(route.params.submissionId ?? ''))
const canGrade = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const canViewOwnGrade = computed(() => authStore.hasAnyRole(['STUDENT']))
const isSubmissionVerified = computed(() => submission.value?.verificationStatus === 'VERIFIED')

async function loadGrade() {
  if (!canGrade.value) {
    grade.value = null
    return
  }

  gradeErrorMessage.value = null

  try {
    grade.value = await gradesService.getForSubmission(submissionId.value)
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      grade.value = null
      return
    }

    gradeErrorMessage.value = extractErrorMessage(error)
  }
}

async function loadStudentGrade() {
  if (!canViewOwnGrade.value) {
    studentGrade.value = null
    return
  }

  isLoadingStudentGrade.value = true
  studentGradeErrorMessage.value = null

  try {
    studentGrade.value = await gradesService.getMyGradeForSubmission(submissionId.value)
  } catch (error) {
    if (axios.isAxiosError(error) && error.response?.status === 404) {
      studentGrade.value = null
      return
    }

    studentGradeErrorMessage.value = extractErrorMessage(error)
  } finally {
    isLoadingStudentGrade.value = false
  }
}

async function loadSubmission() {
  isLoading.value = true
  errorMessage.value = null

  try {
    submission.value = await submissionsService.getById(submissionId.value)
    if (canGrade.value) {
      await loadGrade()
    }

    if (canViewOwnGrade.value) {
      await loadStudentGrade()
    }
  } catch (error) {
    errorMessage.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function loadSubmissionContent() {
  isLoadingContent.value = true
  contentErrorMessage.value = null

  try {
    submissionContent.value = await submissionsService.getContent(submissionId.value)
  } catch (error) {
    contentErrorMessage.value = extractErrorMessage(error)
  } finally {
    isLoadingContent.value = false
  }
}

async function handleCreateGrade(payload: { value: number | string; feedback: string }) {
  isCreatingGrade.value = true
  gradeErrorMessage.value = null
  gradeSuccessMessage.value = null

  try {
    grade.value = await gradesService.create(submissionId.value, {
      value: Number(payload.value),
      feedback: payload.feedback,
    })
    gradeSuccessMessage.value = 'Grade submitted successfully.'
  } catch (error) {
    gradeErrorMessage.value = extractErrorMessage(error)
  } finally {
    isCreatingGrade.value = false
  }
}

async function handleUpdateGrade(payload: { value: number | string; feedback: string }) {
  if (!grade.value) return

  isUpdatingGrade.value = true
  gradeErrorMessage.value = null
  gradeSuccessMessage.value = null

  try {
    grade.value = await gradesService.update(grade.value.id, {
      value: Number(payload.value),
      feedback: payload.feedback,
    })
    gradeSuccessMessage.value = 'Grade updated successfully.'
  } catch (error) {
    gradeErrorMessage.value = extractErrorMessage(error)
  } finally {
    isUpdatingGrade.value = false
  }
}

onMounted(() => {
  void loadSubmission()
})
</script>

<template>
  <section class="space-y-6">
    <div class="page-hero">
      <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div class="max-w-3xl">
          <p class="section-kicker">Submission evidence</p>
          <h2 class="section-title">Review integrity and authorship metadata</h2>
          <p class="section-copy">
            This screen keeps the cryptographic evidence easy to inspect: digest, signature
            algorithm, and verification result remain prominent, while plaintext retrieval stays a
            separate audited action.
          </p>
        </div>
        <button type="button" class="btn-secondary self-start" @click="loadSubmission">
          Refresh
        </button>
      </div>
    </div>

    <div v-if="errorMessage" class="alert-error">{{ errorMessage }}</div>
    <div v-else-if="isLoading" class="empty-state">Loading submission evidence…</div>

    <template v-else-if="submission">
      <div class="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <SubmissionMetaPanel :submission="submission" />
        <SubmissionVerificationPanel :submission="submission" />
      </div>

      <SubmissionContentPanel
        :is-loading-content="isLoadingContent"
        :content-error-message="contentErrorMessage"
        :submission-content="submissionContent"
        @retrieve="loadSubmissionContent"
      />

      <GradePanel
        v-if="canGrade"
        :submission-verified="isSubmissionVerified"
        :is-creating="isCreatingGrade"
        :is-updating="isUpdatingGrade"
        :error-message="gradeErrorMessage"
        :success-message="gradeSuccessMessage"
        :existing-grade="grade"
        @create="handleCreateGrade"
        @update="handleUpdateGrade"
      />

      <StudentGradePanel
        v-if="canViewOwnGrade"
        :grade="studentGrade"
        :is-loading="isLoadingStudentGrade"
        :error-message="studentGradeErrorMessage"
      />
    </template>
  </section>
</template>


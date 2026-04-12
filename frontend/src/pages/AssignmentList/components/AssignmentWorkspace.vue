<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { assignmentsService } from '@/services/assignments'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { AssignmentSummary } from '@/types/assignment'
import AssignmentListActionPanel from './AssignmentListActionPanel.vue'
import AssignmentCreateForm from './AssignmentCreateForm.vue'
import AssignmentListHeader from './AssignmentListHeader.vue'
import AssignmentListItems from './AssignmentListItems.vue'
import AssignmentListMetricsPanel from './AssignmentListMetricsPanel.vue'

const props = withDefaults(
  defineProps<{
    embedded?: boolean
    spaceId?: string | null
    spaceName?: string | null
  }>(),
  {
    embedded: false,
    spaceId: null,
    spaceName: null,
  },
)

const authStore = useAuthStore()

const assignments = ref<AssignmentSummary[]>([])
const isLoading = ref(true)
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const createSuccess = ref<string | null>(null)
const isCreating = ref(false)

const canCreateAssignments = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const canReviewSubmissions = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const isStudent = computed(() => authStore.hasAnyRole(['STUDENT']))
const visibleAssignments = computed(() =>
  props.spaceId
    ? assignments.value.filter((assignment) => assignment.spaceId === props.spaceId)
    : assignments.value,
)
const sortedAssignments = computed(() =>
  [...visibleAssignments.value].sort(
    (left, right) => new Date(left.dueAt).getTime() - new Date(right.dueAt).getTime(),
  ),
)
const canCreateInCurrentView = computed(() => canCreateAssignments.value && Boolean(props.spaceId))
const openAssignments = computed(() => sortedAssignments.value.filter((assignment) => assignment.open).length)
const closedAssignments = computed(() => sortedAssignments.value.filter((assignment) => !assignment.open).length)
const submittedAssignments = computed(() =>
  sortedAssignments.value.filter((assignment) => Boolean(assignment.latestSubmissionId)).length,
)

const embeddedHeading = computed(() =>
  props.spaceName ? `Assignments in ${props.spaceName}` : 'Assignments in this space',
)

const embeddedCopy = computed(() =>
  canCreateAssignments.value
    ? 'Create coursework, monitor due dates, and open student submission flows from inside this space.'
    : 'Open coursework, review due dates, and reach the secure submission flow from inside this space.',
)

async function loadAssignments() {
  isLoading.value = true
  loadError.value = null

  try {
    assignments.value = await assignmentsService.list()
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function handleCreate(payload: { title: string; description: string; dueAt: string }) {
  if (!props.spaceId) {
    createError.value = 'Select a space before creating an assignment.'
    createSuccess.value = null
    return
  }

  isCreating.value = true
  createError.value = null
  createSuccess.value = null

  try {
    const created = await assignmentsService.create({
      title: payload.title,
      description: payload.description,
      dueAt: new Date(payload.dueAt).toISOString(),
      spaceId: props.spaceId,
    })

    assignments.value = [
      {
        id: created.id,
        title: created.title,
        dueAt: created.dueAt,
        spaceId: created.spaceId,
        open: created.open,
        latestSubmissionId: null,
        latestSubmittedAt: null,
      },
      ...assignments.value,
    ]

    createSuccess.value = 'Assignment created successfully.'
  } catch (error) {
    createError.value = extractErrorMessage(error)
  } finally {
    isCreating.value = false
  }
}

onMounted(() => {
  void loadAssignments()
})

watch(
  () => props.spaceId,
  () => {
    createError.value = null
    createSuccess.value = null
  },
)
</script>

<template>
  <div class="desktop-page-panel panel-shell min-h-0 gap-6">
    <template v-if="props.embedded">
      <section class="page-section">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div class="max-w-3xl">
            <p class="section-kicker">Assignments</p>
            <h3 class="section-title">{{ embeddedHeading }}</h3>
            <p class="section-copy max-w-none">
              {{ embeddedCopy }}
            </p>
          </div>
          <div v-if="props.spaceName" class="surface-panel-muted px-5 py-4">
            <p class="meta-label">Working inside</p>
            <p class="meta-value">{{ props.spaceName }}</p>
          </div>
        </div>
      </section>
    </template>
    <template v-else>
      <div class="desktop-page-grid">
        <AssignmentListHeader class="xl:col-span-8 xl:row-span-2" />

        <AssignmentListMetricsPanel
          class="xl:col-span-4"
          :total-assignments="sortedAssignments.length"
          :open-assignments="openAssignments"
          :closed-assignments="closedAssignments"
          :submitted-assignments="submittedAssignments"
        />

        <section
          v-if="canCreateAssignments"
          class="page-section desktop-page-panel panel-shell-spread xl:col-span-4 xl:row-span-2"
        >
          <div>
            <div class="panel-header">
              <h3 class="panel-title">Assignment creation is space-scoped</h3>
              <p class="panel-copy">
                Open a space to create coursework for that staff-managed roster. This overview route
                keeps cross-space visibility while creation remains anchored to the correct class area.
              </p>
            </div>
          </div>

          <div class="surface-panel-muted mt-6 px-5 py-4">
            <p class="meta-label">Staff path</p>
            <p class="mt-2 text-sm leading-6 text-[var(--color-text-soft)]">
              Use the adjacent support panel to jump to `Spaces`, then create coursework from the
              relevant managed space.
            </p>
          </div>
        </section>

        <AssignmentListActionPanel
          v-else
          class="xl:col-span-4 xl:row-span-2"
          :is-student="isStudent"
          :can-create-assignments="canCreateAssignments"
          :can-review-submissions="canReviewSubmissions"
        />

        <AssignmentListItems
          class="xl:col-span-8 xl:row-span-3"
          :assignments="sortedAssignments"
          :is-loading="isLoading"
          :load-error="loadError"
          :is-student="isStudent"
          :can-review-submissions="canReviewSubmissions"
          :space-id="props.spaceId"
          @refresh="loadAssignments"
        />

        <AssignmentListActionPanel
          v-if="canCreateAssignments"
          class="xl:col-span-4"
          :is-student="isStudent"
          :can-create-assignments="canCreateAssignments"
          :can-review-submissions="canReviewSubmissions"
        />
      </div>
    </template>

    <template v-if="props.embedded">
      <AssignmentCreateForm
        v-if="canCreateInCurrentView"
        :create-error="createError"
        :create-success="createSuccess"
        :is-creating="isCreating"
        :space-name="props.spaceName"
        @submit="handleCreate"
      />

      <section v-else-if="canCreateAssignments" class="page-section">
        <div class="panel-header">
          <h3 class="panel-title">Assignment creation is space-scoped</h3>
          <p class="panel-copy">
            Open a space to create coursework for that staff-managed roster. This view shows all
            assignments you can currently access across spaces.
          </p>
        </div>
      </section>

      <AssignmentListItems
        class="flex-1"
        :assignments="sortedAssignments"
        :is-loading="isLoading"
        :load-error="loadError"
        :is-student="isStudent"
        :can-review-submissions="canReviewSubmissions"
        :space-id="props.spaceId"
        @refresh="loadAssignments"
      />
    </template>
  </div>
</template>


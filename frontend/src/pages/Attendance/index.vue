<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'

import { attendanceService } from '@/services/attendance'
import { extractErrorMessage } from '@/services/http'
import { spacesService } from '@/services/spaces'
import { useAuthStore } from '@/stores/auth'
import { AttendanceHeader, AttendanceMetricsPanel } from './components'
import type {
  AttendanceSession,
  AttendanceSessionPayload,
  AttendanceSessionRecordsResponse,
  AttendanceStatus,
  UpdateAttendanceSessionPayload,
} from '@/types/attendance'
import type { SpaceSummary } from '@/types/space'

const authStore = useAuthStore()

const sessions = ref<AttendanceSession[]>([])
const manageableSpaces = ref<SpaceSummary[]>([])
const selectedManageSessionId = ref<string | null>(null)
const selectedSessionRecords = ref<AttendanceSessionRecordsResponse | null>(null)
const recordDrafts = ref<Record<string, AttendanceStatus | ''>>({})

const isLoading = ref(true)
const isLoadingSpaces = ref(false)
const isLoadingRecords = ref(false)
const isSubmitting = ref(false)
const isSavingRecords = ref(false)
const editingSessionId = ref<string | null>(null)

const loadError = ref<string | null>(null)
const formError = ref<string | null>(null)
const formSuccess = ref<string | null>(null)
const recordsError = ref<string | null>(null)
const recordsSuccess = ref<string | null>(null)
const selectedFilterSpaceId = ref('')
const filterFromDate = ref('')
const filterToDate = ref('')

const canManageAttendance = computed(() => authStore.hasAnyRole(['LECTURER', 'ADMIN']))
const hasSessions = computed(() => sessions.value.length > 0)
const filterableSpaces = computed(() => {
  const uniqueSpaces = new Map<string, { id: string; code: string; name: string }>()

  for (const session of sessions.value) {
    if (!uniqueSpaces.has(session.spaceId)) {
      uniqueSpaces.set(session.spaceId, {
        id: session.spaceId,
        code: session.spaceCode,
        name: session.spaceName,
      })
    }
  }

  return [...uniqueSpaces.values()].sort((left, right) => {
    const codeCompare = left.code.localeCompare(right.code)
    return codeCompare !== 0 ? codeCompare : left.name.localeCompare(right.name)
  })
})
const filteredSessions = computed(() => sessions.value.filter((session) => matchesFilters(session)))
const hasFilteredSessions = computed(() => filteredSessions.value.length > 0)
const manageableSessionCount = computed(() => filteredSessions.value.filter((session) => session.canManage).length)
const totalUnrecordedSlots = computed(() =>
  filteredSessions.value.reduce((sum, session) => sum + sessionUnrecordedCount(session), 0),
)
const statusOptions: Array<{ value: AttendanceStatus; label: string }> = [
  { value: 'PRESENT', label: 'Present' },
  { value: 'LATE', label: 'Late' },
  { value: 'ABSENT', label: 'Absent' },
  { value: 'EXCUSED', label: 'Excused' },
]

const createForm = ref<AttendanceSessionPayload>(emptyCreateForm())
const editForm = ref<UpdateAttendanceSessionPayload>(emptyEditForm())

function emptyCreateForm(): AttendanceSessionPayload {
  return {
    spaceId: '',
    title: '',
    description: '',
    startsAt: '',
    endsAt: '',
  }
}

function emptyEditForm(): UpdateAttendanceSessionPayload {
  return {
    title: '',
    description: '',
    startsAt: '',
    endsAt: '',
  }
}

function toLocalDateTimeInput(value: string) {
  const date = new Date(value)
  const offsetMs = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16)
}

function toCreateApiPayload(payload: AttendanceSessionPayload): AttendanceSessionPayload {
  return {
    ...payload,
    title: payload.title.trim(),
    description: payload.description.trim(),
    startsAt: new Date(payload.startsAt).toISOString(),
    endsAt: new Date(payload.endsAt).toISOString(),
  }
}

function toUpdateApiPayload(payload: UpdateAttendanceSessionPayload): UpdateAttendanceSessionPayload {
  return {
    ...payload,
    title: payload.title.trim(),
    description: payload.description.trim(),
    startsAt: new Date(payload.startsAt).toISOString(),
    endsAt: new Date(payload.endsAt).toISOString(),
  }
}

function applyUpsert(updatedSession: AttendanceSession) {
  const existingIndex = sessions.value.findIndex((session) => session.id === updatedSession.id)

  if (existingIndex >= 0) {
    sessions.value.splice(existingIndex, 1, updatedSession)
  } else {
    sessions.value = [...sessions.value, updatedSession]
  }
}

function updateSessionFromRecords(response: AttendanceSessionRecordsResponse) {
  selectedSessionRecords.value = response
  applyUpsert(response.session)
  recordDrafts.value = Object.fromEntries(
    response.records.map((record) => [record.studentUserId, record.status ?? '']),
  )
}

async function loadSessions() {
  isLoading.value = true
  loadError.value = null

  try {
    sessions.value = await attendanceService.listSessions()
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoading.value = false
  }
}

async function loadManageableSpaces() {
  if (!canManageAttendance.value) {
    return
  }

  isLoadingSpaces.value = true

  try {
    manageableSpaces.value = (await spacesService.list()).filter((space) => space.canManage && !space.archived)
    if (!createForm.value.spaceId && manageableSpaces.value.length > 0) {
      createForm.value.spaceId = manageableSpaces.value[0].id
    }
  } catch (error) {
    loadError.value = extractErrorMessage(error)
  } finally {
    isLoadingSpaces.value = false
  }
}

async function loadSelectedSessionRecords() {
  if (!selectedManageSessionId.value) {
    selectedSessionRecords.value = null
    recordDrafts.value = {}
    return
  }

  isLoadingRecords.value = true
  recordsError.value = null

  try {
    const response = await attendanceService.getSessionRecords(selectedManageSessionId.value)
    updateSessionFromRecords(response)
  } catch (error) {
    recordsError.value = extractErrorMessage(error)
  } finally {
    isLoadingRecords.value = false
  }
}

async function handleCreate() {
  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null

  try {
    const created = await attendanceService.createSession(toCreateApiPayload(createForm.value))
    applyUpsert(created)
    createForm.value = {
      ...emptyCreateForm(),
      spaceId: createForm.value.spaceId,
    }
    formSuccess.value = 'Attendance session created.'
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function startEdit(session: AttendanceSession) {
  editingSessionId.value = session.id
  formError.value = null
  formSuccess.value = null
  editForm.value = {
    title: session.title,
    description: session.description ?? '',
    startsAt: toLocalDateTimeInput(session.startsAt),
    endsAt: toLocalDateTimeInput(session.endsAt),
  }
}

function cancelEdit() {
  editingSessionId.value = null
  editForm.value = emptyEditForm()
}

async function handleUpdate() {
  if (!editingSessionId.value) {
    return
  }

  isSubmitting.value = true
  formError.value = null
  formSuccess.value = null

  try {
    const updated = await attendanceService.updateSession(editingSessionId.value, toUpdateApiPayload(editForm.value))
    applyUpsert(updated)
    if (selectedSessionRecords.value?.session.id === updated.id) {
      selectedSessionRecords.value = {
        ...selectedSessionRecords.value,
        session: updated,
      }
    }
    cancelEdit()
    formSuccess.value = 'Attendance session updated.'
  } catch (error) {
    formError.value = extractErrorMessage(error)
  } finally {
    isSubmitting.value = false
  }
}

function manageSession(session: AttendanceSession) {
  selectedManageSessionId.value = session.id
  recordsSuccess.value = null
  void loadSelectedSessionRecords()
}

async function handleSaveRecords() {
  if (!selectedManageSessionId.value || !selectedSessionRecords.value) {
    return
  }

  isSavingRecords.value = true
  recordsError.value = null
  recordsSuccess.value = null

  try {
    const response = await attendanceService.updateSessionRecords(selectedManageSessionId.value, {
      records: selectedSessionRecords.value.records.map((record) => ({
        studentUserId: record.studentUserId,
        status: recordDrafts.value[record.studentUserId] || null,
      })),
    })
    updateSessionFromRecords(response)
    recordsSuccess.value = 'Attendance records updated.'
  } catch (error) {
    recordsError.value = extractErrorMessage(error)
  } finally {
    isSavingRecords.value = false
  }
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '—'
  }

  return new Date(value).toLocaleString()
}

function statusLabel(status: AttendanceStatus | null) {
  if (!status) {
    return 'Not recorded'
  }

  return status.charAt(0) + status.slice(1).toLowerCase()
}

function sessionUnrecordedCount(session: AttendanceSession) {
  return Math.max(0, session.memberCount - session.recordedCount)
}

function clearFilters() {
  selectedFilterSpaceId.value = ''
  filterFromDate.value = ''
  filterToDate.value = ''
}

function parseLocalDayStart(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, (month ?? 1) - 1, day ?? 1).getTime()
}

function matchesFilters(session: AttendanceSession) {
  if (selectedFilterSpaceId.value && session.spaceId !== selectedFilterSpaceId.value) {
    return false
  }

  const sessionStartsAt = new Date(session.startsAt).getTime()
  const sessionEndsAt = new Date(session.endsAt).getTime()

  if (filterFromDate.value) {
    const fromStart = parseLocalDayStart(filterFromDate.value)
    if (sessionEndsAt < fromStart) {
      return false
    }
  }

  if (filterToDate.value) {
    const toEndExclusive = parseLocalDayStart(filterToDate.value) + 24 * 60 * 60 * 1000
    if (sessionStartsAt >= toEndExclusive) {
      return false
    }
  }

  return true
}

watch(filteredSessions, (nextSessions) => {
  if (!selectedManageSessionId.value) {
    return
  }

  const selectedStillVisible = nextSessions.some((session) => session.id === selectedManageSessionId.value)
  if (!selectedStillVisible) {
    selectedManageSessionId.value = null
    selectedSessionRecords.value = null
    recordDrafts.value = {}
    recordsSuccess.value = null
  }
})

onMounted(async () => {
  await Promise.all([loadSessions(), loadManageableSpaces()])
})
</script>

<template>
  <section class="desktop-page-grid">
    <AttendanceHeader class="xl:col-span-8 xl:row-span-2" />

    <AttendanceMetricsPanel
      class="xl:col-span-4"
      :total-sessions="sessions.length"
      :filtered-sessions="filteredSessions.length"
      :manageable-sessions="manageableSessionCount"
      :unrecorded-slots="totalUnrecordedSlots"
    />

    <section v-if="canManageAttendance" class="page-section desktop-page-panel flex h-full flex-col xl:col-span-4 xl:row-span-3">
      <div>
        <h3 class="panel-title">Create attendance session</h3>
        <p class="panel-copy">
          Session creation snapshots the current space roster so later membership changes do not alter historical attendance evidence.
        </p>
      </div>

      <div class="grid gap-4 lg:grid-cols-2">
        <label class="space-y-2">
          <span class="field-label">Space</span>
          <select v-model="createForm.spaceId" class="form-input" :disabled="isLoadingSpaces || isSubmitting">
            <option value="" disabled>Select a space</option>
            <option v-for="space in manageableSpaces" :key="space.id" :value="space.id">
              {{ space.code }} · {{ space.name }}
            </option>
          </select>
        </label>

        <label class="space-y-2 lg:col-span-2">
          <span class="field-label">Title</span>
          <input v-model="createForm.title" type="text" class="form-input" maxlength="160">
        </label>

        <label class="space-y-2 lg:col-span-2">
          <span class="field-label">Description</span>
          <textarea v-model="createForm.description" class="form-input min-h-28" maxlength="1000"></textarea>
        </label>

        <label class="space-y-2">
          <span class="field-label">Starts at</span>
          <input v-model="createForm.startsAt" type="datetime-local" class="form-input">
        </label>

        <label class="space-y-2">
          <span class="field-label">Ends at</span>
          <input v-model="createForm.endsAt" type="datetime-local" class="form-input">
        </label>
      </div>

      <div class="flex flex-wrap items-center gap-3">
        <button type="button" class="btn-primary" :disabled="isSubmitting || !manageableSpaces.length" @click="handleCreate">
          {{ isSubmitting ? 'Saving…' : 'Create attendance session' }}
        </button>
        <button type="button" class="btn-secondary" @click="loadSessions">Refresh sessions</button>
      </div>

      <p v-if="formError" class="alert-error">{{ formError }}</p>
      <p v-else-if="formSuccess" class="alert-success">{{ formSuccess }}</p>
    </section>

    <section class="page-section desktop-page-panel flex min-h-[36rem] flex-col xl:col-span-8 xl:row-span-4">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h3 class="panel-title">Visible attendance sessions</h3>
          <p class="panel-copy">
            Sessions are ordered newest first and include basic reporting totals for recorded attendance.
          </p>
        </div>
        <button type="button" class="btn-secondary" @click="loadSessions">Refresh</button>
      </div>

      <div v-if="hasSessions" class="grid gap-4 lg:grid-cols-[1.1fr_0.8fr_0.8fr_auto] lg:items-end">
        <label class="space-y-2">
          <span class="field-label">Filter by space</span>
          <select v-model="selectedFilterSpaceId" class="form-input">
            <option value="">All visible spaces</option>
            <option v-for="space in filterableSpaces" :key="space.id" :value="space.id">
              {{ space.code }} · {{ space.name }}
            </option>
          </select>
        </label>

        <label class="space-y-2">
          <span class="field-label">From date</span>
          <input v-model="filterFromDate" type="date" class="form-input">
        </label>

        <label class="space-y-2">
          <span class="field-label">To date</span>
          <input v-model="filterToDate" type="date" class="form-input">
        </label>

        <button type="button" class="btn-secondary" @click="clearFilters">
          Clear filters
        </button>
      </div>

      <div v-if="loadError" class="alert-error">{{ loadError }}</div>
      <div v-else-if="isLoading" class="empty-state">Loading attendance sessions…</div>
      <div v-else-if="!hasSessions" class="empty-state">No attendance sessions are visible yet.</div>
      <div v-else-if="!hasFilteredSessions" class="empty-state">No attendance sessions match the current filters.</div>

      <div v-else class="panel-scroll-list">
        <article v-for="session in filteredSessions" :key="session.id" class="record-card">
          <template v-if="editingSessionId === session.id">
            <div class="grid gap-4 lg:grid-cols-2">
              <label class="space-y-2 lg:col-span-2">
                <span class="field-label">Title</span>
                <input v-model="editForm.title" type="text" class="form-input" maxlength="160">
              </label>

              <label class="space-y-2 lg:col-span-2">
                <span class="field-label">Description</span>
                <textarea v-model="editForm.description" class="form-input min-h-28" maxlength="1000"></textarea>
              </label>

              <label class="space-y-2">
                <span class="field-label">Starts at</span>
                <input v-model="editForm.startsAt" type="datetime-local" class="form-input">
              </label>

              <label class="space-y-2">
                <span class="field-label">Ends at</span>
                <input v-model="editForm.endsAt" type="datetime-local" class="form-input">
              </label>
            </div>

            <div class="mt-4 flex flex-wrap items-center gap-3">
              <button type="button" class="btn-primary" :disabled="isSubmitting" @click="handleUpdate">
                {{ isSubmitting ? 'Saving…' : 'Save changes' }}
              </button>
              <button type="button" class="btn-secondary" :disabled="isSubmitting" @click="cancelEdit">
                Cancel
              </button>
            </div>
          </template>

          <template v-else>
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div class="space-y-3">
                <div>
                  <p class="text-sm font-medium uppercase tracking-[0.2em] text-[var(--color-text-soft)]">
                    {{ session.spaceCode }} · {{ session.spaceName }}
                  </p>
                  <h4 class="mt-1 font-display text-xl font-semibold text-[var(--color-heading)]">
                    {{ session.title }}
                  </h4>
                </div>

                <div class="flex flex-wrap items-center gap-3 text-sm text-[var(--color-text-soft)]">
                  <span>{{ formatDateTime(session.startsAt) }} to {{ formatDateTime(session.endsAt) }}</span>
                  <span>{{ session.memberCount }} students snapshotted</span>
                  <span>{{ session.recordedCount }} recorded</span>
                  <span v-if="!session.canManage">Your status: {{ statusLabel(session.myStatus) }}</span>
                </div>

                <p v-if="session.description" class="text-base leading-7 text-[var(--color-text)]">
                  {{ session.description }}
                </p>

                <dl class="stats-grid">
                  <div class="stat-card bg-[var(--color-surface-offset)]">
                    <dt class="meta-label">Present</dt>
                    <dd class="meta-value text-lg font-semibold">{{ session.presentCount }}</dd>
                  </div>
                  <div class="stat-card">
                    <dt class="meta-label">Late</dt>
                    <dd class="meta-value">{{ session.lateCount }}</dd>
                  </div>
                  <div class="stat-card">
                    <dt class="meta-label">Absent</dt>
                    <dd class="meta-value">{{ session.absentCount }}</dd>
                  </div>
                  <div class="stat-card">
                    <dt class="meta-label">Excused</dt>
                    <dd class="meta-value">{{ session.excusedCount }}</dd>
                  </div>
                  <div class="stat-card">
                    <dt class="meta-label">Not recorded</dt>
                    <dd class="meta-value">{{ sessionUnrecordedCount(session) }}</dd>
                  </div>
                </dl>
              </div>

              <div v-if="session.canManage" class="flex flex-wrap gap-3 self-start">
                <button type="button" class="btn-secondary" @click="startEdit(session)">
                  Edit session
                </button>
                <button type="button" class="btn-primary" @click="manageSession(session)">
                  Manage roster
                </button>
              </div>
            </div>
          </template>
        </article>
      </div>
    </section>

    <section v-if="canManageAttendance" class="page-section desktop-page-panel flex min-h-[32rem] flex-col xl:col-span-12">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h3 class="panel-title">Roster attendance editor</h3>
          <p class="panel-copy">
            Select a manageable session to record or revise student attendance against the snapshotted roster.
          </p>
        </div>
        <button type="button" class="btn-secondary" :disabled="!selectedManageSessionId" @click="loadSelectedSessionRecords">
          Refresh roster
        </button>
      </div>

      <div v-if="recordsError" class="alert-error">{{ recordsError }}</div>
      <p v-else-if="recordsSuccess" class="alert-success">{{ recordsSuccess }}</p>

      <div v-if="!selectedManageSessionId" class="empty-state">
        Choose “Manage roster” on a session above to record attendance.
      </div>
      <div v-else-if="isLoadingRecords" class="empty-state">Loading session roster…</div>
      <div v-else-if="!selectedSessionRecords" class="empty-state">No roster data is available for the selected session.</div>

      <template v-else>
        <div class="surface-panel-muted p-4">
          <p class="meta-label">Selected session</p>
          <p class="mt-2 text-base font-medium text-[var(--color-heading)]">
            {{ selectedSessionRecords.session.spaceCode }} · {{ selectedSessionRecords.session.title }}
          </p>
          <p class="mt-1 text-sm text-[var(--color-text-soft)]">
            {{ selectedSessionRecords.records.length }} roster records · {{ formatDateTime(selectedSessionRecords.session.startsAt) }}
          </p>
        </div>

        <div class="panel-scroll-list">
          <article
            v-for="record in selectedSessionRecords.records"
            :key="record.studentUserId"
            class="record-card"
          >
            <div class="grid gap-4 lg:grid-cols-[1.3fr_0.7fr_0.8fr] lg:items-center">
              <div>
                <p class="text-base font-semibold text-[var(--color-heading)]">{{ record.studentFullName }}</p>
                <p class="text-sm text-[var(--color-text-soft)]">{{ record.studentEmail }}</p>
              </div>

              <label class="space-y-2">
                <span class="field-label">Attendance status</span>
                <select v-model="recordDrafts[record.studentUserId]" class="form-input">
                  <option value="">Not recorded</option>
                  <option v-for="option in statusOptions" :key="option.value" :value="option.value">
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <div class="text-sm text-[var(--color-text-soft)]">
                <p class="meta-label">Last recorded</p>
                <p class="mt-2">{{ formatDateTime(record.recordedAt) }}</p>
              </div>
            </div>
          </article>
        </div>

        <div class="flex flex-wrap items-center gap-3">
          <button type="button" class="btn-primary" :disabled="isSavingRecords" @click="handleSaveRecords">
            {{ isSavingRecords ? 'Saving…' : 'Save attendance records' }}
          </button>
        </div>
      </template>
    </section>
  </section>
</template>



<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

import { spaceChatService } from '@/services/spaceChat'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { SpaceChatMessage, SpaceChatMessagePage } from '@/types/spaceChat'
import SpaceChatComposer from './SpaceChatComposer.vue'
import SpaceChatMessageList from './SpaceChatMessageList.vue'

const POLL_INTERVAL_MS = 15_000
const INITIAL_PAGE_SIZE = 30

const props = defineProps<{
  spaceId: string
  archived: boolean
}>()

const authStore = useAuthStore()

const messages = ref<SpaceChatMessage[]>([])
const isLoading = ref(false)
const loadError = ref<string | null>(null)
const refreshError = ref<string | null>(null)
const isLoadingOlder = ref(false)
const loadOlderError = ref<string | null>(null)
const hasMore = ref(false)
const nextCursorBeforeCreatedAt = ref<string | null>(null)
const nextCursorBeforeMessageId = ref<string | null>(null)
const draftBody = ref('')
const isSending = ref(false)
const sendError = ref<string | null>(null)

let pollHandle: number | null = null

const currentUserId = computed(() => authStore.user?.userId ?? null)

function compareMessages(left: SpaceChatMessage, right: SpaceChatMessage): number {
  const timeDifference = new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime()
  if (timeDifference !== 0) {
    return timeDifference
  }
  return left.id.localeCompare(right.id)
}

function mergeMessages(...messageSets: SpaceChatMessage[][]): SpaceChatMessage[] {
  const byId = new Map<string, SpaceChatMessage>()

  for (const messageSet of messageSets) {
    for (const message of messageSet) {
      byId.set(message.id, message)
    }
  }

  return [...byId.values()].sort(compareMessages)
}

function applyPage(page: SpaceChatMessagePage) {
  messages.value = page.items
  hasMore.value = page.hasMore
  nextCursorBeforeCreatedAt.value = page.nextCursorBeforeCreatedAt
  nextCursorBeforeMessageId.value = page.nextCursorBeforeMessageId
}

async function loadInitial() {
  isLoading.value = true
  loadError.value = null
  refreshError.value = null
  loadOlderError.value = null

  try {
    const page = await spaceChatService.listMessages(props.spaceId, { limit: INITIAL_PAGE_SIZE })
    applyPage(page)
  } catch (error) {
    loadError.value = extractErrorMessage(error)
    messages.value = []
    hasMore.value = false
    nextCursorBeforeCreatedAt.value = null
    nextCursorBeforeMessageId.value = null
  } finally {
    isLoading.value = false
  }
}

async function loadOlder() {
  if (!hasMore.value || !nextCursorBeforeCreatedAt.value || !nextCursorBeforeMessageId.value || isLoadingOlder.value) {
    return
  }

  isLoadingOlder.value = true
  loadOlderError.value = null

  try {
    const page = await spaceChatService.listMessages(props.spaceId, {
      limit: INITIAL_PAGE_SIZE,
      beforeCreatedAt: nextCursorBeforeCreatedAt.value,
      beforeMessageId: nextCursorBeforeMessageId.value,
    })

    messages.value = mergeMessages(messages.value, page.items)
    hasMore.value = page.hasMore
    nextCursorBeforeCreatedAt.value = page.nextCursorBeforeCreatedAt
    nextCursorBeforeMessageId.value = page.nextCursorBeforeMessageId
  } catch (error) {
    loadOlderError.value = extractErrorMessage(error)
  } finally {
    isLoadingOlder.value = false
  }
}

async function pollLatest() {
  if (document.visibilityState !== 'visible' || isLoading.value || isLoadingOlder.value) {
    return
  }

  try {
    const page = await spaceChatService.listMessages(props.spaceId, { limit: INITIAL_PAGE_SIZE })
    messages.value = mergeMessages(messages.value, page.items)
    refreshError.value = null
  } catch (error) {
    if (messages.value.length > 0) {
      refreshError.value = extractErrorMessage(error)
      return
    }
    loadError.value = extractErrorMessage(error)
  }
}

async function sendMessage() {
  if (isSending.value || props.archived || draftBody.value.trim().length === 0) {
    return
  }

  isSending.value = true
  sendError.value = null

  try {
    const createdMessage = await spaceChatService.createMessage(props.spaceId, {
      body: draftBody.value,
    })
    messages.value = mergeMessages(messages.value, [createdMessage])
    draftBody.value = ''
    refreshError.value = null
  } catch (error) {
    sendError.value = extractErrorMessage(error)
  } finally {
    isSending.value = false
  }
}

function startPolling() {
  stopPolling()
  pollHandle = window.setInterval(() => {
    void pollLatest()
  }, POLL_INTERVAL_MS)
}

function stopPolling() {
  if (pollHandle !== null) {
    window.clearInterval(pollHandle)
    pollHandle = null
  }
}

watch(
  () => props.spaceId,
  () => {
    messages.value = []
    draftBody.value = ''
    sendError.value = null
    void loadInitial()
    startPolling()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <section class="page-section space-y-5">
    <div class="panel-header flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <h3 class="font-display text-2xl font-semibold text-[var(--color-heading)]">Space chat</h3>
        <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
          Chat is shared by everyone who already has access to this space. Messages stay readable
          after archival, but new posts are blocked once the space is archived.
        </p>
      </div>
      <button type="button" class="btn-secondary self-start" @click="loadInitial">Refresh chat</button>
    </div>

    <div v-if="props.archived" class="alert-error">
      This space is archived. Existing chat history remains visible, but new messages are disabled.
    </div>

    <SpaceChatMessageList
      :messages="messages"
      :current-user-id="currentUserId"
      :is-loading="isLoading"
      :load-error="loadError"
      :refresh-error="refreshError"
      :has-more="hasMore"
      :is-loading-older="isLoadingOlder"
      :load-older-error="loadOlderError"
      @retry="loadInitial"
      @load-older="loadOlder"
    />

    <SpaceChatComposer
      v-model="draftBody"
      :is-sending="isSending"
      :send-error="sendError"
      :is-archived="props.archived"
      @submit="sendMessage"
    />
  </section>
</template>


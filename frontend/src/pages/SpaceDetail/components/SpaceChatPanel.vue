<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'

import {
  decryptSpaceChatMessage,
  detectBrowserCryptoSupport,
  encryptSpaceChatMessage,
  generateChatKeyPairMaterial,
  generateSpaceRoomKey,
  unwrapRoomKeyFromPublisher,
  wrapRoomKeyForRecipient,
} from '@/services/chatCrypto'
import { chatKeyStore } from '@/services/chatKeyStore'
import type { StoredChatKeyMaterial } from '@/services/chatKeyStore'
import { spaceChatService } from '@/services/spaceChat'
import { extractErrorMessage } from '@/services/http'
import { useAuthStore } from '@/stores/auth'
import type { SpaceChatMessage, SpaceChatMessagePage } from '@/types/spaceChat'
import type {
  CurrentUserChatKeyResponse,
  SpaceChatE2eeRecipientResponse,
  SpaceChatE2eeStateResponse,
} from '@/types/spaceChatCrypto'
import SpaceChatComposer from './SpaceChatComposer.vue'
import SpaceChatMessageList from './SpaceChatMessageList.vue'

const POLL_INTERVAL_MS = 15_000
const INITIAL_PAGE_SIZE = 30

const props = defineProps<{
  spaceId: string
  archived: boolean
  canManage: boolean
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
const currentUserChatKey = ref<CurrentUserChatKeyResponse | null>(null)
const e2eeState = ref<SpaceChatE2eeStateResponse | null>(null)
const localKeyFingerprint = ref<string | null>(null)
const e2eeStatusError = ref<string | null>(null)
const browserSupportsEncryptedChatSetup = ref(true)
const isSettingUpEncryptedChat = ref(false)
const isPublishingEncryptedRoomKey = ref(false)
const publishEncryptedRoomKeyError = ref<string | null>(null)
const publishEncryptedRoomKeySuccess = ref<string | null>(null)
const currentRoomKey = ref<CryptoKey | null>(null)
const currentRoomKeyVersion = ref<number | null>(null)

let pollHandle: number | null = null

const currentUserId = computed(() => authStore.user?.userId ?? null)
const encryptedChatBootstrapEnabled = computed(() => Boolean(authStore.user?.chatE2eeEnabled || currentUserChatKey.value?.e2eeEnabled))
const encryptedChatNeedsSetup = computed(() => Boolean(encryptedChatBootstrapEnabled.value && currentUserChatKey.value && !currentUserChatKey.value.keyRegistered))
const encryptedChatMissingLocalKey = computed(() => Boolean(encryptedChatBootstrapEnabled.value && currentUserChatKey.value?.keyRegistered && !localKeyFingerprint.value))
const encryptedChatLocalKeyMismatch = computed(() => Boolean(
  encryptedChatBootstrapEnabled.value
  && currentUserChatKey.value?.keyRegistered
  && localKeyFingerprint.value
  && currentUserChatKey.value.fingerprint
  && localKeyFingerprint.value !== currentUserChatKey.value.fingerprint,
))
const encryptedChatReadyOnThisDevice = computed(() => Boolean(
  encryptedChatBootstrapEnabled.value
  && currentUserChatKey.value?.keyRegistered
  && localKeyFingerprint.value
  && currentUserChatKey.value.fingerprint
  && localKeyFingerprint.value === currentUserChatKey.value.fingerprint,
))
const canManageEncryptedRoomKey = computed(() => Boolean(
  encryptedChatBootstrapEnabled.value
  && props.canManage
  && !props.archived
  && encryptedChatReadyOnThisDevice.value,
))
const encryptedRoomKeyActionLabel = computed(() => {
  if ((e2eeState.value?.activeKeyVersion ?? 0) < 1) {
    return 'Initialize encrypted room key'
  }

  return e2eeState.value?.requiresRekey ? 'Publish rotated room key' : 'Rotate encrypted room key'
})
const sendDisabledReason = computed(() => {
  if (!encryptedChatBootstrapEnabled.value) {
    return null
  }

  if (!browserSupportsEncryptedChatSetup.value) {
    return 'This browser does not support the encrypted chat features required to send messages in this space.'
  }

  if (!encryptedChatReadyOnThisDevice.value) {
    return 'Register encrypted chat on this device before sending messages in this space.'
  }

  if ((e2eeState.value?.activeKeyVersion ?? 0) < 1) {
    return props.canManage
      ? 'Initialize the encrypted room key for this space before sending new messages.'
      : 'A staff member must initialize the encrypted room key for this space before you can send messages.'
  }

  if (e2eeState.value?.requiresRekey) {
    return props.canManage
      ? 'Publish a rotated encrypted room key before sending new messages.'
      : 'A staff member must rotate the encrypted room key before new messages can be sent.'
  }

  if (!currentRoomKey.value || currentRoomKeyVersion.value !== e2eeState.value?.activeKeyVersion) {
    return 'This device does not currently have access to the active encrypted room key for this space.'
  }

  return null
})

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

function clearCurrentRoomKey() {
  currentRoomKey.value = null
  currentRoomKeyVersion.value = null
}

async function resolveRoomKeyForVersion(
  keyVersion: number,
  localKey: StoredChatKeyMaterial | null = null,
): Promise<CryptoKey | null> {
  const userId = currentUserId.value
  if (!userId) {
    return null
  }

  const storedRoomKey = await chatKeyStore.loadRoomKey(userId, props.spaceId, keyVersion)
  if (storedRoomKey) {
    if (e2eeState.value?.activeKeyVersion === keyVersion) {
      currentRoomKey.value = storedRoomKey
      currentRoomKeyVersion.value = keyVersion
    }
    return storedRoomKey
  }

  if (
    e2eeState.value?.activeKeyVersion !== keyVersion
    || !e2eeState.value.currentUserWrappedKey
  ) {
    return null
  }

  const keyMaterial = localKey ?? await chatKeyStore.loadCurrentUserKey(userId)
  if (!keyMaterial) {
    return null
  }

  const unwrappedRoomKey = await unwrapRoomKeyFromPublisher(
    keyMaterial.privateKey,
    e2eeState.value.currentUserWrappedKey.publisherPublicKeyJwk,
    e2eeState.value.currentUserWrappedKey.wrapNonce,
    e2eeState.value.currentUserWrappedKey.wrappedKeyCiphertext,
  )
  await chatKeyStore.saveRoomKey(userId, props.spaceId, keyVersion, unwrappedRoomKey)
  currentRoomKey.value = unwrappedRoomKey
  currentRoomKeyVersion.value = keyVersion
  return unwrappedRoomKey
}

async function syncCurrentRoomKey(localKey: StoredChatKeyMaterial | null = null) {
  const activeKeyVersion = e2eeState.value?.activeKeyVersion
  if (!activeKeyVersion) {
    clearCurrentRoomKey()
    return
  }

  try {
    const roomKey = await resolveRoomKeyForVersion(activeKeyVersion, localKey)
    if (!roomKey) {
      clearCurrentRoomKey()
    }
  } catch {
    clearCurrentRoomKey()
  }
}

async function resolveDisplayMessage(message: SpaceChatMessage): Promise<SpaceChatMessage> {
  if (message.ciphertext && message.nonce && message.keyVersion && message.contentType) {
    try {
      const roomKey = await resolveRoomKeyForVersion(message.keyVersion)
      if (!roomKey) {
        return {
          ...message,
          encrypted: true,
          bodyState: 'undecryptable',
          displayBody: 'Unable to decrypt this message on this device.',
        }
      }

      const displayBody = await decryptSpaceChatMessage(
        roomKey,
        message.spaceId,
        message.keyVersion,
        message.nonce,
        message.ciphertext,
        message.contentType,
      )

      return {
        ...message,
        encrypted: true,
        bodyState: 'decrypted',
        displayBody,
      }
    } catch {
      return {
        ...message,
        encrypted: true,
        bodyState: 'undecryptable',
        displayBody: 'Unable to decrypt this message on this device.',
      }
    }
  }

  return {
    ...message,
    encrypted: false,
    bodyState: 'legacy-plaintext',
    displayBody: message.body ?? '',
  }
}

async function resolvePageItems(items: SpaceChatMessage[]): Promise<SpaceChatMessage[]> {
  return Promise.all(items.map(resolveDisplayMessage))
}

async function applyResolvedPage(page: SpaceChatMessagePage) {
  messages.value = await resolvePageItems(page.items)
  hasMore.value = page.hasMore
  nextCursorBeforeCreatedAt.value = page.nextCursorBeforeCreatedAt
  nextCursorBeforeMessageId.value = page.nextCursorBeforeMessageId
}

async function loadEncryptedChatBootstrapState() {
  currentUserChatKey.value = null
  e2eeState.value = null
  localKeyFingerprint.value = null
  e2eeStatusError.value = null
  clearCurrentRoomKey()

  const userId = currentUserId.value
  if (!userId || !authStore.user?.chatE2eeEnabled) {
    browserSupportsEncryptedChatSetup.value = true
    return
  }

  try {
    const support = await detectBrowserCryptoSupport()
    browserSupportsEncryptedChatSetup.value = support.subtleAvailable && support.indexedDbAvailable && support.cryptoKeyStructuredCloneAvailable

    if (!browserSupportsEncryptedChatSetup.value) {
      return
    }

    const localKey = await chatKeyStore.loadCurrentUserKey(userId)
    localKeyFingerprint.value = localKey?.fingerprint ?? null

    const [keyResponse, stateResponse] = await Promise.all([
      spaceChatService.getCurrentUserChatKey(),
      spaceChatService.getE2eeState(props.spaceId),
    ])
    currentUserChatKey.value = keyResponse
    e2eeState.value = stateResponse
    await syncCurrentRoomKey(localKey)
  } catch (error) {
    e2eeStatusError.value = extractErrorMessage(error)
  }
}

async function setupEncryptedChatOnThisDevice() {
  const userId = currentUserId.value
  if (!userId || isSettingUpEncryptedChat.value) {
    return
  }

  isSettingUpEncryptedChat.value = true
  e2eeStatusError.value = null

  try {
    const support = await detectBrowserCryptoSupport()
    browserSupportsEncryptedChatSetup.value = support.subtleAvailable && support.indexedDbAvailable && support.cryptoKeyStructuredCloneAvailable

    if (!browserSupportsEncryptedChatSetup.value) {
      e2eeStatusError.value = 'This browser cannot store the encrypted chat key material required for setup.'
      return
    }

    const material = await generateChatKeyPairMaterial()
    await chatKeyStore.saveCurrentUserKey(userId, material)
    localKeyFingerprint.value = material.fingerprint

    currentUserChatKey.value = await spaceChatService.upsertCurrentUserChatKey({
      algorithm: material.algorithm,
      publicKeyJwk: material.publicKeyJwkJson,
      fingerprint: material.fingerprint,
    })

    e2eeState.value = await spaceChatService.getE2eeState(props.spaceId)
    await syncCurrentRoomKey({
      userId,
      algorithm: material.algorithm,
      fingerprint: material.fingerprint,
      publicKeyJwkJson: material.publicKeyJwkJson,
      privateKey: material.privateKey,
      createdAt: material.createdAt,
    })
    await authStore.fetchCurrentUser().catch(() => null)
  } catch (error) {
    e2eeStatusError.value = extractErrorMessage(error)
  } finally {
    isSettingUpEncryptedChat.value = false
  }
}

function resolveRotationReason(): string {
  if ((e2eeState.value?.activeKeyVersion ?? 0) < 1) {
    return 'INITIAL_SETUP'
  }

  return e2eeState.value?.requiresRekey ? 'MEMBERSHIP_CHANGED' : 'MANUAL_ROTATION'
}

async function publishEncryptedRoomKey() {
  const userId = currentUserId.value
  if (!userId || isPublishingEncryptedRoomKey.value || !canManageEncryptedRoomKey.value) {
    return
  }

  isPublishingEncryptedRoomKey.value = true
  publishEncryptedRoomKeyError.value = null
  publishEncryptedRoomKeySuccess.value = null

  try {
    const localKey = await chatKeyStore.loadCurrentUserKey(userId)
    if (!localKey) {
      throw new Error('This device does not currently hold the active encrypted chat private key.')
    }

    const recipients = await spaceChatService.listE2eeRecipients(props.spaceId)
    const recipientsMissingKeys = recipients.filter((recipient) => !recipient.keyRegistered || !recipient.publicKeyJwk)
    if (recipientsMissingKeys.length > 0) {
      const names = recipientsMissingKeys.map((recipient) => recipient.displayName).join(', ')
      throw new Error(`Encrypted room key publishing is blocked until every participant registers a device key. Missing: ${names}`)
    }

    const roomKey = await generateSpaceRoomKey()
    const wrappedRecipients = await Promise.all(
      recipients.map(async (recipient: SpaceChatE2eeRecipientResponse) => ({
        recipientUserId: recipient.userId,
        ...(await wrapRoomKeyForRecipient(localKey.privateKey, recipient.publicKeyJwk ?? '', roomKey)),
      })),
    )

    const publishResponse = await spaceChatService.publishE2eeKeyVersion(props.spaceId, {
      keyVersion: (e2eeState.value?.activeKeyVersion ?? 0) + 1,
      rotationReason: resolveRotationReason(),
      recipients: wrappedRecipients,
    })

    await chatKeyStore.saveRoomKey(userId, props.spaceId, publishResponse.keyVersion, roomKey)
    currentRoomKey.value = roomKey
    currentRoomKeyVersion.value = publishResponse.keyVersion
    e2eeState.value = await spaceChatService.getE2eeState(props.spaceId)
    publishEncryptedRoomKeySuccess.value = publishResponse.keyVersion === 1
      ? 'Encrypted room key initialized for this space.'
      : `Encrypted room key version ${publishResponse.keyVersion} published successfully.`
  } catch (error) {
    publishEncryptedRoomKeyError.value = extractErrorMessage(error)
  } finally {
    isPublishingEncryptedRoomKey.value = false
  }
}

async function loadInitial() {
  isLoading.value = true
  loadError.value = null
  refreshError.value = null
  loadOlderError.value = null

  try {
    const page = await spaceChatService.listMessages(props.spaceId, { limit: INITIAL_PAGE_SIZE })
    await applyResolvedPage(page)
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

    messages.value = await resolvePageItems(mergeMessages(messages.value, page.items))
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
    messages.value = await resolvePageItems(mergeMessages(messages.value, page.items))
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
    let createdMessage: SpaceChatMessage

    if (encryptedChatBootstrapEnabled.value) {
      e2eeState.value = await spaceChatService.getE2eeState(props.spaceId)
      await syncCurrentRoomKey()

      if (sendDisabledReason.value) {
        throw new Error(sendDisabledReason.value)
      }

      createdMessage = await spaceChatService.createMessage(props.spaceId, await encryptSpaceChatMessage(
        currentRoomKey.value as CryptoKey,
        props.spaceId,
        e2eeState.value?.activeKeyVersion as number,
        draftBody.value,
      ))
    } else {
      createdMessage = await spaceChatService.createMessage(props.spaceId, {
        body: draftBody.value,
      })
    }

    messages.value = await resolvePageItems(mergeMessages(messages.value, [createdMessage]))
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
  () => [props.spaceId, currentUserId.value, authStore.user?.chatE2eeEnabled],
  () => {
    messages.value = []
    draftBody.value = ''
    sendError.value = null
    publishEncryptedRoomKeyError.value = null
    publishEncryptedRoomKeySuccess.value = null
    void loadInitial()
    void loadEncryptedChatBootstrapState()
    startPolling()
  },
  { immediate: true },
)

onBeforeUnmount(() => {
  stopPolling()
})
</script>

<template>
  <section class="page-section desktop-page-panel flex min-h-[32rem] flex-col space-y-5">
    <div class="panel-header flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
      <div class="max-w-3xl">
        <h3 class="panel-title text-2xl">Space chat</h3>
        <p class="panel-copy">
          Chat is shared by everyone who already has access to this space. Messages stay readable
          after archival, but new posts are blocked once the space is archived.
        </p>
      </div>
      <button type="button" class="btn-secondary self-start" @click="loadInitial">Refresh chat</button>
    </div>

    <div v-if="props.archived" class="alert-error">
      This space is archived. Existing chat history remains visible, but new messages are disabled.
    </div>

    <section
      v-if="encryptedChatBootstrapEnabled"
      class="rounded-2xl border border-black/10 bg-[var(--color-surface-2)] p-5 text-sm"
    >
      <div class="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
        <div class="space-y-2">
          <h4 class="font-display text-lg font-semibold text-[var(--color-heading)]">Encrypted chat setup</h4>
          <p class="leading-6 text-[var(--color-text-soft)]">
            Browser key registration is now available for this space. Messages still use the current chat flow
            for now, but this device can already prepare and register its encrypted-chat identity.
          </p>
        </div>
        <div class="flex flex-wrap gap-2 text-xs uppercase tracking-[0.16em] text-[var(--color-text-soft)]">
          <span class="status-pill">E2EE {{ currentUserChatKey?.e2eeEnabled ? 'enabled' : 'planned' }}</span>
          <span v-if="e2eeState?.activeKeyVersion !== null && e2eeState?.activeKeyVersion !== undefined" class="status-pill">
            Key v{{ e2eeState.activeKeyVersion }}
          </span>
          <span v-if="e2eeState?.requiresRekey" class="status-pill status-pill-warning">Rekey required</span>
        </div>
      </div>

      <div v-if="e2eeStatusError" class="alert-error mt-4">{{ e2eeStatusError }}</div>
      <div v-else-if="!browserSupportsEncryptedChatSetup" class="alert-error mt-4">
        This browser does not support the Web Crypto + IndexedDB features required for encrypted chat setup.
      </div>
      <div v-else-if="encryptedChatReadyOnThisDevice" class="mt-4 rounded-2xl border border-emerald-500/20 bg-emerald-500/5 p-4 text-[var(--color-text)]">
        This device already has the active encrypted chat key registered.
        <div class="mt-2 text-xs text-[var(--color-text-soft)]">
          Fingerprint: {{ currentUserChatKey?.fingerprint }}
        </div>
      </div>
      <div v-else-if="encryptedChatNeedsSetup" class="mt-4 space-y-3 rounded-2xl border border-[var(--color-accent)]/20 bg-[var(--color-accent)]/5 p-4 text-[var(--color-text)]">
        <p>This account has not registered an encrypted chat key yet for this device.</p>
        <button type="button" class="btn-secondary" :disabled="isSettingUpEncryptedChat" @click="setupEncryptedChatOnThisDevice">
          {{ isSettingUpEncryptedChat ? 'Preparing encrypted chat…' : 'Set up encrypted chat on this device' }}
        </button>
      </div>
      <div v-else-if="encryptedChatMissingLocalKey || encryptedChatLocalKeyMismatch" class="mt-4 space-y-3 rounded-2xl border border-amber-500/20 bg-amber-500/5 p-4 text-[var(--color-text)]">
        <p>
          This browser does not currently hold the active encrypted chat key for your account.
          Registering here will replace the currently registered device key.
        </p>
        <button type="button" class="btn-secondary" :disabled="isSettingUpEncryptedChat" @click="setupEncryptedChatOnThisDevice">
          {{ isSettingUpEncryptedChat ? 'Registering this device…' : 'Register this device key' }}
        </button>
      </div>

      <div v-if="canManageEncryptedRoomKey" class="mt-4 space-y-3 rounded-2xl border border-black/10 bg-white/60 p-4 text-[var(--color-text)]">
        <div class="flex flex-col gap-2 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p class="font-medium text-[var(--color-heading)]">Encrypted room key management</p>
            <p class="mt-1 leading-6 text-[var(--color-text-soft)]">
              Staff can initialize the encrypted room key for this space and publish a new version after membership changes.
            </p>
          </div>
          <button type="button" class="btn-secondary self-start" :disabled="isPublishingEncryptedRoomKey" @click="publishEncryptedRoomKey">
            {{ isPublishingEncryptedRoomKey ? 'Publishing room key…' : encryptedRoomKeyActionLabel }}
          </button>
        </div>

        <div v-if="publishEncryptedRoomKeyError" class="alert-error">{{ publishEncryptedRoomKeyError }}</div>
        <div v-else-if="publishEncryptedRoomKeySuccess" class="rounded-xl border border-emerald-500/20 bg-emerald-500/5 px-4 py-3 text-sm text-[var(--color-text)]">
          {{ publishEncryptedRoomKeySuccess }}
        </div>
      </div>
    </section>

    <div class="min-h-0 flex-1">
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
    </div>

    <SpaceChatComposer
      v-model="draftBody"
      :is-sending="isSending"
      :send-error="sendError"
      :is-archived="props.archived"
      :is-encrypted-chat-active="encryptedChatBootstrapEnabled"
      :send-disabled-reason="sendDisabledReason"
      @submit="sendMessage"
    />
  </section>
</template>


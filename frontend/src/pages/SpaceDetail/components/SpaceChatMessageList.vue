<script setup lang="ts">
import type { SpaceChatMessage } from '@/types/spaceChat'

const props = defineProps<{
  messages: SpaceChatMessage[]
  currentUserId: string | null
  isLoading: boolean
  loadError: string | null
  refreshError: string | null
  hasMore: boolean
  isLoadingOlder: boolean
  loadOlderError: string | null
}>()

const emit = defineEmits<{
  (e: 'retry'): void
  (e: 'loadOlder'): void
}>()

function formatDate(value: string): string {
  return new Intl.DateTimeFormat(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function isOwnMessage(message: SpaceChatMessage): boolean {
  return Boolean(props.currentUserId) && props.currentUserId === message.authorUserId
}
</script>

<template>
  <div class="space-y-4">
    <div v-if="props.loadError && props.messages.length === 0" class="space-y-3">
      <div class="alert-error">{{ props.loadError }}</div>
      <button type="button" class="btn-secondary" @click="emit('retry')">Retry chat load</button>
    </div>

    <div v-else-if="props.isLoading && props.messages.length === 0" class="empty-state">
      Loading chat…
    </div>

    <template v-else>
      <div class="flex flex-col gap-3">
        <button
          v-if="props.hasMore"
          type="button"
          class="btn-secondary self-start"
          :disabled="props.isLoadingOlder"
          @click="emit('loadOlder')"
        >
          {{ props.isLoadingOlder ? 'Loading older messages…' : 'Load older messages' }}
        </button>

        <div v-if="props.loadOlderError" class="alert-error">{{ props.loadOlderError }}</div>
        <div v-if="props.refreshError" class="alert-error">{{ props.refreshError }}</div>
      </div>

      <div v-if="props.messages.length === 0" class="empty-state">
        No messages yet. Start the conversation.
      </div>

      <div v-else class="space-y-3">
        <article
          v-for="message in props.messages"
          :key="message.id"
          class="rounded-2xl border p-4 shadow-sm"
          :class="isOwnMessage(message)
            ? 'border-[var(--color-accent)]/30 bg-[var(--color-accent)]/5'
            : 'border-black/10 bg-white/80'"
        >
          <div class="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
            <div>
              <p class="font-medium text-[var(--color-heading)]">{{ message.authorDisplayName }}</p>
              <p class="text-xs uppercase tracking-[0.18em] text-[var(--color-text-soft)]">
                {{ isOwnMessage(message) ? 'You' : 'Space member' }}
              </p>
            </div>
            <p class="text-sm text-[var(--color-text-soft)]">{{ formatDate(message.createdAt) }}</p>
          </div>
          <p class="mt-3 whitespace-pre-wrap break-words text-base leading-7 text-[var(--color-text)]">
            {{ message.body }}
          </p>
        </article>
      </div>
    </template>
  </div>
</template>


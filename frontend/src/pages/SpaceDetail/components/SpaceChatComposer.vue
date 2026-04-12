<script setup lang="ts">
const props = defineProps<{
  modelValue: string
  isSending: boolean
  sendError: string | null
  isArchived: boolean
  isEncryptedChatActive?: boolean
  sendDisabledReason?: string | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'submit'): void
}>()

function updateValue(event: Event) {
  emit('update:modelValue', (event.target as HTMLTextAreaElement).value)
}
</script>

<template>
  <section class="rounded-2xl border border-black/10 bg-[var(--color-surface-2)] p-5">
    <div class="flex items-start justify-between gap-4">
      <div>
        <h4 class="font-display text-lg font-semibold text-[var(--color-heading)]">Send a message</h4>
        <p class="mt-1 text-sm leading-6 text-[var(--color-text-soft)]">
          {{ props.isEncryptedChatActive
            ? 'Messages sent from this device now use the encrypted room key when one is available.'
            : 'Messages are stored as plain text and new posts are blocked once the space is archived.' }}
        </p>
      </div>
      <span v-if="props.isArchived" class="status-pill status-pill-warning">Read-only</span>
    </div>

    <div v-if="props.sendError" class="alert-error mt-4">{{ props.sendError }}</div>
    <div v-else-if="props.sendDisabledReason" class="alert-error mt-4">{{ props.sendDisabledReason }}</div>

    <form class="mt-4 space-y-4" @submit.prevent="emit('submit')">
      <label class="block">
        <span class="field-label">Message</span>
        <textarea
          :value="props.modelValue"
          rows="4"
          class="form-input"
          maxlength="2000"
          placeholder="Write a message to everyone in this space…"
          :disabled="props.isSending || props.isArchived || Boolean(props.sendDisabledReason)"
          @input="updateValue"
        />
      </label>

      <div class="flex items-center justify-between gap-4">
        <p class="text-sm text-[var(--color-text-soft)]">
          {{ props.modelValue.trim().length }}/2000 characters
        </p>
        <button
          type="submit"
          class="btn-primary"
          :disabled="props.isSending || props.isArchived || Boolean(props.sendDisabledReason) || props.modelValue.trim().length === 0"
        >
          {{ props.isSending ? 'Sending…' : 'Send message' }}
        </button>
      </div>
    </form>
  </section>
</template>


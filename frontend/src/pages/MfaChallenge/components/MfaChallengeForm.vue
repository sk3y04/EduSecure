<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  errorMessage: string | null
  isSubmitting: boolean
}>()

const emit = defineEmits<{
  (e: 'verify', code: string): void
  (e: 'startOver'): void
}>()

const verificationCode = ref('')

function handleVerify() {
  emit('verify', verificationCode.value)
}
</script>

<template>
  <form class="mt-6 space-y-5" @submit.prevent="handleVerify">
    <div v-if="props.errorMessage" class="alert-error">
      {{ props.errorMessage }}
    </div>

    <label class="block">
      <span class="field-label">Verification code</span>
      <input
        v-model="verificationCode"
        type="text"
        inputmode="numeric"
        autocomplete="one-time-code"
        required
        class="form-input tracking-[0.35em]"
        placeholder="123456"
      />
    </label>

    <div class="flex flex-col gap-3 sm:flex-row">
      <button type="submit" class="btn-primary flex-1" :disabled="props.isSubmitting">
        {{ props.isSubmitting ? 'Verifying…' : 'Verify and continue' }}
      </button>
      <button type="button" class="btn-secondary" @click="emit('startOver')">
        Start over
      </button>
    </div>
  </form>
</template>


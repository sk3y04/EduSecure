<script setup lang="ts">
import { reactive } from 'vue'

const props = defineProps<{
  isDisabling: boolean
}>()

const emit = defineEmits<{
  (e: 'disable', payload: { password: string; verificationCode: string }): void
}>()

const form = reactive({ password: '', verificationCode: '' })

function handleDisable() {
  emit('disable', { ...form })
  form.password = ''
  form.verificationCode = ''
}
</script>

<template>
  <section class="page-section">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Disable MFA</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        The backend requires both the current password and a valid TOTP or recovery code to
        disable MFA, which helps prevent easy downgrade attacks.
      </p>
    </div>

    <form class="mt-6 space-y-4" @submit.prevent="handleDisable">
      <label class="block">
        <span class="field-label">Current password</span>
        <input v-model="form.password" type="password" required class="form-input" />
      </label>
      <label class="block">
        <span class="field-label">TOTP or recovery code</span>
        <input v-model="form.verificationCode" type="text" required class="form-input" />
      </label>
      <button type="submit" class="btn-danger" :disabled="props.isDisabling">
        {{ props.isDisabling ? 'Disabling…' : 'Disable MFA' }}
      </button>
    </form>
  </section>
</template>


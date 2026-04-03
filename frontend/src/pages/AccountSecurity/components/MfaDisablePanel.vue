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
  <section class="surface-panel p-8">
    <div class="border-b border-slate-200 pb-5">
      <h3 class="text-xl font-semibold text-slate-900">Disable MFA</h3>
      <p class="mt-2 text-sm leading-6 text-slate-600">
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


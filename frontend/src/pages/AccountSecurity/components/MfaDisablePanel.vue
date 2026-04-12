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
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Disable MFA</h3>
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


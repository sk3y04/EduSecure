<script setup lang="ts">
import { reactive } from 'vue'

const props = defineProps<{
  createError: string | null
  createSuccess: string | null
  isCreating: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { name: string; code: string; description: string }): void
}>()

const form = reactive({ name: '', code: '', description: '' })

function handleSubmit() {
  emit('submit', { ...form })
  form.name = ''
  form.code = ''
  form.description = ''
}
</script>

<template>
  <section class="page-section">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Create space</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Create a collaboration area with a stable code that can later anchor roster management and
        related academic workflows.
      </p>
    </div>

    <div v-if="props.createError" class="alert-error mb-4">{{ props.createError }}</div>
    <div v-if="props.createSuccess" class="alert-success mb-4">{{ props.createSuccess }}</div>

    <form class="grid gap-5 lg:grid-cols-2" @submit.prevent="handleSubmit">
      <label class="block lg:col-span-1">
        <span class="field-label">Name</span>
        <input v-model="form.name" type="text" required class="form-input" placeholder="Applied Cryptography Group A" />
      </label>

      <label class="block lg:col-span-1">
        <span class="field-label">Code</span>
        <input v-model="form.code" type="text" required class="form-input" placeholder="CRYPTO-A" />
      </label>

      <label class="block lg:col-span-2">
        <span class="field-label">Description</span>
        <textarea
          v-model="form.description"
          required
          rows="4"
          class="form-input"
          placeholder="Shared space for lectures, secure resources, and student coordination."
        />
      </label>

      <div class="lg:col-span-2">
        <button type="submit" class="btn-primary" :disabled="props.isCreating">
          {{ props.isCreating ? 'Creating…' : 'Create space' }}
        </button>
      </div>
    </form>
  </section>
</template>


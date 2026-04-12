<script setup lang="ts">
import { reactive } from 'vue'

const props = defineProps<{
  createError: string | null
  createSuccess: string | null
  isCreating: boolean
  spaceName?: string | null
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { title: string; description: string; dueAt: string }): void
}>()

const form = reactive({ title: '', description: '', dueAt: '' })

function handleSubmit() {
  emit('submit', { ...form })
  form.title = ''
  form.description = ''
  form.dueAt = ''
}
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Create assignment</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Create a coursework entry for {{ props.spaceName ?? 'the current space' }}.
      </p>
    </div>

    <div v-if="props.createError" class="alert-error mb-4">
      {{ props.createError }}
    </div>
    <div v-if="props.createSuccess" class="alert-success mb-4">
      {{ props.createSuccess }}
    </div>

    <form class="grid flex-1 gap-5 lg:grid-cols-2" @submit.prevent="handleSubmit">
      <label class="block lg:col-span-1">
        <span class="field-label">Title</span>
        <input
          v-model="form.title"
          type="text"
          required
          class="form-input"
          placeholder="Cryptography Coursework 1"
        />
      </label>

      <label class="block lg:col-span-1">
        <span class="field-label">Due date</span>
        <input v-model="form.dueAt" type="datetime-local" required class="form-input" />
      </label>

      <label class="block lg:col-span-2">
        <span class="field-label">Description</span>
        <textarea
          v-model="form.description"
          required
          rows="4"
          class="form-input"
          placeholder="Submit your signed artefact or simulated content."
        />
      </label>

      <div class="lg:col-span-2">
        <button type="submit" class="btn-primary" :disabled="props.isCreating">
          {{ props.isCreating ? 'Creating…' : 'Create assignment' }}
        </button>
      </div>
    </form>
  </section>
</template>



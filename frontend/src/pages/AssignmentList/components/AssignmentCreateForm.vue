<script setup lang="ts">
import { reactive } from 'vue'

const props = defineProps<{
  createError: string | null
  createSuccess: string | null
  isCreating: boolean
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
  <section class="surface-panel p-8">
    <div class="mb-6 border-b border-slate-200 pb-5">
      <h3 class="text-xl font-semibold text-slate-900">Create assignment</h3>
      <p class="mt-2 text-sm leading-6 text-slate-600">
        A compact form for creating coursework entries without distracting styling.
      </p>
    </div>

    <div v-if="props.createError" class="alert-error mb-4">
      {{ props.createError }}
    </div>
    <div v-if="props.createSuccess" class="alert-success mb-4">
      {{ props.createSuccess }}
    </div>

    <form class="grid gap-5 lg:grid-cols-2" @submit.prevent="handleSubmit">
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



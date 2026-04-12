<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{
  initialName: string
  initialCode: string
  initialDescription: string
  initialArchived: boolean
  updateError: string | null
  updateSuccess: string | null
  isUpdating: boolean
}>()

const emit = defineEmits<{
  (e: 'submit', payload: { name: string; code: string; description: string; archived: boolean }): void
}>()

const form = reactive({
  name: props.initialName,
  code: props.initialCode,
  description: props.initialDescription,
  archived: props.initialArchived,
})

watch(
  () => [props.initialName, props.initialCode, props.initialDescription, props.initialArchived],
  ([name, code, description, archived]) => {
    form.name = name as string
    form.code = code as string
    form.description = description as string
    form.archived = archived as boolean
  },
)

function handleSubmit() {
  emit('submit', { ...form })
}
</script>

<template>
  <section class="page-section desktop-page-panel flex h-full flex-col">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Update space</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Ownership stays enforced on the backend. This form only appears when the API has
        already confirmed management access.
      </p>
    </div>

    <div v-if="props.updateError" class="alert-error mb-4">{{ props.updateError }}</div>
    <div v-if="props.updateSuccess" class="alert-success mb-4">{{ props.updateSuccess }}</div>

    <form class="grid flex-1 gap-5" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="field-label">Name</span>
        <input v-model="form.name" type="text" required class="form-input" />
      </label>

      <label class="block">
        <span class="field-label">Code</span>
        <input v-model="form.code" type="text" required class="form-input" />
      </label>

      <label class="block">
        <span class="field-label">Description</span>
        <textarea v-model="form.description" rows="5" required class="form-input" />
      </label>

      <label class="flex items-center gap-3 rounded-md border border-black/10 bg-[var(--color-surface-2)] px-4 py-3 text-base text-[var(--color-text)]">
        <input v-model="form.archived" type="checkbox" class="h-4 w-4" />
        Archive this space
      </label>

      <div>
        <button type="submit" class="btn-primary" :disabled="props.isUpdating">
          {{ props.isUpdating ? 'Saving…' : 'Save changes' }}
        </button>
      </div>
    </form>
  </section>
</template>


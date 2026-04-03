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
  <div class="surface-panel p-8">
    <div class="mb-6 border-b border-slate-200 pb-5">
      <h3 class="text-xl font-semibold text-slate-900">Update space</h3>
      <p class="mt-2 text-sm leading-6 text-slate-600">
        Ownership stays enforced on the backend. This form only appears when the API has
        already confirmed management access.
      </p>
    </div>

    <div v-if="props.updateError" class="alert-error mb-4">{{ props.updateError }}</div>
    <div v-if="props.updateSuccess" class="alert-success mb-4">{{ props.updateSuccess }}</div>

    <form class="grid gap-5" @submit.prevent="handleSubmit">
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

      <label class="flex items-center gap-3 rounded-sm border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-700">
        <input v-model="form.archived" type="checkbox" class="h-4 w-4" />
        Archive this space
      </label>

      <div>
        <button type="submit" class="btn-primary" :disabled="props.isUpdating">
          {{ props.isUpdating ? 'Saving…' : 'Save changes' }}
        </button>
      </div>
    </form>
  </div>
</template>


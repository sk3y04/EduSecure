<script setup lang="ts">
import { ref } from 'vue'

const props = defineProps<{
  membershipError: string | null
  membershipSuccess: string | null
  isAddingStudent: boolean
}>()

const emit = defineEmits<{
  (e: 'addStudent', email: string): void
}>()

const studentEmail = ref('')

function handleSubmit() {
  emit('addStudent', studentEmail.value)
  studentEmail.value = ''
}
</script>

<template>
  <section class="page-section">
    <div class="panel-header">
      <h3 class="font-display text-xl font-semibold text-[var(--color-heading)]">Add student</h3>
      <p class="mt-2 text-base leading-7 text-[var(--color-text-soft)]">
        Membership is assigned by student email. The backend validates existence, role, and
        duplicate space membership.
      </p>
    </div>

    <div v-if="props.membershipError" class="alert-error mb-4">{{ props.membershipError }}</div>
    <div v-if="props.membershipSuccess" class="alert-success mb-4">{{ props.membershipSuccess }}</div>

    <form class="space-y-4" @submit.prevent="handleSubmit">
      <label class="block">
        <span class="field-label">Student email</span>
        <input
          v-model="studentEmail"
          type="email"
          required
          class="form-input"
          placeholder="student@example.com"
        />
      </label>

      <div>
        <button type="submit" class="btn-primary" :disabled="props.isAddingStudent">
          {{ props.isAddingStudent ? 'Adding…' : 'Add student' }}
        </button>
      </div>
    </form>
  </section>
</template>


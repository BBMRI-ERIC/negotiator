<template>
  <NegotiatorModal :id="id" :title="title">
    <template #body>
      <p>
        {{ text }}
      </p>
      <input
        v-model="name"
        placeholder="name"
        class="form-control text-secondary-text mb-2"
        :required="true"
      />
      <input
        v-model="label"
        placeholder="label"
        class="form-control text-secondary-text mb-2"
        :required="true"
      />
      <textarea
        v-model="description"
        placeholder="description"
        class="form-control text-secondary-text mb-2"
        :required="true"
      />
    </template>
    <template #footer>
      <button type="button" class="btn btn-dark" data-bs-dismiss="modal" @click="message = ''">
        Cancel
      </button>
      <button type="button" class="btn btn-danger" data-bs-dismiss="modal" @click="emitConfirm">
        Confirm
      </button>
    </template>
  </NegotiatorModal>
</template>

<script setup>
import NegotiatorModal from './NegotiatorModal.vue'
import { ref } from 'vue'

defineProps({
  id: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  text: {
    type: String,
    required: true,
  },
})

const name = ref('')
const label = ref('')
const description = ref('')

const emit = defineEmits(['confirm'])

function emitConfirm() {
  emit('confirm', name.value, label.value, description.value)
  name.value = ''
  label.value = ''
  description.value = ''
}
</script>

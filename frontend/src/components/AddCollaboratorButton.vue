<template>
  <div>
    <button
      class="pdf-text-hover cursor-pointer btn btn-link p-0"
      :style="{ color: uiConfigurationTheme.primaryTextColor }"
      @click="openModal"
    >
      <i class="bi bi-person-plus-fill me-1"></i>
      Add Collaborator
    </button>

    <AddCollaboratorModal
      :is-open="isModalOpen"
      :negotiation-id="negotiationId"
      @update:is-open="isModalOpen = $event"
      @collaborator-added="handleCollaboratorAdded"
      @cancel="closeModal"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import AddCollaboratorModal from '@/components/modals/AddCollaboratorModal.vue'

const props = defineProps({
  negotiationId: {
    type: String,
    required: true,
  },
})

const uiConfigurationStore = useUiConfiguration()
const uiConfigurationTheme = computed(() => uiConfigurationStore.uiConfiguration?.theme)

const isModalOpen = ref(false)
const emit = defineEmits(['collaborator-added'])

function openModal() {
  isModalOpen.value = true
}

function closeModal() {
  isModalOpen.value = false
}

function handleCollaboratorAdded(user) {
  emit('collaborator-added', user)
}
</script>

<style scoped>
.btn-link {
  text-decoration: none;
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 1rem;
  background: none;
  border: none;
}

.btn-link:hover,
.btn-link:hover i {
  color: var(--bs-primary) !important;
}

.btn-link:focus {
  outline: 2px solid #80bdff;
  outline-offset: 2px;
}
</style>


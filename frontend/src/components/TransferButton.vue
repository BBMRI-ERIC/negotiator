<template>
  <div>
    <!-- Transfer Button -->
    <button
      class="pdf-text-hover cursor-pointer btn btn-link p-0"
      @click="openModal"
      :style="{ color: uiConfigurationTheme.primaryTextColor }"
    >
      <i class="bi bi-person-fill-add me-1"></i>
      Transfer
    </button>

    <!-- Transfer Negotiation Modal -->
    <TransferNegotiationModal
      :is-open="isModalOpen"
      :subject-id="subjectId"
      :negotiation-id="negotiationId"
      @update:is-open="isModalOpen = $event"
      @update:subject-id="subjectId = $event"
      @confirm="handleConfirm"
      @cancel="closeModal"
    />
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import TransferNegotiationModal from '@/components/modals/TransferNegotiationModal.vue'

defineProps({
  negotiationId: {
    type: String,
    required: true
  },
})

const uiConfigurationStore = useUiConfiguration()
const uiConfigurationTheme = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

const isModalOpen = ref(false)
const subjectId = ref('')

const emit = defineEmits(['transfer-negotiation'])

function openModal() {
  isModalOpen.value = true
  subjectId.value = '' // Reset input
}

function closeModal() {
  isModalOpen.value = false
  subjectId.value = ''
}

function handleConfirm(subjectId) {
  emit('transfer-negotiation', subjectId)
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
  color: #dc3545 !important; /* Bootstrap's danger red */
}

.btn-link:focus {
  outline: 2px solid #80bdff;
  outline-offset: 2px;
}
</style>

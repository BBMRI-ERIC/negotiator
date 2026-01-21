<template>
  <div class="request-summary form d-flex flex-column mb-4">
    <div v-if="discoveryServices.length > 0" class="alert alert-info mb-3" role="alert">
      <div class="d-flex align-items-start">
        <i class="bi bi-lightbulb-fill me-2 mt-1 flex-shrink-0"></i>
        <div class="flex-grow-1">
          <strong>Need more resources?</strong>
          <p class="mb-2 small">
            You can find and add additional resources from these catalogues:
          </p>
          <div class="discovery-services-list">
            <a
              v-for="service in discoveryServices"
              :key="service.id"
              :href="service.url"
              target="_blank"
              rel="noopener noreferrer"
              class="badge bg-primary text-decoration-none me-2 mb-1"
            >
              {{ service.name }}
              <i class="bi bi-box-arrow-up-right ms-1 small"></i>
            </a>
          </div>
        </div>
      </div>
    </div>

    <div class="resources-wrapper">
      <ResourcesList
        :resources="props.requestSummary?.resources"
        @remove-resource="handleRemoveResource"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import ResourcesList from '../../components/ResourcesList.vue'
import { useDiscoveryServicesStore } from '../../store/discoveryServices.js'
import { useNegotiationPageStore } from '../../store/negotiationPage.js'
import { useNotificationsStore } from '../../store/notifications.js'

const props = defineProps({
  requestSummary: {
    type: Object,
    required: true,
    default: () => {},
  },
  negotiationId: {
    type: String,
    required: false,
    default: null,
  },
})

const emit = defineEmits(['resource-removed'])

const discoveryServicesStore = useDiscoveryServicesStore()
const negotiationPageStore = useNegotiationPageStore()
const notificationsStore = useNotificationsStore()
const discoveryServices = ref([])

onMounted(async () => {
  try {
    discoveryServices.value = await discoveryServicesStore.fetchDiscoveryServices()
  } catch (error) {
    console.error('Error fetching discovery services:', error)
    // Silently fail - the tip just won't show if there's an error
  }
})

async function handleRemoveResource(resource) {
  if (!props.negotiationId) {
    notificationsStore.setNotification('Cannot remove resource: negotiation ID not provided', 'danger')
    return
  }

  const success = await negotiationPageStore.removeResource(props.negotiationId, resource.id)
  if (success) {
    emit('resource-removed', resource.id)
  }
}
</script>

<style scoped>
.alert-info {
  border-radius: 0.375rem;
  font-size: 0.95rem;
  background-color: #e7f5ff;
  border-color: #74c0fc;
  color: #1864ab;
}

.alert-info .bi-lightbulb-fill {
  color: #228be6;
  font-size: 1.1rem;
}

.discovery-services-list {
  display: flex;
  flex-wrap: wrap;
  gap: 0.25rem;
}

.badge {
  font-size: 0.85rem;
  font-weight: 500;
  padding: 0.4rem 0.65rem;
  transition: all 0.2s ease;
}

.badge:hover {
  opacity: 0.85;
  transform: translateY(-1px);
}

.resources-wrapper {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 0.5rem;
}

.resources-wrapper::-webkit-scrollbar {
  width: 8px;
}

.resources-wrapper::-webkit-scrollbar-track {
  background: #f1f3f5;
  border-radius: 4px;
}

.resources-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 4px;
}

.resources-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}

@media (max-width: 768px) {
  .resources-wrapper {
    max-height: 50vh;
  }
}
</style>

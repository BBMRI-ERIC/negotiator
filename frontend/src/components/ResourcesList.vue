<template>
  <div class="shopping-cart-container">
    <!-- Cart Header -->
    <div class="cart-header mb-5 d-flex align-items-center justify-content-between">
      <div>
        <h2 class="fw-bold mb-0">Selected {{ $t('negotiationPage.resources', 2) }}</h2>
        <p class="mb-0 mt-2 text-muted">
          {{ numberOfResources }} {{ $t('negotiationPage.resources', numberOfResources) }}
        </p>
      </div>
      <div v-if="activeDiscoveryServices.length > 0" class="dropdown">
        <PrimaryButton
          size="sm"
          :backgroundColor="primaryColor"
          :textColor="'#ffffff'"
          :hoverBackgroundColor="primaryColor"
          :hoverTextColor="'#ffffff'"
          data-bs-toggle="dropdown"
          aria-expanded="false"
        >
          <i class="bi bi-plus-lg me-1"></i>Add {{ $t('negotiationPage.resources', 2) }}
        </PrimaryButton>
        <ul class="dropdown-menu dropdown-menu-end">
          <li v-for="service in activeDiscoveryServices" :key="service.id">
            <a
              class="dropdown-item"
              :href="service.url"
              target="_blank"
              rel="noopener noreferrer"
            >
              <i class="bi bi-box-arrow-up-right me-2"></i>{{ service.name }}
            </a>
          </li>
        </ul>
      </div>
    </div>

    <!-- Cart Items -->
    <div class="cart-items-container">
      <div v-for="resource in props.resources" :key="resource.id" class="cart-item">
        <div class="item-details">
          <div class="item-icon" :style="iconStyle">
            <i class="bi bi-database-fill"></i>
          </div>
          <div class="item-info">
            <div class="item-name">
              <a
                v-if="resource.uri"
                :href="resource.uri"
                target="_blank"
                rel="noopener noreferrer"
                class="item-name-link"
                :style="{ color: linksColor }"
              >
                {{ resource.name }}
                <i class="bi bi-box-arrow-up-right ms-1"></i>
              </a>
              <span v-else>{{ resource.name }}</span>
            </div>
            <div v-if="resource.description" class="item-description">
              {{ resource.description }}
            </div>
            <div class="item-org-tag">
              <i class="bi bi-building me-1"></i>{{ resource.organization.name }}
            </div>
          </div>
        </div>
        <button
          class="remove-btn"
          :disabled="numberOfResources === 1"
          @click="handleRemoveResource(resource)"
          :title="
            numberOfResources === 1
              ? 'Cannot remove the last resource. Please delete the draft completely instead.'
              : `Remove ${resource.name}`
          "
        >
          <i class="bi bi-trash3"></i>
        </button>
      </div>

      <!-- Empty State -->
      <div v-if="numberOfResources === 0" class="empty-cart">
        <i class="bi bi-cart-x empty-icon"></i>
        <p class="empty-text">No resources selected</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useDiscoveryServicesStore } from '@/store/discoveryServices.js'
import PrimaryButton from '@/components/ui/buttons/PrimaryButton.vue'

const props = defineProps({
  resources: {
    type: Array[Object],
    default: [],
  },
})

const emit = defineEmits(['remove-resource'])

const numberOfResources = computed(() => props.resources.length)

function handleRemoveResource(resource) {
  emit('remove-resource', resource)
}

const uiConfigurationStore = useUiConfiguration()
const discoveryServicesStore = useDiscoveryServicesStore()
const activeDiscoveryServices = ref([])

const primaryColor = computed(
  () => uiConfigurationStore.uiConfiguration?.theme?.primaryColor || '#26336B',
)

onMounted(async () => {
  const services = await discoveryServicesStore.fetchDiscoveryServices()
  activeDiscoveryServices.value = services.filter((s) => s.active)
})
const linksColor = computed(
  () => uiConfigurationStore.uiConfiguration?.theme?.linksColor || primaryColor.value,
)

const iconStyle = computed(() => ({
  backgroundColor: primaryColor.value + '1a',
  color: primaryColor.value,
}))
</script>

<style scoped>
.shopping-cart-container {
  background-color: transparent;
  border-radius: 0;
  box-shadow: none;
  overflow: visible;
  min-height: auto;
}

.cart-header {
  padding: 0;
  background: transparent;
  border-bottom: none;
  margin-bottom: 0;
}

.cart-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #212529;
}

.cart-header p {
  font-size: 1rem;
  color: #6c757d;
  line-height: 1.5;
}

.cart-items-container {
  overflow-y: visible;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.cart-items-container::-webkit-scrollbar {
  display: none;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.25rem;
  background-color: #ffffff;
  border: 1px solid #e8ecef;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s ease;
}

.cart-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.cart-item:last-child {
  border-bottom: 1px solid #e8ecef;
}

.item-details {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  flex: 1;
  min-width: 0;
}

.item-icon {
  flex-shrink: 0;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 1.5rem;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 1.125rem;
  font-weight: 600;
  color: #212529;
  margin-bottom: 0.5rem;
  line-height: 1.4;
}

.item-name-link {
  font-size: 1.125rem;
  font-weight: 600;
  text-decoration: none;
}

.item-name-link:hover {
  text-decoration: underline;
}

.item-name-link .bi-box-arrow-up-right {
  font-size: 0.8rem;
  opacity: 0.6;
}

.item-description {
  font-size: 0.875rem;
  color: #6c757d;
  margin-top: 0.25rem;
  line-height: 1.4;
}

.item-org-tag {
  display: inline-flex;
  align-items: center;
  margin-top: 0.5rem;
  font-size: 0.8rem;
  font-weight: 500;
  color: #495057;
  background-color: #f1f3f5;
  border: 1px solid #dee2e6;
  border-radius: 20px;
  padding: 0.2rem 0.65rem;
}

.item-org-tag .bi-building {
  font-size: 0.75rem;
}

.remove-btn {
  flex-shrink: 0;
  background: none;
  border: 2px solid #dee2e6;
  color: #dc3545;
  font-size: 1.125rem;
  padding: 0.5rem;
  cursor: pointer;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
}

.remove-btn:hover {
  border-color: #dc3545;
  color: #c82333;
}

.remove-btn:active {
  background-color: #f8f9fa;
}

.remove-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  color: #adb5bd;
  border-color: #dee2e6;
}

.remove-btn:disabled:hover {
  border-color: #dee2e6;
  color: #adb5bd;
}

.empty-cart {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 4rem 1rem;
  color: #adb5bd;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1.5rem;
  opacity: 0.5;
}

.empty-text {
  font-size: 1.25rem;
  margin: 0;
  color: #6c757d;
}

@media (max-width: 768px) {
  .shopping-cart-container {
    min-height: auto;
  }

  .cart-header h2 {
    font-size: 1.25rem;
  }

  .cart-header p {
    font-size: 0.9rem;
  }

  .cart-items-container {
    padding: 0;
  }

  .cart-item {
    padding: 0.875rem 0 0.875rem 1.5rem;
  }

  .item-details {
    gap: 1rem;
  }

  .item-icon {
    width: 48px;
    height: 48px;
    font-size: 1.25rem;
  }

  .item-name {
    font-size: 1rem;
  }

  .item-description {
    font-size: 0.8rem;
  }

  .remove-btn {
    width: 32px;
    height: 32px;
    font-size: 1rem;
  }
}
</style>

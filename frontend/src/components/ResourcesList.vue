<template>
  <div class="shopping-cart-container">
    <!-- Cart Header -->
    <div class="cart-header mb-5">
      <h2 class="fw-bold mb-0">{{ $t('negotiationPage.resources') }}</h2>
      <p class="mb-0 mt-2 text-muted">
        {{ numberOfResources }} {{ numberOfResources === 1 ? 'resource' : 'resources' }} selected
      </p>
    </div>

    <!-- Cart Items -->
    <div class="cart-items-container">
      <div
        v-for="[orgId, org] in Object.entries(organizationsById)"
        :key="orgId"
        class="organization-section"
      >
        <!-- Organization/Vendor Header -->
        <div class="vendor-header">
          <div class="d-flex align-items-center gap-3">
            <i class="bi bi-building vendor-icon"></i>
            <span class="vendor-name">{{ org.name }}</span>
          </div>
          <span class="vendor-badge">{{ org.resources.length }}</span>
        </div>

        <!-- Resources/Items List -->
        <div class="items-list">
          <div v-for="resource in org.resources" :key="resource.id" class="cart-item">
            <div class="item-details">
              <div class="item-icon">
                <i class="bi bi-database-fill"></i>
              </div>
              <div class="item-info">
                <div class="item-name">{{ resource.name }}</div>
                <div class="item-meta">
                  <i class="bi bi-tag-fill me-2"></i>
                  <span class="item-id">{{ resource.sourceId }}</span>
                </div>
              </div>
            </div>
            <button
              class="remove-btn"
              @click="handleRemoveResource(resource)"
              :title="`Remove ${resource.name}`"
            >
              <i class="bi bi-trash3"></i>
            </button>
          </div>
        </div>
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
import { computed } from 'vue'

const props = defineProps({
  resources: {
    type: Array[Object],
    default: [],
  },
})

const emit = defineEmits(['remove-resource'])

const organizationsById = computed(() => {
  return props.resources.reduce((organizations, resource) => {
    if (resource.organization.externalId in organizations) {
      organizations[resource.organization.externalId].resources.push(resource)
    } else {
      organizations[resource.organization.externalId] = {
        name: resource.organization.name,
        resources: [resource],
      }
    }
    return organizations
  }, {})
})

const numberOfResources = computed(() => {
  return props.resources.length
})

function handleRemoveResource(resource) {
  emit('remove-resource', resource)
}
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
}

.cart-items-container::-webkit-scrollbar {
  display: none;
}

.organization-section {
  background-color: transparent;
  border-radius: 0;
  margin-bottom: 1.5rem;
  overflow: visible;
}

.organization-section:last-child {
  margin-bottom: 0;
}

.vendor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  background-color: transparent;
  border-left: 3px solid #495057;
  padding-left: 1rem;
  margin-bottom: 0.75rem;
}

.vendor-icon {
  font-size: 1.5rem;
  color: #495057;
}

.vendor-name {
  font-weight: 700;
  font-size: 1.125rem;
  color: #212529;
}

.vendor-badge {
  background-color: #495057;
  color: #ffffff;
  font-size: 0.875rem;
  font-weight: 700;
  padding: 0.4rem 0.875rem;
  border-radius: 16px;
}

.items-list {
  background-color: transparent;
  padding: 0;
}

.cart-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 0 1rem 2rem;
  background-color: transparent;
  border-left: 2px solid #e9ecef;
  margin-bottom: 0;
  border-bottom: 1px solid #f1f3f5;
}

.cart-item:last-child {
  border-bottom: none;
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
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-radius: 12px;
  color: #1976d2;
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

.item-meta {
  display: flex;
  align-items: center;
  font-size: 0.95rem;
  color: #6c757d;
}

.item-id {
  font-family: 'Courier New', monospace;
  background-color: #f1f3f5;
  padding: 0.25rem 0.75rem;
  border-radius: 6px;
  color: #495057;
  font-size: 0.875rem;
  border: 1px solid #dee2e6;
  font-weight: 500;
}

.remove-btn {
  flex-shrink: 0;
  background: none;
  border: 2px solid #dee2e6;
  color: #dc3545;
  font-size: 1.5rem;
  padding: 0.625rem;
  cursor: pointer;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
}

.remove-btn:hover {
  border-color: #dc3545;
  color: #c82333;
}

.remove-btn:active {
  background-color: #f8f9fa;
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

  .item-meta {
    font-size: 0.85rem;
  }

  .item-id {
    font-size: 0.8rem;
  }

  .remove-btn {
    width: 42px;
    height: 42px;
    font-size: 1.25rem;
  }
}
</style>

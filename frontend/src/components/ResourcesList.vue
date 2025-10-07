<template>
  <div>
    <span class="fs-5 fw-bold mt-3" :style="{ color: uiConfiguration?.primaryTextColor }">
      {{ $t('negotiationPage.resources') }} ({{ numberOfResources }})
    </span>
    <div class="resource-cards">
      <div v-for="[orgId, org] in Object.entries(organizationsById)" :key="orgId" class="card my-2">
        <div
          class="card-header cursor-pointer fw-bold d-flex justify-content-between align-items-center"
          :style="{ color: uiConfiguration?.primaryTextColor }"
          data-bs-toggle="collapse"
          :data-bs-target="`#card-body-block-${getElementIdFromResourceId(orgId)}`"
          aria-expanded="false"
          :aria-controls="`card-body-block-${getElementIdFromResourceId(orgId)}`"
        >
          <span>{{ `${org.name} (${org.resources.length})` }}</span>
          <i class="bi bi-chevron-down collapse-icon"></i>
        </div>
        <div :id="`card-body-block-${getElementIdFromResourceId(orgId)}`" class="collapse">
          <div v-for="resource in org.resources" :key="resource.id" class="card-body">
            <span
              :style="{ color: uiConfiguration?.secondaryTextColor }"
              :for="getElementIdFromResourceId(resource.id)"
            >
              {{ resource.name }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'

const props = defineProps({
  resources: {
    type: Array[Object],
    default: [],
  },
})

const uiConfigurationStore = useUiConfiguration()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

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

function getElementIdFromResourceId(resourceId) {
  if (typeof resourceId !== String) {
    return resourceId
  } else {
    return resourceId.replaceAll(':', '_')
  }
}
</script>

<style scoped>
.resource-cards {
  max-height: 300px;
  overflow-y: scroll;
}

.collapse-icon {
  transition: transform 0.3s ease;
  font-size: 1.2rem;
}

.card-header[aria-expanded='true'] .collapse-icon {
  transform: rotate(180deg);
}

.card-header:hover {
  background-color: rgba(0, 0, 0, 0.03);
}
</style>

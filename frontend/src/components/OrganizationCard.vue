<script setup>
</script>
<template>
  <div class="card mb-2">
    <OrganizationHeader
      :org-id="orgId"
      :org="org"
      :ui-configuration="uiConfiguration"
      :sorted-states="sortedStates"
      :dropdown-visible="dropdownVisible"
      @toggle-dropdown="toggleDropdown"
      @toggle-collapse="toggleCollapse"
      @update-org-status="handleUpdateOrgStatus"
    />
    <div :id="`card-body-block-${sanitizeId(orgId)}`" class="collapse multi-collapse">
      <ResourceItem
        v-for="resource in resources"
        :key="resource.id"
        :resource="resource"
        :ui-configuration="uiConfiguration"
        :isAdmin="isAdmin"
        @open-form-modal="openFormModal"
        @open-modal="openModal"
        @update-resource-state="updateResourceState"
        @editInfoSubmission="editInfoSubmission"
      />

      <div v-if="pageInfo && pageInfo.totalPages > 1" class="d-flex justify-content-between align-items-center p-2 border-top">
        <button
            class="btn btn-sm btn-outline-secondary"
            :disabled="pageInfo.number === 0 || isFetchingResources"
            @click="fetchResources(pageInfo.number - 1)"
        >
          Previous
        </button>
        <span class="small text-muted">
          Page {{ pageInfo.number + 1 }} of {{ pageInfo.totalPages }}
        </span>
        <button
            class="btn btn-sm btn-outline-secondary"
            :disabled="pageInfo.number >= pageInfo.totalPages - 1 || isFetchingResources"
            @click="fetchResources(pageInfo.number + 1)"
        >
          Next
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, onBeforeMount, ref, watch } from 'vue'
import OrganizationHeader from './OrganizationHeader.vue'
import ResourceItem from './ResourceItem.vue'
import { useNegotiationPageStore } from '../store/negotiationPage.js'

const props = defineProps({
  orgId: { type: String, default: undefined },
  org: { type: Object, default: () => ({}) },
  resourceStates: { type: Array, default: () => [] },
  negotiationId: { type: String, default: undefined },
  uiConfiguration: { type: Object, required: true },
  isAdmin: { type: Boolean, default: false },
  resourcesLastUpdated: { type: Number, default: 0 },
})
const emit = defineEmits([
  'open-form-modal',
  'open-modal',
  'update-resource-state',
  'update-org-status',
  'edit-info-submission',
  'toggle-collapse',
])

const resources = ref([])
const isFetchingResources = ref( false )
const pageInfo = ref({ number: 0, totalPages: 0, size: 20 })
const negotiationPageStore = useNegotiationPageStore()

const dropdownVisible = reactive({})

const sanitizeId = (id) => id.replaceAll(':', '_')

const toggleDropdown = (orgId) => {
  // Close other dropdowns in this card
  Object.keys(dropdownVisible).forEach((key) => {
    if (key !== orgId) dropdownVisible[key] = false
  })
  dropdownVisible[orgId] = !dropdownVisible[orgId]
}

const toggleCollapse = (orgId) => {
  emit('toggle-collapse', orgId)
}

const handleUpdateOrgStatus = (state, organization, orgId) => {
  emit('update-org-status', state, organization, orgId)
}

const sortedStates = computed(() =>
  props.resourceStates.slice().sort((a, b) => Number(a.ordinal) - Number(b.ordinal)),
)

const openModal = (href, resourceId) => {
  emit('open-modal', href, resourceId)
}

const openFormModal = (href) => {
  emit('open-form-modal', href)
}

const updateResourceState = (link) => {
  emit('update-resource-state', link)
}

function editInfoSubmission(href) {
  emit('edit-info-submission', href)
}

async function fetchResources(targetPage = 0) {
  if (isFetchingResources.value) return

  isFetchingResources.value = true

  try {
    const response = await negotiationPageStore.retrieveResourcesByNegotiationIdAndOrganizationIdPaginated(
        props.negotiationId,
        props.orgId,
        { page: targetPage, size: pageInfo.value.size || 20, sort: 'id' }
    )

    if (response !== undefined) {
      resources.value = response?._embedded?.resources || []

      if (response?.page) {
        pageInfo.value = response.page
      }
    }
  } finally {
    isFetchingResources.value = false
  }
}

watch(
    () => props.resourcesLastUpdated,
    () => {
      fetchResources(pageInfo.value.number)
    }
)

onBeforeMount(async () => {
  await fetchResources(0)
})

defineExpose({
  fetchResources,
})

</script>

<style scoped>
/* You can place card-level styles here if needed */
</style>

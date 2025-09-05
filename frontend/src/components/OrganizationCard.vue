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
        v-for="resource in org.resources"
        :key="resource.id"
        :resource="resource"
        :ui-configuration="uiConfiguration"
        :isAdmin="isAdmin"
        @open-form-modal="openFormModal"
        @open-modal="openModal"
        @update-resource-state="updateResourceState"
        @editInfoSubmission="editInfoSubmission"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, reactive } from 'vue'
import OrganizationHeader from './OrganizationHeader.vue'
import ResourceItem from './ResourceItem.vue'

const props = defineProps({
  orgId: { type: String, default: undefined },
  org: { type: Object, default: () => ({}) },
  resourceStates: { type: Array, default: () => [] },
  negotiationId: { type: String, default: undefined },
  uiConfiguration: { type: Object, required: true },
  isAdmin: { type: Boolean, default: false },
})
const emit = defineEmits([
  'open-form-modal',
  'open-modal',
  'update-resource-state',
  'update-org-status',
  'edit-info-submission',
])

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
</script>

<style scoped>
/* You can place card-level styles here if needed */
</style>

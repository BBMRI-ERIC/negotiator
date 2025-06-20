<template>
  <div class="card-header">
    <div class="form-check d-flex align-items-center">
      <!-- Collapse Organization Header -->
      <div
        class="collapse-organization d-flex align-items-center cursor-pointer"
        data-bs-toggle="collapse"
        aria-expanded="false"
        :data-bs-target="`#card-body-block-${sanitizeId(orgId)}`"
        :aria-controls="`card-body-block-${sanitizeId(orgId)}`"
        @click="onToggleCollapse"
      >
        <i class="bi" :class="isCollapsed ? 'bi-chevron-right' : 'bi-chevron-down'" />
        <i class="bi bi-buildings mx-2" :style="{ color: uiConfiguration.primaryTextColor }" />
        <span class="fw-bold" :style="{ color: uiConfiguration.secondaryTextColor }">
          {{ org.name }}
        </span>
      </div>

      <!-- Status Dropdown -->
      <div class="status-dropdown-container ms-auto" :data-org-id="orgId">
        <button
          type="button"
          class="status-box p-1 d-flex align-items-center btn"
          title="Select current status. The term Resource is abstract and can for example refer to biological samples, datasets or a service such as sequencing."
          @click.stop="onToggleDropdown"
        >
          <span class="badge text-wrap" :class="getStatusColor(org.status)">
            <i :class="getStatusIcon(org.status)" class="px-1" />
            {{ org.status?.replace(/_/g, ' ') || '' }}
          </span>
          <i
            v-if="org.updatable"
            class="bi icon-smaller mx-1"
            :class="dropdownVisible[orgId] ? 'bi-caret-up-fill' : 'bi-caret-down-fill'"
          />
        </button>
        <ul v-if="org.updatable && dropdownVisible[orgId]" class="dropdown-menu show">
          <li
            v-for="state in sortedStates"
            :key="state.value"
            class="dropdown-item cursor-pointer"
            data-bs-toggle="modal"
            data-bs-target="#statusUpdateModal"
            @click="onUpdateOrgStatus(state)"
          >
            <i :class="getStatusIcon(state.value)" class="px-1" />
            {{ state.label }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getStatusColor, getStatusIcon } from '../composables/utils.js'

const props = defineProps({
  orgId: { type: String, required: true },
  org: { type: Object, required: true },
  uiConfiguration: { type: Object, required: true },
  sortedStates: { type: Array, default: () => [] },
  dropdownVisible: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['toggle-dropdown', 'toggle-collapse', 'update-org-status'])

const isCollapsed = ref(false)
const sanitizeId = (id) => id.replaceAll(':', '_')

const onToggleDropdown = () => {
  emit('toggle-dropdown', props.orgId)
}

const onToggleCollapse = () => {
  isCollapsed.value = !isCollapsed.value
  emit('toggle-collapse', props.orgId)
}

const onUpdateOrgStatus = (state) => {
  emit('update-org-status', state, props.org, props.orgId)
}
</script>

<style scoped>
/* Collapse Icon */
.collapse-organization {
  transition: all 0.2s ease;
}

/* Status Dropdown Container */
.status-dropdown-container {
  position: relative;
  display: inline-block;
}

.status-dropdown-container .dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 0.5rem;
  z-index: 1000;
}

/* Mobile adjustments */
@media (max-width: 576px) {
  .status-dropdown-container {
    width: 100%;
    text-align: center;
    margin-top: 0.5rem;
  }

  .status-dropdown-container .dropdown-menu {
    position: relative;
    top: 0;
    left: 0;
    width: 100%;
    margin-top: 0.5rem;
    text-align: left;
  }
}

/* Icon size */
.icon-smaller {
  font-size: 0.8rem;
}
</style>

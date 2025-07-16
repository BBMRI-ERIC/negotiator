<template>
  <div class="mt-3">
    <div v-if="loading" class="text-center p-4">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Loading organizations...</span>
      </div>
      <p class="mt-2">Loading organizations data...</p>
    </div>
    <template v-else>
      <div class="summary-section mb-4 p-3 bg-light border rounded">
        <h4>Summary</h4>
        <ul class="list-unstyled mb-0">
          <li><strong>Total Organizations:</strong> {{ organizations.length }}</li>
          <li><strong>Active Organizations:</strong> {{ activeOrganizations.length }}</li>
          <li><strong>Withdrawn Organizations:</strong> {{ withdrawnOrganizations.length }}</li>
          <li><strong>Total Resources:</strong> {{ totalResources }}</li>
          <li><strong>Active Resources:</strong> {{ activeResources }}</li>
          <li>
            <strong>Resources Without Representatives:</strong>
            {{ resourcesWithoutRepresentatives }}
          </li>
        </ul>
      </div>

      <!-- Active Organizations -->
      <div v-if="activeOrganizations.length > 0">
        <h5 class="section-title">
          <i class="bi bi-building text-success"></i>
          Active Organizations ({{ activeOrganizations.length }})
        </h5>
        <div class="organizations-section">
          <OrganizationCard
            v-for="organization in activeOrganizations"
            :key="organization.id"
            :organization="organization"
          />
        </div>
      </div>

      <!-- Withdrawn Organizations -->
      <div v-if="withdrawnOrganizations.length > 0" class="withdrawn-section">
        <h5 class="section-title mt-4">
          <i class="bi bi-archive text-warning"></i>
          Withdrawn Organizations ({{ withdrawnOrganizations.length }})
        </h5>
        <div class="alert alert-warning" role="alert">
          <i class="bi bi-exclamation-triangle-fill me-2"></i>
          These organizations are no longer active and cannot participate in new negotiations.
        </div>
        <div class="organizations-section">
          <OrganizationCard
            v-for="organization in withdrawnOrganizations"
            :key="organization.id"
            :organization="organization"
            :is-withdrawn="true"
          />
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="organizations.length === 0" class="text-center p-4">
        <i class="bi bi-building text-muted" style="font-size: 3rem"></i>
        <p class="text-muted mt-2">No organizations found in this network.</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import OrganizationCard from './OrganizationCard.vue'

const props = defineProps({
  organizations: {
    type: Array,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

const activeOrganizations = computed(() => {
  return props.organizations.filter((org) => !org.withdrawn)
})

const withdrawnOrganizations = computed(() => {
  return props.organizations.filter((org) => org.withdrawn)
})

const totalResources = computed(() => {
  return props.organizations.reduce((sum, org) => sum + org.resources.length, 0)
})

const activeResources = computed(() => {
  return props.organizations.reduce((sum, org) => {
    if (org.withdrawn) return sum
    return sum + org.resources.filter((resource) => !resource.withdrawn).length
  }, 0)
})

const resourcesWithoutRepresentatives = computed(() => {
  return props.organizations.reduce(
    (sum, org) =>
      sum +
      org.resources.filter(
        (resource) => resource.representatives.length === 0 && resource.withdrawn === false,
      ).length,
    0,
  )
})
</script>

<style scoped>
.summary-section {
  background-color: #f8f9fa;
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  padding: 1rem;
  margin-bottom: 1rem;
}

.summary-section h4 {
  color: #343a40;
  margin-bottom: 1rem;
}

.list-unstyled {
  list-style: none;
  padding-left: 0;
}

.mb-0 {
  margin-bottom: 0;
}

.mb-4 {
  margin-bottom: 1.5rem;
}

.mt-3 {
  margin-top: 1rem;
}

.p-3 {
  padding: 1rem;
}

.bg-light {
  background-color: #f8f9fa !important;
}

.border {
  border: 1px solid #dee2e6 !important;
}

.rounded {
  border-radius: 0.375rem !important;
}

.spinner-border {
  width: 3rem;
  height: 3rem;
  border: 0.25em solid currentcolor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spinner-border 0.75s linear infinite;
}

@keyframes spinner-border {
  to {
    transform: rotate(360deg);
  }
}

.visually-hidden {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  white-space: nowrap !important;
  border: 0 !important;
}

.p-4 {
  padding: 1.5rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

.text-center {
  text-align: center;
}

.section-title {
  color: #343a40;
  margin-bottom: 1rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.organizations-section {
  margin-bottom: 2rem;
}

.withdrawn-section {
  border-top: 2px solid #dee2e6;
  padding-top: 2rem;
  margin-top: 2rem;
}

.alert {
  border-radius: 0.375rem;
  padding: 1rem;
  margin-bottom: 1rem;
}

.alert-warning {
  background-color: #fff3cd;
  border: 1px solid #ffecb5;
  color: #664d03;
}

.me-2 {
  margin-right: 0.5rem;
}

.mt-4 {
  margin-top: 1.5rem;
}

.text-success {
  color: #198754 !important;
}
</style>

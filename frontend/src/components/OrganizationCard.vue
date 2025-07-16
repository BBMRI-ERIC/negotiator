<template>
  <div class="card mb-3 position-relative" :class="{ 'withdrawn-card': isWithdrawn }">
    <!-- Organization Header -->
    <div class="card-header d-flex justify-content-between align-items-center" :class="{ 'withdrawn-header': isWithdrawn }">
      <h5 class="mb-0" :class="{ 'text-muted': isWithdrawn }">
        {{ organization.name }} ({{ organization.externalId }})
        <span v-if="isWithdrawn" class="withdrawn-badge">
          <i class="bi bi-archive-fill"></i>
          WITHDRAWN
        </span>
      </h5>
      <!-- Status Icon -->
      <div>
        <i
          v-if="isWithdrawn"
          class="bi bi-x-octagon-fill text-warning"
          title="This organization is withdrawn and no longer active"
        ></i>
        <i
          v-else-if="allResourcesHaveRepresentatives(organization.resources)"
          class="bi bi-check-circle-fill text-success"
          title="All resources have representatives"
        ></i>
        <i
          v-else
          class="bi bi-exclamation-triangle-fill text-warning"
          title="At least one resource has no representative"
        ></i>
      </div>
    </div>

    <!-- Organization Body -->
    <div class="card-body">
      <p :class="{ 'text-muted': isWithdrawn }">{{ organization.description }}</p>

      <!-- Withdrawn notice -->
      <div v-if="isWithdrawn" class="alert alert-warning mb-3" role="alert">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        This organization is no longer active and cannot participate in new negotiations.
      </div>

      <ul class="list-unstyled">
        <li>
          <i class="bi bi-envelope"></i>
          <a :href="'mailto:' + organization.contactEmail" :class="{ 'text-muted': isWithdrawn }">
            {{ organization.contactEmail }}
          </a>
        </li>
        <li>
          <i class="bi bi-globe"></i>
          <a :href="organization.uri" target="_blank" :class="{ 'text-muted': isWithdrawn }">
            {{ organization.uri }}
          </a>
        </li>
      </ul>

      <!-- Resources Accordion -->
      <div class="accordion" :id="'accordionResources_' + organization.id">
        <!-- Toggle for Resources List -->
        <div class="accordion-item">
          <h2 class="accordion-header" :id="'headingResources_' + organization.id">
            <button
              class="accordion-button collapsed"
              :class="{ 'withdrawn-accordion': isWithdrawn }"
              type="button"
              data-bs-toggle="collapse"
              :data-bs-target="'#collapseResources_' + organization.id"
              aria-expanded="false"
              :aria-controls="'collapseResources_' + organization.id"
            >
              Resources ({{ organization.resources.length }})
              <span v-if="activeResourcesCount < organization.resources.length" class="ms-2 text-muted small">
                {{ activeResourcesCount }} active, {{ withdrawnResourcesCount }} withdrawn
              </span>
            </button>
          </h2>
          <div
            :id="'collapseResources_' + organization.id"
            class="accordion-collapse collapse"
            :aria-labelledby="'headingResources_' + organization.id"
          >
            <div class="accordion-body">
              <!-- Individual Resources Accordion -->
              <ResourceItem
                v-for="resource in organization.resources"
                :key="resource.id"
                :resource="resource"
                :organization-withdrawn="isWithdrawn"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ResourceItem from './ResourceItem.vue'

const props = defineProps({
  organization: {
    type: Object,
    required: true,
  },
  isWithdrawn: {
    type: Boolean,
    default: false,
  },
})

const activeResourcesCount = computed(() => {
  return props.organization.resources.filter(resource => !resource.withdrawn).length
})

const withdrawnResourcesCount = computed(() => {
  return props.organization.resources.filter(resource => resource.withdrawn).length
})

// Helper function to check if all resources have representatives
const allResourcesHaveRepresentatives = (resources) => {
  return resources.every(
    (resource) => resource.representatives.length > 0 || resource.withdrawn === true,
  )
}
</script>

<style scoped>
.card {
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  background-color: #fff;
}

.card-header {
  background-color: #f8f9fa;
  border-bottom: 1px solid #dee2e6;
  padding: 1rem;
}

.card-body {
  padding: 1rem;
}

.card-body p {
  color: #6c757d;
  margin-bottom: 1rem;
}

.card-body ul li {
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.card-body a {
  color: #0366d6;
  text-decoration: none;
}

.card-body a:hover {
  text-decoration: underline;
}

.mb-3 {
  margin-bottom: 1rem;
}

.mb-0 {
  margin-bottom: 0;
}

.position-relative {
  position: relative;
}

.d-flex {
  display: flex;
}

.justify-content-between {
  justify-content: space-between;
}

.align-items-center {
  align-items: center;
}

.list-unstyled {
  list-style: none;
  padding-left: 0;
}

.text-success {
  color: #198754 !important;
}

.text-warning {
  color: #ffc107 !important;
}

.accordion {
  border: 1px solid #dee2e6;
  border-radius: 0.375rem;
  margin-top: 1rem;
}

.accordion-item {
  border-bottom: 1px solid #dee2e6;
}

.accordion-item:last-child {
  border-bottom: none;
}

.accordion-button {
  background-color: #f8f9fa;
  border: none;
  padding: 1rem;
  text-align: left;
  width: 100%;
}

.accordion-button:focus {
  box-shadow: none;
  border-color: #dee2e6;
}

.accordion-button.collapsed {
  background-color: #ffffff;
}

.accordion-body {
  padding: 1rem;
  background-color: #ffffff;
}

.withdrawn-card {
  opacity: 0.7;
  border-color: #ffc107;
  background-color: #fffbf0;
}

.withdrawn-header {
  background-color: #fff3cd;
  border-bottom-color: #ffecb5;
}

.withdrawn-badge {
  font-size: 0.75rem;
  background-color: #ffc107;
  color: #000;
  padding: 0.25rem 0.5rem;
  border-radius: 0.25rem;
  margin-left: 0.5rem;
  font-weight: 600;
}

.withdrawn-accordion {
  background-color: #fff3cd;
  color: #664d03;
}

.ms-2 {
  margin-left: 0.5rem;
}

.small {
  font-size: 0.875rem;
}

.alert {
  border-radius: 0.375rem;
  padding: 0.75rem;
  margin-bottom: 1rem;
  border: 1px solid transparent;
}

.alert-warning {
  background-color: #fff3cd;
  border-color: #ffecb5;
  color: #664d03;
}

.me-2 {
  margin-right: 0.5rem;
}

.mb-3 {
  margin-bottom: 1rem;
}
</style>

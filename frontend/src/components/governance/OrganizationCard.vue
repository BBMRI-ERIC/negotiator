<template>
  <div
    class="organization-card"
    :class="{ 'expanded': isExpanded }"
  >
    <!-- Organization Header -->
    <div
      class="organization-header"
      @click="$emit('toggleExpanded')"
      @keydown="handleKeyDown"
      role="button"
      tabindex="0"
      :aria-expanded="isExpanded"
      :aria-controls="`resources-${organization.id}`"
    >
      <div class="organization-info">
        <div class="organization-main">
          <i
            class="expand-icon bi"
            :class="isExpanded ? 'bi-chevron-down' : 'bi-chevron-right'"
          ></i>
          <div class="organization-details">
            <h5 class="organization-name">{{ organization.name }}</h5>
            <div class="organization-meta">
              <span class="external-id">ID: {{ organization.externalId }}</span>
              <span class="separator">•</span>
              <span class="contact-email">{{ organization.contactEmail || 'No contact' }}</span>
              <span class="separator">•</span>
              <span
                class="status-badge"
                :class="organization.withdrawn ? 'status-withdrawn' : 'status-active'"
              >
                {{ organization.withdrawn ? 'Withdrawn' : 'Active' }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="organization-actions">
        <button
          class="btn btn-sm btn-outline-secondary"
          @click.stop="$emit('editOrganization')"
          title="Edit Organization"
        >
          <i class="bi bi-pencil"></i>
        </button>
      </div>
    </div>

    <!-- Resources Dropdown -->
    <ResourcesList
      v-if="isExpanded"
      :id="`resources-${organization.id}`"
      :resources="resources"
      :loading="resourcesLoading"
      @edit-resource="$emit('editResource', $event)"
      @add-resources="$emit('addResources')"
      @representatives-updated="$emit('representativesUpdated', $event)"
    />
  </div>
</template>

<script setup>
import ResourcesList from './ResourcesList.vue'

defineProps({
  organization: {
    type: Object,
    required: true
  },
  isExpanded: {
    type: Boolean,
    required: true
  },
  resources: {
    type: Array,
    default: () => []
  },
  resourcesLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggleExpanded', 'editOrganization', 'editResource', 'addResources', 'representativesUpdated'])

// Keyboard event handler for accessibility
function handleKeyDown(event) {
  if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    emit('toggleExpanded')
  }
}
</script>

<style scoped>
.organization-card {
  border: 1px solid #e9ecef;
  border-radius: 0.5rem;
  overflow: hidden;
  transition: all 0.3s ease;
  background: #ffffff;
  margin-bottom: 1rem;
}

/* Gray styling for withdrawn organizations */
.organization-card:has(.status-withdrawn) {
  background: #f8f9fa;
  border-color: #d1d1d1;
  opacity: 0.7;
}

.organization-card:has(.status-withdrawn) .organization-header {
  background: #f8f9fa;
}

.organization-card:has(.status-withdrawn) .organization-name {
  color: #6c757d !important;
}

.organization-card:has(.status-withdrawn) .organization-meta {
  color: #868e96 !important;
}

.organization-card:has(.status-withdrawn) .expand-icon {
  color: #868e96 !important;
}

.organization-card:has(.status-withdrawn):hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  border-color: #adb5bd;
}

.organization-card.expanded {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  border-color: #0d6efd;
}

/* Override expanded styling for withdrawn organizations */
.organization-card:has(.status-withdrawn).expanded {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #adb5bd;
}

.organization-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem;
  cursor: pointer;
  transition: background-color 0.2s ease;
  background: #ffffff;
}

.organization-header:hover {
  background: #f8f9fa;
}

.organization-info {
  display: flex;
  align-items: center;
  flex: 1;
  justify-content: space-between;
}

.organization-main {
  display: flex;
  align-items: center;
}

.expand-icon {
  font-size: 1.2rem;
  color: #6c757d;
  margin-right: 1rem;
  transition: transform 0.2s ease;
}

.organization-details h5 {
  margin: 0 0 0.25rem 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #2c3e50;
}

.organization-meta {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: #6c757d;
}

.separator {
  color: #dee2e6;
}

.status-badge {
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.status-active {
  background: #d1edff;
  color: #0c63e4;
}

.status-withdrawn {
  background: #f8d7da;
  color: #721c24;
}

@media (max-width: 768px) {
  .organization-header {
    padding: 1rem;
  }

  .organization-info {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}

@media (max-width: 576px) {
  .organization-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.25rem;
  }

  .separator {
    display: none;
  }
}
</style>

<template>
  <div class="filters-container mb-4">
    <div class="row g-3">
      <!-- Status Filter -->
      <div class="col-md-3">
        <label for="statusFilter" class="form-label">Status</label>
        <select
          id="statusFilter"
          v-model="localFilters.statusFilter"
          class="form-select"
          @change="handleStatusChange"
        >
          <option value="active">Active Only</option>
          <option value="withdrawn">Withdrawn Only</option>
          <option value="all">All Organizations</option>
        </select>
      </div>

      <!-- Name Search -->
      <div class="col-md-4">
        <label for="nameSearch" class="form-label">Search Organizations</label>
        <input
          id="nameSearch"
          v-model="localFilters.name"
          type="text"
          class="form-control"
          placeholder="Search by name or external ID..."
          @input="handleNameInput"
        />
      </div>

      <!-- Resource Search -->
      <div class="col-md-4">
        <label for="resourceSearch" class="form-label">Search Resources</label>
        <input
          id="resourceSearch"
          v-model="localFilters.resourceName"
          type="text"
          class="form-control"
          placeholder="Search by resource name..."
          @input="handleResourceInput"
        />
      </div>

      <!-- Clear Filters Button -->
      <div class="col-md-1 d-flex align-items-end">
        <button
          class="btn btn-outline-secondary"
          @click="$emit('clearFilters')"
          :disabled="loading"
          title="Clear all filters"
        >
          <i class="bi bi-x-circle"></i>
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  filters: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['updateFilters', 'clearFilters', 'debouncedSearch', 'applyFilters'])

// Create local reactive copy of filters
const localFilters = ref({ ...props.filters })

// Watch for external filter changes (e.g., from clear filters)
watch(() => props.filters, (newFilters) => {
  localFilters.value = { ...newFilters }
}, { deep: true })

const handleStatusChange = () => {
  emit('updateFilters', { statusFilter: localFilters.value.statusFilter })
  // Status filter should trigger immediate load, not debounced search
  emit('applyFilters')
}

const handleNameInput = () => {
  emit('updateFilters', { name: localFilters.value.name })
  emit('debouncedSearch')
}

const handleResourceInput = () => {
  emit('updateFilters', { resourceName: localFilters.value.resourceName })
  emit('debouncedSearch')
}
</script>

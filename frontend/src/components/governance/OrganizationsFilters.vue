<template>
  <div class="filters-container mb-4">
    <div class="row g-3">
      <!-- Status Filter -->
      <div class="col-md-3">
        <label for="statusFilter" class="form-label">Status</label>
        <select
          id="statusFilter"
          :model-value="filters.statusFilter"
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
          :model-value="filters.name"
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
          :model-value="filters.resourceName"
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
defineProps({
  filters: {
    type: Object,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['updateFilters', 'clearFilters', 'debouncedSearch'])

const handleStatusChange = (event) => {
  emit('updateFilters', { statusFilter: event.target.value })
}

const handleNameInput = (event) => {
  emit('updateFilters', { name: event.target.value })
  emit('debouncedSearch')
}

const handleResourceInput = (event) => {
  emit('updateFilters', { resourceName: event.target.value })
  emit('debouncedSearch')
}
</script>

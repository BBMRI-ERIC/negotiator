<template>
  <div class="filters-container mb-4">
    <div class="row g-3 v-step-governance-2">
      <!-- Status Filter (FilterButton with dropdown) -->
      <div class="col-md-3 d-flex flex-column">
        <label class="form-label">Status</label>
        <div class="dropdown">
          <FilterButton
            :customStyle="
              localFilters.statusFilter !== '' ? returnButtonActiveColor : returnButtonColor
            "
            customClass="dropdown-toggle custom-button-hover w-100"
            :active="localFilters.statusFilter !== ''"
            size="sm"
            dropdown
            type="button"
            v-bind="{
              'data-bs-toggle': 'dropdown',
              'data-bs-auto-close': 'outside',
              'aria-expanded': 'false',
            }"
          >
            {{
              statusOptions.find((opt) => opt.value === localFilters.statusFilter)?.label ||
              'Select Status'
            }}
          </FilterButton>
          <ul class="dropdown-menu w-100" aria-labelledby="dropdownSortingButton" role="menu">
            <div v-for="option in statusOptions" :key="option.value">
              <label
                :class="localFilters.statusFilter === option.value ? 'active' : ''"
                class="dropdown-item status-dropdown-label"
                href="#"
                @click.prevent="handleStatusButton(option.value)"
              >
                {{ option.label }}
              </label>
            </div>
          </ul>
        </div>
      </div>

      <!-- Name Search -->
      <div class="col-md-4">
        <label for="nameSearch" class="form-label">Search Organizations</label>
        <input
          id="nameSearch"
          v-model="localFilters.name"
          type="text"
          class="form-control form-control-sm"
          placeholder="Search by name or external ID..."
          @input="handleNameInput"
        />
      </div>

      <!-- Clear Filters Button (using FilterButton) -->
      <div class="col-md-1 d-flex align-items-end">
        <FilterButton
          customClass="btn-outline-secondary w-50"
          :disabled="loading"
          size="sm"
          title="Clear all filters"
          @click="$emit('clearFilters')"
        >
          <i class="bi bi-x-circle"></i>
        </FilterButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import FilterButton from '@/components/ui/buttons/FilterButton.vue'
// Status filter options
const statusOptions = [
  { value: 'active', label: 'Active Only' },
  { value: 'withdrawn', label: 'Withdrawn Only' },
  { value: 'all', label: 'All Organizations' },
]

const props = defineProps({
  filters: {
    type: Object,
    required: true,
  },
  loading: {
    type: Boolean,
    required: true,
  },
})

const emit = defineEmits(['updateFilters', 'clearFilters', 'debouncedSearch', 'applyFilters'])

// Create local reactive copy of filters
const localFilters = ref({ ...props.filters })

// Watch for external filter changes (e.g., from clear filters)
watch(
  () => props.filters,
  (newFilters) => {
    localFilters.value = { ...newFilters }
  },
  { deep: true },
)

const handleStatusButton = (value) => {
  if (localFilters.value.statusFilter !== value) {
    localFilters.value.statusFilter = value
    emit('updateFilters', { statusFilter: value })
    emit('applyFilters')
  }
}

const returnButtonActiveColor = computed(() => {
  return {
    'border-color': 'var(--bs-sort-filter-button-outline)',
    '--hovercolor': 'var(--bs-sort-filter-button-outline)',
    'background-color': 'var(--bs-sort-filter-button-outline)',
    color: '#FFFFFF',
  }
})
const returnButtonColor = computed(() => {
  return {
    'border-color': 'var(--bs-sort-filter-button-outline)',
    '--hovercolor': 'var(--bs-sort-filter-button-outline)',
    'background-color': '#FFFFFF',
    color: 'var(--bs-sort-filter-button-outline)',
  }
})

const handleNameInput = () => {
  emit('updateFilters', { name: localFilters.value.name })
  emit('debouncedSearch')
}
</script>

<style scoped>
.status-dropdown-label.active,
.status-dropdown-label:hover {
  background-color: var(--bs-sort-filter-button-outline) !important;
}
</style>

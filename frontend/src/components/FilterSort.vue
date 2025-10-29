<template>
  <div class="container d-flex flex-row flex-wrap justify-content-between">
    <div class="d-flex flex-row gap-2 my-2 mx-auto mx-md-0 v-step-20">
      <div class="sort-by v-step-21">
        <FilterButton
          :customStyle="filtersSortData.sortBy !== '' ? returnButtonActiveColor : returnButtonColor"
          customClass="dropdown-toggle custom-button-hover"
          :active="filtersSortData.sortBy !== ''"
          size="sm"
          dropdown
          type="button"
          v-bind="{
            'data-bs-toggle': 'dropdown',
            'data-bs-auto-close': 'outside',
            'aria-expanded': 'false',
          }"
        >
          Sort by
        </FilterButton>
        <ul class="dropdown-menu" aria-labelledby="dropdownSortingButton" role="menu">
          <div v-for="(sort, index) in sortBy" :key="index" class="form-check mx-2 my-2">
            <input
              :id="index"
              v-model="filtersSortData.sortBy"
              class="form-check-input"
              type="radio"
              name="sort"
              :value="sort.value"
              :checked="isChecked(sort.value)"
              @change="emitFilterSortData"
            />
            <label class="form-check-label text-primary-text">
              {{ sort.label }}
            </label>
          </div>
        </ul>
      </div>

      <div class="v-step-22">
        <FilterButton
          :customStyle="returnButtonColor"
          customClass="custom-button-hover"
          size="sm"
          @click="changeSortDirection"
        >
          <template #icon>
            <i v-if="filtersSortData.sortDirection === 'DESC'" class="bi bi-sort-down" />
            <i v-if="filtersSortData.sortDirection === 'ASC'" class="bi bi-sort-up" />
          </template>
        </FilterButton>
      </div>

      <div id="v-step-3 v-step-23" class="filter-by-status">
        <FilterButton
          :customStyle="
            filtersSortData.status.length > 0 ? returnButtonActiveColor : returnButtonColor
          "
          customClass="dropdown-toggle custom-button-hover"
          :active="filtersSortData.status.length > 0"
          size="sm"
          dropdown
          type="button"
          v-bind="{
            'data-bs-toggle': 'dropdown',
            'data-bs-auto-close': 'outside',
            'aria-expanded': 'false',
          }"
        >
          Filter by status
        </FilterButton>
        <ul class="dropdown-menu" aria-labelledby="dropdownSortingButton" role="menu">
          <div v-for="(status, index) in filtersStatus" :key="index" class="form-check mx-2 my-2">
            <input
              v-model="filtersSortData.status"
              class="form-check-input"
              type="checkbox"
              :value="status.value"
              @change="emitFilterSortData"
            />
            <label class="form-check-label text-primary-text">
              {{ status.label }}
            </label>
          </div>
        </ul>
      </div>

      <div id="v-step-4" class="filter-by-org" v-if="filterOrganizations.length > 0">
        <FilterButton
          :customStyle="
            filtersSortData.organizations.length > 0 ? returnButtonActiveColor : returnButtonColor
          "
          customClass="dropdown-toggle custom-button-hover"
          :active="filtersSortData.organizations.length > 0"
          size="sm"
          dropdown
          type="button"
          v-bind="{
            'data-bs-toggle': 'dropdown',
            'data-bs-auto-close': 'outside',
            'aria-expanded': 'false',
          }"
        >
          Filter by Organization
        </FilterButton>
        <ul class="dropdown-menu" aria-labelledby="dropdownSortingButton" role="menu">
          <div
            v-for="(org, index) in filterOrganizations"
            :key="index"
            class="form-check mx-2 my-2"
          >
            <input
              v-model="filtersSortData.organizations"
              class="form-check-input"
              type="checkbox"
              :value="org.id"
              @change="emitFilterSortData"
            />
            <label class="form-check-label text-primary-text">
              {{ org.name }}
            </label>
          </div>
        </ul>
      </div>

      <div class="filter-by-date v-step-24">
        <FilterButton
          :customStyle="
            filtersSortData.dateStart !== '' || filtersSortData.dateEnd !== ''
              ? returnButtonActiveColor
              : returnButtonColor
          "
          customClass="dropdown-toggle custom-button-hover"
          :active="filtersSortData.dateStart !== '' || filtersSortData.dateEnd !== ''"
          size="sm"
          dropdown
          type="button"
          v-bind="{
            'data-bs-toggle': 'dropdown',
            'data-bs-auto-close': 'outside',
            'aria-expanded': 'false',
          }"
        >
          Filter by date
        </FilterButton>
        <ul class="dropdown-menu" aria-labelledby="dropdownMenuButton1">
          <div class="mx-2 my-2 dropdown-contents text-primary-text">
            <div class="d-flex align-items-center mb-2">
              <label class="pe-2 w-25" for="startDate">Start:</label>
              <input
                id="startDate"
                v-model="filtersSortData.dateStart"
                class="form-control form-control-sm text-primary-text"
                type="date"
                @input="emitFilterSortData"
              />
            </div>
            <div class="d-flex align-items-center">
              <label for="endDate" class="pe-3 w-25">End:</label>
              <input
                id="endDate"
                v-model="filtersSortData.dateEnd"
                class="form-control form-control-sm text-primary-text"
                type="date"
                @input="emitFilterSortData"
              />
            </div>
          </div>
        </ul>
      </div>
    </div>

    <div class="my-2 ms-auto v-step-25">
      <FilterButton
        :customStyle="{
          'border-color': 'var(--bs-sort-filter-clear-button-outline)',
          '--hovercolor': 'var(--bs-sort-filter-clear-button-outline)',
          'background-color': '#FFFFFF',
          color: 'var(--bs-sort-filter-clear-button-outline)',
        }"
        customClass="custom-button-hover"
        size="sm"
        @click="clearAllFilters"
      >
        <template #icon>
          <i class="bi bi-x-circle" />
        </template>
        Clear all filters
      </FilterButton>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { ROLES } from '@/config/consts'
import { useRouter } from 'vue-router'
import FilterButton from '@/components/ui/buttons/FilterButton.vue'
import { useVueTourStore } from '../store/vueTour'

const filtersSortData = defineModel('filtersSortData')
const vueTourStore = useVueTourStore()

const router = useRouter()

const props = defineProps({
  filtersStatus: {
    type: Array,
    default: () => [],
  },
  filterOrganizations: {
    type: Array,
    default: () => [],
  },
  userRole: {
    type: String,
    required: true,
    validator: (prop) =>
      [ROLES.RESEARCHER, ROLES.REPRESENTATIVE, ROLES.ADMINISTRATOR].includes(prop),
  },
})

const emit = defineEmits(['filtersSortData'])

const sortBy = [
  { value: 'title', label: 'Title' },
  { value: 'creationDate', label: 'Creation Date' },
  { value: 'currentState', label: 'Current State' },
]

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

function emitFilterSortData() {
  emit('filtersSortData', filtersSortData.value)
}

function changeSortDirection() {
  if (filtersSortData.value.sortDirection === 'DESC') {
    filtersSortData.value.sortDirection = 'ASC'
  } else {
    filtersSortData.value.sortDirection = 'DESC'
  }
  emit('filtersSortData', filtersSortData.value)
}

function clearAllFilters() {
  filtersSortData.value.status = []
  filtersSortData.value.organizations = []
  filtersSortData.value.dateStart = ''
  filtersSortData.value.dateEnd = ''
  filtersSortData.value.sortBy = 'creationDate'
  filtersSortData.value.sortDirection = 'DESC'

  emit('filtersSortData', filtersSortData.value)
  router.push({ query: {} })
}

function isChecked(value) {
  return filtersSortData.value.sortBy === value
}

function initializeRepresentativeDefaults() {
  if (props.userRole === ROLES.REPRESENTATIVE) {
    const defaultStatusValues = ['IN_PROGRESS', 'ABANDONED', 'CONCLUDED']
    const availableDefaults = defaultStatusValues.filter((statusValue) =>
      props.filtersStatus.some((status) => status.value === statusValue),
    )
    if (availableDefaults.length > 0 && filtersSortData.value.status.length === 0) {
      filtersSortData.value.status = availableDefaults
      emit('filtersSortData', filtersSortData.value)
    }
  }
}

onMounted(() => {
  initializeRepresentativeDefaults()
  vueTourStore.isFilterSortVisible = true
})
</script>

<style scoped>
.custom-button-hover:hover {
  background-color: var(--hovercolor) !important;
  color: #ffffff !important;
  border-color: var(--hovercolor) !important;
}
</style>

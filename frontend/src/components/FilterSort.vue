<template>
  <div class="container d-flex flex-row flex-wrap justify-content-between">
    <div class="d-flex flex-row gap-2 my-2 mx-auto mx-md-0">
      <div class="sort-by">
        <button
          class="btn btn-sm dropdown-toggle custom-button-hover"
          :style="filtersSortData.sortBy !== '' ? returnButtonActiveColor : returnButtonColor"
          :class="filtersSortData.sortBy !== '' ? 'show' : ''"
          type="button"
          data-bs-toggle="dropdown"
          data-bs-auto-close="outside"
          aria-expanded="false"
        >
          Sort by
        </button>
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
            <label
              class="form-check-label"
              :style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
            >
              {{ sort.label }}
            </label>
          </div>
        </ul>
      </div>

      <button
        class="btn btn-sm custom-button-hover"
        :style="returnButtonColor"
        type="button"
        @click="changeSortDirection()"
      >
        <i v-if="filtersSortData.sortDirection === 'DESC'" class="bi bi-sort-down" />
        <i v-if="filtersSortData.sortDirection === 'ASC'" class="bi bi-sort-up" />
      </button>

      <div id="v-step-3" class="filter-by-status">
        <button
          class="btn btn-sm dropdown-toggle custom-button-hover"
          :style="filtersSortData.status.length > 0 ? returnButtonActiveColor : returnButtonColor"
          :class="filtersSortData.status.length > 0 ? 'show' : ''"
          type="button"
          data-bs-toggle="dropdown"
          data-bs-auto-close="outside"
          aria-expanded="false"
        >
          Filter by status
        </button>
        <ul class="dropdown-menu" aria-labelledby="dropdownSortingButton" role="menu">
          <div v-for="(status, index) in filtersStatus" :key="index" class="form-check mx-2 my-2">
            <input
              v-model="filtersSortData.status"
              class="form-check-input"
              type="checkbox"
              :value="status.value"
              @change="emitFilterSortData"
            />
            <label
              class="form-check-label"
              :style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
            >
              {{ status.label }}
            </label>
          </div>
        </ul>
      </div>

      <div id="v-step-4" class="filter-by-org" v-if="filterOrganizations.length > 0">
        <button
          class="btn btn-sm dropdown-toggle custom-button-hover"
          :style="
            filtersSortData.organizations.length > 0 ? returnButtonActiveColor : returnButtonColor
          "
          :class="filtersSortData.organizations.length > 0 ? 'show' : ''"
          type="button"
          data-bs-toggle="dropdown"
          data-bs-auto-close="outside"
          aria-expanded="false"
        >
          Filter by Organization
        </button>
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
            <label
              class="form-check-label"
              :style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
            >
              {{ org.name }}
            </label>
          </div>
        </ul>
      </div>

      <div class="filter-by-date">
        <button
          class="btn btn-sm dropdown-toggle custom-button-hover"
          :style="
            filtersSortData.dateStart !== '' || filtersSortData.dateEnd !== ''
              ? returnButtonActiveColor
              : returnButtonColor
          "
          :class="
            filtersSortData.dateStart !== '' || filtersSortData.dateEnd !== ''
              ? 'btn-primary show'
              : ''
          "
          type="button"
          data-bs-toggle="dropdown"
          data-bs-auto-close="outside"
          aria-expanded="false"
        >
          Filter by date
        </button>
        <ul class="dropdown-menu" aria-labelledby="dropdownMenuButton1">
          <div
            class="mx-2 my-2 dropdown-contents"
            :style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
          >
            <div class="d-flex align-items-center mb-2">
              <label class="pe-2 w-25" for="startDate">Start:</label>
              <input
                id="startDate"
                v-model="filtersSortData.dateStart"
                class="form-control form-control-sm"
                :style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
                type="date"
                @input="emitFilterSortData"
              />
            </div>
            <div class="d-flex align-items-center">
              <label for="endDate" class="pe-3 w-25">End:</label>
              <input
                id="endDate"
                v-model="filtersSortData.dateEnd"
                class="form-control form-control-sm"
                :style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
                type="date"
                @input="emitFilterSortData"
              />
            </div>
          </div>
        </ul>
      </div>
    </div>

    <div class="my-2 ms-auto">
      <button
        type="button"
        :style="returnClearButtonColor"
        class="btn btn-sm custom-button-hover"
        @click="clearAllFilters()"
      >
        <i class="bi bi-x-circle" />
        Clear all filters
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { ROLES } from '@/config/consts'
import { useRouter } from 'vue-router'
import { useUiConfiguration } from '../store/uiConfiguration.js'

const filtersSortData = defineModel('filtersSortData')
const uiConfigurationStore = useUiConfiguration()
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

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.filtersSort
})

const returnButtonActiveColor = computed(() => {
  return {
    'border-color': uiConfiguration.value?.filtersSortButtonColor,
    '--hovercolor': uiConfiguration.value?.filtersSortButtonColor,
    'background-color': uiConfiguration.value?.filtersSortButtonColor,
    color: '#FFFFFF',
  }
})
const returnButtonColor = computed(() => {
  return {
    'border-color': uiConfiguration.value?.filtersSortButtonColor,
    '--hovercolor': uiConfiguration.value?.filtersSortButtonColor,
    'background-color': '#FFFFFF',
    color: uiConfiguration.value?.filtersSortButtonColor,
  }
})

const returnClearButtonColor = computed(() => {
  return {
    'border-color': uiConfiguration.value?.filtersSortClearButtonColor,
    '--hovercolor': uiConfiguration.value?.filtersSortClearButtonColor,
    'background-color': '#FFFFFF',
    color: uiConfiguration.value?.filtersSortClearButtonColor,
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
    const availableDefaults = defaultStatusValues.filter(statusValue =>
      props.filtersStatus.some(status => status.value === statusValue)
    )
    if (availableDefaults.length > 0 && filtersSortData.value.status.length === 0) {
      filtersSortData.value.status = availableDefaults
      emit('filtersSortData', filtersSortData.value)
    }
  }
}

onMounted(() => {
  initializeRepresentativeDefaults()
})
</script>

<style scoped>
.custom-button-hover:hover {
  background-color: var(--hovercolor) !important;
  color: #ffffff !important;
  border-color: var(--hovercolor) !important;
}
</style>

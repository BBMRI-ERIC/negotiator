<template>
  <div class="container d-flex flex-row flex-wrap justify-content-between">
    <div class="d-flex flex-row gap-2 my-2 mx-auto mx-md-0">
      <div class="sort-by">
        <button
          class="btn dropdown-toggle custom-button-hover"
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
          <div v-for="(sort, index) in sortByFields.fields" :key="index" class="form-check mx-2 my-2">
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
        class="btn custom-button-hover"
        :style="returnButtonColor"
        type="button"
        @click="changeSortDirection()"
      >
        <i v-if="filtersSortData.sortOrder === 'DESC'" class="bi bi-sort-down" />
        <i v-if="filtersSortData.sortOrder === 'ASC'" class="bi bi-sort-up" />
      </button>

      <div class="row align-items-center ms-1">
        <input
          type="hidden"
          class="form-control"
        />
        <div v-for="field in filtersFields" :key="field.name" class="col-auto">
          <div class="input-group">
            <span class="input-group-text">{{ field.label  }}</span>
            <input
              :id="field.name"
              v-model="filtersSortData[field.name]"
              type="text"
              class="form-control"
              @input="debouncedFilter"
            />
          </div>
        </div>
      </div>
    </div>
    <div class="my-2 ms-auto">
      <button
        type="button"
        :style="returnClearButtonColor"
        class="btn custom-button-hover"
        @click="clearAllFilters()"
      >
        <i class="bi bi-x-circle" />
        Clear all filters
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ROLES } from '@/config/consts'
// import { useRouter } from 'vue-router'
import { useUiConfiguration } from '../store/uiConfiguration.js'

const filtersSortData = defineModel('filtersSortData')
const uiConfigurationStore = useUiConfiguration()
// const router = useRouter()

const props = defineProps({
  filtersFields: {
    type: Array,
    required: true,
    default: () => []
  },
  userRole: {
    type: String,
    required: true,
    validator: (prop) =>
      [ROLES.RESEARCHER, ROLES.REPRESENTATIVE, ROLES.ADMINISTRATOR].includes(prop),
  },
  sortByFields: {
    type: Array,
    required: true,
    default: () => []
  }
})

const emit = defineEmits(['filtersSortData'])

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

// Simple debounce function to replace lodash
const debounce = (func, wait) => {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

const debouncedFilter = debounce(() => {
  emitFilterSortData()
}, 500)

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
  props.filtersFields.forEach((filterDefinition) => {
    filtersSortData.value[filterDefinition.name] = filterDefinition.default
  }) 
  filtersSortData.value.sortBy = props.sortByFields.defaultField
  filtersSortData.value.sortDirection = props.sortByFields.defaultDirection

  emit('filtersSortData', filtersSortData.value)
  // router.push({ query: {} })
}

function isChecked(value) {
  return filtersSortData.value.sortBy === value
}
</script>

<style scoped>
.custom-button-hover:hover {
  background-color: var(--hovercolor) !important;
  color: #ffffff !important;
  border-color: var(--hovercolor) !important;
}
</style>

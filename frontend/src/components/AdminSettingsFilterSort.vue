<template>
  <div class="container">
    <div class="row gap-2 my-2 mx-auto mx-md-0 mb-3">
      <div class="col d-flex flex-row">
        <div class="dropdown">
          <button
            class="btn dropdown-toggle custom-button-hover mx-2"
            :style="filtersSortData.sortBy !== '' ? buttonActiveStyle : buttonStyle"
            :class="filtersSortData.sortBy !== '' ? 'show' : ''"
            type="button"
            data-bs-toggle="dropdown"
            data-bs-auto-close="outside"
            aria-expanded="false"
          >
            Sort by {{ currentSortBy }}
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
          :style="buttonStyle"
          type="button"
          @click="changeSortDirection()"
        >
          <i v-if="filtersSortData.sortOrder === 'DESC'" class="bi bi-sort-down" />
          <i v-if="filtersSortData.sortOrder === 'ASC'" class="bi bi-sort-up" />
        </button>

        <button
          type="button"
          :style="clearButtonStyle"
          class="btn custom-button-hover ms-auto"
          @click="clearAllFilters()"
        >
          <i class="bi bi-x-circle" />
          Clear all filters
        </button>
      </div>
    </div>

    <div class="row row-cols-auto gap-2 ms-1 my-2">
      <div v-for="field in filtersFields" :key="field.name" class="col mx-1">
        <TextFilter 
          v-if="field.type == 'text' || field.type == 'email'"
          :name="field.name"
          :label="field.label"
          :type="field.type"
          :placeholder="field.placeholder"
          v-model:value="filtersSortData[field.name]"
          @input="debouncedFilter"
        />
        <OptionsFilter v-else-if="field.type === 'radio' || field.type === 'checkbox'" 
          :name="field.name"
          :label="field.label"
          :type="field.type"
          :options="field.options"
          :button-style="filtersSortData[field.name] !== '' 
            ? buttonActiveStyle 
            : buttonStyle"
          :clear-button-style="clearButtonStyle"
          :label-style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
          @change="emitFilterSortData"
          v-model:value="filtersSortData[field.name]"
        />
        <DateRangeFilter
          v-if="field.type == 'date-range'"
          :name="field.name"
          :label="field.label"
          :button-style="filtersSortData[field.name].start !== '' || filtersSortData[field.name].end !== ''
            ? buttonActiveStyle
            : buttonStyle"
          :type="field.inputType"
          v-model:start="filtersSortData[field.name].start"
          v-model:end="filtersSortData[field.name].end"
          @startChanged="emitFilterSortData"
          @endChanged="emitFilterSortData"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ROLES } from '@/config/consts'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import TextFilter from './filters/TextFilter.vue'
import OptionsFilter from './filters/OptionsFilter.vue'
import DateRangeFilter from './filters/DateRangeFilter.vue'

const filtersSortData = defineModel('filtersSortData')
const uiConfigurationStore = useUiConfiguration()

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
    required: false
  }
})

const emit = defineEmits(['filtersSortData'])

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.filtersSort
})

const buttonActiveStyle = computed(() => {
  return {
    'border-color': uiConfiguration.value?.filtersSortButtonColor,
    '--hovercolor': uiConfiguration.value?.filtersSortButtonColor,
    'background-color': uiConfiguration.value?.filtersSortButtonColor,
    color: '#FFFFFF',
  }
})

const buttonStyle = computed(() => {
  return {
    'border-color': uiConfiguration.value?.filtersSortButtonColor,
    '--hovercolor': uiConfiguration.value?.filtersSortButtonColor,
    'background-color': '#FFFFFF',
    color: uiConfiguration.value?.filtersSortButtonColor,
  }
})

const clearButtonStyle = computed(() => {
  return {
    'border-color': uiConfiguration.value?.filtersSortClearButtonColor,
    '--hovercolor': uiConfiguration.value?.filtersSortClearButtonColor,
    'background-color': '#FFFFFF',
    color: uiConfiguration.value?.filtersSortClearButtonColor,
  }
})

const currentSortBy = computed(() => {
  const field = props.sortByFields.fields.find(field => field.value === filtersSortData.value.sortBy)
  if (field) {
    return field.label 
  }
  return '' 
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
  if (filtersSortData.value.sortOrder === 'DESC') {
    filtersSortData.value.sortOrder = 'ASC'
  } else {
    filtersSortData.value.sortOrder = 'DESC'
  }
  emit('filtersSortData', filtersSortData.value)
}

function clearAllFilters() {
  props.filtersFields.forEach((filterDefinition) => {
    filtersSortData.value[filterDefinition.name] = filterDefinition.default
  }) 
  filtersSortData.value.sortBy = props.sortByFields.defaultField
  filtersSortData.value.sortOrder = props.sortByFields.defaultOrder

  emit('filtersSortData', filtersSortData.value)
}

function isChecked(value) {
  console.log(value)
  console.log(filtersSortData.value.sortBy === value)
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

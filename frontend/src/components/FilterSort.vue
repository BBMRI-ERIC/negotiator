<template>
  <div class="container d-flex flex-row flex-wrap justify-content-between">
    <div class="d-flex flex-row gap-2 my-2 mx-auto mx-md-0">
      <div class="sort-by">
        <button
            class="btn btn-sm dropdown-toggle custom-button-hover"
            :style="filtersSortData.sortBy !== '' ? returnButtonActiveColor : returnButtonColor"
            :class="filtersSortData.sortBy !== '' ? 'show': ''"
            type="button"
            data-bs-toggle="dropdown"
            data-bs-auto-close="outside"
            aria-expanded="false"
        >
          Sort by
        </button>
        <ul
            class="dropdown-menu"
            aria-labelledby="dropdownSortingButton"
            role="menu"
        >
          <div
              v-for="(sort, index) in sortBy"
              :key="index"
              class="form-check mx-2 my-2"
          >
            <input
                :id="index"
                v-model="filtersSortData.sortBy"
                class="form-check-input"
                type="radio"
                name="sort"
                :value="sort.value"
                :checked="isChecked(sort.value)"
                @change="emitFilterSortData"
            >
            <label
                class="form-check-label"
                :style="{'color':uiConfiguration?.filtersSortDropdownTextColor}"
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
        <i
            v-if="filtersSortData.sortDirection === 'DESC'"
            class="bi bi-sort-down"
        />
        <i
            v-if="filtersSortData.sortDirection === 'ASC'"
            class="bi bi-sort-up"
        />
      </button>

      <div
          id="v-step-3"
          class="filter-by-status"
      >
        <button
            class="btn btn-sm dropdown-toggle custom-button-hover"
            :style="filtersSortData.status.length > 0 ? returnButtonActiveColor : returnButtonColor"
            :class="filtersSortData.status.length > 0 ? 'show':''"
            type="button"
            data-bs-toggle="dropdown"
            data-bs-auto-close="outside"
            aria-expanded="false"
        >
          Filter by status
        </button>
        <ul
            class="dropdown-menu"
            aria-labelledby="dropdownSortingButton"
            role="menu"
        >
          <div
              v-for="(status, index) in filtersStatus"
              :key="index"
              class="form-check mx-2 my-2"
          >
            <input
                v-model="filtersSortData.status"
                class="form-check-input"
                type="checkbox"
                :value="status.value"
                @change="emitFilterSortData"
            >
            <label
                class="form-check-label"
                :style="{'color':uiConfiguration?.filtersSortDropdownTextColor}"
            >
              {{ status.label }}
            </label>
          </div>
        </ul>
      </div>

      <div class="filter-by-date">
        <button
            class="btn btn-sm dropdown-toggle custom-button-hover"
            :style="filtersSortData.dateStart !== '' || filtersSortData.dateEnd !== '' ? returnButtonActiveColor : returnButtonColor"
            :class="filtersSortData.dateStart !== '' || filtersSortData.dateEnd !== '' ? 'btn-primary show':''"
            type="button"
            data-bs-toggle="dropdown"
            data-bs-auto-close="outside"
            aria-expanded="false"
        >
          Filter by date
        </button>
        <ul
            class="dropdown-menu"
            aria-labelledby="dropdownMenuButton1"
        >
          <div
              class="mx-2 my-2 dropdown-contents"
              :style="{'color':uiConfiguration?.filtersSortDropdownTextColor}"
          >
            <div class="d-flex align-items-center mb-2">
              <label
                  class="pe-2 w-25"
                  for="startDate"
              >Start:</label>
              <input
                  id="startDate"
                  v-model="filtersSortData.dateStart"
                  class="form-control form-control-sm"
                  :style="{'color':uiConfiguration?.filtersSortDropdownTextColor}"
                  type="date"
                  @input="emitFilterSortData"
              >
            </div>
            <div class="d-flex align-items-center">
              <label
                  for="endDate"
                  class="pe-3 w-25"
              >End:</label>
              <input
                  id="endDate"
                  v-model="filtersSortData.dateEnd"
                  class="form-control form-control-sm"
                  :style="{'color':uiConfiguration?.filtersSortDropdownTextColor}"
                  type="date"
                  @input="emitFilterSortData"
              >
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
        <i class="bi bi-x-circle"/>
        Clear all filters
      </button>
    </div>
  </div>
</template>

<script setup>
import {computed} from "vue"
import {ROLES} from "@/config/consts"
import {useRouter} from "vue-router"
import {useUiConfiguration} from "../store/uiConfiguration.js"

const uiConfigurationStore = useUiConfiguration()
const router = useRouter()

const props = defineProps({
  filtersSortData: {
    type: Object,
    default: undefined
  },
  filtersStatus: {
    type: Array,
    default: () => []
  },
  userRole: {
    type: String,
    required: true,
    validator: (prop) => [ROLES.RESEARCHER, ROLES.REPRESENTATIVE, ROLES.ADMINISTRATOR].includes(prop)
  }
})

const emit = defineEmits(["filtersSortData"])

const sortBy = [
  {value: "title", label: "Title"},
  {value: "creationDate", label: "Creation Date"},
  {value: "currentState", label: "Current State"}
]

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.filtersSort
})

const returnButtonActiveColor = computed(() => {
  return {
    "border-color": uiConfiguration.value?.filtersSortButtonColor,
    "--hovercolor": uiConfiguration.value?.filtersSortButtonColor,
    "background-color": uiConfiguration.value?.filtersSortButtonColor,
    color: "#FFFFFF"
  }
})
const returnButtonColor = computed(() => {
  return {
    "border-color": uiConfiguration.value?.filtersSortButtonColor,
    "--hovercolor": uiConfiguration.value?.filtersSortButtonColor,
    "background-color": "#FFFFFF",
    color: uiConfiguration.value?.filtersSortButtonColor
  }
})

const returnClearButtonColor = computed(() => {
  return {
    "border-color": uiConfiguration.value?.filtersSortClearButtonColor,
    "--hovercolor": uiConfiguration.value?.filtersSortClearButtonColor,
    "background-color": "#FFFFFF",
    color: uiConfiguration.value?.filtersSortClearButtonColor
  }
})

function emitFilterSortData() {
  emit("filtersSortData", props.filtersSortData)
}

function changeSortDirection() {
  if (props.filtersSortData.sortDirection === "DESC") {
    props.filtersSortData.sortDirection = "ASC"
  } else {
    props.filtersSortData.sortDirection = "DESC"
  }
  emit("filtersSortData", props.filtersSortData)
}

function clearAllFilters() {
  props.filtersSortData.status = []
  props.filtersSortData.dateStart = ""
  props.filtersSortData.dateEnd = ""
  props.filtersSortData.sortBy = "creationDate"
  props.filtersSortData.sortDirection = "DESC"

  emit("filtersSortData", props.filtersSortData)
  router.push({query: {}})
}

function isChecked(value) {
  return props.filtersSortData.sortBy === value
}
</script>

<style scoped>
.custom-button-hover:hover {
  background-color: var(--hovercolor) !important;
  color: #FFFFFF !important;
  border-color: var(--hovercolor) !important;
}
</style>

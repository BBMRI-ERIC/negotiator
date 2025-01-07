<template>
  <div
    class="modal"
    :class="{ fade: fade }"
    tabindex="-1"
    :aria-labelledby="`${id}Label`"
    aria-hidden="true"
  >
    <div
      class="modal-dialog modal-dialog-centered modal-xl"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h4 class="justify-content-center">
            Edit Resources
          </h4>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          />
        </div>
        <div class="modal-body text-left">
          <h5 class="text-center mb-3">
            Select Resources and their desired Status
          </h5>
          <div class="input-group flex-nowrap">
            <input
              v-model="searchQuery"
              type="text"
              class="form-control mb-3"
              placeholder="Search by name..."
              aria-label="Search"
              aria-describedby="addon-wrapping"
              @input="onSearch"
            >
          </div>
          <div class="d-flex justify-content-between">
            <div class="ms-3 text-muted">
              Found Resources: {{ getNumberOfFoundResources() }}
            </div>
            <div class="d-flex justify-content-end mb-2">
              Selected Resources: {{ getNumberOfSelectedResources() }}
            </div>
          </div>
          <div class="d-flex flex-row justify-content-end mb-2">
            Status:
            <div class="col-3 ms-2">
              <select
                v-model="selectedState"
                class="form-select form-select-sm btn-outline-sort-filter-button-outline"
              >
                <option
                  disabled
                  value=""
                >
                  Select a status...
                </option>
                <option
                  v-for="(state, index) in states"
                  :key="index"
                  :value="state"
                >
                  {{ state.label }}
                </option>
              </select>
            </div>
          </div>
          <!-- Loading Spinner -->
          <div
            v-if="loading"
            class="text-center my-5"
          >
            <div
              class="spinner-border"
              role="status"
            >
              <span class="visually-hidden">Loading...</span>
            </div>
          </div>
          <div v-else>
            <div class="mb-3 d-flex justify-content-between">
              <div>
                <input
                  v-model="selectAll"
                  type="checkbox"
                  class="form-check-input"
                  @change="toggleSelectAll"
                >
                <label class="form-check-label ms-2">Select All</label>
              </div>
              <button
                class="btn btn-primary mx-2"
                @click="addResources"
              >
                Add
              </button>
            </div>
            <table class="table table-sm">
              <thead>
              <tr>
                <th scope="col">
                  Select
                </th>
                <th scope="col">
                  Resource Name
                </th>
                <th scope="col">
                  Resource ID
                </th>
              </tr>
              </thead>
              <tbody>
              <tr
                  v-for="collection in resources"
                  :key="collection.id"
              >
                <td>
                  <input
                      :id="collection.id"
                      v-model="selectedResources"
                      type="checkbox"
                      :value="collection.id"
                      class="form-check-input"
                      @change="handleCheckboxChange"
                  >
                </td>
                <td>
                  <label
                      :for="collection.id"
                      class="form-check-label"
                  >{{ collection.name }}</label>
                </td>
                <td>
                  <label
                      :for="collection.id"
                      class="form-check-label"
                  >{{ collection.sourceId }}</label>
                </td>
              </tr>
              </tbody>
            </table>
            <!-- Pagination Controls -->
            <div class="d-flex justify-content-center my-4">
              <button
                class="btn btn-secondary me-2"
                :disabled="pageNumber === 0"
                @click="fetchPage(pageLinks.previous.href)"
              >
                Previous
              </button>
              <span class="mx-2">Page {{ pageNumber + 1 }} of {{ totalPages }}</span>
              <button
                class="btn btn-secondary ms-2"
                :disabled="pageNumber === totalPages - 1"
                @click="fetchPage(pageLinks.next.href)"
              >
                Next
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, ref, watch } from 'vue'
import { Tooltip } from 'bootstrap'
import debounce from "@popperjs/core/lib/utils/debounce"
import { useNegotiationPageStore } from '@/store/negotiationPage'

const resources = ref([])
const selectedResources = ref([])
const selectAll = ref(false)
const loading = ref(true)
const pageNumber = ref(0)
const totalPages = ref(0)
const totalElements = ref(0)
const pageLinks = ref({})
const searchQuery = ref("")
const states = ref([])
const selectedState = ref({})
const props = defineProps({
  shown: {
    type: Boolean,
    required: true
  },
  negotiationId: {
    type: String,
    required: true
  }
})
const store = useNegotiationPageStore()
watch(() => props.shown, () => {
  if (props.shown !== false) {
    loadResources()
    loadStates()
  }
})
watch(selectedResources, (newVal) => {
  selectAll.value = newVal.length === resources.value.length
})
onMounted(() => {
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']"
  })
})

async function loadResources(name = '') {
  const response = await store.retrieveAllResources(name)
  resources.value = response?._embedded?.resources ?? []
  pageLinks.value = response._links
  pageNumber.value = response.page.number
  totalPages.value = response.page.totalPages
  totalElements.value = response.page.totalElements
  loading.value = false
}

async function loadStates() {
  const response = await store.retrieveResourceAllStates()
  states.value = response
}
const emit = defineEmits(["confirm"])

async function addResources() {
  let data = { resourceIds: selectedResources.value }
  if (selectedState.value) {
    data = {
      resourceIds: selectedResources.value,
      state: selectedState.value.value
    }
  }
  const negotiationId = props.negotiationId
  await store.addResources(data, negotiationId)
  selectedResources.value = []
  emit("confirm")
}
const toggleSelectAll = () => {
  if (selectAll.value) {
    // Select all resources
    selectedResources.value = resources.value.map(resource => resource.id)
  } else {
    // Deselect all resources
    selectedResources.value = []
  }
}
const handleCheckboxChange = () => {
  // If not all resources are selected, uncheck the Select All checkbox
  if (selectedResources.value.length !== resources.value.length) {
    selectAll.value = false
  }
}

async function fetchPage(url) {
  const response = await store.dispatch('fetchURL', { url })
  resources.value = response._embedded.resources
  pageLinks.value = response._links
  pageNumber.value = response.page.number
}
// Method to handle search input
const onSearch = debounce(async () => {
  if (searchQuery.value.length >= 3) {
    loading.value = true
    await loadResources(searchQuery.value)
  }
  if (searchQuery.value.length === 0) {
    loading.value = true
    await loadResources(searchQuery.value)
  }
}, 1000) // Debounce delay in milliseconds
function getNumberOfSelectedResources() {
  return selectedResources.value.length
}

function getNumberOfFoundResources() {
  return totalElements.value
}
</script>

<style scoped>

</style>

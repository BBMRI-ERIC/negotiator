<template>
  <div class="elements-management">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="text-left">Form Elements</h2>
      <button
        class="btn btn-primary btn-sm"
        data-bs-toggle="modal"
        data-bs-target="#elementModal"
        @click="openCreateModal"
      >
        <i class="bi bi-plus-circle"></i> Add Element
      </button>
    </div>

    <div v-if="loading" class="text-center">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>

    <div v-else-if="elements && elements.length > 0">
      <div class="table-container">
        <table class="table table-hover">
          <thead>
            <tr>
              <th scope="col">#</th>
              <th scope="col">Name</th>
              <th scope="col">Label</th>
              <th scope="col">Type</th>
              <th scope="col">Description</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(element, index) in paginatedElements" :key="element.id"
                class="clickable-row"
                data-bs-toggle="modal"
                data-bs-target="#elementModal"
                @click="openEditModal(element)"
                :title="`Click to edit ${element.label}`">
              <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
              <td>{{ element.name }}</td>
              <td>{{ element.label }}</td>
              <td>
                <span class="badge bg-secondary">{{ formatElementType(element.type) }}</span>
              </td>
              <td class="text-truncate" style="max-width: 200px;" :title="element.description">
                {{ element.description }}
              </td>
            </tr>
          </tbody>
        </table>

        <div class="pagination">
          <button
            @click="previousPage"
            :disabled="currentPage === 1"
            class="page-button"
          >
            Prev
          </button>
          <span class="page-info">Page {{ currentPage }} of {{ totalPages }}</span>
          <button
            @click="nextPage"
            :disabled="currentPage === totalPages"
            class="page-button"
          >
            Next ›
          </button>
          <input
            v-model.number="pageSize"
            @change="resetPage"
            type="number"
            min="1"
            max="100"
            class="page-size-input"
            placeholder="Page size"
          />
        </div>
      </div>
    </div>

    <div v-else class="alert alert-light my-3">
      No elements available. Click "Add Element" to create your first form element.
    </div>

    <!-- Element Modal -->
    <NegotiatorModal
      id="elementModal"
      :title="isEditing ? 'Edit Element' : 'Create New Element'"
    >
      <template #body>
        <form @submit.prevent="saveElement">
          <div class="mb-3">
            <label for="elementName" class="form-label">Name *</label>
            <input
              type="text"
              class="form-control"
              id="elementName"
              v-model="currentElement.name"
              required
              placeholder="Enter element name"
            >
          </div>
          <div class="mb-3">
            <label for="elementLabel" class="form-label">Label *</label>
            <input
              type="text"
              class="form-control"
              id="elementLabel"
              v-model="currentElement.label"
              required
              placeholder="Enter element label"
            >
          </div>
          <div class="mb-3">
            <label for="elementDescription" class="form-label">Description *</label>
            <textarea
              class="form-control"
              id="elementDescription"
              v-model="currentElement.description"
              required
              rows="3"
              placeholder="Enter element description"
            ></textarea>
          </div>
          <div class="mb-3">
            <label for="elementType" class="form-label">Type *</label>
            <select
              class="form-select"
              id="elementType"
              v-model="currentElement.type"
              required
            >
              <option value="">Select element type</option>
              <option v-for="type in elementTypes" :key="type" :value="type">
                {{ formatElementType(type) }}
              </option>
            </select>
          </div>
        </form>
      </template>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
        <button type="button" class="btn btn-primary" data-bs-dismiss="modal" @click="saveElement" :disabled="!isFormValid">
          {{ isEditing ? 'Update' : 'Create' }}
        </button>
      </template>
    </NegotiatorModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useFormsStore } from '@/store/forms'
import NegotiatorModal from '@/components/modals/NegotiatorModal.vue'

const formsStore = useFormsStore()

// Reactive data
const elements = ref([])
const loading = ref(false)
const isEditing = ref(false)

// Pagination
const currentPage = ref(1)
const pageSize = ref(10)

// Element types based on the Java enum
const elementTypes = [
  'SINGLE_CHOICE',
  'MULTIPLE_CHOICE',
  'FILE',
  'TEXT',
  'TEXT_LARGE',
  'BOOLEAN',
  'DATE',
  'NUMBER',
  'INFORMATION'
]

// Current element being edited/created
const currentElement = ref({
  name: '',
  label: '',
  description: '',
  type: '',
  valueSetId: null
})

// Computed properties
const totalPages = computed(() => {
  return Math.ceil(elements.value.length / pageSize.value)
})

const paginatedElements = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return elements.value.slice(start, end)
})

const isFormValid = computed(() => {
  return currentElement.value.name &&
         currentElement.value.label &&
         currentElement.value.description &&
         currentElement.value.type
})

// Pagination methods
function nextPage() {
  if (currentPage.value < totalPages.value) {
    currentPage.value++
  }
}

function previousPage() {
  if (currentPage.value > 1) {
    currentPage.value--
  }
}

function resetPage() {
  currentPage.value = 1
}

// Methods
function formatElementType(type) {
  return type.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, l => l.toUpperCase())
}

function resetForm() {
  currentElement.value = {
    name: '',
    label: '',
    description: '',
    type: '',
    valueSetId: null
  }
  isEditing.value = false
}

function openCreateModal() {
  resetForm()
}

function openEditModal(element) {
  isEditing.value = true
  currentElement.value = {
    id: element.id,
    name: element.name,
    label: element.label,
    description: element.description,
    type: element.type,
    valueSetId: element.valueSetId || null
  }
}

// Methods
async function saveElement() {
  try {
    const elementData = {
      name: currentElement.value.name,
      label: currentElement.value.label,
      description: currentElement.value.description,
      type: currentElement.value.type,
      valueSetId: currentElement.value.valueSetId || null
    }

    if (isEditing.value) {
      await formsStore.updateElement(currentElement.value.id, elementData)
    } else {
      await formsStore.createElement(elementData)
    }

    // Refresh elements list
    await loadElements()
    resetForm()

    // Modal closes automatically due to data-bs-dismiss on the button
  } catch (error) {
    console.error('Error saving element:', error)
  }
}

async function loadElements() {
  loading.value = true
  try {
    const response = await formsStore.retrieveAllElements()
    elements.value = response || []
  } catch (error) {
    console.error('Error loading elements:', error)
    elements.value = []
  } finally {
    loading.value = false
  }
}

// Lifecycle
onMounted(() => {
  loadElements()
})
</script>

<style scoped>
.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.table-container {
  margin-top: 1rem;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 1rem;
}

.page-button {
  padding: 0.5rem 1rem;
  margin: 0 0.5rem;
  background-color: #f8f9fa;
  border: 1px solid #e8ecef;
  cursor: pointer;
  font-size: 0.95rem;
}

.page-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.page-info {
  font-size: 0.95rem;
  color: #6c757d;
}

.page-size-input {
  padding: 0.3rem 0.75rem;
  font-size: 0.95rem;
  margin-left: 1rem;
  width: 60px;
  border: 1px solid #e8ecef;
  text-align: center;
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: #f1f1f1;
}
</style>

<template>
  <div class="elements-management">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2 class="text-left">Form Elements</h2>
      <button
        class="btn btn-success"
        data-bs-toggle="modal"
        data-bs-target="#elementModal"
        @click="openCreateModal"
      >
        Add Element
      </button>
    </div>

    <p class="text-muted mb-4">
      Manage form elements that can be used to build dynamic access forms. Each element represents a specific input type (text, choice, file upload, etc.) that can be added to forms for data collection.
    </p>

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
              <th scope="col">ID</th>
              <th scope="col">Label</th>
              <th scope="col">Description</th>
              <th scope="col">Identifier</th>
              <th scope="col">Type</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(element, index) in paginatedElements" :key="element.id"
                class="clickable-row"
                data-bs-toggle="modal"
                data-bs-target="#elementModal"
                @click="openEditModal(element)">
              <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
              <td>{{ element.label }}</td>
              <td class="text-truncate" style="max-width: 200px;" :title="element.description">
                {{ element.description }}
              </td>
              <td>{{ element.name }}</td>
              <td>
                {{ element.type }}
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

          <!-- Value Set dropdown - only show for choice types -->
          <div v-if="isChoiceType" class="mb-3">
            <label for="elementValueSet" class="form-label">Value Set *</label>
            <select
              class="form-select"
              id="elementValueSet"
              v-model="currentElement.valueSetId"
              @change="onValueSetChange"
              required
            >
              <option value="CREATE_NEW">Create new value set</option>
              <option v-for="valueSet in valueSets" :key="valueSet.id" :value="valueSet.id">
                {{ valueSet.name }}
              </option>
            </select>
          </div>

          <!-- Value Set Name - only show when creating new value set -->
          <div v-if="isChoiceType && currentElement.valueSetId === 'CREATE_NEW'" class="mb-3">
            <label for="valueSetName" class="form-label">Value Set Name *</label>
            <input
              type="text"
              class="form-control"
              id="valueSetName"
              v-model="valueSetName"
              required
              placeholder="Enter value set name"
            >
          </div>

          <!-- Values field - show for both new and existing value sets -->
          <div v-if="isChoiceType" class="mb-3">
            <label for="valueSetValues" class="form-label">
              Values *
              <small class="text-muted">(separate with semicolons)</small>
            </label>
            <textarea
              class="form-control"
              id="valueSetValues"
              v-model="valueSetValues"
              :readonly="currentElement.valueSetId !== 'CREATE_NEW'"
              required
              rows="3"
              :placeholder="currentElement.valueSetId === 'CREATE_NEW' ? 'Enter values separated by semicolons (e.g., Option 1;Option 2;Option 3)' : 'Values from selected value set'"
            ></textarea>
            <div v-if="currentElement.valueSetId !== 'CREATE_NEW'" class="form-text">
              These are the existing values from the selected value set (read-only).
            </div>
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
import { ref, computed, onMounted, nextTick } from 'vue'
import { useFormsStore } from '@/store/forms'
import NegotiatorModal from '@/components/modals/NegotiatorModal.vue'

const formsStore = useFormsStore()

// Reactive data
const elements = ref([])
const valueSets = ref([])
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
  valueSetId: 'CREATE_NEW'
})

// Additional fields for value set management
const valueSetName = ref('')
const valueSetValues = ref('')

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
  const basicFieldsValid = currentElement.value.name &&
         currentElement.value.label &&
         currentElement.value.description &&
         currentElement.value.type

  // If it's a choice type, also require valueSetId and additional fields
  if (isChoiceType.value) {
    const valueSetValid = currentElement.value.valueSetId
    const additionalFieldsValid = currentElement.value.valueSetId === 'CREATE_NEW'
      ? (valueSetName.value && valueSetValues.value)
      : valueSetValues.value

    return basicFieldsValid && valueSetValid && additionalFieldsValid
  }

  return basicFieldsValid
})

const isChoiceType = computed(() => {
  return currentElement.value.type === 'SINGLE_CHOICE' || currentElement.value.type === 'MULTIPLE_CHOICE'
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
    valueSetId: 'CREATE_NEW'
  }
  valueSetName.value = ''
  valueSetValues.value = ''
  isEditing.value = false
}

function openCreateModal() {
  resetForm()
}

function openEditModal(element) {
  isEditing.value = true

  // Extract valueSetId from linkedValueSet if it exists
  const valueSetId = element.linkedValueSet ? element.linkedValueSet.id : 'CREATE_NEW'

  currentElement.value = {
    id: element.id,
    name: element.name,
    label: element.label,
    description: element.description,
    type: element.type,
    valueSetId: valueSetId
  }

  // Reset value set fields
  valueSetName.value = ''
  valueSetValues.value = ''

  // If editing an element with existing value set, load its values
  if (element.linkedValueSet && element.linkedValueSet.availableValues) {
    // Use the values directly from the linkedValueSet in the element
    valueSetValues.value = element.linkedValueSet.availableValues.join(';')
  }

  // Also trigger the value set change to ensure proper loading
  // This handles cases where the element has a value set but we need to populate from the dropdown list
  if (valueSetId !== 'CREATE_NEW') {
    // Use nextTick to ensure the valueSetId is set before calling onValueSetChange
    nextTick(() => {
      onValueSetChange()
    })
  }
}

// Handle value set selection change
function onValueSetChange() {
  if (currentElement.value.valueSetId === 'CREATE_NEW') {
    valueSetName.value = ''
    valueSetValues.value = ''
  } else {
    // Load values from selected value set
    // Convert to number for comparison since IDs from API are numbers
    const selectedId = Number(currentElement.value.valueSetId)
    const selectedValueSet = valueSets.value.find(vs => vs.id === selectedId)
    if (selectedValueSet && selectedValueSet.availableValues) {
      valueSetValues.value = selectedValueSet.availableValues.join(';')
    } else {
      valueSetValues.value = ''
    }
  }
}

// Methods
async function saveElement() {
  try {
    let finalValueSetId = currentElement.value.valueSetId

    // If creating a new value set, create it first
    if (currentElement.value.valueSetId === 'CREATE_NEW' && isChoiceType.value) {
      const valueSetData = {
        name: valueSetName.value,
        availableValues: valueSetValues.value.split(';').map(v => v.trim()).filter(v => v)
      }

      const createdValueSet = await formsStore.createValueSet(valueSetData)
      finalValueSetId = createdValueSet.id

      // Refresh value sets list
      await loadValueSets()
    }

    const elementData = {
      name: currentElement.value.name,
      label: currentElement.value.label,
      description: currentElement.value.description,
      type: currentElement.value.type,
      valueSetId: finalValueSetId !== 'CREATE_NEW' ? finalValueSetId : null
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

async function loadValueSets() {
  try {
    const response = await formsStore.retrieveAllValueSets()
    valueSets.value = response || []
  } catch (error) {
    console.error('Error loading value sets:', error)
    valueSets.value = []
  }
}

// Lifecycle
onMounted(() => {
  loadElements()
  loadValueSets()
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

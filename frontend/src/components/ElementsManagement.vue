<template>
  <div class="elements-management">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h2>Form Elements</h2>
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

    <div v-else-if="elements.length > 0">
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
            <tr
              v-for="(element, index) in paginatedElements"
              :key="element.id"
              class="clickable-row"
              data-bs-toggle="modal"
              data-bs-target="#elementModal"
              @click="openEditModal(element)"
            >
              <td>{{ (currentPage - 1) * pageSize + index + 1 }}</td>
              <td>{{ element.label }}</td>
              <td class="text-truncate" style="max-width: 200px;" :title="element.description">
                {{ element.description }}
              </td>
              <td>{{ element.name }}</td>
              <td>{{ formatElementType(element.type) }}</td>
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

          <template v-if="isChoiceType">
            <div class="mb-3">
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

            <div v-if="isCreatingNewValueSet" class="mb-3">
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

            <div class="mb-3">
              <label for="valueSetValues" class="form-label">
                Values *
                <small class="text-muted">(separate with semicolons)</small>
              </label>
              <textarea
                class="form-control"
                id="valueSetValues"
                v-model="valueSetValues"
                required
                rows="3"
                :placeholder="isCreatingNewValueSet ? 'Enter values separated by semicolons (e.g., Option 1;Option 2;Option 3)' : 'Edit values separated by semicolons'"
              ></textarea>
              <div v-if="!isCreatingNewValueSet" class="form-text">
                You can edit the values for the selected value set.
              </div>
            </div>
          </template>
        </form>
      </template>

      <template #footer>
        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">
          Cancel
        </button>
        <button
          type="button"
          class="btn btn-primary"
          data-bs-dismiss="modal"
          @click="saveElement"
          :disabled="!isFormValid"
        >
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

const elements = ref([])
const valueSets = ref([])
const loading = ref(false)
const isEditing = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

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

const currentElement = ref({
  name: '',
  label: '',
  description: '',
  type: '',
  valueSetId: 'CREATE_NEW'
})

const valueSetName = ref('')
const valueSetValues = ref('')

const totalPages = computed(() => Math.ceil(elements.value.length / pageSize.value))

const paginatedElements = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return elements.value.slice(start, end)
})

const isChoiceType = computed(() =>
  ['SINGLE_CHOICE', 'MULTIPLE_CHOICE'].includes(currentElement.value.type)
)

const isCreatingNewValueSet = computed(() =>
  currentElement.value.valueSetId === 'CREATE_NEW'
)

const isFormValid = computed(() => {
  const basicFieldsValid = currentElement.value.name &&
         currentElement.value.label &&
         currentElement.value.description &&
         currentElement.value.type

  if (!isChoiceType.value) return basicFieldsValid

  const valueSetValid = currentElement.value.valueSetId
  const additionalFieldsValid = isCreatingNewValueSet.value
    ? (valueSetName.value && valueSetValues.value)
    : valueSetValues.value

  return basicFieldsValid && valueSetValid && additionalFieldsValid
})

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
  const valueSetId = element.linkedValueSet?.id || 'CREATE_NEW'

  currentElement.value = {
    id: element.id,
    name: element.name,
    label: element.label,
    description: element.description,
    type: element.type,
    valueSetId
  }

  valueSetName.value = ''
  valueSetValues.value = ''

  if (element.linkedValueSet?.availableValues) {
    valueSetValues.value = element.linkedValueSet.availableValues.join(';')
  }

  if (valueSetId !== 'CREATE_NEW') {
    nextTick(() => onValueSetChange())
  }
}

function onValueSetChange() {
  if (isCreatingNewValueSet.value) {
    valueSetName.value = ''
    valueSetValues.value = ''
  } else {
    const selectedValueSet = valueSets.value.find(vs => vs.id === Number(currentElement.value.valueSetId))
    valueSetValues.value = selectedValueSet?.availableValues?.join(';') || ''
  }
}

async function saveElement() {
  try {
    let finalValueSetId = currentElement.value.valueSetId

    if (isChoiceType.value) {
      if (isCreatingNewValueSet.value) {
        // Create new value set
        const valueSetData = {
          name: valueSetName.value,
          availableValues: valueSetValues.value.split(';').map(v => v.trim()).filter(Boolean)
        }

        const createdValueSet = await formsStore.createValueSet(valueSetData)
        finalValueSetId = createdValueSet.id
        await loadValueSets()
      } else {
        // Update existing value set
        const valueSetData = {
          name: valueSetName.value || valueSets.value.find(vs => vs.id === Number(currentElement.value.valueSetId))?.name,
          availableValues: valueSetValues.value.split(';').map(v => v.trim()).filter(Boolean)
        }

        await formsStore.updateValueSet(Number(currentElement.value.valueSetId), valueSetData)
        await loadValueSets()
      }
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

    await loadElements()
    resetForm()
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

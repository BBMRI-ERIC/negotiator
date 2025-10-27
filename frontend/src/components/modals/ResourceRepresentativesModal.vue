<template>
  <div
    :id="modalId"
    class="modal fade"
    tabindex="-1"
    :aria-labelledby="`${modalId}Label`"
    aria-hidden="true"
  >
    <div class="modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable">
      <div class="modal-content">
        <div class="modal-header">
          <h5 :id="`${modalId}Label`" class="modal-title">Manage Resource Representatives</h5>
          <button
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>

        <div class="modal-body">
          <div class="mb-3">
            <h6>Resource: {{ resource?.name }}</h6>
            <p class="text-muted small">{{ resource?.sourceId || resource?.id }}</p>
          </div>

          <div v-if="isSaving" class="text-center mb-3">
            <div class="spinner-border spinner-border-sm me-2" role="status">
              <span class="visually-hidden">Loading...</span>
            </div>
            <span>Saving changes... ({{ currentOperation }}/{{ totalOperations }})</span>
          </div>

          <div v-if="hasChanges && !isSaving" class="alert alert-info mb-3">
            <h6 class="mb-2">Pending Changes:</h6>
            <ul class="mb-0">
              <li v-if="representativesToAdd.length > 0">
                Add {{ representativesToAdd.length }} representative(s):
                <span class="fw-bold">{{
                  representativesToAdd.map((r) => r.name).join(', ')
                }}</span>
              </li>
              <li v-if="representativesToRemove.length > 0">
                Remove {{ representativesToRemove.length }} representative(s):
                <span class="fw-bold">{{
                  representativesToRemove.map((r) => r.name).join(', ')
                }}</span>
              </li>
            </ul>
          </div>

          <div class="mb-4" :class="{ 'opacity-50 pe-none': isSaving }">
            <h6>Add Representatives</h6>
            <div class="col d-flex flex-row">
            <div class="input-group mb-3">
              <TextFilter
                name="name"
                label="Name"
                type="text"
                placeholder="Enter representative's name"
                v-model:value="representativesFilterData.name"
                @input="handleSearchInput"
              />
            </div>
            <div class="input-group ms-3 mb-3">
              <TextFilter
                name="email"
                label="Email"
                type="email"
                placeholder="Enter representative's email address"
                v-model:value="representativesFilterData.email"
                @input="handleSearchInput"
              />
            </div>
            </div>
            <div v-if="isLoading" class="text-center py-3">
              <div class="spinner-border spinner-border-sm" role="status">
                <span class="visually-hidden">Loading...</span>
              </div>
            </div>

            <div
              v-else-if="searchResults.length > 0"
              class="border rounded p-3 mb-3"
              style="max-height: 200px; overflow-y: auto"
            >
              <div
                v-for="user in searchResults"
                :key="user.id"
                class="d-flex justify-content-between align-items-center py-2 border-bottom"
              >
                <div>
                  <strong>{{ user.name }}</strong>
                  <br />
                  <small class="text-muted">{{ user.email }}</small>
                </div>
                <button
                  class="btn btn-sm btn-primary"
                  type="button"
                  :disabled="isCurrentRepresentative(user.id) || isSaving"
                  @click="stageAddRepresentative(user)"
                >
                  <i class="bi bi-plus"></i>
                  {{ getAddButtonText(user.id) }}
                </button>
              </div>
            </div>

            <div
              v-else-if="hasSearched && searchResults.length === 0"
              class="text-muted text-center py-3"
            >
              No users found matching your search.
            </div>
          </div>

          <div :class="{ 'opacity-50 pe-none': isSaving }">
            <h6>Current Representatives</h6>
            <div v-if="currentRepresentatives.length === 0" class="text-muted">
              No representatives assigned to this resource.
            </div>
            <div v-else class="border rounded p-3">
              <div
                v-for="representative in currentRepresentatives"
                :key="representative.id"
                class="d-flex justify-content-between align-items-center py-2 border-bottom"
                :class="{
                  'text-decoration-line-through opacity-50': isMarkedForRemoval(representative.id),
                  'text-success fw-bold': isMarkedForAddition(representative.id),
                }"
              >
                <div>
                  <strong>{{ representative.name }}</strong>
                  <br />
                  <small class="text-muted">{{ representative.email }}</small>
                  <span v-if="isMarkedForAddition(representative.id)" class="badge bg-success ms-2">
                    New
                  </span>
                  <span v-if="isMarkedForRemoval(representative.id)" class="badge bg-danger ms-2">
                    To Remove
                  </span>
                </div>
                <button
                  v-if="!isMarkedForRemoval(representative.id)"
                  class="btn btn-sm btn-outline-danger"
                  type="button"
                  :disabled="isSaving"
                  @click="stageRemoveRepresentative(representative)"
                >
                  <i class="bi bi-trash"></i>
                  Remove
                </button>
                <button
                  v-else
                  class="btn btn-sm btn-outline-success"
                  type="button"
                  :disabled="isSaving"
                  @click="unstageRemoveRepresentative(representative)"
                >
                  <i class="bi bi-arrow-clockwise"></i>
                  Undo
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button
            type="button"
            class="btn btn-secondary"
            :disabled="isSaving"
            data-bs-dismiss="modal"
            @click="handleCancel"
          >
            Cancel
          </button>
          <button
            type="button"
            class="btn btn-primary"
            :disabled="!hasChanges || isSaving"
            @click="handleSave"
          >
            <span v-if="isSaving" class="spinner-border spinner-border-sm me-2" role="status">
              <span class="visually-hidden">Loading...</span>
            </span>
            {{ isSaving ? 'Saving...' : 'Save Changes' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useAdminStore } from '@/store/admin'
import { useResourcesStore } from '@/store/resources'
import { useNotificationsStore } from '@/store/notifications'
import TextFilter from '../filters/TextFilter.vue'

const props = defineProps({
  modalId: {
    type: String,
    required: true,
  },
  resource: {
    type: Object,
    default: null,
  },
})

const emit = defineEmits(['representativesUpdated'])

const adminStore = useAdminStore()
const resourcesStore = useResourcesStore()
const notifications = useNotificationsStore()

const representativesFilterData = ref({
  'name': '',
  'email': ''
})
const searchResults = ref([])
const isLoading = ref(false)
const hasSearched = ref(false)
const isSaving = ref(false)
const currentOperation = ref(0)
const totalOperations = ref(0)

const originalRepresentatives = ref([])
const currentRepresentatives = ref([])
const representativesToAdd = ref([])
const representativesToRemove = ref([])

let searchTimeout = null

const hasChanges = computed(
  () => representativesToAdd.value.length > 0 || representativesToRemove.value.length > 0,
)

const handleSearchInput = () => {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    if (representativesFilterData.value.name.trim() || representativesFilterData.value.email.trim()) {
      searchUsers()
    }
  }, 300)
}

const searchUsers = async () => {
  if (!representativesFilterData.value.name.trim() && !representativesFilterData.value.email.trim()) return

  isLoading.value = true
  hasSearched.value = true

  try {
    const filtersSortData = {
      name: representativesFilterData.value.name,
      email: representativesFilterData.value.email,
    }

    const result = await adminStore.retrieveUsers(0, 20, filtersSortData)
    searchResults.value = result.users || []
  } catch {
    notifications.setNotification('Error searching users')
    searchResults.value = []
  } finally {
    isLoading.value = false
  }
}

const isCurrentRepresentative = (userId) => {
  return currentRepresentatives.value.some((rep) => rep.id === userId)
}

const isMarkedForAddition = (userId) => {
  return representativesToAdd.value.some((rep) => rep.id === userId)
}

const isMarkedForRemoval = (userId) => {
  return representativesToRemove.value.some((rep) => rep.id === userId)
}

const getAddButtonText = (userId) => {
  if (isCurrentRepresentative(userId)) return 'Already Added'
  if (isMarkedForAddition(userId)) return 'Staged'
  return 'Add'
}

const stageAddRepresentative = (user) => {
  if (!isCurrentRepresentative(user.id)) {
    const userDto = {
      id: user.id,
      name: user.name,
      email: user.email,
    }

    currentRepresentatives.value.push(userDto)
    representativesToAdd.value.push(userDto)
  }
}

const stageRemoveRepresentative = (representative) => {
  const wasOriginal = originalRepresentatives.value.some((rep) => rep.id === representative.id)

  if (wasOriginal) {
    representativesToRemove.value.push(representative)
  } else {
    currentRepresentatives.value = currentRepresentatives.value.filter(
      (rep) => rep.id !== representative.id,
    )
    representativesToAdd.value = representativesToAdd.value.filter(
      (rep) => rep.id !== representative.id,
    )
  }
}

const unstageRemoveRepresentative = (representative) => {
  representativesToRemove.value = representativesToRemove.value.filter(
    (rep) => rep.id !== representative.id,
  )
}

const closeModal = () => {
  const modalElement = document.getElementById(props.modalId)
  if (modalElement) {
    const closeButton = modalElement.querySelector('[data-bs-dismiss="modal"]')
    if (closeButton) {
      closeButton.click()
    }
  }
}

const handleCancel = () => {
  resetState()
  closeModal()
}

const handleSave = async () => {
  if (!hasChanges.value || !props.resource?.id) return

  isSaving.value = true
  currentOperation.value = 0
  totalOperations.value = representativesToAdd.value.length + representativesToRemove.value.length

  try {
    for (const representative of representativesToRemove.value) {
      currentOperation.value++
      await resourcesStore.removeRepresentativeFromResource(
        representative.id,
        props.resource.id,
        true,
      )
    }

    for (const representative of representativesToAdd.value) {
      currentOperation.value++
      await resourcesStore.addRepresentativeToResource(representative.id, props.resource.id, true)
    }

    representativesToAdd.value = []
    representativesToRemove.value = []
    originalRepresentatives.value = [
      ...currentRepresentatives.value.filter((rep) => !isMarkedForRemoval(rep.id)),
    ]

    notifications.setNotification('Representatives updated successfully', 'success')

    emit('representativesUpdated', {
      resourceId: props.resource.id,
      representatives: originalRepresentatives.value,
    })
  } catch {
    notifications.setNotification('Error saving representative changes. Please try again.')
  } finally {
    isSaving.value = false
    currentOperation.value = 0
    totalOperations.value = 0
  }

  closeModal()
}

const resetState = () => {
  currentRepresentatives.value = [...originalRepresentatives.value]
  representativesToAdd.value = []
  representativesToRemove.value = []
  representativesFilterData.value.name = ''
  representativesFilterData.value.email = ''
  searchResults.value = []
  hasSearched.value = false
}

const initializeModal = () => {
  const modalElement = document.getElementById(props.modalId)
  if (modalElement) {
    modalElement.addEventListener('hidden.bs.modal', resetState)
  }
}

watch(
  () => props.resource,
  (newResource) => {
    if (newResource) {
      const reps = newResource.representatives || []
      originalRepresentatives.value = [...reps]
      currentRepresentatives.value = [...reps]
      resetSearchState()
    }
  },
  { immediate: true },
)

const resetSearchState = () => {
  representativesToAdd.value = []
  representativesToRemove.value = []
  representativesFilterData.value.name = ''
  representativesFilterData.value.email = ''
  searchResults.value = []
  hasSearched.value = false
}

onMounted(() => {
  initializeModal()
})

onUnmounted(() => {
  const modalElement = document.getElementById(props.modalId)
  if (modalElement) {
    modalElement.removeEventListener('hidden.bs.modal', resetState)
  }

  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }
})
</script>

<style scoped>
.border-bottom:last-child {
  border-bottom: none !important;
}

.pe-none {
  pointer-events: none;
}

.modal-body {
  max-height: 70vh;
}

.opacity-50 {
  opacity: 0.5;
}
</style>

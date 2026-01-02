<template>
  <div class="card-body">
    <div class="form-check d-flex flex-column">
      <div class="d-flex flex-row align-items-center flex-wrap">
        <div class="me-3 mb-2">
          <label
            class="form-check-label text-truncate"
            :style="{ color: uiConfiguration.primaryTextColor, maxWidth: '300px' }"
            :title="resource.name"
            :for="sanitizeId(resource.sourceId)"
          >
            <i class="bi bi-box-seam" />
            {{ resource.name }}
          </label>
          <UiBadge color="bg-status-badge rounded-pill ms-2">{{ getStatusForResource() }}</UiBadge>
        </div>
        <!-- Resource URI if available -->
        <div v-if="resource.uri" class="me-3 mb-2">
          <a :href="resource.uri" target="_blank" class="text-decoration-none">
            <i class="bi bi-box-arrow-up-right" /> View Details
          </a>
        </div>
        <div class="d-flex flex-wrap">
          <div class="me-3 mb-2">
            <span :style="{ color: uiConfiguration.primaryTextColor, opacity: 0.7 }">
              {{ resource.sourceId }}
              <!-- CopyTextButton component assumed to be globally registered or imported -->
              <CopyTextButton :text="resource.sourceId" />
            </span>
          </div>
          <!-- Resource Lifecycle Links -->
          <div v-if="getLifecycleLinks(resource._links).length" class="me-3 mb-2">
            Update status:
            <div
              v-for="(link, index) in getLifecycleLinks(resource._links)"
              :key="index"
              class="lifecycle-links d-inline-block ms-2"
            >
              <a class="lifecycle-text cursor-pointer" @click="onUpdateResourceState(link.href)">
                <i class="bi bi-patch-check" /> {{ link.name }}
              </a>
            </div>
          </div>
        </div>
      </div>
      <!-- Submitted Requirements (Green) -->
      <div v-for="(link, index) in getSubmissionLinks" :key="index" class="mt-1">
        <div v-if="isSubmittedRequirementsEdit[link.href] && !isAdmin">
          <a
            class="submission-text-edit cursor-pointer"
            @click="$emit('editInfoSubmission', link.href)"
          >
            <i class="bi bi-pencil-square" /> {{ link.name }} Edit
          </a>
        </div>
        <div v-else>
          <a class="submission-text cursor-pointer" @click.prevent="onOpenFormModal(link.href)">
            <i class="bi bi-check-circle" /> {{ link.name }} Submitted
          </a>
        </div>
      </div>
      <!-- Missing Requirements (Red) -->
      <div v-for="(link, index) in getRequirementLinks" :key="index" class="mt-1">
        <a class="requirement-text cursor-pointer" @click="onOpenModal(link.href, resource.id)">
          <i class="bi bi-exclamation-circle-fill" /> {{ link.title }} Required
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, computed, ref } from 'vue'
import { transformStatus } from '../composables/utils.js'
import CopyTextButton from '@/components/CopyTextButton.vue'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import UiBadge from '@/components/ui/UiBadge.vue'

const props = defineProps({
  resource: { type: Object, required: true },
  uiConfiguration: { type: Object, required: true },
  isAdmin: { type: Boolean, default: false },
})

const negotiationPageStore = useNegotiationPageStore()
const emit = defineEmits(['open-form-modal', 'open-modal', 'update-resource-state'])
const sanitizeId = (id) => id.replaceAll(':', '_')

const getStatusForResource = () => {
  return props.resource.currentState ? transformStatus(props.resource.currentState) : ''
}

const getSubmissionLinks = computed(() => {
  return Object.entries(props.resource._links)
    .filter(([key]) => key.startsWith('submission-'))
    .map(([, value]) => value)
})

const getRequirementLinks = computed(() => {
  return Object.entries(props.resource._links)
    .filter(([key]) => key.startsWith('requirement-'))
    .map(([, value]) => value)
})

const getLifecycleLinks = (links) =>
  Object.values(links).filter((link) => link.title === 'Next Lifecycle event')

const onOpenModal = (href, resourceId) => {
  emit('open-modal', href, resourceId)
}

const isSubmittedRequirementsEdit = ref({})
onMounted(() => {
  getSubmissionLinks.value.forEach((element) => {
    negotiationPageStore.retrieveInformationSubmission(element.href).then((res) => {
      isSubmittedRequirementsEdit.value[element.href] = res.editable
    })
  })
})

const onOpenFormModal = (href) => {
  emit('open-form-modal', href)
}

const onUpdateResourceState = (link) => {
  emit('update-resource-state', link)
}
</script>

<style scoped>
/* Text truncation */
.text-truncate {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  vertical-align: middle;
}

/* Badge styling */
.bg-status-badge {
  background-color: #f0f0f0;
  color: #333;
}

/* Requirement (missing) - red text */
.requirement-text {
  color: red;
  opacity: 0.8;
}

/* Submission (submitted) - green text */
.submission-text {
  color: green;
}

/* Submission (submitted and editable) - orange text */
.submission-text-edit {
  color: var(--bs-warning);
}

/* Resource link */
.resource-link {
  color: var(--bs-link-color);
  text-decoration: none;
}

.resource-link:hover {
  text-decoration: underline;
}
</style>

<template>
  <div v-if="!loading">
    <GoBackButton />
    <confirmation-modal
      id="abandonModal"
      :title="`Are you sure you want to ${selectedStatus ? selectedStatus.label.toLowerCase() : 'Unknown'} this Negotiation?`"
      text="Please confirm your action and, possibly, leave a comment"
      :message-enabled="true"
      @confirm="updateNegotiation"
    />
    <confirmation-modal
      id="negotiationUpdateModal"
      title="Negotiation update"
      text="Are you sure you want to update Negotiation"
      :message-enabled="false"
      @confirm="updateNegotiationPayload()"
    />

    <confirmation-modal
      id="negotiationDeleteModal"
      title="Negotiation delete"
      text="Are you sure you want to delete the Negotiation. All your data will be lost."
      :message-enabled="false"
      @confirm="deleteNegotiation()"
    />
    <div class="row mt-4">
      <div class="row-col-2">
        <h1 class="fw-bold" :style="{ color: uiConfiguration.primaryTextColor }">
          {{ negotiation ? negotiation.payload.project.title?.toUpperCase() : '' }}
        </h1>
        <p
          v-if="negotiation.status === 'DRAFT'"
          class="fw-bold alert alert-light"
          :style="{ color: uiConfiguration.primaryTextColor }"
        >
          This Negotiation is currently saved as a draft. Please review and edit the information
          below to ensure accuracy and completeness before publishing.
        </p>
        <span :class="getBadgeColor(negotiation.status)" class="badge py-2 rounded-pill bg"
          ><i :class="getBadgeIcon(negotiation.status)" class="px-1" />
          {{ negotiation ? transformStatus(negotiation.status) : '' }}</span
        >
      </div>
      <div class="col-12 col-md-8 order-2 order-md-1">
        <ul class="list-group list-group-flush rounded border px-3 my-3">
          <li
            v-for="(element, key) in negotiation.payload"
            :key="element"
            class="list-group-item p-3"
          >
            <div
              v-if="negotiation?._links?.Update && Object.keys(negotiation.payload)[0] === key"
              class="position-absolute top-0 end-0 mt-2"
            >
              <button
                type="button"
                class="btn status-box cursor-pointer"
                data-bs-toggle="modal"
                data-bs-target="#negotiationUpdateModal"
              >
                Edit <i class="bi bi-pencil-square cursor-pointer" />
              </button>
            </div>
            <span class="fs-5 fw-bold mt-3" :style="{ color: uiConfiguration.primaryTextColor }">
              {{ transformDashToSpace(key).toUpperCase() }}</span
            >
            <div v-for="(subelement, subelementkey) in element" :key="subelement" class="mt-3">
              <div
                class="me-2 fw-bold"
                :style="{ color: uiConfiguration.secondaryTextColor }"
                v-html="decodeHTML(subelementkey)"
              ></div>
              <span
                v-if="isAttachment(subelement)"
                :style="{ color: uiConfiguration.secondaryTextColor }"
              >
                <span v-if="subelement.name" class="d-flex col">
                  <span class="text-truncate" :title="subelement.name">{{ subelement.name }}</span>
                  <font-awesome-icon
                    v-if="isAttachment(subelement)"
                    class="ms-1 cursor-pointer"
                    icon="fa fa-download"
                    fixed-width
                    @click.prevent="downloadAttachment(subelement.id, subelement.name)"
                  />
                </span>
                <span v-else>
                  <div v-for="(choice, index) in subelement" :key="index">
                    {{ choice }}
                  </div>
                </span>
              </span>
              <span
                v-else
                class="text-break"
                :style="{ color: uiConfiguration.secondaryTextColor }"
              >
                {{ translateTrueFalse(subelement) }}
              </span>
            </div>
          </li>
          <li class="list-group-item p-3">
            <span
              class="fs-5 fw-bold mt-3 mb-3"
              :style="{ color: uiConfiguration.primaryTextColor }"
            >
              ATTACHMENTS
            </span>
            <NegotiationAttachment
              v-for="attachment in attachments"
              :id="attachment.id"
              :key="attachment.id"
              class="mb-2"
              :name="attachment.name"
              :size="attachment.size"
              :content-type="attachment.contentType"
              :is-downloading="downloadingAttachments.has(attachment.id)"
              @download="downloadAttachment(attachment.id, attachment.name)"
            />
          </li>
          <li class="list-group-item p-3">
            <div class="d-flex flex-row mb-3 justify-content-between" style="min-height: 38px">
              <div
                data-bs-toggle="collapse"
                data-bs-target="#requestsHumanReadable"
                aria-controls="requestsHumanReadable"
                aria-expanded="true"
                type="button"
              >
                <span
                  class="fs-5 fw-bold mt-3"
                  :style="{ color: uiConfiguration.primaryTextColor }"
                >
                  <i class="bi bi-diagram-3" />
                  SEARCH PARAMETERS
                </span>
              </div>
              <div
                data-bs-toggle="collapse"
                data-bs-target="#requestsHumanReadable"
                aria-controls="requestsHumanReadable"
                aria-expanded="false"
                type="button"
                class="collections-header justify-content-end pt-1"
              >
                <i class="bi bi-chevron-down" />
                <i class="bi bi-chevron-up" />
              </div>
            </div>
            <div id="requestsHumanReadable" class="collapse">
              <pre v-if="negotiation?.humanReadable">{{ negotiation?.humanReadable }}</pre>
              <pre v-else class="text-muted"> There are no data available </pre>
            </div>
          </li>
          <li class="list-group-item p-3">
            <div class="d-flex flex-row mb-3 justify-content-between">
              <div class="d-flex flex-row">
                <div
                  data-bs-toggle="collapse"
                  data-bs-target="#resourcesList"
                  aria-controls="resourcesList"
                  aria-expanded="true"
                  type="button"
                  title="The term Resource is abstract and can for example refer to biological samples, datasets or a service such as sequencing."
                >
                  <span
                    class="fs-5 fw-bold mt-3"
                    :style="{ color: uiConfiguration.primaryTextColor }"
                  >
                    <i class="bi bi-buildings mx-2" />
                    {{
                      $t('negotiationPage.organisations', Object.keys(organizationsById).length)
                    }}
                    ({{ Object.keys(organizationsById).length }}) |
                    <i class="bi bi-box-seam" />
                    {{ $t('negotiationPage.resources', numberOfResources) }} ({{
                      numberOfResources
                    }})
                  </span>
                </div>
                <add-resources-button
                  v-if="isAddResourcesButtonVisible"
                  class="mb-1"
                  :negotiation-id="negotiationId"
                  @new-resources="reloadResources()"
                />
              </div>
              <div
                data-bs-toggle="collapse"
                data-bs-target="#resourcesList"
                aria-controls="resourcesList"
                aria-expanded="true"
                type="button"
                class="collections-header justify-content-end pt-1"
              >
                <i class="bi bi-chevron-down" />
                <i class="bi bi-chevron-up" />
              </div>
            </div>
            <div id="resourcesList" class="collapse show">
              <div class="involved-organizations-resources">
                <label
                  v-if="Object.entries(representedOrganizationsById).length > 0"
                  class="me-2 fw-bold"
                  :style="{ color: uiConfiguration.secondaryTextColor }"
                >
                  Involved
                  {{
                    $t(
                      'negotiationPage.organisations',
                      Object.entries(representedOrganizationsById).length,
                    )
                  }}/{{
                    $t(
                      'negotiationPage.resources',
                      Object.entries(representedOrganizationsById).length,
                    )
                  }}
                </label>
                <div
                  v-for="[orgId, org] in Object.entries(representedOrganizationsById)"
                  :key="orgId"
                >
                  <OrganizationContainer
                    :org-id="orgId"
                    :org="org"
                    :negotiation-id="negotiationId"
                    :resources="resources"
                    :resource-states="resourceStates"
                    :isAdmin="isAdmin"
                    @reload-resources="reloadResources()"
                  />
                </div>
              </div>
              <div class="not-involved-organizations-resources">
                <hr
                  v-if="
                    Object.entries(representedOrganizationsById).length > 0 &&
                    Object.entries(notRepresentedOrganizationsById).length > 0
                  "
                />
                <div
                  v-for="[orgId, org] in Object.entries(notRepresentedOrganizationsById)"
                  :key="orgId"
                >
                  <OrganizationContainer
                    :org-id="orgId"
                    :org="org"
                    :negotiation-id="negotiationId"
                    :resources="resources"
                    :resource-states="resourceStates"
                    :isAdmin="isAdmin"
                    @reload-resources="reloadResources()"
                  />
                </div>
              </div>
            </div>
          </li>
        </ul>
        <NegotiationPosts
          ref="negotiationPosts"
          v-if="negotiation"
          :negotiation="negotiation"
          :resources="resources"
          :organizations="organizationsById"
          :recipients="postsRecipients"
          :external-posts="posts"
          :timeline-events="timelineEvents"
          @new_attachment="retrieveAttachments()"
          class="col-11 ms-2"
        />
      </div>
      <NegotiationSidebar
        :negotiation="negotiation"
        :author="author"
        :possible-events="possibleEvents"
        :ui-configuration="uiConfiguration"
        :can-delete="canDelete"
        @assign-status="assignStatus"
        @download-attachment-from-link="downloadAttachmentFromLink"
      />
    </div>
  </div>
  <div v-else class="d-flex justify-content-center flex-row">
    <div class="d-flex justify-content-center">
      <div class="spinner-border d-flex justify-content-center" role="status" />
      <div class="d-flex justify-content-center">
        <h4 class="mb-3 ms-3">Loading ...</h4>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeMount, onMounted, ref } from 'vue'
import NegotiationPosts from '@/components/NegotiationPosts.vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import NegotiationAttachment from '@/components/NegotiationAttachment.vue'
import GoBackButton from '@/components/GoBackButton.vue'
import OrganizationContainer from '@/components/OrganizationContainer.vue'
import { getBadgeColor, getBadgeIcon, transformStatus } from '../composables/utils.js'
import AddResourcesButton from '@/components/AddResourcesButton.vue'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { useUserStore } from '../store/user.js'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useRouter } from 'vue-router'
import NegotiationSidebar from '@/components/NegotiationSidebar.vue'
import { ROLES } from '@/config/consts.js'

const props = defineProps({
  negotiationId: {
    type: String,
    default: undefined,
  },
})

const uiConfigurationStore = useUiConfiguration()
const negotiation = ref(undefined)
const resources = ref([])
const representedResourcesIds = ref([])
const possibleEvents = ref([])
const selectedStatus = ref(undefined)
const attachments = ref([])
const downloadingAttachments = ref(new Set())
const isAddResourcesButtonVisible = ref(false)
const resourceStates = ref([])
const userStore = useUserStore()
const negotiationPageStore = useNegotiationPageStore()
const router = useRouter()
const negotiationPosts = ref(null)
const timelineEvents = ref([])

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})
const getResources = computed(() => {
  return resources.value
})
const organizations = computed(() => {
  return Object.entries(organizationsById.value).map(([k, v]) => {
    return { externalId: k, name: v.name }
  })
})
const organizationsById = computed(() => {
  // Create a map of state ordinals for quick lookup
  const stateOrdinalMap = resourceStates.value.reduce((map, state) => {
    map[state.value] = state.ordinal
    return map
  }, {})

  return getResources.value.reduce((organizations, resource) => {
    const currentState = resource.currentState
    const currentOrdinal = stateOrdinalMap[currentState] || 0 // Default to 0 if no match found

    const orgId = resource.organization.externalId

    if (orgId in organizations) {
      // Push the resource to the organization's resources array
      organizations[orgId].resources.push(resource)

      // Check if the current resource state has a higher ordinal
      const orgStatusOrdinal = stateOrdinalMap[organizations[orgId].status] || 0
      if (currentOrdinal > orgStatusOrdinal) {
        organizations[orgId].status = currentState // Update org status to the one with the highest ordinal
      }

      // Check if the organization has at least one represented resource
      if (isResourceRepresented(resource)) {
        organizations[orgId].updatable = true
      }
    } else {
      // Add a new organization entry
      organizations[orgId] = {
        name: resource.organization.name,
        resources: [resource],
        status: currentState, // Set initial status
        updatable: isResourceRepresented(resource), // Set updateable to true if any resource is represented
      }
    }
    return organizations
  }, {})
})

const userInfo = computed(() => {
  return userStore.userInfo
})

const isAdmin = computed(() => {
  return userInfo.value.roles.includes(ROLES.ADMINISTRATOR)
})

const representedOrganizationsById = computed(() => {
  return Object.entries(organizationsById.value)
    .filter(
      ([, value]) => value.updatable === true && value.status !== 'REPRESENTATIVE_UNREACHABLE',
    )
    .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {})
})

const notRepresentedOrganizationsById = computed(() => {
  return Object.entries(organizationsById.value)
    .filter(
      ([, value]) => value.updatable === false || value.status === 'REPRESENTATIVE_UNREACHABLE',
    )
    .reduce((acc, [key, value]) => ({ ...acc, [key]: value }), {})
})

// Helper function to check if a resource is represented
function isResourceRepresented(resource) {
  for (const key in resource._links) {
    if (resource._links[key].title === 'Next Lifecycle event') {
      return true
    }
  }
  return false
}

const numberOfResources = computed(() => {
  return getResources.value.length
})

const postsRecipients = computed(() => {
  return organizations.value.map((org) => {
    return { id: org.externalId, name: org.name }
  })
})

function assignStatus(status) {
  selectedStatus.value = status
}

const author = computed(() => {
  return negotiation.value.author
})

const loading = computed(() => {
  return negotiation.value === undefined || resources.value.length === 0
})

onBeforeMount(async () => {
  negotiation.value = await negotiationPageStore.retrieveNegotiationById(props.negotiationId)
  const resourceResponse = await negotiationPageStore.retrieveResourcesByNegotiationId(
    props.negotiationId,
  )
  if (resourceResponse !== undefined) {
    resources.value = resourceResponse
    const resourceResponseLinks = await negotiationPageStore.retrieveResourcesByNegotiationIdLinks(
      props.negotiationId,
    )
    isAddResourcesButtonVisible.value = hasRightsToAddResources(resourceResponseLinks._links)
  }
  await negotiationPageStore
    .retrieveUserIdRepresentedResources(userStore.userInfo?.id)
    .then((resp) => {
      if (resp) {
        representedResourcesIds.value = resp.map((a) => a.sourceId)
      }
    })
  possibleEvents.value = await negotiationPageStore.retrievePossibleEvents(props.negotiationId)
  resourceStates.value = await negotiationPageStore.retrieveResourceAllStates()
  timelineEvents.value = await negotiationPageStore.retrieveNegotiationTimeline(props.negotiationId)
})

retrieveAttachments()

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})

async function retrieveAttachments() {
  await negotiationPageStore
    .retrieveAttachmentsByNegotiationId(props.negotiationId)
    .then((response) => {
      attachments.value = response
    })
}

function hasRightsToAddResources(links) {
  for (const key in links) {
    if (key === 'add_resources') {
      return true
    }
  }
  return false
}

function isAttachment(value) {
  return value instanceof Object
}

function decodeHTML(htmlString) {
  let spacedString = transformDashToSpace(htmlString)
  const parser = new DOMParser()
  const decodedString = parser.parseFromString(spacedString, 'text/html').body.textContent
  const txt = document.createElement('div')
  txt.innerHTML = decodedString
  return txt.innerHTML
}

async function updateNegotiation(message) {
  await negotiationPageStore
    .updateNegotiationStatus(negotiation.value.id, selectedStatus.value.value, message)
    .then(async () => {
      negotiation.value = await negotiationPageStore.retrieveNegotiationById(props.negotiationId)
    })
  await reloadResources()
  await reloadStates()

  negotiationPosts.value.retrievePostsByNegotiationId()
}

function canDelete() {
  return (
    negotiation.value.status === 'DRAFT' &&
    (isAdmin.value || userInfo.value.subjectId === negotiation.value.author.subjectId)
  )
}

async function deleteNegotiation() {
  await negotiationPageStore.deleteNegotiation(negotiation.value.id).then(router.push('/'))
}

function translateTrueFalse(value) {
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  return value
}

async function reloadResources() {
  const resourceResponse = await negotiationPageStore.retrieveResourcesByNegotiationId(
    props.negotiationId,
  )
  if (resourceResponse !== undefined) {
    resources.value = resourceResponse
  }
  negotiation.value = await negotiationPageStore.retrieveNegotiationById(props.negotiationId)
  timelineEvents.value = await negotiationPageStore.retrieveNegotiationTimeline(props.negotiationId)
}

async function reloadStates() {
  possibleEvents.value = await negotiationPageStore.retrievePossibleEvents(props.negotiationId)
}

async function downloadAttachment(id, name) {
  try {
    downloadingAttachments.value.add(id)
    await negotiationPageStore.downloadAttachment(id, name)
  } catch (error) {
    console.error('Download failed:', error)
  } finally {
    downloadingAttachments.value.delete(id)
  }
}

function downloadAttachmentFromLink(href) {
  negotiationPageStore.downloadAttachmentFromLink(href)
}

function transformDashToSpace(text) {
  if (text) {
    return text.split('-').join(' ')
  }

  return ''
}

function updateNegotiationPayload() {
  router.push(`/edit/requests/${props.negotiationId}`)
}
</script>

<style scoped>
.collections-header[aria-expanded='true'] .bi-chevron-down {
  display: none;
}

.collections-header:not([aria-expanded]) .bi-chevron-up {
  display: none;
}

.collections-header[aria-expanded='false'] .bi-chevron-up {
  display: none;
}

.icon-smaller {
  font-size: 0.85em;
  position: relative;
  top: 2px;
  color: black;
}

.submission-text {
  color: green;
  opacity: 0.7;
}
.unpack:hover {
  background-color: lightgray;
  color: #212529;
}
.status-box:hover {
  background-color: lightgray;
  color: #212529;
}

.requirement-text {
  color: red;
  opacity: 0.7;
}

.lifecycle-text:hover {
  color: orange;
}

.abandon-text {
  color: #3c3c3d;
}

.nav-item.dropdown .dropdown-menu {
  min-width: 140px;
  max-width: 200px;
  background-color: #e7e7e7;
  border: 1px solid #dee2e6;
  border-radius: 0;
  box-shadow: none;
}

.nav-item.dropdown .dropdown-item {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #495057;
  background-color: #e7e7e7;
}
.nav-item:hover .nav-link,
.nav-item.dropdown .dropdown-item:hover,
.nav-item.dropdown .dropdown-item:focus {
  background-color: lightgray;
  color: #212529;
}
.abandon-text:hover {
  color: #dc3545;
}
</style>

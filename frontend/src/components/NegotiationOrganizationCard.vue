<template>
  <form-submission-modal
    id="formSubmissionModal"
    :title="requiredAccessForm.name"
    :negotiation-id="negotiationId"
    :requirement-id="requirementId"
    :resource-id="resourceId"
    :required-access-form-id="requiredAccessForm.id"
    @confirm="hideFormSubmissionModal()"
  />
  <form-view-modal id="formViewModal" :payload="submittedForm" />
  <confirmation-modal
    id="statusUpdateModal"
    :title="`Status update for ${
      selectedOrganization ? selectedOrganization.name : 'Unknown'
    }`"
    :text="`Are you sure you want to change the status of all ${
      selectedOrganization ? selectedOrganization.name : 'Unknown'
    } resources you represent in this Negotiation to ${
      orgStatus ? orgStatus.label : 'Unknown'
    } ?`"
    :message-enabled="true"
    @confirm="updateOrganization()"
  />
  <div class="card mb-2">
    <div class="card-header">
      <div class="form-check d-flex">
        <div
          class="collapse-organization d-flex justify-content-end pt-1 p-1 cursor-pointer unpack align-items-center"
          data-bs-toggle="collapse"
          aria-expanded="false"
          :data-bs-target="`#card-body-block-${getElementIdFromResourceId(
            orgId
          )}`"
          :aria-controls="`card-body-block-${getElementIdFromResourceId(
            orgId
          )}`"
        >
          <div>
            <i class="bi bi-chevron-down" />
            <i class="bi bi-chevron-up" />
          </div>
          <div class="cursor-pointer">
            <i
              class="bi bi-buildings mx-2"
              :style="{ color: uiConfiguration.primaryTextColor }"
            />
            <label
              class="fw-bold ml-2 cursor-pointer"
              :style="{ color: uiConfiguration.secondaryTextColor }"
            >
              {{ org.name }}
            </label>
          </div>
        </div>
        <div class="ms-2 mx-2 ms-auto d-flex flex-column">
          <div
            class="ms-2 mx-2 ms-auto d-inline-flex align-items-center status-box p-1 cursor-pointer"
            role="button"
            title="Select current status.The term Resource is abstract and can for example refer to biological samples, datasets or a service such as sequencing."
            @click="toggleDropdown(orgId)"
          >
            <span class="badge" :class="getStatusColor(org.status)">
              <i :class="getStatusIcon(org.status)" class="px-1" />
              {{ org.status?.replace(/_/g, " ") || "" }}
            </span>
            <i
              v-if="org.updatable"
              class="bi bi-caret-down-fill icon-smaller mx-1"
            />
          </div>
          <div>
            <ul
              v-if="org.updatable"
              class="dropdown-menu"
              :class="{ show: dropdownVisible[orgId] }"
            >
              <li
                v-for="state in sortedStates"
                :key="state.value"
                class="dropdown-item cursor-pointer"
                data-bs-toggle="modal"
                data-bs-target="#statusUpdateModal"
                @click="updateOrgStatus(state, org, orgId)"
              >
                <i :class="getStatusIcon(state.value)" class="px-1" />
                {{ state.label }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
    <div
      :id="`card-body-block-${getElementIdFromResourceId(orgId)}`"
      class="collapse multi-collapse"
    >
      <div
        v-for="resource in org.resources"
        :key="resource.id"
        class="card-body"
      >
        <div class="form-check">
          <div class="d-flex flex-row align-items-center flex-row">
            <div>
              <label
                class="form-check-label"
                :style="{ color: uiConfiguration.primaryTextColor }"
                :for="getElementIdFromResourceId(resource.sourceId)"
              >
                <i class="bi bi-box-seam" />
                {{ resource.name }}
              </label>
              <span class="badge rounded-pill bg-status-badge ms-4">
                {{ getStatusForResource(resource.id) }}
              </span>
              <div
                :style="{
                  color: uiConfiguration.primaryTextColor,
                  opacity: 0.7,
                }"
              >
                {{ resource.sourceId }}
                <CopyTextButton :text="resource.sourceId" />
              </div>
            </div>
            <div
              v-if="getLifecycleLinks(resource._links).length > 0"
              class="ms-4"
            >
              Update status:
              <div
                v-for="(link, index) in getLifecycleLinks(resource._links)"
                :key="index"
                class="lifecycle-links flex-column"
              >
                <a
                  class="lifecycle-text cursor-pointer"
                  @click="updateResourceState(link.href)"
                ><i class="bi bi-patch-check" /> {{ link.name }}</a
                >
              </div>
            </div>
          </div>

          <div v-for="(link, index) in getSubmissionLinks(resource._links)" :key="index">
            <a
              class="submission-text cursor-pointer"
              @click.prevent="openFormModal(link.href)"
            ><i class="bi bi-check-circle" /> {{ link.name }}
            </a>
          </div>
          <div v-for="(link, index) in getRequirementLinks(resource._links)" :key="index">
            <a
              class="requirement-text cursor-pointer"
              @click="openModal(link.href, resource.id)"
            ><i class="bi bi-exclamation-circle-fill" />
              {{ link.title }} required</a
            >
          </div>
          <div class="col-12 col-md-4 order-2 order-md-2" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import {
  getStatusColor,
  getStatusIcon,
  transformStatus
} from '../composables/utils.js';
import CopyTextButton from "@/components/CopyTextButton.vue";
// eslint-disable-next-line
import FormViewModal from "@/components/modals/FormViewModal.vue";
// eslint-disable-next-line
import FormSubmissionModal from "@/components/modals/FormSubmissionModal.vue";

const props = defineProps({
  orgId: {
    type: Object,
    default: () => ({})
  },
  org: {
    type: Object,
    default: () => ({})
  },
  resources: {
    type: Array,
    default: () => ([])
  },
  resourceStates: {
    type: Array,
    default: () => ([])
  },
  negotiationId: {
    type: String,
    default: undefined
  }
});

const uiConfigurationStore = useUiConfiguration();
const requirementId = ref(undefined);
const resourceId = ref(undefined);
const requiredAccessForm = ref({});
const formSubmissionModal = ref(null);
const dropdownVisible = reactive({});
const selectedOrganization = ref(undefined);
const orgStatus = ref(undefined);
const negotiationPageStore = useNegotiationPageStore();
const submittedForm = ref(undefined);
const formViewModal = ref(null);
const emit = defineEmits(["reloadResources"]);

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme;
});

const resourcesById = computed(() => {
  return getResources.value.reduce((resourcesObjects, resource) => {
    resourcesObjects[resource.id] = resource;
    return resourcesObjects;
  }, {});
});

const getResources = computed(() => {
  return props.resources
})

const sortedStates = computed(() => {
  return props.resourceStates
    .slice()
    .sort((a, b) => Number(a.ordinal) - Number(b.ordinal));
});

const toggleDropdown = (orgId) => {
  dropdownVisible[orgId] = !dropdownVisible[orgId];
};

function getElementIdFromResourceId(resourceId) {
  return resourceId.replaceAll(":", "_");
}

async function updateOrgStatus(state, organization, orgId) {
  toggleDropdown(orgId);
  selectedOrganization.value = organization;
  orgStatus.value = state;
}

function getStatusForResource(resourceId) {
  const resource = resourcesById.value[resourceId].currentState;
  return transformStatus(resource);
}
async function updateResourceState(link) {
  await negotiationPageStore.updateResourceStatus(link);
  emit("reloadResources");
}
function getSubmissionLinks(links) {
  const submissionLinks = [];
  for (const key in links) {
    // Check if the key starts with "submission-"
    if (key.startsWith("submission-")) {
      // Push the href value of the link to the submissionLinks array
      submissionLinks.push(links[key]);
    }
  }
  return submissionLinks;
}

async function openModal(href, resourcesId) {
  const requirement = await negotiationPageStore.retrieveInfoRequirement(href);
  resourceId.value = resourcesId;
  requiredAccessForm.value = requirement.requiredAccessForm;
  requirementId.value = requirement.id;
  // eslint-disable-next-line
  formSubmissionModal.value = new Modal(
    document.querySelector('#formSubmissionModal')
  );
  formSubmissionModal.value.show();
}

async function openFormModal(href) {
  const payload = await negotiationPageStore.retrieveInformationSubmission(
    href
  );
  submittedForm.value = payload.payload;
  // eslint-disable-next-line
  formViewModal.value = new Modal(document.querySelector("#formViewModal"));
  formViewModal.value.show();
}

function getRequirementLinks(links) {
  const requirementLinks = [];
  for (const key in links) {
    if (key.startsWith("requirement-")) {
      requirementLinks.push(links[key]);
    }
  }
  return requirementLinks;
}

function getLifecycleLinks(links) {
  const lifecycleLinks = [];
  for (const key in links) {
    if (links[key].title === "Next Lifecycle event") {
      lifecycleLinks.push(links[key]);
    }
  }
  return lifecycleLinks;
}

async function hideFormSubmissionModal() {
  formSubmissionModal.value.hide();
  emit("reloadResources");
}

function getRepresentedResources(resources) {
  const resourceIds = [];

  // Use for...of to iterate over the array of resources
  for (const resource of resources) {
    // Iterate over the _links of the resource
    for (const key in resource._links) {
      if (resource._links[key].title === "Next Lifecycle event") {
        resourceIds.push(resource.id);
        break; // Exit inner loop when condition is met
      }
    }
  }
  return resourceIds;
}

async function updateOrganization() {
  const data = {
    resourceIds: getRepresentedResources(selectedOrganization.value.resources),
    state: orgStatus.value.value,
  };
  const negotiationId = props.negotiationId;
  await negotiationPageStore.addResources(data, negotiationId);
  emit("reloadResources");
}
</script>

<style scoped>
.collapse-organization[aria-expanded="true"] .bi-chevron-down {
  display: none;
}

.collapse-organization:not([aria-expanded]) .bi-chevron-up {
  display: none;
}

.collapse-organization[aria-expanded="false"] .bi-chevron-up {
  display: none;
}
</style>

<template>
  <confirmation-modal
    id="saveModal"
    title="Are you sure you want to save?"
    text="Confirming, you will change ui-configuration."
    :message-enabled="false"
    @confirm="save()"
  />
  <h1 class="mb-5">UI Configuration</h1>
  <div class="ui-configuration">
    <div class="theme mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Theme Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse1"
          aria-expanded="true"
          aria-controls="collapse1"
        >
          <i class="bi bi-arrows-angle-expand" /> collapse/expand
        </button>
      </div>
      <div id="collapse1" class="theme-config row collapse show">
        <UiConfigurationSetting v-model="uiConfiguration.theme" />
      </div>
    </div>
    <div class="nav-bar mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Navbar Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse2"
          aria-expanded="false"
          aria-controls="collapse2"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse2" class="navbar-config row collapse">
        <h5 class="mb-3 bold text-muted">
          You can see the changes live in the navbar above!
          <i class="bi bi-arrow-up" />
        </h5>
        <UiConfigurationSetting v-model="uiConfiguration.navbar" />
      </div>
    </div>
    <div class="login mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Login page Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse3"
          aria-expanded="false"
          aria-controls="collapse3"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse3" class="login-config row collapse">
        <UiConfigurationSetting v-model="uiConfiguration.login" />
        <HomePage :is-ui-config-active="true" />
      </div>
    </div>
    <div class="filter mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Filter Sort Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse4"
          aria-expanded="false"
          aria-controls="collapse4"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse4" class="filter-config row collapse mb-5">
        <UiConfigurationSetting v-model="uiConfiguration.filtersSort" />
        <FilterSort
          v-model:filtersSortData="filtersSortData"
          :user-role="userRole"
          :filters-status="filtersStatus"
          @filters-sort-data="retrieveNegotiationsBySortAndFilter"
        />
      </div>
    </div>
    <div class="negotiation-list mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Negotiation List Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse5"
          aria-expanded="false"
          aria-controls="collapse5"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse5" class="negotiation-list-config row collapse mb-5">
        <UiConfigurationSetting v-model="uiConfiguration.negotiationList" />
        <NegotiationList
          :negotiations="negotiations"
          :pagination="pagination"
          :user-role="userRole"
          v-model:filtersSortData="filtersSortData"
          @filters-sort-data="retrieveNegotiationsBySortAndFilter"
        />
      </div>
    </div>
    <div class="new-request mb-2">
      <div class="mb-3 text-left fw-bold h3">
        New Request Button
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse6"
          aria-expanded="false"
          aria-controls="collapse6"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse6" class="new-request-config row collapse">
        <UiConfigurationSetting v-model="uiConfiguration.newRequestButton" />
        <NewRequestButton v-if="!networkActivated" />
      </div>
    </div>
    <div class="footer mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Footer Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse7"
          aria-expanded="false"
          aria-controls="collapse7"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse7" class="footer-config row collapse">
        <h5 class="mb-3 bold text-muted">
          You can see the changes live in the footer below!
          <i class="bi bi-arrow-down" />
        </h5>
        <UiConfigurationSetting v-model="uiConfiguration.footer" />
      </div>
    </div>
  </div>
  <div class="sticky-bottom pb-3">
    <button class="btn btn-sm bg-primary sm my-2" @click="restartSettings()">
      <i class="bi bi-arrow-clockwise text-white">Reset to default settings</i>
    </button>
    <button
      class="btn btn-sm bg-primary sm my-2 float-end"
      data-bs-toggle="modal"
      data-bs-target="#saveModal"
    >
      <i class="bi bi-floppy text-white"> Save Changes </i>
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import UiConfigurationSetting from '../components/UiConfigurationSetting.vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import HomePage from '../views/HomePage.vue'
import FilterSort from '@/components/FilterSort.vue'
import NegotiationList from '@/components/NegotiationList.vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'

const uiConfigurationStore = useUiConfiguration()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration
})

function restartSettings() {
  uiConfigurationStore.retrieveUiConfiguration()
}

function save() {
  uiConfigurationStore.updateUiConfiguration(uiConfiguration.value)
}

const filtersSortData = ref({
  status: [],
  dateStart: '',
  dateEnd: '',
  sortBy: 'creationDate',
  sortDirection: 'DESC',
})
const userRole = ref('ROLE_ADMIN')
const filtersStatus = ref([
  {
    value: 'SUBMITTED',
    label: 'Under review',
    description: 'The negotiation has been submitted for review',
    _links: {
      states: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states' },
      self: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states/SUBMITTED' },
    },
  },
  {
    value: 'APPROVED',
    label: 'Approved',
    description: 'The negotiation has been approved',
    _links: {
      states: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states' },
      self: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states/APPROVED' },
    },
  },
  {
    value: 'DECLINED',
    label: 'Declined',
    description: 'The negotiation has been declined',
    _links: {
      states: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states' },
      self: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states/DECLINED' },
    },
  },
  {
    value: 'IN_PROGRESS',
    label: 'In Progress',
    description: 'The negotiation is currently in progress',
    _links: {
      states: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states' },
      self: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states/IN_PROGRESS' },
    },
  },
  {
    value: 'ABANDONED',
    label: 'Abandoned',
    description: 'The negotiation has been abandoned',
    _links: {
      states: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states' },
      self: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states/ABANDONED' },
    },
  },
  {
    value: 'CONCLUDED',
    label: 'Concluded',
    description: 'The negotiation has been concluded',
    _links: {
      states: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states' },
      self: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states/CONCLUDED' },
    },
  },
  {
    value: 'PAUSED',
    label: 'Paused',
    description: 'The negotiation is paused',
    _links: {
      states: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states' },
      self: { href: 'http://localhost:8081/api/v3/negotiation-lifecycle/states/PAUSED' },
    },
  },
])
const pagination = ref({ size: 50, totalElements: 501, totalPages: 10, number: 0 })
const negotiations = ref([
  {
    id: 'afa2d124-6870-1516-bc1a-d734c4be2617',
    author: {
      id: '10301',
      subjectId: 'a281825e7316bf3ef31d2ba32d3a7c5319c1573b@lifescience-ri.eu',
      name: 'Milos Terzic',
      email: 'milos.terzic@bbmri-eric.eu',
      representativeOfAnyResource: false,
      admin: false,
      networkManager: false,
    },
    payload: {
      project: {
        title: 'Evaluation of an in-vitro test for the detection of tumor diseases',
        description:
          'Malignant diseases (ICD-10 C00–97, excluding C44) constitute one of the most common causes of death in Germany, according to the Federal Statistical Office (Destatis), with nearly 500,000 new cases expected each year. Despite all preventive programs and improved treatment options, approximately 240,000 patients die from cancer each year in Germany, and these numbers remain constant.\n\nEarly detection of malignant diseases, even in the subclinical stage, could lead to better treatment outcomes for many tumor entities due to the expected lower tumor stages. The value of determining currently prevalent tumor markers (such as CEA, CA 19-9) generally shows insufficient sensitivity or specificity. For this reason, they are generally not suitable for screening, except for markers such as AFP and ß-HCG. A routine laboratory test for body fluids that captures all malignant tumor types—a "global tumor marker"—is currently not known or available. In principle, a tumor marker suitable for screening should have high sensitivity and specificity for the presence or exclusion of a malignant disease, ideally even specific to different tumor entities and tumor burden. Furthermore, monitoring therapy response or the possibility of recurrence diagnosis would be desirable.\n\nThis study aims to evaluate the performance and feasibility for quality control of a new in-vitro test.',
      },
      request: {
        description:
          'I would like to request n=100 EDTA-Plasma samples (about 330μl each) from patients (male/female, age 18-90 years) with a C25.- diagnosis. ',
      },
      'ethics-vote': {
        'ethics-vote':
          'The Ethics Committee of the University of Regensburg, in its meeting on January 24, 2024, deliberated on the aforementioned research project based on the documents listed in the appendix. There are no professional ethical or legal concerns arising from the submitted research project.',
        'ethics-vote-attachment': {
          id: '221c1255-57ef-47c9-8373-587b868fe0e2',
          name: '101-Neuantrag - Votum positiv uneingeschränkt_23-3510-101.pdf',
          size: 52972,
          contentType: 'application/pdf',
          organizationId: null,
        },
      },
    },
    status: 'CONCLUDED',
    humanReadable:
      'C25.% and male or female and 18 ≤ Diagnosis Age Donor ≤ 90 and 18 ≤ Donor Age ≤ 90 and blood-plasma',
    publicPostsEnabled: true,
    privatePostsEnabled: true,
    creationDate: '2024-03-18T10:00:49.912635',
    modifiedDate: '2024-03-18T10:07:05.424134',
    _links: {
      self: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/afa2d124-6870-4516-bc4a-d734c4be2647',
      },
      posts: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/afa2d124-6870-4516-bc4a-d734c4be2647/posts',
      },
      attachments: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/afa2d124-6870-4516-bc4a-d734c4be2647/attachments',
      },
      resources: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/afa2d124-6870-4516-bc4a-d734c4be2647/resources',
      },
    },
  },
  {
    id: '7ef6bd2b-3edf-1f8a-bd4d-3c8acc085385',
    author: {
      id: '671',
      subjectId: '3d5ed31d339a538122c9b72bb1f7d2dc95d39f10@lifescience-ri.eu',
      name: 'Milos Terzic',
      email: 'milos.terzic@bbmri-eric.eu',
      representativeOfAnyResource: false,
      admin: false,
      networkManager: false,
    },
    payload: {
      project: {
        title: 'CONTROL COHORT SERUM SAMPLES',
        description:
          'We are requesting 30 male peripheral blood serum control samples for an ongoing project (ethics committee approved). The samples will be used for analysis using the MALDI-TOF fingerprinting methodology for the development of potential new diagnostic methods.',
      },
      request: {
        description:
          'We are requesting 30 peripheral blood serum control samples for an ongoing project (ethics committee approved). The samples will be used for analysis using the MALDI-TOF fingerprinting methodology for the development of potential new diagnostic methods.',
      },
      'ethics-vote': {
        'ethics-vote': 'approved',
        'ethics-vote-attachment': null,
      },
    },
    status: 'SUBMITTED',
    humanReadable: '#1: Text search is masaryk memo',
    publicPostsEnabled: true,
    privatePostsEnabled: true,
    creationDate: '2024-03-11T13:51:38.133107',
    modifiedDate: '2024-03-11T14:38:09.178148',
    _links: {
      self: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/7ef6bd2b-3edf-4f8a-bd4d-3c8acc085385',
      },
      posts: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/7ef6bd2b-3edf-4f8a-bd4d-3c8acc085385/posts',
      },
      attachments: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/7ef6bd2b-3edf-4f8a-bd4d-3c8acc085385/attachments',
      },
      resources: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/7ef6bd2b-3edf-4f8a-bd4d-3c8acc085385/resources',
      },
    },
  },
  {
    id: '9872ead0-9a4a-1a21-9c53-05f166877c1a',
    author: {
      id: '671',
      subjectId: '3d5ed31d339a538122c9b72bb1f7d2dc95d39f10@lifescience-ri.eu',
      name: 'Milos Terzic',
      email: 'milos.terzic@bbmri-eric.eu',
      representativeOfAnyResource: false,
      admin: false,
      networkManager: false,
    },
    payload: {
      project: {
        title: 'Renal Cell Carcinoma samples',
        description:
          'We are requesting peripheral blood serum samples from patients with RCC for an ongoing project (ethics committee approved). The samples will be used for analysis using the MALDI-TOF fingerprinting methodology for the development of potential new diagnostic methods.',
      },
      request: {
        description:
          'We are requesting approx. 40 peripheral blood serum samples from patients with RCC for an ongoing project (ethics committee approved). The samples will be used for analysis using the MALDI-TOF fingerprinting methodology for the development of potential new diagnostic methods.',
      },
      'ethics-vote': {
        'ethics-vote': 'approved',
        'ethics-vote-attachment': null,
      },
    },
    status: 'ABANDONED',
    humanReadable: '#1: Text search is masaryk memo\r\n#2: Text search is masaryk memo',
    publicPostsEnabled: true,
    privatePostsEnabled: true,
    creationDate: '2024-03-11T13:31:55.785951',
    modifiedDate: '2024-03-11T14:37:52.159067',
    _links: {
      self: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/9872ead0-9a4a-4a21-9c53-05f166877c4a',
      },
      posts: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/9872ead0-9a4a-4a21-9c53-05f166877c4a/posts',
      },
      attachments: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/9872ead0-9a4a-4a21-9c53-05f166877c4a/attachments',
      },
      resources: {
        href: 'https://negotiator.acc.bbmri-eric.eu/api/v3/negotiations/9872ead0-9a4a-4a21-9c53-05f166877c4a/resources',
      },
    },
  },
])

function retrieveNegotiationsBySortAndFilter() {}
</script>

<style scoped>
.ui-configuration {
  min-height: 70vh;
}
</style>

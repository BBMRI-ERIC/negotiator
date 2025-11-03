<template>
  <div v-if="!loading" class="container">
    <NewRequestButton v-if="!networkActivated" />
    <div class="pt-1">
      <div class="row mt-5 pt-3">
        <div class="col-12 mb-3">
          <div class="input-group">
            <span class="input-group-text">
              <i class="bi bi-search"></i>
            </span>
            <input
              v-model="searchQuery"
              type="text"
              class="form-control"
              placeholder="Search by Display ID, or Title..."
              @input="handleSearchInput"
            />
            <button
              v-if="searchQuery"
              class="btn btn-outline-secondary"
              type="button"
              @click="clearSearch"
            >
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
        </div>
      </div>
      <div class="row row-cols-2 d-grid-row">
        <p>
          <span
            class="negotiations-search-results"
            :style="{ color: 'var(--bs-search-results-text)' }"
          >
            <strong>Search results: </strong>
          </span>
          <br />
          <span
            class="negotiations-number"
            :style="{ color: 'var(--bs-search-results-text)', opacity: 0.5 }"
            >{{ pagination.totalElements }} Negotiations found</span
          >
        </p>

        <div class="text-end my-2">
          <button
            v-if="negotiations.length > 0"
            type="button"
            class="btn btn-sm me-2"
            :class="savedNegotiationsView === 'Card-one-column' ? '' : 'bg-body'"
            :style="
              savedNegotiationsView === 'Card-one-column'
                ? { 'background-color': 'var(--bs-display-view-button-color)' }
                : ''
            "
            @click="setSavedNegotiationsView('Card-one-column')"
          >
            <i class="bi bi-list" />
          </button>

          <button
            v-if="negotiations.length > 1"
            type="button"
            class="btn btn-sm me-2"
            :class="savedNegotiationsView === 'Card-two-column' ? '' : 'bg-body'"
            :style="
              savedNegotiationsView === 'Card-two-column'
                ? { 'background-color': 'var(--bs-display-view-button-color)' }
                : ''
            "
            @click="
              ((savedNegotiationsView = 'Card-two-column'),
              setSavedNegotiationsView('Card-two-column'))
            "
          >
            <i class="bi bi-grid" />
          </button>

          <button
            v-if="negotiations.length > 0"
            type="button"
            class="btn btn-sm"
            :class="savedNegotiationsView === 'Table' ? '' : 'bg-body'"
            :style="
              savedNegotiationsView === 'Table'
                ? { 'background-color': 'var(--bs-display-view-button-color)' }
                : ''
            "
            @click="((savedNegotiationsView = 'Table'), setSavedNegotiationsView('Table'))"
          >
            <i class="bi bi-table" />
          </button>
        </div>
      </div>
      <div
        v-if="
          savedNegotiationsView === 'Card-one-column' || savedNegotiationsView === 'Card-two-column'
        "
        class="row row-cols-1 d-grid-row"
        :class="savedNegotiationsView === 'Card-one-column' ? 'row-cols-md-1' : 'row-cols-md-2'"
      >
        <NegotiationCard
          v-for="fn in negotiations"
          :id="fn.id"
          :key="fn.id"
          :title="fn.payload.project.title"
          :status="fn.status"
          :submitter="fn.author.name"
          :creation-date="formatDate(fn.creationDate)"
          :class="networkActivated === true ? '' : 'cursor-pointer'"
          @click="goToNegotiation(fn)"
        />
      </div>

      <div v-if="savedNegotiationsView === 'Table'">
        <PrimaryTable :headers="tableHeaders" :data="tableData">
          <template #header-title>
            <span class="text-table-header-text">Title</span>
            <SortButton
              :sortKey="'title'"
              :filtersSortData="filtersSortData"
              :color="'var(--bs-table-header-text)'"
              iconAsc="bi bi-sort-alpha-up-alt"
              iconDesc="bi bi-sort-alpha-down"
              defaultIcon="bi bi-sort-alpha-up-alt"
              @sort="
                (key) => {
                  changeSortDirection(key)
                  emitFilterSortData()
                }
              "
            />
          </template>

          <template #header-creationDate>
            <span class="text-table-header-text">Created on</span>
            <SortButton
              :sortKey="'creationDate'"
              :filtersSortData="filtersSortData"
              :color="'var(--bs-table-header-text)'"
              iconAsc="bi bi-sort-numeric-up-alt"
              iconDesc="bi bi-sort-numeric-down"
              defaultIcon="bi bi-sort-numeric-up-alt"
              @sort="
                (key) => {
                  changeSortDirection(key)
                  emitFilterSortData()
                }
              "
            />
          </template>

          <template #header-status>
            <span class="text-table-header-text">Status</span>
            <SortButton
              id="v-step-2"
              :sortKey="'currentState'"
              :filtersSortData="filtersSortData"
              :color="'var(--bs-table-header-text)'"
              iconAsc="bi bi-sort-up-alt"
              iconDesc="bi bi-sort-down"
              defaultIcon="bi bi-sort-up-alt"
              @sort="
                (key) => {
                  changeSortDirection(key)
                  emitFilterSortData()
                }
              "
            />
          </template>
          <template #creationDate="{ value }">
            <TimeStamp :value="value" :muted="true" />
          </template>
          <template #status="{ value }">
            <UiBadge :class="getBadgeColor(value)" :icon="getBadgeIcon(value)" width="120px">
              {{ transformStatus(value) }}
            </UiBadge>
          </template>
          <template #chevron>
            <i
              class="bi bi-chevron-right float-end"
              :style="{ color: 'var(--bs-table-header-text)' }"
            />
          </template>
        </PrimaryTable>
      </div>

      <h2 v-if="negotiations.length === 0" class="text-center">No Negotiations found</h2>
    </div>

    <div v-if="pagination.totalElements === 0" class="d-flex justify-content-center">
      <div class="d-flex row justify-content-center">
        <h3 class="text-center mt-3">
          <i style="color: #7c7c7c" class="bi bi-circle" />
        </h3>
        <h4 class="mb-3 ms-3 mt-3 text-center">There aren’t any negotiations.</h4>
      </div>
    </div>
  </div>
  <div v-else-if="loading" class="d-flex justify-content-center flex-row">
    <div class="d-flex justify-content-center">
      <div class="spinner-border d-flex justify-content-center" role="status" />
      <div class="d-flex justify-content-center">
        <h4 class="mb-3 ms-3">Loading ...</h4>
      </div>
    </div>
  </div>
  <div v-else class="d-flex justify-content-center flex-row">
    <div class="d-flex justify-content-center">
      <div class="d-flex justify-content-center">
        <h4 class="mb-3 ms-3">No Negotiations found :(</h4>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeMount, ref, watch } from 'vue'
import NegotiationCard from '@/components/NegotiationCard.vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import PrimaryTable from '@/components/ui/table/PrimaryTable.vue'
import { ROLES } from '@/config/consts'
import { useRouter } from 'vue-router'
import {
  getBadgeColor,
  getBadgeIcon,
  transformStatus,
  formatTimestampToLocalDateTime,
} from '../composables/utils.js'
import NewRequestButton from '../components/NewRequestButton.vue'
import { useNegotiationsViewStore } from '../store/negotiationsView.js'
import TimeStamp from '@/components/ui/TimeStamp.vue'
import SortButton from '@/components/ui/SortButton.vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()
const filtersSortData = defineModel('filtersSortData')
const router = useRouter()
const negotiationsViewStore = useNegotiationsViewStore()
const searchQuery = ref('')
let searchTimeout = null

const props = defineProps({
  negotiations: {
    type: Array,
    default: undefined,
  },
  pagination: {
    type: Object,
    default: undefined,
  },
  userRole: {
    type: String,
    required: true,
    validator: (prop) =>
      [ROLES.RESEARCHER, ROLES.REPRESENTATIVE, ROLES.ADMINISTRATOR].includes(prop),
  },
  networkActivated: {
    type: Boolean,
    default: false,
  },
})

const tableHeaders = [
  {
    key: 'title',
    label: 'Title',
    slot: undefined,
    style: { color: '#3c3c3d' },
  },
  {
    key: 'displayId',
    label: t('negotiationPage.displayId'),
    slot: undefined,
    style: { color: '#3c3c3d' },
  },
  {
    key: 'creationDate',
    label: 'Created on',
    slot: 'creationDate',
    style: {},
  },
  {
    key: 'author',
    label: 'Author',
    slot: undefined,
    style: { color: '#3c3c3d' },
  },
  {
    key: 'status',
    label: 'Status',
    slot: 'status',
    style: {},
  },
  {
    key: 'chevron',
    label: '',
    slot: 'chevron',
    style: {},
  },
]

const tableData = computed(() =>
  Array.isArray(props.negotiations)
    ? props.negotiations.map((fn) => ({
        title: fn.payload?.project?.title,
        displayId: fn.displayId,
        creationDate: fn.creationDate,
        author: fn.author.name,
        status: fn.status,
        chevron: '',
        href: router.resolve({ name: 'negotiation-page', params: { negotiationId: fn.id } }).href,
        _raw: fn,
      }))
    : [],
)

const loading = computed(() => {
  return props.negotiations === undefined
})

const savedNegotiationsView = computed(() => {
  return negotiationsViewStore.savedNegotiationsView
})

onBeforeMount(() => {
  if (negotiationsViewStore.savedNegotiationsView === '') {
    setSavedNegotiationsView('Table')
  }
})

function setSavedNegotiationsView(view) {
  negotiationsViewStore.savedNegotiationsView = view
}

function formatDate(date) {
  return formatTimestampToLocalDateTime(date)
}

function changeSortDirection(sortBy) {
  if (filtersSortData.value.sortDirection === 'DESC') {
    filtersSortData.value.sortBy = sortBy
    filtersSortData.value.sortDirection = 'ASC'
  } else {
    filtersSortData.value.sortBy = sortBy
    filtersSortData.value.sortDirection = 'DESC'
  }
}

const emit = defineEmits(['filtersSortData'])

function emitFilterSortData() {
  emit('filtersSortData', props.filtersSortData)
}

function goToNegotiation(negotiation) {
  router.push({
    name: 'negotiation-page',
    params: { negotiationId: negotiation.id },
  })
}

function handleSearchInput() {
  if (searchTimeout) {
    clearTimeout(searchTimeout)
  }

  searchTimeout = setTimeout(() => {
    filtersSortData.value.search = searchQuery.value
    emitFilterSortData()
  }, 500) // 500ms debounce
}

function clearSearch() {
  searchQuery.value = ''
  filtersSortData.value.search = ''
  emitFilterSortData()
}

// Initialize search query from filtersSortData
watch(
  () => filtersSortData.value?.search,
  (newSearch) => {
    if (newSearch !== searchQuery.value) {
      searchQuery.value = newSearch || ''
    }
  },
  { immediate: true },
)
</script>

<style scoped>
tbody a.d-table-row > .d-table-cell {
  vertical-align: middle;
  align-items: center;
  display: table-cell;
}

tbody a.d-table-row {
  border-bottom: 1px solid #dee2e6; /* Bootstrap table border color */
  box-sizing: border-box;
}
</style>

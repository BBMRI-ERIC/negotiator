<template>
  <div v-if="!loading" class="container">
    <NewRequestButton v-if="!networkActivated" />
    <div v-if="userRole === ROLES.ADMINISTRATOR && addAcessFormFeatureFlag" class="float-end ms-2">
      <RedirectButton
        :buttonText="'+  Add Access Form'"
        :redirectRouteName="'customize-form'"
        :titleModal="'Are you sure?'"
        :textModal="'You will be redirected to page where you can add access form'"
      />
    </div>
    <div class="pt-1">
      <div class="row row-cols-2 d-grid-row mt-5 pt-3">
        <p>
          <span
            class="negotiations-search-results"
            :style="{ color: uiConfiguration?.searchResultsTextColor }"
          >
            <strong>Search results: </strong>
          </span>
          <br />
          <span
            class="negotiations-number"
            :style="{ color: uiConfiguration?.searchResultsTextColor, opacity: 0.5 }"
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
                ? { 'background-color': uiConfiguration?.displayViewButtonColor }
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
                ? { 'background-color': uiConfiguration?.displayViewButtonColor }
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
            id="v-step-1"
            type="button"
            class="btn btn-sm"
            :class="savedNegotiationsView === 'Table' ? '' : 'bg-body'"
            :style="
              savedNegotiationsView === 'Table'
                ? { 'background-color': uiConfiguration?.displayViewButtonColor }
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
        <div class="table-responsive">
          <table class="table table-hover">
            <thead class="text-nowrap">
              <tr class="text-table-header-text">
                <th scope="col" :style="{ color: uiConfiguration?.tableTextColor }">
                  Title
                  <button
                    class="btn btn-sm py-0"
                    :style="{ color: uiConfiguration?.tableTextColor }"
                    type="button"
                    @click="(changeSortDirection('title'), emitFilterSortData())"
                  >
                    <i
                      :class="
                        filtersSortData.sortDirection === 'ASC' &&
                        filtersSortData.sortBy === 'title'
                          ? 'bi bi-sort-alpha-up-alt'
                          : 'bi-sort-alpha-down'
                      "
                    />
                    <i v-if="filtersSortData.sortBy !== 'title'" class="bi bi-sort-alpha-up-alt" />
                  </button>
                </th>
                <th scope="col" :style="{ color: uiConfiguration?.tableTextColor }">
                  Negotiation ID
                </th>
                <th scope="col" :style="{ color: uiConfiguration?.tableTextColor }">
                  Created on
                  <button
                    class="btn btn-sm py-0"
                    :style="{ color: uiConfiguration?.tableTextColor }"
                    type="button"
                    @click="(changeSortDirection('creationDate'), emitFilterSortData())"
                  >
                    <i
                      :class="
                        filtersSortData.sortDirection === 'ASC' &&
                        filtersSortData.sortBy === 'creationDate'
                          ? 'bi bi-sort-numeric-up-alt'
                          : 'bi-sort-numeric-down'
                      "
                    />
                    <i
                      v-if="filtersSortData.sortBy !== 'creationDate'"
                      class="bi bi-sort-numeric-up-alt"
                    />
                  </button>
                </th>
                <th scope="col" :style="{ color: uiConfiguration?.tableTextColor }">Author</th>
                <th scope="col" :style="{ color: uiConfiguration?.tableTextColor }">
                  Status
                  <button
                    id="v-step-2"
                    class="btn btn-sm py-0"
                    :style="{ color: uiConfiguration?.tableTextColor }"
                    type="button"
                    @click="(changeSortDirection('currentState'), emitFilterSortData())"
                  >
                    <i
                      :class="
                        filtersSortData.sortDirection === 'ASC' &&
                        filtersSortData.sortBy === 'currentState'
                          ? 'bi bi-sort-up-alt'
                          : 'bi-sort-down'
                      "
                    />
                    <i v-if="filtersSortData.sortBy !== 'currentState'" class="bi bi-sort-up-alt" />
                  </button>
                </th>
                <th scope="col" />
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(fn, index) in negotiations"
                :key="index"
                @click="goToNegotiation(fn)"
                class="cursor-pointer"
              >
                <th scope="row" :style="{ color: uiConfiguration?.tableTextColor }">
                  <span>
                    {{ fn.payload.project.title }}
                  </span>
                </th>
                <td>
                  <span :style="{ color: uiConfiguration?.tableTextColor, opacity: 0.7 }">
                    {{ fn.id }}
                  </span>
                </td>
                <td>
                  <span :style="{ color: uiConfiguration?.tableTextColor, opacity: 0.7 }">
                    {{ formatDate(fn.creationDate) }}
                  </span>
                </td>
                <td>
                  <span :style="{ color: uiConfiguration?.tableTextColor, opacity: 0.7 }">
                    {{ fn.author.name }}
                  </span>
                </td>
                <td>
                  <span class="badge" :class="getBadgeColor(fn.status)" style="width: 120px">
                    <i :class="getBadgeIcon(fn.status)" class="px-1" />
                    {{ transformStatus(fn.status) }}
                  </span>
                </td>
                <td>
                  <i
                    class="bi bi-chevron-right float-end"
                    :style="{ color: uiConfiguration?.tableTextColor }"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
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
import { computed, onBeforeMount } from 'vue'
import NegotiationCard from '@/components/NegotiationCard.vue'
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
import { useUiConfiguration } from '../store/uiConfiguration.js'
import RedirectButton from '../components/RedirectButton.vue'
import allFeatureFlags from '@/config/featureFlags.js'


const filtersSortData = defineModel('filtersSortData')
const uiConfigurationStore = useUiConfiguration()
const router = useRouter()
const negotiationsViewStore = useNegotiationsViewStore()
const addAcessFormFeatureFlag = !!(
  allFeatureFlags.add_acess_form === 'true' || allFeatureFlags.add_acess_form === true
)

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

const loading = computed(() => {
  return props.negotiations === undefined
})

const savedNegotiationsView = computed(() => {
  return negotiationsViewStore.savedNegotiationsView
})

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.negotiationList
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
</script>

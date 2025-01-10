<template>
  <FilterSort
    v-if="!loading"
    v-model:filtersSortData="filtersSortData"
    :user-role="userRole"
    :filters-status="filtersStatus"
    @filters-sort-data="retrieveNegotiationsBySortAndFilter"
  />
  <NegotiationList
    :negotiations="negotiations"
    :pagination="pagination"
    :user-role="userRole"
    v-model:filtersSortData="filtersSortData"
    @filters-sort-data="retrieveNegotiationsBySortAndFilter"
  />
  <NegotiationPagination
    :negotiations="negotiations"
    :pagination="pagination"
    @current-page-number="retrieveNegotiationsByPage"
  />
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import moment from 'moment'
import NegotiationList from '@/components/NegotiationList.vue'
import NegotiationPagination from '@/components/NegotiationPagination.vue'
import FilterSort from '@/components/FilterSort.vue'
import { ROLES } from '@/config/consts.js'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../store/user'
import { useNegotiationsStore } from '../store/negotiations'

const userStore = useUserStore()
const negotiationsStore = useNegotiationsStore()
const router = useRouter()
const route = useRoute()

const props = defineProps({
  userRole: {
    type: String,
    required: true,
    validator: (prop) =>
      [ROLES.RESEARCHER, ROLES.REPRESENTATIVE, ROLES.ADMINISTRATOR].includes(prop),
  },
})

const negotiations = ref(undefined)
const pagination = ref(undefined)
const user = ref(undefined)
const userId = ref(undefined)
const filtersStatus = ref([])
const filtersSortData = ref({
  status: [],
  dateStart: '',
  dateEnd: '',
  sortBy: 'creationDate',
  sortDirection: 'DESC',
})

const loading = computed(() => {
  return negotiations.value === undefined
})

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
  user.value = userStore.userInfo
  userId.value = user.value?.id
  filtersStatus.value = await negotiationsStore.retrieveNegotiationLifecycleStates()

  if (Object.keys(route?.query).length) {
    loadActivefiltersSortDataFromURL()
  }
  retrieveNegotiationsByUserRole(0)
})

async function retrieveNegotiationsByUserRole(pageNumber) {
  if (props.userRole === 'ROLE_ADMIN') {
    negotiations.value = await negotiationsStore.retrieveNegotiations(
      filtersSortData.value,
      pageNumber,
    )
  }

  if (props.userRole === 'ROLE_RESEARCHER') {
    negotiations.value = await negotiationsStore.retrieveNegotiationsByUserId(
      'author',
      filtersSortData.value,
      userId.value,
      pageNumber,
    )
  }

  if (props.userRole === 'ROLE_REPRESENTATIVE') {
    negotiations.value = await negotiationsStore.retrieveNegotiationsByUserId(
      'representative',
      filtersSortData.value,
      userId.value,
      pageNumber,
    )
  }

  pagination.value = negotiations.value.page
  if (negotiations.value.page.totalElements === 0) {
    negotiations.value = {}
  } else {
    negotiations.value = negotiations.value._embedded.negotiations
  }
}

function retrieveNegotiationsByPage(currentPageNumber) {
  retrieveNegotiationsByUserRole(currentPageNumber - 1)
  updateRoutingParams(currentPageNumber)
}

function retrieveNegotiationsBySortAndFilter(filtersSortData) {
  filtersSortData.value = filtersSortData

  incriseDateEndIfSame()
  retrieveNegotiationsByUserRole(0)

  updateRoutingParams(1)
}

function updateRoutingParams(currentPageNumber) {
  router.push({
    query: { filtersSort: encodeURI(stringify(filtersSortData.value)), currentPageNumber },
  })
}

function stringify(obj) {
  let cache = []
  const str = JSON.stringify(obj, function (key, value) {
    if (typeof value === 'object' && value !== null) {
      if (cache.indexOf(value) !== -1) {
        // Circular reference found, discard key
        return
      }
      // Store value in our collection
      cache.push(value)
    }
    return value
  })
  cache = null // reset the cache
  return str
}

function loadActivefiltersSortDataFromURL() {
  if (route?.query.filtersSort) {
    filtersSortData.value = JSON.parse(decodeURI(route?.query.filtersSort))
  }

  if (route?.query.currentPageNumber) {
    retrieveNegotiationsByUserRole(route?.query.currentPageNumber - 1)
  } else {
    retrieveNegotiationsByUserRole(0)
  }
}

function incriseDateEndIfSame() {
  if (
    filtersSortData.value.dateStart &&
    filtersSortData.value.dateStart === filtersSortData.value.dateEnd
  ) {
    filtersSortData.value.dateEnd = moment(filtersSortData.value.dateEnd)
      .add(1, 'days')
      .format('YYYY-MM-DD')
  }
}
</script>

<template>
  <div class="mt-3">
    <div v-if="loading" class="text-center p-4">
      <div class="spinner-border" role="status">
        <span class="visually-hidden">Loading negotiations...</span>
      </div>
      <p class="mt-2">Loading negotiations data...</p>
    </div>
    <template v-else>
      <FilterSort
        v-if="isLoaded"
        :user-role="userRole"
        :filters-status="states"
        :filter-organizations="organizations"
        :filters-sort-data="filtersSortData"
        @filters-sort-data="$emit('retrieveNegotiationsBySortAndFilter')"
      />
      <NegotiationList
        :negotiations="negotiations"
        :pagination="pagination"
        :network-activated="true"
        :user-role="userRole"
        :filters-sort-data="filtersSortData"
        @filters-sort-data="$emit('retrieveNegotiationsBySortAndFilter')"
      />
      <NegotiationPagination
        :negotiations="negotiations"
        :pagination="pagination"
        @current-page-number="$emit('retrieveNegotiationsByPage', $event)"
      />
    </template>
  </div>
</template>

<script setup>
import FilterSort from './FilterSort.vue'
import NegotiationList from './NegotiationList.vue'
import NegotiationPagination from './NegotiationPagination.vue'

defineProps({
  negotiations: {
    type: Array,
    required: true,
  },
  pagination: {
    type: Object,
    required: true,
  },
  states: {
    type: Array,
    required: true,
  },
  organizations: {
    type: Array,
    required: true,
  },
  filtersSortData: {
    type: Object,
    required: true,
  },
  userRole: {
    type: String,
    required: true,
  },
  isLoaded: {
    type: Boolean,
    required: true,
  },
  loading: {
    type: Boolean,
    default: false,
  },
})

defineEmits(['retrieveNegotiationsBySortAndFilter', 'retrieveNegotiationsByPage'])
</script>

<style scoped>
.mt-3 {
  margin-top: 1rem;
}

.spinner-border {
  width: 3rem;
  height: 3rem;
  border: 0.25em solid currentcolor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spinner-border 0.75s linear infinite;
}

@keyframes spinner-border {
  to {
    transform: rotate(360deg);
  }
}

.visually-hidden {
  position: absolute !important;
  width: 1px !important;
  height: 1px !important;
  padding: 0 !important;
  margin: -1px !important;
  overflow: hidden !important;
  clip: rect(0, 0, 0, 0) !important;
  white-space: nowrap !important;
  border: 0 !important;
}

.p-4 {
  padding: 1.5rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

.text-center {
  text-align: center;
}
</style>

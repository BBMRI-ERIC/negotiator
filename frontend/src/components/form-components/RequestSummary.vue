<template>
  <div class="request-summary form d-flex flex-column">
    <h2 class="fw-bold mb-3">SEARCH PARAMETERS</h2>
    <div v-if="queryParameters">
      <div v-for="parameter in queryParameters" :key="parameter">
        <p class="my-0">{{ parameter }}</p>
      </div>
    </div>

    <div class="my-5">
      <ResourcesList :resources="requestSummary?.resources" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import ResourcesList from '../../components/ResourcesList.vue'

const props = defineProps({
  requestSummary: {
    type: Object,
    required: true,
    default: () => {},
  },
})

const queryParameters = computed(() => {
  return props.requestSummary?.humanReadable.split('\r\n')
})
</script>

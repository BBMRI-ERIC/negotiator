<template>
  <div class="request-summary form d-flex flex-column mb-4">
    <!-- <h2 class="fw-bold mb-3">SEARCH PARAMETERS</h2>
    <div v-if="queryParameters" class="query-parameters">
      <div v-for="parameter in queryParameters" :key="parameter">
        <p class="my-0">{{ parameter }}</p>
      </div>
    </div> -->

    <div class="resources-wrapper">
      <ResourcesList :resources="props.requestSummary?.resources" />
    </div>
  </div>
</template>

<script setup>
import ResourcesList from '../../components/ResourcesList.vue'

const props = defineProps({
  requestSummary: {
    type: Object,
    required: true,
    default: () => {},
  },
})
</script>

<style scoped>
.resources-wrapper {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 0.5rem;
}

.resources-wrapper::-webkit-scrollbar {
  width: 8px;
}

.resources-wrapper::-webkit-scrollbar-track {
  background: #f1f3f5;
  border-radius: 4px;
}

.resources-wrapper::-webkit-scrollbar-thumb {
  background: #cbd5e0;
  border-radius: 4px;
}

.resources-wrapper::-webkit-scrollbar-thumb:hover {
  background: #a0aec0;
}

@media (max-width: 768px) {
  .resources-wrapper {
    max-height: 50vh;
  }
}
</style>

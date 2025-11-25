<template>
  <div>
    <div class="card mb-2" :class="isHomePage ? 'card-home-page border-0 rounded-top-4' : ''">
      <div
        class="card-header bg-body mt-1 pb-1"
        :class="isHomePage ? 'rounded-top-4' : ''"
        :style="{ color: uiConfiguration?.cardTextColor }"
      >
        <span class="h5">{{ title }}</span>
        <h6 class="float-end">
          <span :class="getBadgeColor(status)" class="badge rounded-3" style="width: 125px">
            <i :class="getBadgeIcon(status)" class="px-1" />
            {{ transformStatus(status) }}
          </span>
        </h6>
      </div>
      <div class="card-body" :style="{ color: uiConfiguration?.cardTextColor, opacity: 0.7 }">
        <h6 class="card-subtitle mb-2">Negotiation ID: {{ id }}</h6>
        <h6 class="card-subtitle mb-2">Created on: {{ creationDate }}</h6>
        <h6 class="card-subtitle mb-2">Author: {{ submitter }}</h6>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { transformStatus, getBadgeColor, getBadgeIcon } from '../composables/utils.js'
import { useUiConfiguration } from '../store/uiConfiguration.js'

defineProps({
  isHomePage: {
    type: Boolean,
    required: false,
    default: false,
  },
  id: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '',
  },
  status: {
    type: String,
    default: '',
  },
  submitter: {
    type: String,
    default: '',
  },
  creationDate: {
    type: Date,
    default: undefined,
  },
})

const uiConfigurationStore = useUiConfiguration()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.negotiationList
})
</script>

<style scoped>
.card-home-page {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 1rem;
}
</style>

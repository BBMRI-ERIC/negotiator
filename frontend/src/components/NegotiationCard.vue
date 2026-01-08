<template>
  <div>
    <div class="card mb-2">
      <div class="card-header bg-body" :style="{ color: uiConfiguration?.cardTextColor }">
        <span class="h5">{{ title }}</span>
        <h6 class="float-end">
          <UiBadge :class="getBadgeColor(status)" :icon="getBadgeIcon(status)" width="125px">
            {{ transformStatus(status) }}
          </UiBadge>
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
import UiBadge from '@/components/ui/UiBadge.vue'

defineProps({
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

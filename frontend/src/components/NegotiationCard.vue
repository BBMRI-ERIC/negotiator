<template>
  <div>
    <div class="card border-0 rounded-top-4 mb-2">
      <div
        class="card-header bg-body mt-1 pb-1 rounded-top-4"
        :style="{ color: 'var(--bs-card-title-text)' }"
      >
        <span class="h5">{{ title }}</span>
        <h6 class="float-end">
          <UiBadge :class="getBadgeColor(status)" :icon="getBadgeIcon(status)" width="125px">
            {{ transformStatus(status) }}
          </UiBadge>
        </h6>
      </div>
      <div class="card-body" :style="{ color: 'var(--bs-card-title-text)', opacity: 0.7 }">
        <h6 class="card-subtitle mb-2">Negotiation ID: {{ id }}</h6>
        <h6 class="card-subtitle mb-2">Created on: {{ creationDate }}</h6>
        <h6 class="card-subtitle mb-2">Author: {{ submitter }}</h6>
      </div>
    </div>
  </div>
</template>

<script setup>
import { transformStatus, getBadgeColor, getBadgeIcon } from '../composables/utils.js'
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
</script>

<style scoped>
.card {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  border-radius: 1rem;
}
</style>

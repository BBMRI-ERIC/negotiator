<template>
  <div>
    <div v-if="selectedChannelName" class="mt-2">
      <UiBadge
        :color="channelId === 'public' ? 'bg-warning rounded-pill' : 'bg-primary rounded-pill'"
        :icon="channelId === 'public' ? 'bi bi-globe' : 'bi bi-lock-fill'"
      >
        Channel: {{ selectedChannelName }}
      </UiBadge>
      <div class="text-muted small mt-1">
        {{ channelVisibilityMessage }}
      </div>
    </div>
    <select
      id="recipient"
      v-model="channelId"
      class="form-select w-50 mb-3 mt-2"
      :style="{ color: uiConfiguration.primaryTextColor }"
    >
      <option disabled selected value="">-- Select channel --</option>
      <option value="public">Public channel</option>
      <optgroup label="Private channels">
        <option
          v-for="recipient in recipients"
          :key="recipient.id"
          :value="recipient.id"
          :disabled="!negotiation.privatePostsEnabled"
        >
          Author - {{ recipient.name }}
        </option>
      </optgroup>
    </select>
  </div>
</template>

<script setup>
import UiBadge from '@/components/ui/UiBadge.vue'
defineProps({
  selectedChannelName: String,
  channelVisibilityMessage: String,
  negotiation: {
    type: Object,
    required: true,
    default: () => ({ privatePostsEnabled: false }),
  },
  recipients: {
    type: Array,
    default: () => [],
  },
  uiConfiguration: {
    type: Object,
    required: true,
    default: () => ({ primaryTextColor: '#000' }),
  },
})

const channelId = defineModel('channelId', { type: String, default: '' })
</script>

<style scoped>
.form-select {
  font-size: 1rem; /* Matches typical Bootstrap form-control size */
  padding: 0.375rem 2.25rem 0.375rem 0.75rem; /* Matches Bootstrap form-select padding */
}
</style>

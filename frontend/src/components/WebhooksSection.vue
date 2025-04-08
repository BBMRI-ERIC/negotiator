<template>
  <div class="specific-area panel panel-default border-">
    <div class="d-flex justify-content-between align-items-center mb-1">
      <h2 class="text-left">Webhooks</h2>
      <button class="btn btn-success" @click="addWebhook">Add Webhook</button>
    </div>
    <div class="text-muted mb-3">
      Webhooks allow external services to be notified when certain events happen. When the specified events occur, we
      send a POST request to each URL provided. Learn more in our
      <a href="https://bbmri-eric.github.io/negotiator/administrator#webhooks" target="_blank"
         rel="noopener noreferrer">Webhooks Documentation</a>.
    </div>

    <div class="container mt-4">
      <WebhookCard
        v-for="wh in webhooks"
        :key="wh.id"
        :webhook="wh"
        @edit="$emit('edit-webhook', $event)"
        @delete="$emit('delete-webhook', $event)"
        @test="$emit('test-webhook', $event)"
      />
    </div>
  </div>
</template>

<script setup>
import WebhookCard from './WebhookCard.vue'

const emit = defineEmits(['add-webhook', 'edit-webhook', 'delete-webhook'])

defineProps({
  webhooks: {
    type: Array,
    required: true
  }
})

const addWebhook = () => {
  emit('add-webhook')
}
</script>

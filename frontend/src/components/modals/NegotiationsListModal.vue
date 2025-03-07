<template>
  <NegotiatorModal :id="id" :title="title">
    <template #body>
      <ul>
        <li class="clickable-item" v-for="(negotiationId, index) in negotiations"
            :key="index" :value="negotiationId" @click="goToNegotiation(negotiationId)" data-bs-dismiss="modal">
          {{ negotiationId }}
        </li>
      </ul>
    </template>
    <template #footer>
      <button type="button" class="btn btn-dark" data-bs-dismiss="modal" @click="message = ''">
        Close
      </button>
    </template>
  </NegotiatorModal>
</template>
<script setup>

import NegotiatorModal from '@/components/modals/NegotiatorModal.vue'
import { useRouter } from 'vue-router'

defineProps({
  id: {
    type: String,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  negotiations: {
    type: Array,
    required: true
  }
})
const router = useRouter()

function goToNegotiation(id) {
  router.push({
    name: 'negotiation-page',
    params: { negotiationId: id }
  })
}
</script>

<style scoped>
.clickable-item {
  color: #007bff; /* Bootstrap default link color */
  font-weight: bold;
  cursor: pointer; /* Indicate it's clickable */
  padding: 5px;
  transition: color 0.2s ease-in-out;
}

.clickable-item:hover {
  text-decoration: underline; /* Underline on hover */
  color: #0056b3; /* Darker blue for better hover effect */
}

</style>
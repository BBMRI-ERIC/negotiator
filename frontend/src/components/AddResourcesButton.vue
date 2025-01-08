<template>
  <button
    type="button"
    class="btn"
    @click="openModal()"
    :style="{ 'color': uiConfiguration.primaryTextColor}"
  >
    <i class="bi bi-pencil-square" />
    Edit
  </button>
  <add-resources-modal
    id="resourcesModal"
    :shown="shown"
    :negotiation-id="props.negotiationId"
    @confirm="closeModal()"
  />
</template>
<script setup>
import { computed } from 'vue'
import AddResourcesModal from "@/components/modals/AddResourcesModal.vue"
import { Modal } from 'bootstrap'
import { ref } from 'vue'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()
const shown = ref(false)
const resourceModal = ref(undefined)
const props = defineProps({
  negotiationId: {
    type: String,
    required: true
  }
})
const emit = defineEmits(["new-resources"])

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

function openModal() {
  resourceModal.value = new Modal(document.querySelector("#resourcesModal"))
  shown.value = true
  resourceModal.value.show()
}

function closeModal() {
  emit("new-resources")
  resourceModal.value.hide()
}
</script>

<style scoped>
.btn:hover {
  color: #e95713;
}
</style>

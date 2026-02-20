<template>
  <div class="request-summary form d-flex flex-column mb-4">
    <div class="scroll-container">
      <div class="resources-wrapper" ref="wrapperRef" @scroll="onScroll">
        <ResourcesList
          :resources="props.requestSummary?.resources"
          @remove-resource="handleRemoveResource"
        />
      </div>
      <div v-if="isScrollable && !isScrolledToBottom" class="scroll-indicator"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import ResourcesList from '../../components/ResourcesList.vue'
import { useNegotiationPageStore } from '@/store/negotiationPage.js'
import { useNotificationsStore } from '@/store/notifications.js'

const props = defineProps({
  requestSummary: {
    type: Object,
    required: true,
    default: () => {},
  },
  negotiationId: {
    type: String,
    required: false,
    default: null,
  },
})

const emit = defineEmits(['resource-removed'])

const negotiationPageStore = useNegotiationPageStore()
const notificationsStore = useNotificationsStore()

async function handleRemoveResource(resource) {
  if (!props.negotiationId) {
    notificationsStore.setNotification('Cannot remove resource: negotiation ID not provided', 'danger')
    return
  }

  const success = await negotiationPageStore.removeResource(props.negotiationId, resource.id)
  if (success) {
    emit('resource-removed', resource.id)
  }
}

const wrapperRef = ref(null)
const isScrollable = ref(false)
const isScrolledToBottom = ref(false)

function checkScrollable() {
  const el = wrapperRef.value
  if (!el) return
  isScrollable.value = el.scrollHeight > el.clientHeight
  isScrolledToBottom.value = el.scrollHeight - el.scrollTop <= el.clientHeight + 4
}

function onScroll() {
  const el = wrapperRef.value
  if (!el) return
  isScrolledToBottom.value = el.scrollHeight - el.scrollTop <= el.clientHeight + 4
}

onMounted(() => nextTick(checkScrollable))
watch(() => props.requestSummary?.resources, () => nextTick(checkScrollable), { deep: true })
</script>

<style scoped>
.scroll-container {
  position: relative;
}

.scroll-indicator {
  display: none;
}

.resources-wrapper {
  max-height: 60vh;
  overflow-y: scroll;
  padding-right: 0.5rem;
}

.resources-wrapper::-webkit-scrollbar {
  width: 6px;
}

.resources-wrapper::-webkit-scrollbar-track {
  background: #e9ecef;
  border-radius: 4px;
}

.resources-wrapper::-webkit-scrollbar-thumb {
  background: #adb5bd;
  border-radius: 4px;
}

.resources-wrapper::-webkit-scrollbar-thumb:hover {
  background: #6c757d;
}

@media (max-width: 768px) {
  .resources-wrapper {
    max-height: 50vh;
  }
}
</style>

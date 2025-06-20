<template>
  <div v-if="negotiation">
    <Timeline
      :combined-items="combinedItems"
      :ui-configuration="uiConfiguration"
      :organizations="organizations"
    />
    <hr v-if="combinedItems.length === 0" class="my-3" />
    <MessageForm
      :negotiation="negotiation"
      :recipients="recipients"
      :ui-configuration="uiConfiguration"
      :file-extensions="fileExtensions"
      @new-attachment="handleNewAttachment"
      @send-message="handleSendMessage"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeMount, ref } from 'vue'
import Timeline from './NegotiationTimeline.vue'
import MessageForm from './MessageForm.vue'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import fileExtensions from '@/config/uploadFileExtensions.js'

const negotiationPageStore = useNegotiationPageStore()
const uiConfigurationStore = useUiConfiguration()

const props = defineProps({
  negotiation: Object,
  userRole: String,
  timelineEvents: Array,
  recipients: Array,
  organizations: Object,
})

const emit = defineEmits(['new_attachment'])

const posts = ref([])
const uiConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.theme)

const combinedItems = computed(() => {
  const events = props.timelineEvents.map((event) => ({
    ...event,
    type: 'event',
    timestamp: new Date(event.timestamp).getTime(),
    id: `event-${event.id || event.timestamp}`,
  }))
  const postsMapped = posts.value.map((post) => ({
    ...post,
    type: 'post',
    timestamp: new Date(post.creationDate).getTime(),
    id: `post-${post.id}`,
  }))
  return [...events, ...postsMapped].sort((a, b) => a.timestamp - b.timestamp)
})

onBeforeMount(() => {
  retrievePostsByNegotiationId()
})

async function retrievePostsByNegotiationId() {
  await negotiationPageStore.retrievePostsByNegotiationId(props.negotiation.id).then((res) => {
    posts.value = res?._embedded?.posts ?? []
  })
}

async function handleSendMessage({ message, channelId, attachment }) {
  if (message) {
    const data = {
      organizationId: channelId !== 'public' ? channelId : null,
      text: message,
      negotiationId: props.negotiation.id,
      type: channelId === 'public' ? 'PUBLIC' : 'PRIVATE',
    }
    await negotiationPageStore.addMessageToNegotiation(data).then((post) => {
      if (post) {
        retrievePostsByNegotiationId() // Reload posts after successful message post
      }
    })
  }
  if (attachment) {
    const attachmentData = {
      organizationId: channelId !== 'public' ? channelId : null,
      negotiationId: props.negotiation.id,
      attachment,
    }
    await negotiationPageStore.addAttachmentToNegotiation(attachmentData).then(() => {
      retrievePostsByNegotiationId() // Reload posts after successful attachment post
    })
  }
}

async function handleNewAttachment() {
  await retrievePostsByNegotiationId() // Reload posts when new-attachment event is emitted
  emit('new_attachment')
}

defineExpose({ retrievePostsByNegotiationId })
</script>

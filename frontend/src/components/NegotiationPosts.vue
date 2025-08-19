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
      :is-uploading="isUploading"
      :upload-error="uploadError"
      @new-attachment="handleNewAttachment"
      @send-message="handleSendMessage"
      @clear-upload-error="uploadError = ''"
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
const isUploading = ref(false)
const uploadError = ref('')
const uiConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.theme)

const combinedItems = computed(() => {
  const events = props.timelineEvents.map((event) => ({
    ...event,
    type: 'event',
    createdAt: new Date(event.timestamp).getTime(),
    id: `event-${event.id || event.timestamp}`,
  }))
  const postsMapped = posts.value.map((post) => ({
    ...post,
    type: 'post',
    createdAt: new Date(post.creationDate).getTime(),
    id: `post-${post.id}`,
  }))
  return [...events, ...postsMapped].sort((a, b) => a.createdAt - b.createdAt)
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
  try {
    uploadError.value = ''

    if (attachment) {
      isUploading.value = true
    }

    if (message) {
      const data = {
        organizationId: channelId !== 'public' ? channelId : null,
        text: message,
        negotiationId: props.negotiation.id,
        type: channelId === 'public' ? 'PUBLIC' : 'PRIVATE',
      }
      await negotiationPageStore.addMessageToNegotiation(data).then((post) => {
        if (post) {
          retrievePostsByNegotiationId()
        }
      })
    }

    if (attachment) {
      const attachmentData = {
        organizationId: channelId !== 'public' ? channelId : null,
        negotiationId: props.negotiation.id,
        attachment,
      }
      const response = await negotiationPageStore.addAttachmentToNegotiation(attachmentData)

      if (response && response.status >= 400) {
        uploadError.value = response.data?.detail ||
                           response.data?.message ||
                           `Upload failed with status ${response.status}`
      } else {
        retrievePostsByNegotiationId()
        emit('new_attachment')
      }
    }
  } catch (error) {
    console.error('Error sending message or attachment:', error)

    if (attachment) {
      uploadError.value = error.response?.data?.detail ||
                         error.response?.data?.message ||
                         'Failed to upload attachment. Please try again.'
    }
  } finally {
    isUploading.value = false
  }
}

async function handleNewAttachment() {
  await retrievePostsByNegotiationId() // Reload posts when new-attachment event is emitted
  emit('new_attachment')
}

defineExpose({ retrievePostsByNegotiationId })
</script>

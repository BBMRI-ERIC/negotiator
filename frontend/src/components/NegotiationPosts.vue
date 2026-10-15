<template>
  <div v-if="negotiation">
    <Timeline
      :combined-items="combinedItems"
      :ui-configuration="uiConfiguration"
      :organizations="organizations"
      :negotiation="negotiation"
      @reply="startReply"
    />
    <hr v-if="combinedItems.length === 0" class="my-3" />
    <MessageForm
      :negotiation="negotiation"
      :recipients="recipients"
      :ui-configuration="uiConfiguration"
      :file-extensions="fileExtensions"
      :is-uploading="isUploading"
      :upload-error="uploadError"
      :reply-target="replyTarget"
      @new-attachment="handleNewAttachment"
      @send-message="handleSendMessage"
      @clear-upload-error="uploadError = ''"
      @cancel-reply="replyTarget = null"
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
import { POST_TYPE } from '@/config/consts.js'

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
const replyTarget = ref(null)
const uiConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.theme)

const combinedItems = computed(() => {
  const events = props.timelineEvents.map((event) => ({
    ...event,
    type: 'event',
    createdAt: new Date(event.timestamp).getTime(),
    id: `event-${event.id || event.timestamp}`,
  }))
  const postsById = new Map(posts.value.map((post) => [post.id, post]))
  const postsMapped = posts.value.map((post) => ({
    ...post,
    type: 'post',
    createdAt: new Date(post.creationDate).getTime(),
    id: `post-${post.id}`,
    postId: post.id,
    replyChannel: replyChannelFor(post),
    inReplyTo: inReplyToFor(post, postsById),
  }))
  return [...events, ...postsMapped].sort((a, b) => a.createdAt - b.createdAt)
})

// The backend's channel rule means anyone who can see a reply can also see its original,
// so the original is always loaded here; "missing" is only a fallback.
function inReplyToFor(post, postsById) {
  if (!post.replyToId) return null
  const original = postsById.get(post.replyToId)
  if (!original) return { missing: true }
  return {
    postId: original.id,
    authorName: original.createdBy?.name || 'Unknown',
    excerpt: original.text,
  }
}

// Takes the post itself, not the timeline item, whose type combinedItems overwrites.
// A reply to a private post must stay in that post's channel, so that channel is locked.
function replyChannelFor(post) {
  const { publicPostsEnabled, privatePostsEnabled } = props.negotiation
  if (post.type === POST_TYPE.PRIVATE) {
    const isRecipient = props.recipients.some((r) => r.id === post.organizationId)
    return privatePostsEnabled && isRecipient
      ? { channelId: post.organizationId, locked: true }
      : null
  }
  if (!publicPostsEnabled && !privatePostsEnabled) return null
  return { channelId: publicPostsEnabled ? 'public' : '', locked: false }
}

function startReply(item) {
  replyTarget.value = {
    postId: item.postId,
    authorName: item.createdBy?.name || 'Unknown',
    excerpt: item.text,
    ...item.replyChannel,
  }
}

onBeforeMount(() => {
  retrievePostsByNegotiationId()
})

async function retrievePostsByNegotiationId() {
  await negotiationPageStore.retrievePostsByNegotiationId(props.negotiation.id).then((res) => {
    posts.value = res?._embedded?.posts ?? []
  })
}

async function handleSendMessage({ message, channelId, attachment }) {
  const target = replyTarget.value
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
        replyToId: target?.postId,
      }
      await negotiationPageStore.addMessageToNegotiation(data).then((post) => {
        if (post) {
          // Don't drop a reply the user started on another message while this one was sending
          if (replyTarget.value === target) {
            replyTarget.value = null
          }
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
        uploadError.value =
          response.data?.detail ||
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
      uploadError.value =
        error.response?.data?.detail ||
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

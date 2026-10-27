<template>
  <div>
    <h5 :style="{ color: uiConfiguration.primaryTextColor }">Send a message</h5>
    <form class="border rounded mb-4 p-2" @submit.prevent="sendMessage">
      <ChannelSelector
        v-model:channel-id="channelId"
        :selected-channel-name="selectedChannelName"
        :channel-visibility-message="channelVisibilityMessage"
        :negotiation="negotiation"
        :recipients="recipients"
        :ui-configuration="uiConfiguration"
      />
      <textarea
        v-model="message"
        data-cy="message-input"
        class="form-control"
        placeholder="Write your message..."
        :style="{ color: uiConfiguration.secondaryTextColor }"
        :disabled="props.isUploading"
        @keydown.ctrl.enter.prevent="sendMessage"
        @keydown.meta.enter.prevent="sendMessage"
      />
      <NegotiationAttachment
        v-if="attachment"
        class="ms-auto mt-2"
        :name="attachment.name"
        :content-type="attachment.type"
        :size="attachment.size"
        @removed="resetAttachment"
      />
      <div v-if="attachmentError" class="alert alert-danger mt-3" role="alert">
        {{ attachmentError }}
      </div>
      <div class="d-flex flex-row-reverse mt-3 mb-2">
        <span ref="sendTooltipEl">
          <button
            type="submit"
            id="send"
            :disabled="!readyToSend"
            class="btn ms-2"
            :style="{
              'background-color': uiConfiguration.buttonColor,
              'border-color': uiConfiguration.buttonColor,
              color: '#FFFFFF',
            }"
          >
            Send message
          </button>
        </span>
        <div class="d-flex align-items-center">
          <div v-if="props.uploadError" class="text-danger me-2">
            {{ props.uploadError }}
          </div>
          <button type="button" class="btn btn-attachment ms-2 border rounded">
            <input
              :key="fileInputKey"
              id="attachment"
              class="form-control"
              type="file"
              :accept="fileExtensions"
              @change="showAttachment"
            />
            <i class="bi bi-paperclip" />
          </button>
          <small v-if="props.isUploading" class="text-muted ms-2 d-flex align-items-center">
            <span
              class="spinner-border spinner-border-sm me-1"
              role="status"
              aria-hidden="true"
            ></span>
            {{ attachment ? `Uploading ${attachment.name}...` : 'Uploading...' }}
          </small>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Tooltip } from 'bootstrap'
import NegotiationAttachment from './NegotiationAttachment.vue'
import ChannelSelector from './ChannelSelector.vue'
import { isFileExtensionsSupported } from '../composables/utils.js'

const props = defineProps({
  negotiation: {
    type: Object,
    required: true,
    default: () => ({ publicPostsEnabled: false, privatePostsEnabled: false, id: '' }),
  },
  recipients: {
    type: Array,
    default: () => [],
  },
  uiConfiguration: {
    type: Object,
    required: true,
    default: () => ({
      primaryTextColor: '#000',
      secondaryTextColor: '#666',
      buttonColor: '#007bff',
    }),
  },
  fileExtensions: {
    type: Array,
    default: () => [],
  },
  isUploading: {
    type: Boolean,
    default: false,
  },
  uploadError: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['new-attachment', 'send-message', 'clear-upload-error'])

const message = ref('')
const channelId = ref('')
const attachment = ref(undefined)
const fileInputKey = ref(0)
const attachmentError = ref('')

const sendShortcut = /Mac|iPhone|iPad/.test(navigator.userAgent) ? '⌘+Enter' : 'Ctrl+Enter'

const sendTooltipEl = ref(null)
let sendTooltip

const selectedChannelEnabled = computed(() => {
  if (channelId.value === '') return false
  return channelId.value === 'public'
    ? props.negotiation.publicPostsEnabled
    : props.negotiation.privatePostsEnabled
})

const sendTooltipText = computed(() => {
  if (!props.negotiation.publicPostsEnabled && !props.negotiation.privatePostsEnabled) {
    return 'Messaging is currently disabled for this request'
  }
  if (channelId.value === '') return 'Please select channel'
  if (!selectedChannelEnabled.value) {
    return channelId.value === 'public'
      ? 'The public channel is currently disabled for this request'
      : 'Private channels are currently disabled for this request'
  }
  if (message.value === '' && attachment.value === undefined) return 'Message cannot be empty'
  return `Send (${sendShortcut})`
})

onMounted(() => {
  // title as a function so Bootstrap re-reads it on every show instead of caching it
  sendTooltip = new Tooltip(sendTooltipEl.value, { title: () => sendTooltipText.value })
})

onBeforeUnmount(() => {
  sendTooltip?.dispose()
})

const readyToSend = computed(() => {
  return (
    (message.value !== '' || attachment.value !== undefined) &&
    selectedChannelEnabled.value &&
    !props.isUploading
  )
})

const selectedChannelName = computed(() => {
  if (!channelId.value) return ''
  if (channelId.value === 'public') return 'Request-wide'
  const recipient = props.recipients.find((r) => r.id === channelId.value)
  return recipient ? `1:1 · Author & ${recipient.name}` : '1:1 · Author & Unknown'
})

const channelVisibilityMessage = computed(() => {
  if (!channelId.value) return ''
  if (channelId.value === 'public') {
    return 'Visible to everyone involved in this request: the author, representatives and administrators.'
  }
  const recipient = props.recipients.find((r) => r.id === channelId.value)
  return recipient
    ? `Visible only to the request author, representatives of ${recipient.name} and administrators.`
    : 'Visible only to the request author, representatives of the selected organization and administrators.'
})

function resetForm() {
  message.value = ''
  channelId.value = ''
  attachment.value = undefined
  fileInputKey.value++
  attachmentError.value = ''
}

function resetAttachment() {
  attachment.value = undefined
  fileInputKey.value++
  attachmentError.value = ''
}

function showAttachment(event) {
  // Clear any previous upload errors when selecting a new file
  emit('clear-upload-error')

  const file = event.target.files[0]
  if (isFileExtensionsSupported(file)) {
    attachment.value = file
    attachmentError.value = ''
  } else {
    fileInputKey.value++
    attachmentError.value = 'Unsupported file type. Please select a file like PDF, JPEG, or CSV.'
  }
}

async function sendMessage() {
  if (!readyToSend.value) return
  attachmentError.value = ''

  // Always emit the send-message event - let parent handle attachment upload
  emit('send-message', {
    message: message.value,
    channelId: channelId.value,
    attachment: attachment.value,
  })

  // Only emit new-attachment if there's an attachment (for UI refresh)
  if (attachment.value) {
    emit('new-attachment')
  }

  resetForm()
}
</script>

<style scoped>
.btn-attachment {
  position: relative;
  overflow: hidden;
}
.btn-attachment input[type='file'] {
  position: absolute;
  top: 0;
  right: 0;
  min-width: 100%;
  min-height: 100%;
  font-size: 100px;
  text-align: right;
  filter: alpha(opacity=0);
  opacity: 0;
  outline: none;
  cursor: inherit;
  display: block;
}
</style>

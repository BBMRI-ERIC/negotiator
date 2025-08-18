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
        class="form-control mb-3"
        :style="{ color: uiConfiguration.secondaryTextColor }"
        :disabled="props.isUploading"
      />
      <NegotiationAttachment
        v-if="attachment"
        class="ms-auto"
        :name="attachment.name"
        :content-type="attachment.type"
        :size="attachment.size"
        @removed="resetAttachment"
      />
      <div v-if="attachmentError" class="alert alert-danger mt-3" role="alert">
        {{ attachmentError }}
      </div>
      <div class="d-flex flex-row-reverse mt-3 mb-2">
        <span
          data-bs-toggle="tooltip"
          :title="
            negotiation.publicPostsEnabled
              ? ''
              : negotiation.status === 'DRAFT'
                ? 'Messaging is unavailable until you submit the request'
                : 'Messaging is unavailable until the request has been reviewed.'
          "
        >
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
            <div class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></div>
            {{ attachment ? `Uploading ${attachment.name}...` : 'Uploading...' }}
          </small>
        </div>
      </div>
    </form>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
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
})

const emit = defineEmits(['new-attachment', 'send-message'])

const message = ref('')
const channelId = ref('')
const attachment = ref(undefined)
const fileInputKey = ref(0)
const attachmentError = ref('')

const readyToSend = computed(() => {
  return (
    (message.value !== '' || attachment.value !== undefined) &&
    channelId.value !== '' &&
    (props.negotiation.publicPostsEnabled || props.negotiation.privatePostsEnabled) &&
    !props.isUploading
  )
})

const selectedChannelName = computed(() => {
  if (!channelId.value) return ''
  if (channelId.value === 'public') return 'Public channel'
  const recipient = props.recipients.find((r) => r.id === channelId.value)
  return recipient ? `Author - ${recipient.name}` : 'Author - Unknown'
})

const channelVisibilityMessage = computed(() => {
  if (!channelId.value) return ''
  if (channelId.value === 'public') {
    return 'Visible to all authorized negotiation participants (author, representatives, administrator).'
  }
  const recipient = props.recipients.find((r) => r.id === channelId.value)
  return recipient
    ? `Private channel between the negotiation author and ${recipient.name}.`
    : 'Private channel between the negotiation author and the selected organization.'
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

<template>
  <div v-if="negotiation">
    <h5 :style="{ color: uiConfiguration.primaryTextColor }">Comments</h5>
    <div v-for="post in posts" :key="post.id" class="card mb-3">
      <div class="card-header">
        <div class="mb-2">
          <span>
            <span
              v-for="badge in getUserBadges(post)"
              :key="badge"
              data-bs-toggle="tooltip"
              class="badge rounded-pill"
              :style="{ 'background-color': uiConfiguration.primaryTextColor }"
              :title="getBadgeTooltip(badge)"
            >
              {{ badge }}
            </span>
          </span>
        </div>
        <div class="d-flex justify-content-between align-items-center">
          <!-- Left Side: Author Information -->
          <div
            class="d-flex align-items-center"
            :style="{ color: uiConfiguration.primaryTextColor }"
          >
            <i class="bi bi-person-circle" />
            <span class="ms-2">
              <strong>{{ getAuthorName(post) }}</strong>
            </span>
            <span class="ms-1" :style="{ color: uiConfiguration.primaryTextColor, opacity: 0.7 }">
              posted on {{ printDate(post.creationDate) }}
            </span>
          </div>

          <!-- Right Side: Recipient Badge -->
          <span>
            <span class="me-1" :style="{ color: uiConfiguration.primaryTextColor, opacity: 0.7 }">
              to
            </span>
            <span class="badge rounded-pill" :class="getRecipientPostColor(post)">
              <i :class="getRecipientIcon(post)" />
              {{ getRecipientName(post) }}
            </span>
          </span>
        </div>
      </div>
      <div
        class="card-body"
        :style="{ color: uiConfiguration.secondaryTextColor }"
        v-dompurify-html="formatText(post.text)"
      />
    </div>
    <hr v-if="posts.length === 0" class="my-3" />
    <h5 :style="{ color: uiConfiguration.primaryTextColor }">Send a message</h5>
    <div v-if="showMessageRecipientInfo" class="text-muted">
      <i class="bi bi-exclamation-diamond" />
      This message will be visible to the request Author, representatives and the Administrator
    </div>
    <form class="border rounded mb-4 p-2" @submit.prevent="sendMessage">
      <textarea
        v-model="message"
        class="form-control mb-3"
        :style="{ color: uiConfiguration.secondaryTextColor }"
      />
      <NegotiationAttachment
        v-if="attachment"
        class="ms-auto"
        :name="attachment.name"
        :content-type="attachment.type"
        :size="attachment.size"
        @removed="resetAttachment"
      />
      <div class="d-flex flex-row-reverse mt-3 mb-2">
        <span
          data-bs-toggle="tooltip"
          :title="
            negotiation.publicPostsEnabled
              ? ''
              : 'Messaging is unavailable until the request has been reviewed.'
          "
        >
          <button
            type="submit"
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
        <button type="submit" class="btn btn-attachment ms-2 border rounded">
          <input
            id="attachment"
            class="form-control"
            type="file"
            accept="application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            @change="showAttachment"
          />
          <i class="bi bi-paperclip" />
        </button>
        <select
          id="recipient"
          v-model="recipientId"
          class="form-select w-25"
          :style="{ color: uiConfiguration.primaryTextColor }"
        >
          <option disabled selected value="">-- Select recipient --</option>
          <option value="Everyone">Everyone</option>
          <optgroup :label="privatePostsGroupLabel">
            <option
              v-for="recipient in recipients"
              :key="recipient.id"
              :value="recipient.id"
              :disabled="!negotiation.privatePostsEnabled"
            >
              {{ recipient.name }}
            </option>
          </optgroup>
        </select>
      </div>
    </form>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeMount, computed, watch } from 'vue'
import { Tooltip } from 'bootstrap'
import { dateFormat, POST_TYPE } from '@/config/consts'
import moment from 'moment'
import NegotiationAttachment from './NegotiationAttachment.vue'
import { useOidcStore } from '../store/oidc'
import { useNegotiationPageStore } from '../store/negotiationPage.js'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()

const oidcStore = useOidcStore()
const negotiationPageStore = useNegotiationPageStore()

const props = defineProps({
  negotiation: {
    type: Object,
    default: undefined,
  },
  userRole: {
    type: String,
    default: undefined,
  },
  // Array of possible recipients for messages.
  recipients: {
    type: Array,
    // prettier-ignore
    default(rawProps) { // eslint-disable-line no-unused-vars
      return []
    },
  },
  organizations: {
    type: Object,
    // prettier-ignore
    default(rawProps) { // eslint-disable-line no-unused-vars
      return {}
    },
  },
})
const formatText = (text) => text.replace(/\n/g, '<br>')
const posts = ref([])
const message = ref('')
const recipientId = ref('')
const attachment = ref(undefined)

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})

const oidcUser = computed(() => {
  return oidcStore.oidcUser
})

const readyToSend = computed(() => {
  return (
    (message.value !== '' || attachment.value !== undefined) &&
    recipientId.value !== '' &&
    (props.negotiation.publicPostsEnabled || props.negotiation.privatePostsEnabled)
  )
})
computed(() => {
  return props.recipients.reduce(
    (obj, item) => Object.assign(obj, { [item.id]: { name: item.name } }),
    {},
  )
})
const privatePostsGroupLabel = computed(() => {
  if (props.negotiation.privatePostsEnabled) {
    return 'Private message'
  }
  return 'Private messages will be enabled after an administrator will approve the negotiation'
})
const showMessageRecipientInfo = ref(false)
watch(recipientId, (newValue, oldValue) => {
  showMessageRecipientInfo.value = newValue === 'Everyone'
  console.log('recipientId changed from', oldValue, 'to', newValue)
})
onMounted(() => {
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']",
  })
})

onBeforeMount(() => {
  retrievePostsByNegotiationId()
})

async function retrievePostsByNegotiationId() {
  await negotiationPageStore.retrievePostsByNegotiationId(props.negotiation.id).then((res) => {
    posts.value = res?._embedded?.posts ?? []
  })
}

async function addMessageToNegotiation() {
  const data = {
    organizationId: recipientId.value !== 'Everyone' ? recipientId.value : null,
    text: message.value,
    negotiationId: props.negotiation.id,
    type: recipientId.value === 'Everyone' ? POST_TYPE.PUBLIC : POST_TYPE.PRIVATE,
  }
  await negotiationPageStore.addMessageToNegotiation(data).then((post) => {
    if (post) {
      posts.value.push(post)
    }
  })
}

async function addAttachmentToNegotiation() {
  const data = {
    organizationId: recipientId.value !== 'Everyone' ? recipientId.value : null,
    negotiationId: props.negotiation.id,
    attachment: attachment.value,
  }
  await negotiationPageStore.addAttachmentToNegotiation(data).then(() => {
    if (attachment.value) {
      console.log(`Successfully uploaded file: ${attachment.value.name}`)
    }
  })
  emit('new_attachment')
}

function resetForm() {
  message.value = ''
  recipientId.value = ''
  resetAttachment()
}

function resetAttachment() {
  attachment.value = undefined
}

function printDate(date) {
  return moment(date).format(dateFormat)
}

function showAttachment(event) {
  const file = event.target.files[0]
  attachment.value = file
}

async function sendMessage() {
  if (!readyToSend.value) {
    return
  }
  if (attachment.value !== undefined) {
    // send attachment
    await addAttachmentToNegotiation()
  }
  if (message.value !== '') {
    // send a message and add the newly created post
    await addMessageToNegotiation()
  }
  resetForm()
}

function getAuthorName(post) {
  if (post.createdBy.authSubject === oidcUser.value.sub) {
    return 'You'
  } else {
    return `${post.createdBy.name}`
  }
}

function getRecipientPostColor(post) {
  return post.type === POST_TYPE.PUBLIC ? { 'bg-warning': true } : { 'bg-primary': true }
}

function getRecipientIcon(post) {
  return post.type === POST_TYPE.PUBLIC
    ? { 'bi bi-people-fill': true }
    : { 'bi bi-lock-fill': true }
}

function getRecipientName(post) {
  if (post.organizationId !== undefined) {
    return props.organizations[post.organizationId].name
  } else if (post.personRecipient !== undefined) {
    return post.personRecipient.authSubject === oidcUser.value.sub
      ? 'You'
      : post.personRecipient.name
  } else {
    return 'Everyone'
  }
}

function getUserBadges(post) {
  const badges = []
  if (post.createdBy.representativeOfAnyResource === true) {
    badges.push('Representative')
  }
  if (post.createdBy.id === props.negotiation.author.id) {
    badges.push('Author')
  }
  if (post.createdBy.admin === true) {
    badges.push('Admin')
  }
  if (post.createdBy.networkManager === true) {
    badges.push('NetworkManager')
  }
  return badges
}

function getBadgeTooltip(badge) {
  const badgeTooltips = {
    Admin: 'Negotiator Administrator',
    Author: 'Author of this Request',
    Representative: 'Representative of a resource',
    NetworkManager: 'Manager of a Network responsible for moderating requests',
  }
  return badgeTooltips[badge] || 'Badge details'
}

const emit = defineEmits(['new_attachment'])

defineExpose({
  retrievePostsByNegotiationId,
})
</script>

<style scoped>
.btn-attachment  {
    position: relative;
    overflow: hidden;
}
/** the input file is hidden */
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

.bi-exclamation-diamond {
  color: orange;
}
</style>

<template>
  <div class="timeline-item d-flex align-items-start mb-4 position-relative" style="z-index: 1">
    <div class="d-flex align-items-center w-100">
      <div
        class="timeline-icon me-3 position-relative"
        style="z-index: 2; width: 24px; text-align: center; flex-shrink: 0"
      >
        <i
          :class="item.type === 'event' ? 'bi bi-clock-history' : 'bi bi-chat-left-text'"
          class="fs-5 text-primary"
          style="z-index: 2; position: relative"
        />
        <div
          class="position-absolute"
          style="
            width: 24px;
            height: 24px;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            background-color: #fff;
            z-index: 1;
          "
        ></div>
      </div>
      <div class="timeline-content" style="flex: 1; min-width: 0">
        <div v-if="item.type === 'event'" :style="{ color: uiConfiguration.primaryTextColor }">
          {{ item.text }}
        </div>
        <div v-else :id="item.id" class="card" style="width: 100%">
          <div class="card-header">
            <div class="mb-2">
              <span>
                <UiBadge
                  v-for="badge in getUserBadges(item)"
                  :key="badge"
                  :class="'rounded-pill'"
                  :style="{ backgroundColor: uiConfiguration.primaryTextColor }"
                  :title="getBadgeTooltip(badge)"
                  data-bs-toggle="tooltip"
                >
                  {{ badge }}
                </UiBadge>
              </span>
            </div>
            <div class="d-flex justify-content-between align-items-center">
              <div
                class="d-flex align-items-center"
                :style="{ color: uiConfiguration.primaryTextColor }"
              >
                <i class="bi bi-person-circle" />
                <span class="ms-2">
                  <strong>{{ getAuthorName(item) }}</strong>
                </span>
                <span class="ms-1 ui-timestamp-text">
                  posted on <TimeStamp :value="item.creationDate" :muted="true" />
                </span>
              </div>

              <div class="d-flex align-items-center">
                <span class="badge rounded-pill" :class="getChannelPostColor(item)">
                  <i :class="getChannelIcon(item)" />
                  {{ getChannelName(item) }}
                </span>
                <button
                  v-if="item.replyChannel"
                  type="button"
                  class="btn btn-sm btn-link reply-button ms-2"
                  :style="{ color: uiConfiguration.primaryTextColor }"
                  @click="emit('reply', item)"
                >
                  <i class="bi bi-reply" /> Reply
                </button>
              </div>
            </div>
          </div>
          <div v-if="item.inReplyTo" class="px-3 pt-3">
            <div v-if="item.inReplyTo.missing" class="reply-reference small text-muted">
              ↪ Reply to an earlier message
            </div>
            <button
              v-else
              type="button"
              class="reply-reference small"
              :style="{ color: uiConfiguration.secondaryTextColor }"
              @click="showOriginal(item.inReplyTo.postId)"
            >
              ↪ Reply to <strong>{{ item.inReplyTo.authorName }}</strong>
              <span class="reply-excerpt">{{ item.inReplyTo.excerpt }}</span>
            </button>
          </div>
          <div
            class="card-body"
            :style="{ color: uiConfiguration.secondaryTextColor }"
            v-html="formatText(item.text)"
          />
        </div>
      </div>
    </div>
    <div
      v-if="item.type === 'event'"
      class="text-muted small text-end"
      style="min-width: 140px; flex-shrink: 0"
    >
      <TimeStamp :value="item.timestamp" />
    </div>
  </div>
</template>

<script setup>
import UiBadge from '@/components/ui/UiBadge.vue'
import TimeStamp from '@/components/ui/TimeStamp.vue'

const props = defineProps({
  item: {
    type: Object,
    required: true,
    default: () => ({ type: '', text: '', createdBy: { name: '' }, timestamp: '' }),
  },
  uiConfiguration: {
    type: Object,
    required: true,
    default: () => ({ primaryTextColor: '#000', secondaryTextColor: '#666' }),
  },
  organizations: {
    type: Object,
    default: () => ({}),
  },
  negotiation: {
    type: Object,
    required: true,
    default: () => ({ author: { id: '' } }),
  },
})

const emit = defineEmits(['reply'])

function getAuthorName(item) {
  return item.createdBy?.name || 'Unknown'
}

function getChannelPostColor(item) {
  return item.organizationId === undefined ? { 'bg-warning': true } : { 'bg-primary': true }
}

function getChannelIcon(item) {
  return item.organizationId === undefined ? 'bi bi-globe' : 'bi bi-lock-fill'
}

function getChannelName(item) {
  if (item.organizationId && props.organizations[item.organizationId]) {
    return `Author - ${props.organizations[item.organizationId].name}`
  } else if (item.personRecipient) {
    return item.personRecipient.name
  } else {
    return 'Public channel'
  }
}

function getUserBadges(item) {
  const badges = []
  if (item.createdBy?.representativeOfAnyResource === true) {
    badges.push('Representative')
  }
  if (item.createdBy?.id === props.negotiation.author?.id) {
    badges.push('Author')
  }
  if (item.createdBy?.admin === true) {
    badges.push('Admin')
  }
  if (item.createdBy?.networkManager === true) {
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

function showOriginal(postId) {
  const card = document.getElementById(`post-${postId}`)
  if (!card) return
  card.scrollIntoView({ behavior: 'smooth', block: 'center' })
  card.classList.add('reply-highlight')
  setTimeout(() => card.classList.remove('reply-highlight'), 2000)
}

function formatText(text) {
  if (!text) return ''
  // Escape HTML to prevent XSS, then replace newlines with <br>
  const escapedText = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
  return escapedText.replace(/\n/g, '<br>')
}
</script>

<style scoped>
.timeline-icon {
  transition: transform 0.2s ease;
}
.timeline-icon:hover {
  transform: scale(1.1);
}
.timeline-content .card {
  width: 100%;
  transition: box-shadow 0.3s ease;
}

.reply-highlight {
  box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.5);
}

.reply-reference {
  display: block;
  width: 100%;
  padding: 0.25rem 0.5rem;
  border: 0;
  border-left: 3px solid rgba(108, 117, 125, 0.5);
  background-color: rgba(108, 117, 125, 0.08);
  text-align: left;
}

button.reply-reference:hover {
  background-color: rgba(108, 117, 125, 0.16);
}

.reply-excerpt {
  display: block;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.ui-timestamp-text {
  color: #3c3c3d;
  opacity: 0.7;
}
</style>

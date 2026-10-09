<template>
  <div class="btn-group">
    <profileAvatar
      ref="dropdownToggle"
      type="button"
      class="mr-3 text-light"
      :style="{ 'background-color': uiConfiguration?.navbarButtonOutlineColor + '!important' }"
      data-bs-toggle="dropdown"
      data-bs-display="static"
      aria-expanded="false"
    >
      {{ returnAcronymOfName }}
    </profileAvatar>

    <ul class="dropdown-menu dropdown-menu-end mt-1">
      <li class="container mb-3 mt-2 px-3">
        <div class="d-flex flex-row justify-content-between">
          <div class="d-flex flex-row">
            <div class="user-info">
              <div class="user-info-text" :style="{ color: uiConfiguration?.navbarTextColor }">
                {{ user.email }}
              </div>
              <div
                class="user-info-text"
                :style="{ color: uiConfiguration?.navbarTextColor, opacity: 0.7 }"
              >
                {{ user.name }}
              </div>
            </div>
          </div>
          <button
            type="button"
            class="btn btn-sm p-0 border-0 ms-2 lh-1 align-self-start"
            aria-label="Close"
            title="Close"
            @click.stop="closeDropdown"
          >
            <i class="bi bi-x-lg" aria-hidden="true" />
          </button>
        </div>
      </li>
      <li>
        <hr class="dropdown-divider" />
      </li>
      <li>
        <a
          href="https://profile.aai.lifescience-ri.eu/profile"
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-gear" />
          Profile Settings
        </a>
      </li>
      <li v-if="isRepresentative">
        <a
          :href="externalLinks.auth_management_link"
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-person-gear" />
          Authorization Settings
        </a>
      </li>
      <li v-if="isAdmin">
        <router-link
          to="/settings"
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-sliders" />
          Admin Settings
        </router-link>
      </li>
      <li v-if="showLegalLinksSection">
        <hr class="dropdown-divider" />
      </li>
      <li v-if="showPrivacyPolicyLink">
        <a
          :href="privacyPolicyLink"
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-shield-lock" />
          {{ privacyPolicyText }}
        </a>
      </li>
      <li v-if="showAccessPolicyLink">
        <a
          :href="accessPolicyLink"
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-clipboard-check" />
          {{ accessPolicyText }}
        </a>
      </li>
      <li>
        <hr class="dropdown-divider" />
      </li>
      <li class="text-center sign-out">
        <button
          class="btn me-2"
          aria-current="page"
          @click.stop.prevent="signOutOidc"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-box-arrow-right" /> Sign Out
        </button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { Dropdown } from 'bootstrap'
import externalLinks from '@/config/externalLinks'
import { useOidcStore } from '@/store/oidc'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import profileAvatar from '@/components/ProfileAvatar.vue'

const oidcStore = useOidcStore()

const props = defineProps({
  user: {
    type: Object,
    default: () => ({}),
  },
  isAdmin: {
    type: Boolean,
    default: false,
  },
  isRepresentative: {
    type: Boolean,
    default: false,
  },
})

const uiConfigurationStore = useUiConfiguration()

const dropdownToggle = ref(null)

function closeDropdown() {
  const toggleEl = dropdownToggle.value?.$el
  if (toggleEl) {
    Dropdown.getOrCreateInstance(toggleEl).hide()
  }
}

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.navbar
})

const uiConfigurationFooter = computed(() => {
  return uiConfigurationStore.uiConfiguration?.footer
})

const returnAcronymOfName = computed(() => {
  const name = props.user?.name || ''
  const words = name.trim().split(/\s+/).filter(Boolean)
  let initials = ''
  if (words.length === 1) {
    // Single word: take only the first letter
    initials = words[0][0] ? words[0][0].toUpperCase() : ''
  } else if (words.length > 1) {
    // Multiple words: take first letter of first and last word
    initials = words[0][0].toUpperCase() + words[words.length - 1][0].toUpperCase()
  }
  return initials
})

const privacyPolicyLink = computed(() => {
  return (
    uiConfiguration.value?.navbarPrivacyPolicyLink ||
    uiConfigurationFooter.value?.footerPrivacyPolicyLink
  )
})

const privacyPolicyText = computed(() => {
  return (
    uiConfiguration.value?.navbarPrivacyPolicyText ||
    uiConfigurationFooter.value?.footerPrivacyPolicyText
  )
})

const showPrivacyPolicyLink = computed(() => {
  return Boolean(privacyPolicyLink.value && privacyPolicyText.value)
})

const accessPolicyLink = computed(() => {
  return uiConfiguration.value?.navbarAccessPolicyLink
})

const accessPolicyText = computed(() => {
  return uiConfiguration.value?.navbarAccessPolicyText
})

const showAccessPolicyLink = computed(() => {
  return Boolean(accessPolicyLink.value && accessPolicyText.value)
})

const showLegalLinksSection = computed(() => {
  return showPrivacyPolicyLink.value || showAccessPolicyLink.value
})

function signOutOidc() {
  oidcStore.signOutOidc()
}
</script>

<style scoped>
.sign-out:hover {
  color: #dc3545;
}

.user-info-text {
  overflow-wrap: break-word;
  word-break: break-word;
}

@media (min-width: 992px) {
  .dropdown-menu {
    min-width: 320px;
  }
}

@media (max-width: 991.98px) {
  .dropdown-menu {
    position: fixed !important;
    inset: auto 8px auto 8px !important;
    top: 56px !important;
    transform: none !important;
    left: 8px !important;
    right: 8px !important;
    width: auto !important;
    max-width: none !important;
    margin: 0 !important;
  }

  .container {
    padding-left: 1.5rem;
  }
}

.dropdown-menu {
  --bs-dropdown-link-active-bg: var(--bs-tertiary-bg); /* same as the hover color */
  --bs-dropdown-link-active-color: inherit;
}
</style>

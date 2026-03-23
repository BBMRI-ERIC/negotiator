<template>
  <div class="btn-group">
    <profileAvatar
      type="button"
      class="mr-3 text-light"
      :style="{ 'background-color': uiConfiguration?.navbarButtonOutlineColor + '!important' }"
      data-bs-toggle="dropdown"
      aria-expanded="false"
    >
      {{ returnAcronymOfName }}
    </profileAvatar>

    <ul class="dropdown-menu dropdown-menu-end mt-1">
      <li class="container mb-3 mt-2">
        <div class="d-flex flex-row">
          <profileAvatar
            type="button"
            class="me-3 mt-1 text-light"
            :style="{
              'background-color': uiConfiguration?.navbarButtonOutlineColor + '!important',
            }"
            data-bs-toggle="dropdown"
            aria-expanded="false"
          >
            {{ returnAcronymOfName }}
          </profileAvatar>
          <div>
            <div :style="{ color: uiConfiguration?.navbarTextColor }">
              {{ user.email }}
            </div>
            <div :style="{ color: uiConfiguration?.navbarTextColor, opacity: 0.7 }">
              {{ user.name }}
            </div>
          </div>
          <i class="bi bi-x ms-2 h4" />
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
      <li v-if="isAdmin">
        <router-link
          to="/ui-configuration"
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-house-gear" />
          Admin UI Configuration
        </router-link>
      </li>
      <li>
        <hr class="dropdown-divider" />
      </li>
      <li
        v-if="
          (uiConfiguration?.navbarPrivacyPolicyText && uiConfiguration?.navbarPrivacyPolicyLink) ||
          (uiConfigurationFooter?.footerPrivacyPolicyText &&
            uiConfigurationFooter?.footerPrivacyPolicyLink)
        "
      >
        <a
          :href="
            uiConfiguration?.navbarPrivacyPolicyLink ||
            uiConfigurationFooter?.footerPrivacyPolicyLink
          "
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-shield-lock" />
          {{
            uiConfiguration?.navbarPrivacyPolicyText ||
            uiConfigurationFooter?.footerPrivacyPolicyText
          }}
        </a>
      </li>
      <li v-if="uiConfiguration?.navbarAccessPolicyText && uiConfiguration?.navbarAccessPolicyLink">
        <a
          :href="uiConfiguration?.navbarAccessPolicyLink"
          class="dropdown-item"
          :style="{ color: uiConfiguration?.navbarTextColor }"
        >
          <i class="bi bi-clipboard-check" />
          {{ uiConfiguration?.navbarAccessPolicyText }}
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
import { computed } from 'vue'
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
    initials = words[0][0].toUpperCase() + ' ' + words[words.length - 1][0].toUpperCase()
  }
  return initials
})

function signOutOidc() {
  oidcStore.signOutOidc()
}
</script>

<style scoped>
.sign-out:hover {
  color: #dc3545;
}
</style>

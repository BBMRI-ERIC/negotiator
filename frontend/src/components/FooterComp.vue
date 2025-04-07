<template>
  <footer class="bottom-0 p-0 w-100">
    <hr class="mt-10 mb-10" />
    <div class="row d-flex flex-column flex-md-row">
      <div class="col text-center mb-4 mb-md-0">
        <a
          v-if="uiConfiguration?.isFooterLeftSideIconVisible"
          :href="uiConfiguration?.footerLeftSideIconLink"
        >
          <img width="150" :src="returnLogoSrc" alt="logo footer" />
        </a>
      </div>
      <div class="col text-center" :style="{ color: uiConfiguration?.footerTextColor }">
        <div v-if="uiConfiguration?.isFooterFollowUsVisible">
          <span> Follow Us: </span>
          <a
            v-if="uiConfiguration?.footerFollowUsLinkedin"
            :href="uiConfiguration?.footerFollowUsLinkedin"
            class="ms-2 link"
            :style="{ color: uiConfiguration?.footerTextColor }"
          >
            <i class="bi bi-linkedin mr-3" />
          </a>
          <a
            v-if="uiConfiguration?.footerFollowUsX"
            :href="uiConfiguration?.footerFollowUsX"
            class="ms-2 link"
            :style="{ color: uiConfiguration?.footerTextColor }"
          >
            <i class="bi bi-twitter-x" />
          </a>
          <a
            v-if="uiConfiguration?.footerFollowUsPodcast"
            :href="uiConfiguration?.footerFollowUsPodcast"
            class="ms-2 link"
            :style="{ color: uiConfiguration?.footerTextColor }"
          >
            <i class="bi bi-mic-fill" />
          </a>
        </div>
        <div class="mt-2 mb-2 mb-md-0" v-if="uiConfiguration?.isFooterGithubVisible">
          <a
            v-if="uiConfiguration?.footerGithubBackendLink"
            href="https://github.com/BBMRI-ERIC/negotiator"
            class="ps-2 link"
            :style="{ color: uiConfiguration?.footerTextColor }"
          >
            <i class="bi bi-github pe-1" />
            <span>GitHub</span>
          </a>
        </div>

        <div class="mt-2 mb-2 mb-md-0">
          <a
            v-if="uiConfiguration?.footerSwaggerLink && uiConfiguration?.isFooterSwaggerVisible"
            :href="uiConfiguration?.footerSwaggerLink"
            :style="{ color: uiConfiguration?.footerTextColor }"
            class="link"
          >
            <i class="bi bi-braces-asterisk pe-1" />
            <span>{{ uiConfiguration?.footerSwaggerText }}</span>
          </a>
          <a
            v-if="
              uiConfiguration?.footerStatusPageLink && uiConfiguration?.isFooterStatusPageVisible
            "
            :href="uiConfiguration?.footerStatusPageLink"
            :style="{ color: uiConfiguration?.footerTextColor }"
            class="ps-2 link"
          >
            <i class="bi bi-check-circle pe-1" />
            <span>{{ uiConfiguration?.footerStatusPageText }}</span>
          </a>
        </div>
      </div>

      <div class="col text-center">
        <a
          v-if="
            uiConfiguration?.isFooterWorkProgrammeVisible &&
            (uiConfiguration?.footerWorkProgrammeLink || uiConfiguration?.footerWorkProgrammeText)
          "
          :style="{ color: uiConfiguration?.footerTextColor }"
          :href="uiConfiguration?.footerWorkProgrammeLink"
          class="link"
        >
          <img
            v-if="uiConfiguration?.footerWorkProgrammeImageUrl"
            width="22"
            height="22"
            class="col"
            :src="returnWorkProgrammeSrc"
            alt="Work programme image"
          />
          {{ uiConfiguration?.footerWorkProgrammeText }}
        </a>
        <div
          v-if="
            uiConfiguration?.isFooterNewsletterVisible &&
            (uiConfiguration?.footerNewsletterLink || uiConfiguration?.footerNewsletterText)
          "
          class="ms-md-3 mt-2"
        >
          <button
            type="button"
            class="btn ms-md-5 custom-button-hover"
            :href="uiConfiguration?.footerNewsletterLink"
            :style="{
              'border-color': uiConfiguration?.footerNewsletterButtonColor,
              '--hovercolor': uiConfiguration?.footerNewsletterButtonColor,
              'background-color': uiConfiguration?.footerNewsletterButtonColor,
              color: uiConfiguration?.footerTextColor,
            }"
          >
            {{ uiConfiguration?.footerNewsletterText }}
          </button>
        </div>
        <div v-if="uiConfiguration?.isFooterPrivacyPolicyVisible" class="ms-md-2 ms-5 mt-2">
          <a
            class="me-5 link"
            :style="{ color: uiConfiguration?.footerTextColor }"
            :href="uiConfiguration?.footerPrivacyPolicyLink"
          >
            {{ uiConfiguration?.footerPrivacyPolicyText }}
          </a>
        </div>
      </div>
      <div>
        <div class="row mt-4">
          <div class="col text-center" :style="{ color: uiConfiguration?.footerTextColor }">
            <CopyrightText />
          </div>
          <div class="col text-center ms-4"></div>
          <div
            v-if="uiConfiguration?.isFooterHelpLinkVisible"
            class="col text-center ms-5"
            :style="{ color: uiConfiguration?.footerTextColor }"
          >
            Need help?
            <a
              :href="uiConfiguration?.footerHelpLink"
              :style="{ color: uiConfiguration?.footerTextColor, opacity: 0.7 }"
              class="link"
              >Contact us</a
            >.
          </div>
          <div v-else class="col text-center ms-5"></div>
        </div>
        <div class="text-center mb-3">
          <span :style="{ color: uiConfiguration?.footerTextColor, opacity: 0.5 }"
            >This application was created using the
          </span>
          <a
            href="https://github.com/BBMRI-ERIC/negotiator"
            :style="{ color: uiConfiguration?.footerTextColor }"
            class="link"
            >BBMRI-ERIC Negotiator</a
          >
          <span :style="{ color: uiConfiguration?.footerTextColor, opacity: 0.5 }">
            open source software
          </span>
          <a
            href="https://github.com/BBMRI-ERIC/negotiator/blob/master/LICENSE"
            :style="{ color: uiConfiguration?.footerTextColor }"
            class="link"
            >(license: AGPLv3)</a
          >
          <div
            class="col text-center"
            :style="{ color: uiConfiguration?.footerTextColor, opacity: 0.5 }"
          >
            UI: <span class="pe-2">{{ gitTag }}</span
            >Application: <span>{{ backendVersion }}</span>
          </div>
        </div>
      </div>
    </div>
  </footer>
</template>

<script setup>
import { computed, onBeforeMount, ref } from 'vue'
import bbmriLogo from '../assets/images/bbmri/home-bbmri.png'
import canservLogo from '../assets/images/canserv/home-canserv.png'
import eucaimLogo from '../assets/images/eucaim/home-eucaim.png'
import workProgrammeLogo from '../assets/images/work-programme.png'
import { useActuatorInfoStore } from '../store/actuatorInfo.js'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import CopyrightText from '../components/CopyrightText.vue'

const uiConfigurationStore = useUiConfiguration()
const actuatorInfoStore = useActuatorInfoStore()
const viteGitTag = import.meta.env.VITE_GIT_TAG

const gitTag = viteGitTag
const backendVersion = ref('')

onBeforeMount(() => {
  actuatorInfoStore.retrieveBackendActuatorInfo().then(() => {
    backendVersion.value = actuatorInfoStore.actuatorInfoBuildVersion
  })
})

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.footer
})

const returnLogoSrc = computed(() => {
  if (uiConfiguration.value?.footerLeftSideIcon === 'bbmri') {
    return bbmriLogo
  } else if (uiConfiguration.value?.footerLeftSideIcon === 'canserv') {
    return canservLogo
  } else if (uiConfiguration.value?.footerLeftSideIcon === 'eucaim') {
    return eucaimLogo
  }
  return uiConfiguration.value?.footerLeftSideIcon
})

const returnWorkProgrammeSrc = computed(() => {
  if (uiConfiguration.value?.footerWorkProgrammeImageUrl === 'workProgramme') {
    return workProgrammeLogo
  }
  return uiConfiguration.value?.footerWorkProgrammeImageUrl
})
</script>

<style scoped>
a {
  text-decoration: none;
}

.link {
  transition: color 0.3s;
}

.link:hover {
  color: red !important;
}

.custom-button-hover:hover {
  background-color: var(--hovercolor) !important;
  opacity: 0.7;
  border-color: var(--hovercolor) !important;
}
</style>

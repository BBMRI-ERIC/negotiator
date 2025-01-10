<template>
  <div
    v-if="!oidcIsAuthenticated || isUiConfigActive"
    class="container-fluid d-flex justify-content-center align-items-center vh-100"
    :class="isUiConfigActive ? '' : 'mt-5'"
  >
    <div class="row">
      <div class="col-1" />
      <div class="col-sm-10">
        <div class="card py-5 p-3" :style="{ 'background-color': uiConfiguration?.logincardColor }">
          <div class="col-10 col-md-4 align-self-center">
            <img
              :src="returnLogoSrc"
              class="img-fluid mt-4 mb-2"
              style="min-width: 50px"
              alt="home-page-logo"
            />
          </div>
          <h1
            class="text-center card-title fw-bold mb-5"
            :style="{ color: uiConfiguration?.loginNegotiatorTextColor }"
          >
            <b>NEGOTIATOR</b>
          </h1>
          <div class="card-body">
            <h4
              class="card-subtitle text-center fw-bold pb-2"
              :style="{ color: uiConfiguration?.loginTextColor }"
            >
              Choose how to log in
            </h4>
            <div class="d-grid mx-3 mb-5">
              <button class="btn btn-outline-light" @click.stop.prevent="authenticateOidc">
                <img
                  width="28"
                  height="23"
                  class="float-center mb-1 pe-2"
                  src="../assets/images/ls-aai-logo.png"
                  alt="icon"
                />
                <span
                  class="align-self-center pe-4"
                  :style="{ color: uiConfiguration?.loginTextColor }"
                >
                  Life Science Login</span
                >
              </button>
            </div>
          </div>
        </div>
        <div class="text-center mt-2 mb-2" :style="{ color: uiConfiguration?.loginLinksTextColor }">
          Not familiar with LS Login? Visit their
          <a
            :style="{ color: uiConfiguration?.loginLinksColor }"
            target="_blank"
            href="https://lifescience-ri.eu/ls-login.html"
            >Website</a
          >.
        </div>
        <div class="text-center col mb-2">
          <i class="bi bi-github me-1" />
          <a
            href="https://github.com/BBMRI-ERIC/negotiator-v3-frontend"
            :style="{ color: uiConfiguration?.loginLinksColor }"
            >GitHub UI</a
          >
          <span class="ms-2">
            <i class="bi bi-github me-1" />
            <a
              href="https://github.com/BBMRI-ERIC/negotiator"
              :style="{ color: uiConfiguration?.loginLinksColor }"
              >GitHub Application</a
            >
          </span>
        </div>
        <div class="text-center mt-2 mb-2">
          <a href="/api/swagger-ui/index.html" :style="{ color: uiConfiguration?.loginLinksColor }">
            <i class="bi bi-braces-asterisk text-primary-text" />
            API
          </a>
          <a
            href="https://status.bbmri-eric.eu/"
            class="ps-2"
            :style="{ color: uiConfiguration?.loginLinksColor }"
          >
            <i class="bi bi-check-circle text-primary-text" />
            BBMRI-ERIC Status page
          </a>
        </div>
        <div class="text-center mb-2" :style="{ color: uiConfiguration?.loginLinksTextColor }">
          Need help?
          <a
            :style="{ color: uiConfiguration?.loginLinksColor }"
            href="mailto:negotiator@helpdesk.bbmri-eric.eu"
            >Contact us</a
          >.
        </div>
        <div class="text-center">
          <span :style="{ color: uiConfiguration?.loginLinksTextColor, opacity: 0.5 }"
            >This application was created using the
          </span>
          <a
            href="https://github.com/BBMRI-ERIC/negotiator"
            :style="{ color: uiConfiguration?.loginLinksColor }"
            >BBMRI-ERIC Negotiator</a
          >
          <span :style="{ color: uiConfiguration?.loginLinksTextColor, opacity: 0.5 }">
            open source software
          </span>
          <a
            href="https://github.com/BBMRI-ERIC/negotiator/blob/master/LICENSE"
            :style="{ color: uiConfiguration?.loginLinksColor }"
            >(license: AGPLv3)</a
          >
        </div>
        <div
          class="text-center version-class"
          :style="{ color: uiConfiguration?.loginLinksTextColor, opacity: 0.5 }"
        >
          UI version: <span class="pe-2">{{ gitTag }}</span
          >Server version: <span>{{ backendVersion }}</span>
        </div>
        <div class="text-center mb-5" :style="{ color: uiConfiguration?.loginLinksTextColor }">
          <p>&copy; 2024 BBMRI-ERIC</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeMount, computed } from 'vue'
import bbmriLogo from '../assets/images/bbmri/home-bbmri.png'
import eucaimLogo from '../assets/images/eucaim/home-eucaim.png'
import canservLogo from '../assets/images/canserv/home-canserv.png'
import { useRouter } from 'vue-router'
import { useActuatorInfoStore } from '../store/actuatorInfo.js'
import { useOidcStore } from '../store/oidc.js'
import { useUiConfiguration } from '../store/uiConfiguration.js'

const props = defineProps({
  isUiConfigActive: {
    type: Boolean,
    default: false,
  },
})

const viteGitTag = import.meta.env.VITE_GIT_TAG

const uiConfigurationStore = useUiConfiguration()
const actuatorInfoStore = useActuatorInfoStore()

const router = useRouter()

const gitTag = ref(viteGitTag)
const backendVersion = ref('')
const oidcStore = useOidcStore()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.login
})

const oidcIsAuthenticated = computed(() => {
  return oidcStore.oidcIsAuthenticated
})

const returnLogoSrc = computed(() => {
  if (uiConfiguration.value?.loginLogoUrl === 'bbmri') {
    return bbmriLogo
  } else if (uiConfiguration.value?.loginLogoUrl === 'canserv') {
    return canservLogo
  } else if (uiConfiguration.value?.loginLogoUrl === 'eucaim') {
    return eucaimLogo
  }
  return uiConfiguration.value?.loginLogoUrl
})

onBeforeMount(() => {
  if (oidcIsAuthenticated.value && !props.isUiConfigActive) {
    router.push('/researcher')
  }
  actuatorInfoStore.retrieveBackendActuatorInfo().then(() => {
    backendVersion.value = actuatorInfoStore.actuatorInfoBuildVersion
  })
})

function authenticateOidc() {
  oidcStore.authenticateOidc()
}
</script>

<style scoped>
h1 {
  font-size: 3.5rem;
}
</style>

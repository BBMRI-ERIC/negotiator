<template>
  <div
    v-if="!oidcIsAuthenticated || isUiConfigActive"
    class="container-fluid d-flex justify-content-center align-items-center vh-100"
    :class="isUiConfigActive ? '' : 'mt-5'"
  >
    <div class="row">
      <div class="col-1" />
      <div class="col-sm-10">
        <div class="card py-5 p-3">
          <div class="col-10 col-md-4 align-self-center">
            <img
              :src="returnLogoSrc"
              class="img-fluid mt-4 mb-2"
              style="min-width: 50px"
              alt="home-page-logo"
            />
          </div>
          <h1 class="text-center card-title fw-bold mb-5 text-login-tittle-text">
            <b>NEGOTIATOR</b>
          </h1>
          <div class="card-body">
            <h4 class="card-subtitle text-center fw-bold pb-2 text-primary-text">
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
                <span class="align-self-center pe-4 text-primary-link-color">
                  Life Science Login</span
                >
              </button>
            </div>
          </div>
        </div>
        <div class="text-center mt-2 mb-2 text-primary-text">
          Not familiar with LS Login? Visit their
          <a
            class="text-primary-link-color"
            target="_blank"
            href="https://lifescience-ri.eu/ls-login.html"
            >Website</a
          >.
        </div>
        <div class="text-center col mb-2">
          <i class="bi bi-github me-1" />
          <a href="https://github.com/BBMRI-ERIC/negotiator" class="text-primary-link-color"
            >View Source Code</a
          >
        </div>
        <div class="text-center mt-2 mb-2">
          <a href="/api/swagger-ui/index.html" class="text-primary-link-color">
            <i class="bi bi-braces-asterisk text-primary-text" />
            API
          </a>
          <a href="https://status.bbmri-eric.eu/" class="ps-2 text-primary-link-color">
            <i class="bi bi-check-circle text-primary-text" />
            BBMRI-ERIC Status page
          </a>
        </div>
        <div class="text-center mb-2 text-secondary-text">
          Need help?
          <a class="text-primary-link-color" href="mailto:negotiator@helpdesk.bbmri-eric.eu"
            >Contact us</a
          >.
        </div>
        <div class="text-center">
          <span class="text-secondary-text" style="opacity: 0.5"
            >This application was created using the
          </span>
          <a href="https://github.com/BBMRI-ERIC/negotiator" class="text-primary-link-color"
            >BBMRI-ERIC Negotiator</a
          >
          <span class="text-secondary-text" style="opacity: 0.5"> open source software </span>
          <a
            href="https://github.com/BBMRI-ERIC/negotiator/blob/master/LICENSE"
            class="text-primary-link-color"
            >(license: AGPLv3)</a
          >
        </div>
        <div class="text-center version-class text-secondary-text" style="opacity: 0.5">
          UI version: <span class="pe-2">{{ gitTag }}</span
          >Server version: <span>{{ backendVersion }}</span>
        </div>
        <div class="text-center mb-5 text-secondary-text">
          <CopyrightText />
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
import CopyrightText from '../components/CopyrightText.vue'

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

const uiConfigurationLogin = computed(() => {
  return uiConfigurationStore.uiConfiguration?.login
})

const oidcIsAuthenticated = computed(() => {
  return oidcStore.oidcIsAuthenticated
})

const returnLogoSrc = computed(() => {
  if (uiConfigurationLogin.value?.loginLogoUrl === 'bbmri') {
    return bbmriLogo
  } else if (uiConfigurationLogin.value?.loginLogoUrl === 'canserv') {
    return canservLogo
  } else if (uiConfigurationLogin.value?.loginLogoUrl === 'eucaim') {
    return eucaimLogo
  }
  return uiConfigurationLogin.value?.loginLogoUrl
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

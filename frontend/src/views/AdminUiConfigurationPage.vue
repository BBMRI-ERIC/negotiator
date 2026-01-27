<template>
  <confirmation-modal
    id="saveModal"
    title="Are you sure you want to save?"
    text="Confirming, you will change ui-configuration."
    :message-enabled="false"
    @confirm="save()"
  />
  <h1 class="mb-5">UI Configuration</h1>
  <div class="ui-configuration">
    <div class="theme mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Theme Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse1"
          aria-expanded="true"
          aria-controls="collapse1"
        >
          <i class="bi bi-arrows-angle-expand" /> collapse/expand
        </button>
      </div>
      <div id="collapse1" class="theme-config row collapse show">
        <UiConfigurationSetting v-model="uiConfiguration.theme" />
      </div>
    </div>
    <div class="nav-bar mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Navbar Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse2"
          aria-expanded="false"
          aria-controls="collapse2"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse2" class="navbar-config row collapse">
        <h5 class="mb-3 bold text-muted">
          You can see the changes live in the navbar above!
          <i class="bi bi-arrow-up" />
        </h5>
        <UiConfigurationSetting v-model="uiConfiguration.navbar" />
      </div>
    </div>
    <div class="login mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Login page Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse3"
          aria-expanded="false"
          aria-controls="collapse3"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse3" class="login-config row collapse">
        <UiConfigurationSetting v-model="uiConfiguration.login" />
        <HomePage :is-ui-config-active="true" />
      </div>
    </div>

    <div class="new-request mb-2">
      <div class="mb-3 text-left fw-bold h3">
        New Request Button
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse6"
          aria-expanded="false"
          aria-controls="collapse6"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse6" class="new-request-config row collapse">
        <UiConfigurationSetting v-model="uiConfiguration.newRequestButton" />
        <NewRequestButton v-if="!networkActivated" />
      </div>
    </div>
    <div class="footer mb-2">
      <div class="mb-3 text-left fw-bold h3">
        Footer Settings
        <button
          class="btn btn-sm btn-light"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#collapse7"
          aria-expanded="false"
          aria-controls="collapse7"
        >
          <i class="bi bi-arrows-angle-expand" /> expand/collapse
        </button>
      </div>
      <div id="collapse7" class="footer-config row collapse">
        <h5 class="mb-3 bold text-muted">
          You can see the changes live in the footer below!
          <i class="bi bi-arrow-down" />
        </h5>
        <UiConfigurationSetting v-model="uiConfiguration.footer" />
      </div>
    </div>
  </div>
  <div class="sticky-bottom pb-3">
    <button class="btn btn-sm bg-primary sm my-2" @click="restartSettings()">
      <i class="bi bi-arrow-clockwise text-white">Reset to default settings</i>
    </button>
    <button
      class="btn btn-sm bg-primary sm my-2 float-end"
      data-bs-toggle="modal"
      data-bs-target="#saveModal"
    >
      <i class="bi bi-floppy text-white"> Save Changes </i>
    </button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import UiConfigurationSetting from '../components/UiConfigurationSetting.vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import LoginPage from '../views/LoginPage.vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'

const uiConfigurationStore = useUiConfiguration()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration
})

function restartSettings() {
  uiConfigurationStore.retrieveUiConfiguration()
}

function save() {
  uiConfigurationStore.updateUiConfiguration(uiConfiguration.value)
}
</script>

<style scoped>
.ui-configuration {
  min-height: 70vh;
}
</style>

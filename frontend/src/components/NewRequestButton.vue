<template>
  <div v-if="uiConfiguration?.isButtonVisible && uiConfiguration?.buttonText">
    <button
      ref="openModal"
      class="btn btn-sm sm my-2 float-end"
      data-bs-toggle="modal"
      data-bs-target="#newRequestModal"
      :style="{ 'background-color': uiConfiguration?.buttonColor }"
    >
      <span :style="{ color: uiConfiguration?.buttonTextColor }">
        {{ uiConfiguration?.buttonText }}</span
      >
    </button>
    <NewRequestModal
      id="newRequestModal"
      :is-modal-small="true"
      :title="uiConfiguration?.modalTittle"
      :text="uiConfiguration?.modalText"
      dismiss-button-text="Back to HomePage"
      @confirm="startNewRequest"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import NewRequestModal from '../components/modals/NewRequestModal.vue'
import { useApiCallsStore } from '../store/apiCalls.js'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import allFeatureFlags from '@/config/featureFlags.js'

const apiCallsStore = useApiCallsStore()
const uiConfigurationStore = useUiConfiguration()

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.newRequestButton
})


async function startNewRequest() {
  if (allFeatureFlags.dev_mode === 'true' || allFeatureFlags.dev_mode === true) {
    const data = {
      url: 'https://bbmritestnn.gcc.rug.nl',
      humanReadable:
        '#1: No filters used.\r\n#2: No filters used.\r\n#3: No filters used.\r\n#4: No filters used.\r\n#5: No filters used.',
      resources: [
        {
          id: 'bbmri-eric:ID:CZ_MMCI:collection:LTS',
        },
      ],
    }

    await apiCallsStore.createRequests(data).then((redirect) => {
      window.open(redirect.redirectUrl, '_blank')
    })
  } else {
    if (uiConfiguration.value?.buttonUrl) {
      window.open(uiConfiguration.value?.buttonUrl, '_blank')
    }
  }
}
</script>

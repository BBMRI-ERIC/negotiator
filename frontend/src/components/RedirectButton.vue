<template>
  <div>
    <button
      ref="openModal"
      class="btn btn-sm sm"
      data-bs-toggle="modal"
      data-bs-target="#newRedirectModal"
      :style="{ 'background-color': uiConfiguration?.buttonColor }"
    >
      <span :style="{ color: uiConfiguration?.buttonTextColor }"> {{ props?.buttonText }}</span>
    </button>
    <NewRequestModal
      id="newRedirectModal"
      :is-modal-small="true"
      :title="props?.titleModal"
      :text="props?.textModal"
      dismiss-button-text="Back to HomePage"
      @confirm="redirectPage"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import NewRequestModal from '@/components/modals/NewRequestModal.vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'
import { useRouter } from 'vue-router'

const uiConfigurationStore = useUiConfiguration()
const router = useRouter()

const props = defineProps({
  buttonText: {
    type: String,
    default: '',
  },
  redirectRouteName: {
    type: String,
    default: '',
  },
  titleModal: {
    type: String,
    default: '',
  },
  textModal: {
    type: String,
    default: '',
  },
})

const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.newRequestButton
})

function redirectPage() {
  router.push({ name: props.redirectRouteName })
}
</script>

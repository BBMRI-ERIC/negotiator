<template>
  <div class="modal" tabindex="-1" :id="id" :class="isModalFade" :aria-labelledby="`${id}Label`">
    <div class="opacity-75 position-fixed">
      <AlertNotification />
    </div>
    <div
      class="modal-dialog modal-dialog-centered modal-dialog-scrollable"
      :class="optionalSizeOfModal"
    >
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title text-truncate me-3" :title="title">{{ title }}</h5>
          <button
            v-if="isXButtondisplayed"
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          ></button>
        </div>

        <div class="modal-body">
          <div v-if="text" class="modal-body-text">{{ text }}</div>
          <slot name="body" />
        </div>
        <div class="modal-footer justify-content-center">
          <button
            v-if="buttonDismissText"
            type="button"
            data-bs-dismiss="modal"
            aria-label="Close"
            class="btn"
            :class="buttonDismissColor ? 'btn-dark' : ''"
            @click="emitDismiss()"
          >
            {{ buttonDismissText }}
          </button>
          <button
            v-if="buttonSaveText"
            type="button"
            class="btn"
            :class="buttonDismissColor ? 'btn-danger' : ''"
            @click="emitSave()"
          >
            {{ buttonSaveText }}
          </button>
          <slot name="footer" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AlertNotification from '../../components/AlertNotification.vue'

const props = defineProps({
  id: {
    type: String,
    required: true,
  },
  modalFade: {
    type: Boolean,
    required: false,
    default: true,
  },
  title: {
    type: String,
    required: false,
  },
  text: {
    type: String,
    required: false,
  },
  isModalSmall: {
    type: Boolean,
    required: false,
    default: false,
  },
  isModalLarge: {
    type: Boolean,
    required: false,
    default: false,
  },
  isModalExtraLarge: {
    type: Boolean,
    required: false,
    default: false,
  },
  isXButtondisplayed: {
    type: Boolean,
    required: false,
    default: true,
  },
  buttonDismissText: {
    type: String,
    required: false,
    default: 'Close',
  },
  buttonSaveText: {
    type: String,
    required: false,
    default: 'Save changes',
  },
  buttonDismissColor: {
    type: String,
    required: false,
    default: 'btn-dark',
  },
  buttonSaveColor: {
    type: String,
    required: false,
    default: 'btn-primary',
  },
})

const optionalSizeOfModal = computed(() => {
  if (props.isModalSmall) return 'modal-sm'
  if (props.isModalLarge) return 'modal-lg'
  if (props.isModalExtraLarge) return 'modal-xl'
  return ''
})

const isModalFade = computed(() => {
  return props.modalFade ? 'fade' : ''
})

const emit = defineEmits(['dismiss', 'save'])

function emitDismiss() {
  emit('dismiss')
}

function emitSave() {
  emit('save')
}
</script>

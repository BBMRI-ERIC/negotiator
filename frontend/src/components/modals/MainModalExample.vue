<template>
  <div>
    <MainModal
      :id="id"
      title="Example Modal"
      text="This is an example modal."
      :modalFade="true"
      :isModalSmall="false"
      :isModalLarge="false"
      :isModalExtraLarge="true"
      :isXButtondisplayed="true"
      :buttonDismissText="'Close'"
      :buttonSaveText="'Save changes'"
      :buttonDismissColor="'btn-dark'"
      :buttonSaveColor="'btn-danger'"
      @dismiss="emitDismiss()"
      @ok="emitOk()"
      @save="emitSave()"
    >
      <template #body>
        <p>This is the body of the example modal.</p>
      </template>
      <template #footer>
        <button type="button" class="btn btn-primary">Modal bootton example</button>
      </template>
    </MainModal>
  </div>
</template>

<script setup>
import MainModal from '../modals/MainModal.vue'

defineProps({
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
    required: true,
  },
  text: {
    type: String,
    required: true,
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
    default: 'btn-danger',
  },
})

const emit = defineEmits(['dismiss', 'save'])

function emitDismiss() {
  emit('dismiss')
}

function emitSave() {
  emit('save')
}
</script>

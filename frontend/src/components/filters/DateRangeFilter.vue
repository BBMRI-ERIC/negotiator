<template>
  <div>
    <button
      class="btn dropdown-toggle custom-button-hover"
      :style="buttonStyle"
      :class="buttonClass"
      type="button"
      data-bs-toggle="dropdown"
      data-bs-auto-close="outside"
      aria-expanded="false"
    >
      {{ label }}
    </button>
    <ul class="dropdown-menu" aria-labelledby="dropdownMenuButton1">
      <div
        class="mx-2 my-2 dropdown-contents"
        :style="{ color: uiConfiguration?.filtersSortDropdownTextColor }"
      >
        <TextFilter
          v-model:value="start"
          label="Start"
          :type="type"
          @input="emitStartChange"
        />
        <TextFilter
          v-model:value="end"
          class="mt-2"
          label="End"
          :type="type"
          @input="emitEndChange"
        />
      </div>
    </ul>
  </div>
</template>

<script setup>
import TextFilter from './TextFilter.vue'

const start = defineModel("start")
const end = defineModel("end")

const emit = defineEmits(["startChanged", "endChanged"])

defineProps({
  name: {
    type: String,
    required: true
  },
  label: {
    type: String,
    required: true
  },
  type: {
    type: String,
    required: false,
    default: 'date',
    validator: (prop) => ['date', 'datetime', 'date-local', 'datetime-local'].includes(prop)
  },
  buttonStyle: {
    type: Object,
    required: false,
    default: () => {}
  },
  buttonClass: {
    type: Object,
    required: false,
    default: () => {}
  },
  inputStyle: {
    type: Object,
    required: false,
    default: () => {}
  },
  
})

function emitStartChange() {
  emit('startChanged', start.value)
}

function emitEndChange() {
  emit('endChanged', end.value)
}

</script>

<style scoped>
.custom-button-hover:hover {
  background-color: var(--hovercolor) !important;
  color: #ffffff !important;
  border-color: var(--hovercolor) !important;
}
</style>
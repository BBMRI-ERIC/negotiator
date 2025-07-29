<template>
  <div>
    <button
      class="btn dropdown-toggle custom-button-hover show"
      :style="buttonStyle"
      type="button"
      data-bs-toggle="dropdown"
      data-bs-auto-close="outside"
      aria-expanded="false"
    >
      {{ label }}
    </button>
    <ul class="dropdown-menu" aria-labelledby="dropdownSortingButton" role="menu">
      <div v-for="(option, index) in options" :key="index" class="form-check mx-2 my-2">
        <input
          v-model="value"
          class="form-check-input"
          :type="type"
          :value="option.value"
          @change="emitChange"
        />
        <label
          class="form-check-label"
          :style=labelStyle
        >
          {{ option.label }}
        </label>
      </div>
    </ul>
  </div>
</template>

<script setup>

const value = defineModel("value")

const emit = defineEmits(["change"])

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
    default: 'checkbox',
    validator: (prop) => ['checkbox', 'radio'].includes(prop)
  },
  options: {
    type: Array,
    required: true,
    default: () => []
  },
  buttonStyle: {
    type: Object,
    required: false,
    default: () => {}
  },
  labelStyle: {
    type: Object,
    required: false,
    default: () => {}
  }  
})

function emitChange() {
  emit('change', value.value)
}

</script>

<style scoped>
.custom-button-hover:hover {
  background-color: var(--hovercolor) !important;
  color: #ffffff !important;
  border-color: var(--hovercolor) !important;
}
</style>
<template>
  <div class="input-group">
    <span class="input-group-text">{{ label }}</span>
    <input
      :id="name"
      v-model="value"
      :type="type"
      :placeholder="placeholder"
      class="form-control"
      @input="emitInput"
    />
  </div>
</template>

<script setup>

const value = defineModel("value")

const emit = defineEmits(["input"])

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
    default: 'text',
    validator: (prop) => ['test', 'email'].includes(prop)
  },
  placeholder: {
    type: String,
    required: false,
    default: ''
  }
})

function emitInput() {
  emit('input', value.value)
}

</script>
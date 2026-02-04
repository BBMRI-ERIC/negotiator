<template>
  <div class="big-home-button">
    <button class="btn panel-button d-flex flex-row align-items-center rounded-4 me-4 mt-2 px-4">
      <div v-if="buttonIcon" class="my-1">
        <i class="bi me-2 my-0 h4" :class="buttonIcon"></i>
      </div>
      <h4 v-if="buttonText" class="my-0 fw-bold">{{ buttonText }}</h4>
    </button>
  </div>
</template>

<script setup>
defineProps({
  buttonIcon: {
    type: String,
    required: false,
    default: '',
  },
  buttonText: {
    type: String,
    required: false,
    default: '',
  },
})
</script>

<style scoped>
.panel-button {
  background-color: #f8f8f8;
}
.panel-button:hover,
.panel-button:focus {
  background: #3d3d3d;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  outline: none;
}
</style>

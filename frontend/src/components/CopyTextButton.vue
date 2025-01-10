<template>
  <button type="button" class="btn btn-sm btn-muted" @click="copyTextToClipboard(text)">
    <span class="bi-copy" />
  </button>
</template>

<script setup>
defineProps({
  text: {
    type: String,
    default: '',
  },
})

function copyTextToClipboard(text) {
  navigator.clipboard.writeText(text)
}
</script>

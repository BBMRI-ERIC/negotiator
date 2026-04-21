<template>
  <span class="badge rounded-pill bg-light text-dark d-inline-flex align-items-center gap-1">
    <i class="bi bi-fire" aria-hidden="true" />
    <span class="font-monospace">{{ event }}</span>
  </span>
</template>

<script setup>
defineProps({
  event: {
    type: String,
    required: true,
  },
})
</script>

<template>
  <span class="badge rounded-pill bg-light text-dark d-inline-flex align-items-center gap-1">
    <i class="bi bi-box" aria-hidden="true"></i>
    <span class="font-monospace">{{ id }}</span>
  </span>
</template>

<script setup>
defineProps({
  id: {
    type: String,
    required: true,
  },
})
</script>

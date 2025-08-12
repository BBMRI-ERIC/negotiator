<template>
  <div v-if="resourcesWithoutRepresentatives > 0" class="alert alert-warning mt-3" role="alert">
    <i class="bi bi-exclamation-triangle-fill me-2"></i>
    Warning: At least one or more Resources in your Network do not have a single
    <strong>Representative</strong> attached to them. Hence, any new requests for these Resources
    <strong>will not be forwarded</strong>. To see which Resources need to be updated, go to the
    <strong>Organizations</strong> tab. To update them, click on the
    <strong>"Manage Resource Representatives"</strong> button above.
  </div>
</template>

<script setup>
defineProps({
  resourcesWithoutRepresentatives: {
    type: Number,
    required: true,
  },
})
</script>

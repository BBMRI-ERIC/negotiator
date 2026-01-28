<template>
  <th :style="header.style" class="primary-table-header" role="columnheader">
    <template v-if="$slots.default">
      <slot :header="header" :idx="idx" />
    </template>
    <template v-else>
      {{ header.label }}
    </template>
  </th>
</template>

<script setup>
defineProps({
  header: { type: Object, required: true },
  idx: { type: Number, required: true },
})
</script>

<style scoped>
.primary-table-header {
  position: sticky;
  top: 0;
  z-index: 2;
  border-bottom: 1px solid #dee2e6;
  padding: 0.75rem 0.5rem;
  text-align: left;
}
</style>

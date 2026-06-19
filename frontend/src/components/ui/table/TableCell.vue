<template>
  <td :style="header.style" class="primary-table-cell d-table-cell" role="cell">
    <slot :row="row" :value="row[header.key]" :rowIdx="rowIdx" :colIdx="colIdx">
      {{ row[header.key] }}
    </slot>
  </td>
</template>

<script setup>
defineProps({
  row: { type: Object, required: true },
  header: { type: Object, required: true },
  rowIdx: { type: Number, required: true },
  colIdx: { type: Number, required: true },
})
</script>

<style scoped>
.primary-table-cell {
  padding: 0.75rem 0.5rem;
  border-bottom: 1px solid #dee2e6;
  vertical-align: middle;
}
</style>

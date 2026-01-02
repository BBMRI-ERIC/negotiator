<template>
  <div class="ui-table-responsive">
    <table class="ui-table">
      <thead>
        <tr>
          <th
            v-for="(header, idx) in headers"
            :key="idx"
            :style="header.style"
            class="ui-table-header"
          >
            <template v-if="$slots[`header-${header.key}`]">
              <slot :name="`header-${header.key}`" :header="header" :idx="idx" />
            </template>
            <template v-else>
              {{ header.label }}
            </template>
          </th>
        </tr>
      </thead>
      <tbody>
        <a
          v-for="(row, rowIdx) in data"
          :key="rowIdx"
          class="ui-table-row d-table-row text-decoration-none"
          :href="row.href || undefined"
          style="color: inherit; user-select: none; display: table-row"
          tabindex="0"
          @click="rowClick(row, rowIdx)"
        >
          <span
            v-for="(header, colIdx) in headers"
            :key="colIdx"
            :style="header.style"
            class="ui-table-cell d-table-cell"
            style="display: table-cell"
          >
            <slot
              :name="header.slot"
              :row="row"
              :value="row[header.key]"
              :rowIdx="rowIdx"
              :colIdx="colIdx"
            >
              {{ row[header.key] }}
            </slot>
          </span>
        </a>
      </tbody>
    </table>
  </div>
</template>

<script setup>
defineProps({
  headers: {
    type: Array,
    required: true,
    // [{ key: 'title', label: 'Title', slot: 'title', style: {} }, ...]
  },
  data: {
    type: Array,
    required: true,
  },
  rowClick: {
    type: Function,
    default: () => {},
  },
})
</script>

<style scoped>
.ui-table-responsive {
  width: 100%;
  overflow-x: auto;
}
.ui-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: auto;
}
.ui-table-header {
  position: sticky;
  top: 0;
  z-index: 2;
  border-bottom: 1px solid #dee2e6;
  padding: 0.75rem 0.5rem;
  text-align: left;
}
.ui-table-row {
  transition: background 0.2s;
  cursor: pointer;
}
.ui-table-row:hover {
  background: #e9ecef;
}
.ui-table-cell {
  padding: 0.75rem 0.5rem;
  border-bottom: 1px solid #dee2e6;
  vertical-align: middle;
}
</style>

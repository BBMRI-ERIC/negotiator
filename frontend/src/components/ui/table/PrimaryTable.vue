<template>
  <div class="primary-table-responsive">
    <table class="primary-table" role="table">
      <thead role="rowgroup">
        <tr role="row">
          <TableHeader v-for="(header, idx) in headers" :key="idx" :header="header" :idx="idx">
            <template v-if="$slots[`header-${header.key}`]" #default="slotProps">
              <slot :name="`header-${header.key}`" v-bind="slotProps" />
            </template>
          </TableHeader>
        </tr>
      </thead>
      <tbody role="rowgroup">
        <tr
          v-for="(row, rowIdx) in data"
          :key="rowIdx"
          class="primary-table-row d-table-row text-decoration-none"
          :tabindex="0"
          style="cursor: pointer; user-select: none"
          role="row"
        >
          <a :href="row.href || undefined" @click.prevent="handleRowClick(row)">
            <TableCell
              v-for="(header, colIdx) in headers"
              :key="colIdx"
              :row="row"
              :header="header"
              :rowIdx="rowIdx"
              :colIdx="colIdx"
            >
              <template v-if="$slots[header.slot]" #default="slotProps">
                <slot :name="header.slot" v-bind="slotProps" />
              </template>
            </TableCell>
          </a>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import TableHeader from './TableHeader.vue'
import TableCell from './TableCell.vue'
import { useRouter } from 'vue-router'

const router = useRouter()

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

function handleRowClick(row) {
  if (row.href) {
    router.push(row.href)
  }
}
</script>

<style scoped>
.primary-table-responsive {
  width: 100%;
  overflow-x: auto;
}
.primary-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: auto;
}

.primary-table-row {
  transition: background 0.2s;
  cursor: pointer;
}
.primary-table-row:hover {
  background: #e9ecef;
}

tr a {
  display: contents;
}
</style>

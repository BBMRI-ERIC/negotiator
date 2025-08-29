<template>
  <div v-if="totalElements > 0" class="pagination d-flex justify-content-between">
    <div class="div-placeholder col-md-4"></div>
    <div class="pagination-page col-md-4">
      <button
        @click="$emit('previousPage')"
        :disabled="pageNumber === 0 || loading"
        class="page-button"
      >
        ‹ Prev
      </button>
      <span class="page-info">Page {{ pageNumber + 1 }} of {{ totalPages }}</span>
      <button
        @click="$emit('nextPage')"
        :disabled="pageNumber === totalPages - 1 || loading"
        class="page-button"
      >
        Next ›
      </button>
    </div>
    <div class="increment-decrement-input col-md-4 d-flex justify-content-end">
      <button class="button-size-input btn btn-outline-secondary me-1" @click="changePageSize(-1)">
        -
      </button>
      <input
        :model-value="pageSize"
        v-on:input="handlePageSizeChange"
        :placeholder="'Page size ' + pageSize"
        type="number"
        min="1"
        max="100"
        class="page-size-input"
      />

      <button class="button-size-input btn btn-outline-secondary ms-1" @click="changePageSize(1)">
        +
      </button>
    </div>
  </div>
</template>

<script setup>
const props = defineProps({
  pageNumber: {
    type: Number,
    required: true,
  },
  totalPages: {
    type: Number,
    required: true,
  },
  totalElements: {
    type: Number,
    required: true,
  },
  pageSize: {
    type: Number,
    required: true,
  },
  loading: {
    type: Boolean,
    required: true,
  },
})

const emit = defineEmits(['previousPage', 'nextPage', 'updatePageSize'])

const handlePageSizeChange = (event) => {
  emit('updatePageSize', Number(event.target.value))
}

function changePageSize(valueChange) {
  if (props.pageSize + valueChange > 1 || props.pageSize + valueChange < 100) {
    const newPageSize = Math.min(Math.max(1, props.pageSize + valueChange), 100)
    emit('updatePageSize', Number(newPageSize))
  }
}
</script>

<style scoped>
.pagination {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-top: 1rem;
}

.page-button {
  padding: 0.5rem 1rem;
  margin: 0 0.5rem;
  background-color: #f8f9fa;
  border: 1px solid #e8ecef;
  cursor: pointer;
  font-size: 0.95rem;
  border-radius: 0.375rem;
}

.page-button:disabled {
  background-color: #e8ecef;
  color: #6c757d;
  cursor: not-allowed;
}

.page-button:not(:disabled):hover {
  background-color: #e8ecef;
}

.page-info {
  margin: 0 1rem;
  font-size: 0.95rem;
  color: #6c757d;
}
.button-size-input {
  border: 1px solid #e8ecef;
}
.page-size-input {
  padding: 0.5rem;
  width: 110px;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  font-size: 0.9rem;
}

input[type='number']::-webkit-inner-spin-button,
input[type='number']::-webkit-outer-spin-button {
  -webkit-appearance: none;
  -moz-appearance: none;
  appearance: none;
  margin: 0;
}
</style>

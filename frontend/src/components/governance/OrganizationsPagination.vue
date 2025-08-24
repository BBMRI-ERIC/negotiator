<template>
  <div v-if="totalElements > 0" class="pagination">
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
    <input
      :model-value="pageSize"
      @change="handlePageSizeChange"
      type="number"
      min="1"
      max="100"
      class="page-size-input"
      placeholder="Page size"
    />
  </div>
</template>

<script setup>
defineProps({
  pageNumber: {
    type: Number,
    required: true
  },
  totalPages: {
    type: Number,
    required: true
  },
  totalElements: {
    type: Number,
    required: true
  },
  pageSize: {
    type: Number,
    required: true
  },
  loading: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['previousPage', 'nextPage', 'updatePageSize'])

const handlePageSizeChange = (event) => {
  emit('updatePageSize', Number(event.target.value))
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

.page-size-input {
  padding: 0.5rem;
  margin-left: 1rem;
  width: 80px;
  border: 1px solid #e8ecef;
  border-radius: 0.375rem;
  font-size: 0.9rem;
}
</style>

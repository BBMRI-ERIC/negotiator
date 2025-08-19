<template>
  <div class="d-flex justify-content-between align-items-center mb-4">
    <h2 class="section-title">
      <i class="bi bi-diagram-3 me-2"></i>
      Organizations & Resources
    </h2>
    <div class="actions-group">
      <button
        class="btn btn-outline-primary me-2"
        @click="$emit('toggleExpandAll')"
        :disabled="loading"
      >
        <i :class="allExpanded ? 'bi bi-dash-square' : 'bi bi-plus-square'" class="me-2"></i>
        {{ allExpanded ? 'Collapse All' : 'Expand All' }}
      </button>
      <button
        class="btn btn-primary"
        @click="$emit('createOrganization')"
        :disabled="loading"
      >
        <i class="bi bi-plus-circle me-2"></i>
        Create Organization
      </button>
    </div>
  </div>
</template>

<script setup>
defineProps({
  loading: {
    type: Boolean,
    required: true
  },
  allExpanded: {
    type: Boolean,
    required: true
  }
})

defineEmits(['toggleExpandAll', 'createOrganization'])
</script>

<style scoped>
.section-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #2c3e50;
  margin: 0;
}

.actions-group {
  display: flex;
  gap: 0.5rem;
}

@media (max-width: 768px) {
  .actions-group {
    flex-direction: column;
    gap: 0.5rem;
  }
}
</style>

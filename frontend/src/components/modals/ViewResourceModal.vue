<template>
  <div class="modal fade show d-block" tabindex="-1" role="dialog">
    <div class="modal-dialog modal-lg" role="document">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title">
            <i class="bi bi-eye text-primary me-2"></i>
            Resource Details
          </h5>
          <button type="button" class="btn-close" @click="$emit('close')" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <div class="row" v-if="resource">
            <div class="col-md-6">
              <h6 class="text-muted">Resource Name</h6>
              <p>{{ resource.name }}</p>
            </div>
            <div class="col-md-6">
              <h6 class="text-muted">Source ID</h6>
              <p><code>{{ resource.sourceId }}</code></p>
            </div>
            <div class="col-12">
              <h6 class="text-muted">Description</h6>
              <p>{{ resource.description || 'No description available' }}</p>
            </div>
            <div class="col-md-6">
              <h6 class="text-muted">Contact Email</h6>
              <p>{{ resource.contactEmail || 'Not specified' }}</p>
            </div>
            <div class="col-md-6">
              <h6 class="text-muted">Status</h6>
              <span :class="getStatusBadgeClass(resource)">
                {{ getStatusText(resource) }}
              </span>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" @click="$emit('close')">Close</button>
          <button type="button" class="btn btn-primary" @click="$emit('close')">
            <i class="bi bi-pencil me-2"></i>Edit Resource
          </button>
        </div>
      </div>
    </div>
  </div>
  <div class="modal-backdrop fade show"></div>
</template>

<script setup>
const props = defineProps({
  resource: {
    type: Object,
    required: true,
  },
})

const emit = defineEmits(['close'])

const getStatusBadgeClass = (resource) => {
  const baseClass = 'badge '
  return baseClass + (resource.active ? 'bg-success' : 'bg-secondary')
}

const getStatusText = (resource) => {
  return resource.active ? 'Active' : 'Inactive'
}
</script>

<style scoped>
.modal {
  background-color: rgba(0, 0, 0, 0.5);
}

.badge {
  font-size: 0.875rem;
  padding: 0.5rem 0.75rem;
}
</style>

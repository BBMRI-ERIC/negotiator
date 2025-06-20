<template>
  <div class="card mb-3">
    <div class="card-body">
      <h5 class="card-title">Requirement #{{ requirement.id }}</h5>
      <p class="card-text">
        <strong>Access Form:</strong> {{ requirement.requiredAccessForm.name }} (ID:
        {{ requirement.requiredAccessForm.id }})<br />
        <strong>For Event:</strong> {{ requirement.forResourceEvent }}<br />
        <strong>Admin Only:</strong> {{ requirement.viewableOnlyByAdmin ? 'Yes' : 'No' }}
      </p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  requirement: {
    type: Object,
    required: true,
  },
})
</script>

<style scoped>
.card {
  border: 1px solid #e8ecef;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.card-title {
  font-size: 1.25rem;
  color: #2c3e50;
  margin-bottom: 0.75rem;
}

.card-text {
  font-size: 0.95rem;
  color: #34495e;
}

.card-text strong {
  color: #2c3e50;
}
</style>

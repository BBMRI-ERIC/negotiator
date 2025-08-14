<template>
  <div
    class="stat-card flex-fill"
    :class="{ 'cursor-pointer': clickable }"
    :type="clickable ? 'button' : undefined"
    :data-bs-toggle="clickable ? 'modal' : undefined"
    :data-bs-target="clickable ? '#negotiationsModal' : undefined"
    @click="handleClick"
  >
    <div class="stat-label">
      <span>{{ label }}</span>
      <i v-if="tooltip" class="bi bi-info-circle small-icon" :title="tooltip" />
    </div>
    <h5 :class="{ 'text-muted': muted }">{{ value }}</h5>
  </div>
</template>

<script setup>
const props = defineProps({
  label: {
    type: String,
    required: true,
  },
  value: {
    type: [String, Number],
    required: true,
  },
  tooltip: {
    type: String,
    default: '',
  },
  clickable: {
    type: Boolean,
    default: false,
  },
  muted: {
    type: Boolean,
    default: false,
  },
})

const emit = defineEmits(['click'])

const handleClick = () => {
  if (props.clickable) {
    emit('click')
  }
}
</script>

<style scoped>
.stat-card {
  padding: 20px;
  border: 1px solid #dee2e6;
  border-radius: 8px;
  background-color: #f8f9fa;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  width: 100%;
}

.stat-card:hover {
  background-color: #e9ecef;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stat-label {
  display: flex;
  justify-content: space-between;
  font-size: 14px;
  color: #6c757d;
  margin-bottom: 8px;
}

.stat-card h5 {
  font-size: 20px;
  margin: 0;
  color: #343a40;
}

.cursor-pointer {
  cursor: pointer;
}

.small-icon {
  font-size: 0.75rem;
}

.text-muted {
  color: #6c757d !important;
}
</style>

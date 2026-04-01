<template>
  <div class="d-flex justify-content-between align-items-center mb-4">
    <h2 class="section-title">
      <i class="bi bi-diagram-3 me-2"></i>
      Organizations & Resources
    </h2>
    <div class="actions-group" v-if="props.isAdmin">
      <primaryButton :loading="props.loading" @click="$emit('createOrganization')">
        <i class="bi bi-plus-circle me-2 v-step-governance-3"></i>
        Create {{ $t('governancePage.organisations') }}
      </primaryButton>
    </div>
  </div>
</template>

<script setup>
import PrimaryButton from '@/components/ui/buttons/PrimaryButton.vue'
const props = defineProps({
  isAdmin: {
    type: Boolean,
    default: false,
  },
  loading: {
    type: Boolean,
    required: true,
  },
})

defineEmits(['createOrganization'])
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

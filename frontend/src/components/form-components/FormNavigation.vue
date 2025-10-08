<template>
  <div class="form-navigation border-end border-border pe-2">
    <div class="sticky-top">
      <h3>
        {{ formNavigationTittle }}
      </h3>
      <div class="navigation-summary" v-on:click="changeActiveNavIndex(0)">
        <div
          :class="activeNavItemIndex === 0 ? 'form-navigation-item-active' : ''"
          class="form-navigation-item py-3 d-flex flex-row align-items-center py-2 my-3 rounded-2"
        >
          <div class="form-navigation-item-number mx-3">
            <div class="avatar --bs-secondary-bg text-info avatar-background">
              <i class="fs-5 bi bi-info-circle" />
            </div>
          </div>
          <div class="form-navigation-item-text d-flex flex-column">
            <p class="fw-bold mb-0">Request Parameters</p>
          </div>
        </div>
      </div>
      <div
        v-for="(item, index) in navItems"
        v-on:click="changeActiveNavIndex(index + 1)"
        :key="item.id"
        :class="activeNavItemIndex === index + 1 ? 'form-navigation-item-active' : ''"
        class="form-navigation-item d-flex flex-row align-items-center py-2 my-3 rounded-2"
      >
        <div class="form-navigation-item-avatar mx-3">
          <div class="avatar" :class="returnAvatarColor(validationErrorHighlight[item.name])">
            <i
              v-if="validationErrorHighlight[item.name]?.length > 0"
              class="fs-5 bi bi-exclamation-circle"
            />
            <i v-else-if="validationErrorHighlight[item.name]" class="fs-5 bi bi-check-circle" />
            <p v-else class="fs-5 mb-0 tw-2">{{ index + 1 }}</p>
          </div>
        </div>
        <div class="form-navigation-item-text d-flex flex-column pe-3">
          <p class="fw-bold mb-0">{{ item.label }}</p>
          <p class="mb-0">{{ item.description }}</p>
        </div>
      </div>
      <div class="navigation-overview" v-on:click="changeActiveNavIndex(navItems.length + 1)">
        <div
          :class="isLastItemActive ? 'form-navigation-item-active' : ''"
          class="form-navigation-item d-flex flex-row align-items-center py-2 my-3 rounded-2"
        >
          <div class="form-navigation-item-number mx-3">
            <div class="avatar --bs-secondary-bg text-info avatar-background">
              <i class="fs-5 bi bi-info-circle" />
            </div>
          </div>
          <div class="form-navigation-item-text d-flex flex-column">
            <p class="fw-bold mb-0">Overview</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  formNavigationTittle: {
    type: String,
    required: false,
    default: 'Sections',
  },
  navItems: {
    type: Array,
    required: false,
    default: null,
  },
  validationErrorHighlight: {
    type: Object,
    required: false,
    default: () => ({}),
  },
})

const activeNavItemIndex = defineModel('activeNavItemIndex')

const isLastItemActive = computed(() => {
  return activeNavItemIndex.value === props.navItems?.length + 1
})

function changeActiveNavIndex(index) {
  activeNavItemIndex.value = index
}

function returnAvatarColor(validationErrorHighlightItem) {
  if (validationErrorHighlightItem) {
    if (validationErrorHighlightItem?.length > 0) {
      return 'bg-danger text-white'
    } else {
      return 'avatar-background-success text-white'
    }
  }
  return 'avatar-background'
}
</script>

<style scoped>
.form-navigation {
  width: 25%;
}
@media screen and (max-width: 480px) {
  .form-navigation {
    width: 100%;
  }
  .border-end {
    border-right: none !important;
  }
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s ease;
}

.avatar i {
  font-size: 1.125rem;
  line-height: 1;
}

.avatar p {
  line-height: 1;
  margin: 0;
  font-weight: 600;
  font-size: 1rem;
}

.avatar-background {
  background-color: #e1e4e8;
  color: #6c757d;
}

.avatar-background-success {
  background-color: #28a745;
  box-shadow: 0 2px 4px rgba(40, 167, 69, 0.2);
}

.form-navigation-item:hover .avatar {
  transform: scale(1.05);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-navigation-item-active .avatar {
  box-shadow: 0 0 0 3px rgba(13, 110, 253, 0.15);
}

.bg-danger {
  box-shadow: 0 2px 4px rgba(220, 53, 69, 0.2);
}

.form-navigation-item-active {
  background-color: #eff6ff;
  border-left: 3px solid #0d6efd;
  padding-left: calc(0.5rem - 3px);
}

.form-navigation-item {
  cursor: pointer;
  transition: all 0.2s ease;
  border-left: 3px solid transparent;
}

.form-navigation-item:hover {
  background-color: #f8f9fa;
}

.form-navigation-item-text p:first-child {
  font-size: 0.9375rem;
  color: #212529;
}

.form-navigation-item-text p:last-child {
  font-size: 0.8125rem;
  color: #6c757d;
  margin-top: 2px;
}
</style>

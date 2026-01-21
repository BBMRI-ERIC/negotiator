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
              <i class="bi bi-info-circle" />
            </div>
          </div>
          <div class="form-navigation-item-text d-flex flex-column">
            <p class="fw-bold mb-0">Resources</p>
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
              class="bi bi-exclamation-circle"
            />
            <i v-else-if="validationErrorHighlight[item.name]" class="bi bi-check-circle" />
            <p v-else class="mb-0">{{ index + 1 }}</p>
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
              <i class="bi bi-info-circle" />
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
      return 'avatar-background-danger'
    } else {
      return 'avatar-background-success'
    }
  }
  return 'avatar-background'
}
</script>

<style scoped>
.form-navigation {
  width: 30%;
  padding-right: 1.5rem;
}

.form-navigation * {
  outline: none !important;
  box-shadow: none !important;
}

.form-navigation *:focus {
  outline: none !important;
  box-shadow: none !important;
}

.form-navigation *:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}

.form-navigation h3 {
  font-size: 1.75rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
}

@media screen and (max-width: 480px) {
  .form-navigation {
    width: 100%;
  }
  .border-end {
    border-right: none !important;
  }
}

.navigation-summary,
.navigation-overview {
  outline: none !important;
  box-shadow: none !important;
}

.navigation-summary:focus,
.navigation-overview:focus,
.navigation-summary:focus-visible,
.navigation-overview:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}

.avatar {
  width: auto;
  height: auto;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  outline: none !important;
  box-shadow: none !important;
  background: transparent;
}

.avatar:focus,
.avatar:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}

.avatar i {
  font-size: 2.25rem;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  background: transparent !important;
}

.avatar p {
  line-height: 1;
  margin: 0;
  padding: 0;
  font-weight: 700;
  font-size: 1.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-width: 2rem;
  background: transparent !important;
}

.avatar-background i {
  color: #6c757d !important;
}

.avatar-background p {
  color: #6c757d !important;
}

.avatar-background-success i {
  color: #28a745 !important;
}

.avatar-background-success p {
  color: #28a745 !important;
}

.avatar-background-danger i {
  color: #dc3545 !important;
}

.avatar-background-danger p {
  color: #dc3545 !important;
}

.bg-danger i {
  color: #dc3545 !important;
  background: transparent !important;
}

.bg-danger p {
  color: #dc3545 !important;
  background: transparent !important;
}

.form-navigation-item-active .avatar {
  /* No special styling for active state */
}
.form-navigation-item-active {
  background-color: #eff6ff;
  border-left: 4px solid #0d6efd;
  padding-left: calc(1rem - 4px);
  padding-top: 1rem;
  padding-bottom: 1rem;
}

.form-navigation-item {
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border-left: 4px solid transparent;
  padding: 1rem 0.5rem;
  margin: 1rem 0;
  outline: none !important;
  box-shadow: none !important;
}

.form-navigation-item:focus,
.form-navigation-item:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}

.form-navigation-item:hover {
  background-color: #f8f9fa;
  transform: translateX(2px);
}


.form-navigation-item-text {
  padding-right: 1rem;
}

.form-navigation-item-text p:first-child {
  font-size: 1.125rem;
  font-weight: 600;
  color: #212529;
  line-height: 1.4;
  margin-bottom: 0.25rem;
  transition: color 0.2s ease;
}

.form-navigation-item-text p:last-child {
  font-size: 1rem;
  color: #6c757d;
  line-height: 1.4;
  margin-top: 0.25rem;
  transition: color 0.2s ease;
}

.form-navigation-item:hover .form-navigation-item-text p:first-child {
  color: #0d6efd;
}

.form-navigation-item-avatar {
  margin: 0 1rem;
}

@media (max-width: 768px) {
  .form-navigation h3 {
    font-size: 1.5rem;
  }

  .avatar i {
    font-size: 1.875rem;
  }

  .avatar p {
    font-size: 1.25rem;
  }

  .form-navigation-item-text p:first-child {
    font-size: 1rem;
  }

  .form-navigation-item-text p:last-child {
    font-size: 0.9rem;
  }

  /* Reduce animations on mobile for performance */
  .form-navigation-item:hover {
    transform: none;
  }

  .form-navigation-item:hover .avatar i,
  .form-navigation-item:hover .avatar p {
    transform: scale(1.03);
  }
}

/* Reduce motion for users who prefer it */
@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>

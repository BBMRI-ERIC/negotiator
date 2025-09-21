<template>
  <div class="form-navigation w-25 border-end border-border pe-2">
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
            <p class="fw-bold mb-0">Request summary</p>
          </div>
        </div>
      </div>
      <div
        v-for="(item, index) in navItems"
        v-on:click="changeActiveNavIndex(index + 1)"
        :key="item.id"
        :class="activeNavItemIndex === index + 1 ? 'form-navigation-item-active' : ''"
        class="form-navigation-items d-flex flex-row align-items-center py-2 my-3 rounded-2"
      >
        <div class="form-navigation-item form-navigation-item-avatar mx-3">
          <div
            class="avatar"
            :class="
              validationErrorHighlight[item.name]?.length > 0
                ? 'bg-danger text-white'
                : 'avatar-background text-info'
            "
          >
            <i
              v-if="validationErrorHighlight[item.name]?.length > 0"
              class="fs-5 bi bi-exclamation-circle"
            />
            <p v-else class="fs-5 mb-0 tw-2">{{ index + 1 }}</p>
          </div>
        </div>
        <div class="form-navigation-item form-navigation-item-text d-flex flex-column pe-3">
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
              <i class="fs-5 bi bi-file-text" />
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
    default: 'Form Sections',
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
</script>

<style scoped>
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 50px;
}

.avatar-background {
  background-color: #e1e4e8;
}

.avatar-icon {
  width: 20px;
  height: 20px;
}

.avatar > p {
  margin-top: 5px;
}

.avatar > i {
  margin-top: 5px;
}

.form-navigation-item-active {
  background-color: #eff6ff;
}

.form-navigation-item {
  cursor: pointer;
}
</style>

<template>
  <ul class="nav nav-tabs">
    <li class="nav-item tab cursor-pointer">
      <a
        class="nav-link"
        :class="{ active: currentTab === 'overview' }"
        @click="$emit('update:currentTab', 'overview')"
      >
        Overview
      </a>
    </li>
    <li class="nav-item tab cursor-pointer">
      <a
        class="nav-link"
        :class="{ active: currentTab === 'negotiations' }"
        @click="$emit('update:currentTab', 'negotiations')"
      >
        Negotiations
      </a>
    </li>
    <li class="nav-item tab cursor-pointer">
      <a
        class="nav-link"
        :class="{ active: currentTab === 'organizations' }"
        @click="$emit('update:currentTab', 'organizations')"
      >
        Organizations
      </a>
    </li>
  </ul>
</template>

<script setup>
defineProps({
  currentTab: {
    type: String,
    required: true,
  },
})

defineEmits(['update:currentTab'])
</script>

<template>
  <button :id="id" class="btn btn-sm py-0" :style="{ color }" type="button" @click="onClick">
    <template v-if="filtersSortData.sortBy === sortKey">
      <i :class="iconClass" />
    </template>
    <template v-else>
      <i :class="iconAsc" />
      <i :class="iconDesc" />
    </template>
  </button>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  sortKey: { type: String, required: true },
  filtersSortData: { type: Object, required: true },
  color: { type: String, default: undefined },
  iconAsc: { type: String, required: true },
  iconDesc: { type: String, required: true },
  defaultIcon: { type: String, required: true },
  id: { type: String, default: undefined },
})

const emit = defineEmits(['sort'])

const iconClass = computed(() => {
  if (
    props.filtersSortData.sortDirection === 'ASC' &&
    props.filtersSortData.sortBy === props.sortKey
  ) {
    return props.iconAsc
  } else if (props.filtersSortData.sortBy === props.sortKey) {
    return props.iconDesc
  }
  return props.defaultIcon
})

function onClick() {
  emit('sort', props.sortKey)
}
</script>

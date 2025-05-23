<template>
  <div v-if="combinedItems.length" class="timeline mb-5 ps-3 position-relative">
    <div class="position-relative ms-3" style="min-height: 100%;">
      <div
        class="timeline-line position-absolute mt-3"
        style="
          width: 2px;
          top: 0;
          bottom: 0;
          left: 10px;
          background-color: rgba(108, 117, 125, 1);
          z-index: 0;
        "
      ></div>
      <TimelineItem
        v-for="item in combinedItems"
        :key="item.id"
        :item="item"
        :ui-configuration="uiConfiguration"
        :organizations="organizations"
        :negotiation="negotiation"
      />
    </div>
  </div>
</template>

<script setup>
import TimelineItem from './TimelineItem.vue'

defineProps({
  combinedItems: Array,
  uiConfiguration: Object,
  organizations: Object,
  negotiation: Object,
})
</script>
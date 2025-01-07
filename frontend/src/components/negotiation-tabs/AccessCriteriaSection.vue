<template>
  <div
      v-for="criteria in elements"
      :key="criteria.name"
      class="mb-4 ms-3 me-3"
  >
    <label
        v-if="criteria.required"
        class="form-label required"
    >{{ criteria.name }}
    </label>
    <label
        v-else
        class="form-label"
    >{{ criteria.name }} </label>
    <textarea
        v-if="criteria.type === 'textarea'"
        class="form-control"
        :required="criteria.required"
    />
    <input
        v-else
        :type="criteria.type"
        :placeholder="criteria.description"
        class="form-control"
        :required="criteria.required"
    >
  </div>
</template>

<script setup>
const props = defineProps({
  elements: {
    type: Array,
    default() {
      return []
    }
  }
})
</script>

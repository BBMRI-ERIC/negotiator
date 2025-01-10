<template>
  <div v-for="(item, index) in uiConfigurationCategoriesModel" :key="index" class="col-4 pb-3">
    <div>{{ transformNameOfVariable(index) }}:</div>
    <input
      v-if="returnValueType(item) === 'text'"
      type="text"
      v-model="uiConfigurationCategoriesModel[index]"
      class="form-control"
    />
    <input
      v-if="returnValueType(item) === 'color'"
      v-model="uiConfigurationCategoriesModel[index]"
      type="color"
      class="form-control form-control-color mt-1"
      id="myColor"
      title="Choose a color"
    />
    <div v-if="returnValueType(item) === 'boolean'" class="d-flex flex-row mt-2">
      <label class="pe-2 text-muted"> hiden </label>
      <div class="form-check form-switch">
        <input
          class="form-check-input"
          v-model="uiConfigurationCategoriesModel[index]"
          type="checkbox"
          role="switch"
          id="flexSwitchCheckDefault"
        />
        <label class="form-check-label" for="flexSwitchCheckDefault">visible</label>
      </div>
    </div>
  </div>
</template>

<script setup>
const uiConfigurationCategoriesModel = defineModel()

function returnValueType(value) {
  if (typeof value == 'boolean') {
    return 'boolean'
  } else if (CSS.supports('color', value)) {
    return 'color'
  }
  return 'text'
}

function transformNameOfVariable(name) {
  if (name) {
    const nameWithSpacing = name.replace(/([A-Z])/g, ' $1').trim()
    const firstLateToUppercase =
      String(nameWithSpacing).charAt(0).toUpperCase() + String(nameWithSpacing).slice(1)
    return firstLateToUppercase
  }
  return ''
}
</script>

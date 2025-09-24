<template>
  <div class="access-form-overview">
    <div class="overview">
      <h2 class="mb-3">Overview*</h2>
      <p class="mb-0 mb-5">
        Upon confirmation, your request will undergo content review. Our reviewers may contact you
        via email for further details. Upon approval, the respective biobanks you wish to contact
        will be notified of your request. Please click 'Submit request' and then 'Confirm' to
        proceed.
      </p>
    </div>

    <div
      class="section mb-5"
      v-for="(section, sectionIndex) in accessFormWithPayload?.sections"
      :key="section"
    >
      <h2>
        {{ section.label.toUpperCase() }}
      </h2>

      <div
        class="element mt-2"
        v-for="(element, elementIndex) in section?.elements"
        :key="element.id"
        @click="
          $emit('emitErrorElementIndex', {
            elementId: element?.id,
            elementIndex: elementIndex,
            sectionIndex: sectionIndex,
          })
        "
      >
        <p class="element-text mb-0">
          <span class="fw-bold">{{ element.label }}: </span>

          <span
            v-if="
              element.required &&
              (element.value === '' ||
                element.value === null ||
                (Array.isArray(element.value) && element.value.length === 0))
            "
            class="invalid-text"
          >
            this field is required <i class="bi bi-exclamation-circle"></i>
          </span>
          <span v-else-if="isAttachment(element.value)" class="text-truncate">
            <span v-if="element.value.name" :title="element.name">{{ element.value.name }}</span>
            <span v-for="(choice, index) in element.value" v-else :key="index">
              {{ choice }}<span v-if="index < element.value.length - 1">, </span>
            </span>
          </span>
          <span v-else>
            {{ translateTrueFalse(element.value) }}
          </span>
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  accessFormWithPayload: {
    type: Object,
    required: true,
    default: () => {},
  },
  validationErrorHighlight: {
    type: Object,
    required: true,
    default: () => {},
  },
})

function isAttachment(value) {
  return value instanceof File || value instanceof Object
}

function translateTrueFalse(value) {
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  return value
}
</script>

<style scoped>
.invalid-text {
  font-size: 0.875em;
  color: var(--bs-form-invalid-color);
}

.element-text {
  cursor: pointer;
}
</style>

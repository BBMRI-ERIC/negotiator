<template>
  <div class="access-form-section d-flex flex-column">
    <div class="section mb-5">
      <h2 class="fw-bold">
        {{ accessFormWithPayloadSection.label }}
      </h2>
      <p class="mb-0">{{ accessFormWithPayloadSection.description }}</p>
    </div>

    <div
      class="access-form-section-elements mb-3"
      v-for="(element, index) in accessFormWithPayloadSection.elements"
      :key="element.name"
      :class="
        element.required === true &&
        focusElementId === element.id &&
        validationErrorHighlight.includes(element.id)
          ? 'border border-danger rounded p-3'
          : focusElementId === element.id
            ? 'border border-border-color rounded p-3'
            : ''
      "
    >
      <label class="form-label" :class="{ required: element.required }">
        {{ element.label }}
      </label>
      <span v-if="element.description" class="ms-2 text-muted">
        <i
          class="py-1 bi bi-info-circle"
          data-bs-toggle="tooltip"
          :data-bs-title="element.description"
        />
      </span>

      <div
        class="access-form-section-elements mb-3"
        v-on:focusout="handleFocusOutEvent()"
        v-on:focusin="$emit('elementFocusInEvent')"
      >
        <div v-if="element.type === 'TEXT'">
          <input
            v-model="element.value"
            :type="element.type"
            :placeholder="element.description"
            class="form-control text-secondary-text"
            :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
            :required="element.required"
          />
        </div>

        <div v-else-if="element.type === 'BOOLEAN'">
          <div class="form-check form-check-inline">
            <input
              id="inlineRadio1"
              v-model="element.value"
              value="Yes"
              :required="element.required"
              class="form-check-input"
              :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
              type="radio"
            />
            <label class="form-check-label" for="inlineRadio1"> Yes </label>
          </div>
          <div class="form-check form-check-inline">
            <input
              id="inlineRadio2"
              v-model="element.value"
              value="No"
              :required="element.required"
              class="form-check-input"
              :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
              type="radio"
            />
            <label class="form-check-label" for="inlineRadio2"> No </label>
          </div>
        </div>

        <div v-else-if="element.type === 'MULTIPLE_CHOICE'">
          <div
            v-for="(value, index) in negotiationValueSets[element.id]?.availableValues"
            :key="index"
          >
            <div class="form-check form-check-inline">
              <input
                id="inlineCheckbox1"
                v-model="element.value"
                :value="value"
                :required="element.required"
                class="form-check-input"
                :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
                type="checkbox"
              />
              <label class="form-check-label" for="inlineCheckbox1">{{ value }}</label>
            </div>
          </div>
          <div v-if="element.externalDocumentation && element?.externalDocumentation !== 'none'">
            <span class="text-muted"> External Documentation - </span>
            <a
              :href="element?.externalDocumentation"
              :style="{ color: uiConfiguration?.linksTextColor }"
            >
              {{ element?.externalDocumentation }}
            </a>
          </div>
        </div>

        <div v-else-if="element.type === 'SINGLE_CHOICE'">
          <div
            v-for="(value, index) in negotiationValueSets[element.id]?.availableValues"
            :key="index"
          >
            <div class="form-check form-check-inline">
              <input
                id="inlineRadio1"
                v-model="element.value"
                :value="value"
                :required="element.required"
                class="form-check-input"
                :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
                type="radio"
                @click="element.value == value ? (element.value = '') : (element.value = value)"
              />
              <label class="form-check-label" for="inlineRadio1">{{ value }}</label>
            </div>
          </div>
        </div>

        <div v-else-if="element.type === 'TEXT_LARGE'">
          <textarea
            v-model="element.value"
            :placeholder="element.description"
            class="form-control text-secondary-text"
            :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
            :required="element.required"
          />
        </div>

        <div v-else-if="element.type === 'NUMBER'" class="col-5">
          <input
            v-model="element.value"
            :type="element.type"
            :placeholder="element.description"
            class="form-control text-secondary-text"
            :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
            :required="element.required"
            @keypress="isNumber($event)"
          />
        </div>

        <div v-else-if="element.type === 'FILE'">
          <label
            v-if="element.value?.name"
            class="form-label text-primary-text text-truncate w-100"
            :title="element.name"
          >
            Uploaded file: {{ element.value.name }}
          </label>
          <input
            :accept="fileExtensions"
            class="form-control text-secondary-text"
            :required="element.required"
            :placeholder="element.description"
            :type="element.type"
            @change="handleFileUpload($event, index)"
          />
        </div>

        <div v-else-if="element.type === 'DATE'" class="w-25">
          <p v-if="element.description" class="text-muted">
            {{ element.description }}
          </p>
          <input
            id="startDate"
            v-model="element.value"
            value=""
            class="form-control form-control-sm"
            :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
            type="date"
          />
        </div>

        <div v-else-if="element.type === 'INFORMATION'">
          <p v-if="element.description" class="text-muted">
            {{ element.value || element.description }}
          </p>
        </div>

        <input
          v-else
          v-model="element.value"
          :type="element.type"
          :placeholder="element.description"
          class="form-control text-secondary-text"
          :class="validationErrorHighlight?.includes(element.id) ? 'is-invalid' : ''"
        />

        <div
          v-if="validationErrorHighlight && validationErrorHighlight.includes(element.id)"
          class="invalid-text"
        >
          {{ transformMessage(element.type) }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useNegotiationFormStore } from '../../store/negotiationForm'
import { useNotificationsStore } from '../../store/notifications'

import fileExtensions from '@/config/uploadFileExtensions.js'
import { isFileExtensionsSupported } from '../../composables/utils.js'

const accessFormWithPayloadSection = defineModel('accessFormWithPayloadSection')
const negotiationReplacedAttachmentsID = defineModel('negotiationReplacedAttachmentsID')

const negotiationFormStore = useNegotiationFormStore()
const notificationsStore = useNotificationsStore()

const props = defineProps({
  validationErrorHighlight: {
    type: Array,
    required: false,
    default: null,
  },
  existingAttachments: {
    type: Object,
    required: false,
    default: null,
  },
  negotiationAttachments: {
    type: Array,
    required: false,
    default: null,
  },
  focusElementId: {
    type: Number,
    required: false,
    default: null,
  },
})

onMounted(() => {
  accessFormWithPayloadSection.value.elements.forEach((element) => {
    if (element.type === 'MULTIPLE_CHOICE' || element.type === 'SINGLE_CHOICE') {
      getValueSet(element.id)
    }
  })
})

const negotiationValueSets = ref({})

async function getValueSet(id) {
  await negotiationFormStore.retrieveDynamicAccessFormsValueSetByID(id).then((res) => {
    negotiationValueSets.value[id] = res
  })
}

function handleFileUpload(event, indexOfElement) {
  if (
    isFileExtensionsSupported(event.target.files[0]) &&
    !isSameFile(
      accessFormWithPayloadSection.value.elements[indexOfElement].value,
      event.target.files[0],
    ) &&
    !isAttachmentPresentInNegotiation(event.target.files[0])
  ) {
    negotiationReplacedAttachmentsID.value.push(
      accessFormWithPayloadSection.value.elements[indexOfElement].value?.id || null,
    )
    accessFormWithPayloadSection.value.elements[indexOfElement].value = event.target.files[0]
  } else {
    accessFormWithPayloadSection.value.elements[indexOfElement].value = null
  }
}

function isSameFile(file, newFile) {
  let isNameSame = false
  let isSizeSame = false

  if (file && file.name === newFile.name) {
    isNameSame = true
  }
  if (file && file.size === newFile.size) {
    isSizeSame = true
  }
  if (isNameSame && isSizeSame) {
    notificationsStore.setNotification(
      'Attachment already exists with the same name and size, please select a different file or rename the file',
      'danger',
    )
    return true
  }
  return false
}

function isAttachmentPresentInNegotiation(newFile) {
  let isAttachmentPresent = false
  props.negotiationAttachments.forEach((element) => {
    if (isSameFile(element, newFile)) {
      isAttachmentPresent = true
    }
  })
  return isAttachmentPresent
}

function transformMessage(text) {
  if (text == 'SINGLE_CHOICE' || text == 'BOOLEAN') {
    return 'Please select one of available values'
  } else if (text == 'MULTIPLE_CHOICE') {
    return 'Please select at least one of the available values'
  } else if (text == 'TEXT_LARGE') {
    return 'Please provide a text'
  } else {
    return 'Please provide a ' + text?.toLowerCase()
  }
}

const emit = defineEmits(['elementFocusOutEventValidation', 'elementFocusOutEvent'])

function handleFocusOutEvent() {
  emit('elementFocusOutEvent')
  if (props.validationErrorHighlight?.length > 0) {
    emit('elementFocusOutEventValidation')
  }
}
</script>

<style scoped>
.required:after {
  content: '  *\00a0';
  color: red;
}

.invalid-text {
  width: 100%;
  margin-top: 0.25rem;
  font-size: 0.875em;
  color: var(--bs-form-invalid-color);
}
</style>

<template>
  <div
    class="modal"
    :class="{ fade: fade }"
    tabindex="-1"
    :aria-labelledby="`${id}Label`"
    aria-hidden="true"
  >
    <div
      class="modal-dialog modal-dialog-centered"
      :class="isModalSmall === true ? 'modal-m' : 'modal-xl'"
    >
      <div class="modal-content">
        <div class="modal-header">
          <button
            v-if="isXButtondisplayed"
            type="button"
            class="btn-close"
            data-bs-dismiss="modal"
            aria-label="Close"
          />
        </div>
        <div class="modal-body text-left mb-3 mx-3">
          <div class="fs-3 mb-4 fw-bold text-secondary text-center">Submitted Information</div>
          <div>
            <ul class="ps-0">
              <li
                v-for="(element, key) in props.payload"
                :key="element"
                class="list-group-item p-3"
              >
                <span class="fs-5 fw-bold text-primary-text mt-3"> {{ key }}</span>
                <div v-for="(subelement, subelementkey) in element" :key="subelement" class="mt-3">
                  <label class="me-2 fw-bold text-secondary-text">{{ subelementkey }}:</label>
                  <span v-if="isAttachment(subelement)">
                    <span v-if="subelement.name" class="d-flex col">
                      <span class="text-truncate" :title="subelement.name">{{
                        subelement.name
                      }}</span>
                      <font-awesome-icon
                        v-if="isAttachment(subelement)"
                        class="ms-1 cursor-pointer"
                        icon="fa fa-download"
                        fixed-width
                        @click.prevent="downloadAttachment(subelement.id, subelement.name)"
                      />
                    </span>
                    <span v-else>
                      <div v-for="(choice, index) in subelement" :key="index">
                        {{ choice }}
                      </div>
                    </span>
                  </span>
                  <span v-else class="text-break">
                    {{ translateTrueFalse(subelement) }}
                  </span>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { Tooltip } from 'bootstrap'
import 'vue3-form-wizard/dist/style.css'
import { useNegotiationPageStore } from '../../store/negotiationPage.js'

const negotiationPageStore = useNegotiationPageStore()

const props = defineProps({
  payload: {
    type: Object,
    required: true,
  },
  isModalSmall: {
    type: Boolean,
    required: false,
    default: true,
  },
  isXButtondisplayed: {
    type: Boolean,
    required: false,
    default: true,
  },
})

function translateTrueFalse(value) {
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No'
  }
  return value
}

onMounted(() => {
  new Tooltip(document.body, {
    selector: "[data-bs-toggle='tooltip']",
  })
})

function isAttachment(value) {
  return value instanceof Object
}

function downloadAttachment(id, name) {
  negotiationPageStore.downloadAttachment(id, name)
}
</script>

<style scoped>
.required:after {
  content: '  *\00a0';
  color: red;
}

.bi:hover {
  color: #7c7c7c;
}

.invalidText {
  width: 100%;
  margin-top: 0.25rem;
  font-size: 0.875em;
  color: var(--bs-form-invalid-color);
}
</style>

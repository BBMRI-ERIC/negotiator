<template>
  <div class="specific-area panel panel-default border-">
    <h2 class="mb-1 text-left">Information Requirements</h2>
    <div class="text-muted mb-3">
      Setup an information requirement for all Negotiations. A Representative of each Resource in
      a Negotiation will be asked to supply additional information before advancing to a chosen
      state.
    </div>
    <h4>Create:</h4>
    <div class="col-8">
      <div class="d-flex flex-row justify-content-between mb-3">
        Linked access form:
        <div class="col-3">
          <select
            v-model="selectedAccessForm"
            class="form-select form-select-sm btn-outline-sort-filter-button-outline"
          >
            <option disabled value="">Select a form...</option>
            <option v-for="(form, index) in accessForms" :key="index" :value="form">
              {{ form.name }}
            </option>
          </select>
        </div>
      </div>
      <div class="d-flex flex-row justify-content-between mb-3">
        Linked lifecycle event:
        <div class="col-3">
          <select
            v-model="selectedEvent"
            class="form-select form-select-sm btn-outline-sort-filter-button-outline"
          >
            <option disabled value="">Select an event...</option>
            <option v-for="(event, index) in resourceAllEvents" :key="index" :value="event">
              {{ event.label }}
            </option>
          </select>
        </div>
      </div>
      <div class="d-flex flex-row justify-content-between">
        Should only an Administrator see the summary:
        <div class="col-3">
          <select
            v-model="summaryOnlyForAdmin"
            class="form-select form-select-sm btn-outline-sort-filter-button-outline"
          >
            <option value="true" selected>Yes</option>
            <option value="false">No</option>
          </select>
        </div>
      </div>
    </div>

    <div class="d-flex flex-row" />
    <div class="col-2 my-4">
      <button class="btn btn-sm bg-new-request-button-color" @click="setInfoRequirements()">
        <span class="text-white">Save</span>
      </button>
    </div>

    <div class="row row-cols-1 row-cols-md-2 d-grid-row" />
    <h4>
      Saved requirements:
      {{
        infoRequirements?.['info-requirements']
          ? infoRequirements?.['info-requirements'].length
          : 0
      }}
    </h4>
    <div class="table">
      <table v-if="infoRequirements" class="table">
        <thead>
        <tr>
          <th scope="col">#</th>
          <th scope="col">Access-form id</th>
          <th scope="col">Acces-form name</th>
          <th scope="col">For event</th>
          <th scope="col">Only for Admin</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="(requirement, index) in infoRequirements['info-requirements']" :key="index">
          <th scope="row">
            {{ requirement.id }}
          </th>
          <td class="text-muted">
            {{ requirement.requiredAccessForm.id }}
          </td>
          <td class="text-muted">
            {{ requirement.requiredAccessForm.name }}
          </td>
          <td class="text-muted">
            {{ requirement.forResourceEvent }}
          </td>
          <td class="text-muted">
            {{ requirement.viewableOnlyByAdmin }}
          </td>
        </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

defineProps({
  resourceAllEvents: {
    type: Object,
    required: true
  },
  infoRequirements: {
    type: Array,
    required: true
  },
  accessForms: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['set-info-requirements'])

const selectedAccessForm = ref({})
const selectedEvent = ref({})
const summaryOnlyForAdmin = ref(true)

function setInfoRequirements() {
  const data = {
    requiredAccessFormId: selectedAccessForm.value.id,
    forResourceEvent: selectedEvent.value.value,
    viewableOnlyByAdmin: summaryOnlyForAdmin.value
  }
  emit('set-info-requirements', data)
}
</script>
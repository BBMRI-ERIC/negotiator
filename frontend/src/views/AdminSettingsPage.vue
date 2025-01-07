<template>
  <div v-if="!isLoading">
    <h1 class="mb-5 text-center">
      Administrator Console
    </h1>
    <hr>
    <div class="specific-area panel panel-default border-">
      <h2 class="mb-1 text-left">
        Information Requirements
      </h2>
      <div class="text-muted mb-3">
        Setup an information requirement for all Negotiations. A Representative of each Resource in a Negotiation will
        be asked to supply additional information before advancing to a chosen state.
      </div>
      <h4>
        Create:
      </h4>
      <div class="col-8">
        <div class="d-flex flex-row justify-content-between mb-3">
          Linked access form:
          <div class="col-3">
            <select
              v-model="selectedAccessForm"
              class="form-select form-select-sm btn-outline-sort-filter-button-outline"
            >
              <option
                disabled
                value=""
              >
                Select a form...
              </option>
              <option
                v-for="(form, index) in accessForms"
                :key="index"
                :value="form"
              >
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
              <option
                disabled
                value=""
              >
                Select an event...
              </option>
              <option
                v-for="(event, index) in resourceAllEvents"
                :key="index"
                :value="event"
              >
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
              <option
                value="true"
                selected
              >
                Yes
              </option>
              <option value="false">
                No
              </option>
            </select>
          </div>
        </div>
      </div>

      <div class="d-flex flex-row" />
      <div class="col-2 my-4">
        <button
          class="btn btn-sm bg-new-request-button-color"
          @click="setInfoRequirements()"
        >
          <span class="text-white">Save</span>
        </button>
      </div>

      <div class="row row-cols-1 row-cols-md-2 d-grid-row" />
      <h4>
        Saved requirements:
        {{ infoRequirements?.['info-requirements'] ? infoRequirements?.['info-requirements'].length : 0 }}
      </h4>
      <div
        class="table"
      >
        <table
          v-if="infoRequirements"
          class="table"
        >
          <thead>
          <tr>
            <th scope="col">
              #
            </th>
            <th scope="col">
              Access-form id
            </th>
            <th scope="col">
              Acces-form name
            </th>
            <th scope="col">
              For event
            </th>
            <th scope="col">
              Only for Admin
            </th>
          </tr>
          </thead>
          <tbody>
          <tr
              v-for="(requirement, index) in infoRequirements['info-requirements']"
              :key="index"
          >
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
  </div>
  <div
    v-else
    class="d-flex justify-content-center flex-row"
  >
    <div class="d-flex justify-content-center">
      <div
        class="spinner-border d-flex justify-content-center "
        role="status"
      />
      <div class="d-flex justify-content-center">
        <h4 class="mb-3 ms-3">
          Loading ...
        </h4>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useUserStore } from '../store/user.js'
import { useAdminStore } from '../store/admin.js'
import { useFormsStore } from '../store/forms.js'

const userStore = useUserStore()
const adminStore = useAdminStore()
const formsStore = useFormsStore()

const resourceAllEvents = ref({})
const infoRequirements = ref([])
const accessForms = ref([])
const selectedAccessForm = ref({})
const selectedEvent = ref({})
const summaryOnlyForAdmin = ref(true)
const isLoading = ref(true)

onMounted(async () => {
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }

  resourceAllEvents.value = await adminStore.retrieveResourceAllEvents()
  infoRequirements.value = await adminStore.retrieveInfoRequirements()
  accessForms.value = await formsStore.retrieveAllAccessForms()
  selectedEvent.value = resourceAllEvents.value[0]
  selectedAccessForm.value = accessForms.value[0]
  isLoading.value = false
})

async function setInfoRequirements() {
  const data = {}
  console.log(selectedEvent.value)
  data.requiredAccessFormId = selectedAccessForm.value.id
  data.forResourceEvent = selectedEvent.value.value
  data.viewableOnlyByAdmin = summaryOnlyForAdmin.value
  console.log(data)
  await adminStore.setInfoRequirements(data)
  infoRequirements.value = await adminStore.retrieveInfoRequirements()
}
</script>

<style scoped>
a:link {
  text-decoration: none;
}
</style>

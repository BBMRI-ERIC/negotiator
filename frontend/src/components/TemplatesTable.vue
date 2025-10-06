<template>
  <button
    ref="openModalResetTemplate"
    hidden
    data-bs-toggle="modal"
    data-bs-target="#resetTemplateModal"
  />
  <confirmation-modal
    id="resetTemplateModal"
    :title="'Reset Template'"
    :text="'Are you sure you want to reset the template?'"
    :message-enabled="false"
    @confirm="resetTemplate()"
  />
  <div v-if="allTemplates">
    <table v-if="otherTemplates.length" class="table table-sm mt-3">
      <thead>
        <tr>
          <th scope="col">ID</th>
          <th scope="col">Name</th>
          <th scope="col"></th>
          <th scope="col"></th>
        </tr>
      </thead>
      <tbody v-for="(templateName, index) in otherTemplates" :key="index">
        <tr>
          <td scope="row">{{ index + 1 }}</td>
          <td>{{ templateName }}</td>
          <td>
            <button type="button" class="btn float-end btn-sm" @click="openTemplate(templateName)">
              <i class="bi bi-cloud-plus"></i>
              Edit Template
            </button>
          </td>
          <td class="col-2">
            <button
              type="button"
              class="btn float-end btn-sm"
              @click="openResetTemplateModal(templateName)"
            >
              <i class="bi bi-arrow-clockwise"></i>
              Reset Template
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="alert alert-light my-3">No Other Templates Available!</div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useTemplates } from '@/store/templates.js'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'

const templateName = defineModel('templateName')
const emailTemplateData = defineModel('templateData')
const allTemplates = defineModel('alllTemplates')

const templateStore = useTemplates()
const openModalResetTemplate = ref(false)
const otherTemplates = computed(() =>
  allTemplates.value.filter(
    (template) => !template.startsWith('EMAIL-') && !template.startsWith('PDF-'),
  ),
)

function openResetTemplateModal(name) {
  openModalResetTemplate.value.click()
  templateName.value = name
}

function openTemplate(name) {
  templateName.value = name
  templateStore.retrieveTemplateByName(name).then((response) => {
    emailTemplateData.value = response
  })
}

function resetTemplate() {
  templateStore.templateReset(templateName.value)
}
</script>

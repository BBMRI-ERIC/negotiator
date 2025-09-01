<template>
  <div class="templates">
    <confirmation-modal
      id="update-template-modal"
      title="Update Template"
      text="Are you sure you want to update the template?"
      :message-enabled="false"
      @confirm="updateTemplate()"
      :ref="updateTemplateModal"
    />
    <h2>Templates</h2>
    <div class="text-muted mb-3">
      In this section, you can select an email template from the list and choose from the following
      options:
      <div class="mt-3">
        <span class="fw-bold">Edit Template: </span>
        Modify the content or settings of the selected email template to suit your needs.
      </div>
      <div>
        <span class="fw-bold"> Reset Template:</span>
        Revert the selected email template to its default state, discarding any changes made.
      </div>
    </div>
    <div v-if="templateData" class="template-edit">
      <button
        type="button"
        class="btn btn-outline-primary btn-sm mb-3"
        @click="returnToTemplateTable()"
      >
        <i class="bi bi-arrow-return-left"></i>
        Retrun to Template Table
      </button>

      <TemplateEditor v-model:templateData="templateData" />

      <button
        type="button"
        class="btn btn-primary float-end btn-sm my-3"
        @click="openUpdateTemplateModal()"
      >
        Update Template
      </button>
    </div>
    <templates-table
      v-else
      v-model:alllTemplates="allTemplates"
      v-model:templateName="templateName"
      v-model:templateData="templateData"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import TemplatesTable from '@/components/TemplatesTable.vue'
import TemplateEditor from '@/components/TemplateEditor.vue'
import { useTemplates } from '@/store/templates.js'
import { Modal } from 'bootstrap'

const templateStore = useTemplates()

const updateTemplateModal = ref(null)
const allTemplates = ref([])
const templateName = ref('')
const templateData = ref('')

onMounted(() => {
  templateStore.retrieveTemplates().then((response) => {
    allTemplates.value = response
  })
})

function returnToTemplateTable() {
  templateData.value = ''
  templateName.value = ''
}

function openUpdateTemplateModal() {
  updateTemplateModal.value = new Modal(document.querySelector('#update-template-modal'))
  updateTemplateModal.value.show()
}

function updateTemplate() {
  templateStore.updateTemplate(templateName.value, templateData.value)
}
</script>

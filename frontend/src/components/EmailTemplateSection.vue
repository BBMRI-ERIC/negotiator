<template>
  <div class="email-templates">
    <confirmation-modal
      id="update-email-template-modal"
      title="Update Email Template"
      text="Are you sure you want to update the email template?"
      :message-enabled="false"
      @confirm="updateEmailTemplate()"
      :ref="updateEmailTemplateModal"
    />
    <h2>Select and Manage Email Templates</h2>
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
    <div v-if="emailTemplateData" class="template-edit">
      <button
        type="button"
        class="btn btn-outline-primary btn-sm mb-3"
        @click="returnToTamplateTable()"
      >
        <i class="bi bi-arrow-return-left"></i>
        Retrun to Template Table
      </button>

      <email-template-editor v-model:emailTemplateData="emailTemplateData" />

      <button
        type="button"
        class="btn btn-primary float-end btn-sm my-3"
        @click="openUpdateEmailTemplateModal()"
      >
        Update Template
      </button>
    </div>
    <email-templates-table
      v-else
      v-model:allEmailTemplates="allEmailTemplates"
      v-model:templateName="templateName"
      v-model:emailTemplateData="emailTemplateData"
    />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'
import EmailTemplatesTable from '@/components/EmailTemplatesTable.vue'
import EmailTemplateEditor from '@/components/EmailTemplateEditor.vue'
import { useEmailTemplates } from '@/store/emailTemplates.js'
import { Modal } from 'bootstrap'

const emailTemplateStore = useEmailTemplates()

const updateEmailTemplateModal = ref(null)
const allEmailTemplates = ref([])
const templateName = ref('')
const emailTemplateData = ref('')

onMounted(() => {
  emailTemplateStore.retrieveEmailTemplates().then((response) => {
    allEmailTemplates.value = response
  })
})

function returnToTamplateTable() {
  emailTemplateData.value = ''
  templateName.value = ''
}

function openUpdateEmailTemplateModal() {
  updateEmailTemplateModal.value = new Modal(document.querySelector('#update-email-template-modal'))
  updateEmailTemplateModal.value.show()
}

function updateEmailTemplate() {
  emailTemplateStore.updateEmailTemplate(templateName.value, emailTemplateData.value)
}
</script>

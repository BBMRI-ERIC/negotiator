<template>
  <button
    ref="openModalResetEmailTemplate"
    hidden
    data-bs-toggle="modal"
    data-bs-target="#resetEmailTemplateModal"
  />
  <confirmation-modal
    id="resetEmailTemplateModal"
    :title="'Reset Email Template'"
    :text="'Are you sure you want to reset the email template?'"
    :message-enabled="false"
    @confirm="resetEmailTemplate()"
  />
  <div v-if="allEmailTemplates">
    <table v-if="allEmailTemplates" class="table table-sm mt-3">
      <thead>
        <tr>
          <th scope="col">#</th>
          <th scope="col">Name</th>
          <th scope="col"></th>
          <th scope="col"></th>
        </tr>
      </thead>
      <tbody v-for="(templateName, index) in allEmailTemplates" :key="index">
        <tr>
          <td scope="row">{{ index + 1 }}</td>
          <td>{{ templateName }}</td>
          <td>
            <button
              type="button"
              class="btn float-end btn-sm"
              @click="openEmailTemplate(templateName)"
            >
              <i class="bi bi-cloud-plus"></i>
              Edit Template
            </button>
          </td>
          <td class="col-2">
            <button
              type="button"
              class="btn float-end btn-sm"
              @click="openResetEmailTemplateModal(templateName)"
            >
              <i class="bi bi-arrow-clockwise"></i>
              Reset Template
            </button>
          </td>
        </tr>
      </tbody>
    </table>
    <div v-else class="alert alert-light my-5">Email templates unavailable!</div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useEmailTemplates } from '@/store/emailTemplates.js'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'

const templateName = defineModel('templateName')
const emailTemplateData = defineModel('emailTemplateData')
const allEmailTemplates = defineModel('allEmailTemplates')

const emailTemplateStore = useEmailTemplates()
const openModalResetEmailTemplate = ref(false)

function openResetEmailTemplateModal(name) {
  openModalResetEmailTemplate.value.click()
  templateName.value = name
}

function openEmailTemplate(name) {
  templateName.value = name
  emailTemplateStore.retrieveEmailTemplateByName(name).then((response) => {
    emailTemplateData.value = response
  })
}

function resetEmailTemplate() {
  emailTemplateStore.emailTemplateReset(templateName.value)
}
</script>

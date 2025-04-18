<template>
  <div class="email-template">
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
    <button
      ref="openModalUpdateEmailTemplate"
      hidden
      data-bs-toggle="modal"
      data-bs-target="#updateEmailTemplateModal"
    />
    <confirmation-modal
      id="updateEmailTemplateModal"
      :title="'Update Email Template'"
      :text="'Are you sure you want to update the email template?'"
      :message-enabled="false"
      @confirm="updateEmailTemplate()"
    />
    <label class="pb-5 pe-5" :style="{ color: uiConfiguration?.primaryTextColor }">
      <b> Welcome to the email template creation page! </b> Here, you have the power to design and
      save reusable email structures. Our intuitive editor provides you with a flexible and
      user-friendly environment to craft compelling and professional email templates for various
      purposes.
    </label>

    <nav class="navbar navbar-expand-sm bg-body-tertiary">
      <div class="container-fluid">
        <a class="navbar-brand disabled" aria-disabled="true">Choose option</a>
        <button
          class="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navbarNav"
          aria-controls="navbarNav"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarNav">
          <ul class="navbar-nav">
            <li class="nav-item">
              <a
                class="nav-link"
                href="#"
                @click="currentNavbarViewNumber = 0"
                :class="currentNavbarViewNumber === 0 ? 'active' : ''"
              >
                <i class="bi bi-filetype-html"></i>
                Create HTML Email Template
              </a>
            </li>
            <li
              class="nav-item"
              @click="currentNavbarViewNumber = 1"
              :class="currentNavbarViewNumber === 1 ? 'active' : ''"
            >
              <a class="nav-link" href="#">
                <i class="bi bi-copy"></i> Paste existing Email Template code</a
              >
            </li>
            <li
              class="nav-item"
              @click="currentNavbarViewNumber = 2"
              :class="currentNavbarViewNumber === 2 ? 'active' : ''"
            >
              <a class="nav-link" href="#"
                ><i class="bi bi-cloud-plus"></i> Open existing Email Template</a
              >
            </li>
            <li
              class="nav-item"
              @click="currentNavbarViewNumber = 3"
              :class="currentNavbarViewNumber === 3 ? 'active' : ''"
            >
              <a class="nav-link" href="#"><i class="bi bi-arrow-clockwise"></i> Reset Template </a>
            </li>
          </ul>
        </div>
      </div>
    </nav>

    <div class="w-25 row input-group input-group-sm mb-3" v-if="currentNavbarViewNumber !== 2">
      <label class="py-3 pe-5" :style="{ color: uiConfiguration?.primaryTextColor }">
        Template name:
      </label>
      <input
        v-model="templateName"
        placeholder="Enter template name"
        class="form-control text-secondary-text col-5 ms-2"
        :class="isTemplateNameValidClass"
      />
    </div>
    <div class="quill-editor" v-if="currentNavbarViewNumber === 0">
      <label class="py-3 pe-5" :style="{ color: uiConfiguration?.primaryTextColor }">
        Create HTML Email Template:
      </label>
      <quill-editor
        theme="snow"
        toolbar="full"
        v-model:content="htmlEditorData"
        @textChange="textareaData = htmlEditorData"
        content-type="html"
        :placeholder="'Enter text here...'"
      ></quill-editor>
    </div>
    <div class="textarea-editor" v-if="currentNavbarViewNumber === 1">
      <label class="py-3 pe-5" :style="{ color: uiConfiguration?.primaryTextColor }">
        Paste existing Email Template code:
      </label>
      <textarea
        v-model="textareaData"
        :placeholder="'Enter text here...'"
        class="form-control text-secondary-text"
      />
    </div>

    <div v-if="currentNavbarViewNumber === 2">
      <table v-if="allEmailTemplates" class="table table-sm mt-3">
        <thead>
          <tr>
            <th scope="col">#</th>
            <th scope="col">Name</th>
            <th scope="col"></th>
          </tr>
        </thead>
        <tbody v-for="(template, index) in allEmailTemplates" :key="index">
          <tr>
            <td scope="row">{{ index + 1 }}</td>
            <td>{{ template }}</td>
            <td>
              <button type="button" class="btn float-end btn-sm" @click="openTemplate(template)">
                <i class="bi bi-cloud-plus"></i>
                Open Template
              </button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-else class="alert alert-light my-5">Email templates unavailable!</div>
    </div>

    <div class="action-buttons">
      <button
        v-if="currentNavbarViewNumber === 3"
        type="button"
        class="btn float-end mt-3 btn-sm"
        :style="{
          'background-color': uiConfiguration.buttonColor,
          'border-color': uiConfiguration.buttonColor,
          color: '#FFFFFF',
        }"
        @click="openModal(openModalResetEmailTemplate)"
      >
        <i class="bi bi-arrow-clockwise" />
        Reset Template
      </button>
      <button
        v-if="currentNavbarViewNumber !== 2 && currentNavbarViewNumber !== 3"
        type="button"
        class="btn float-end mt-3 btn-sm"
        :style="{
          'background-color': uiConfiguration.buttonColor,
          'border-color': uiConfiguration.buttonColor,
          color: '#FFFFFF',
        }"
        @click="openModal(openModalUpdateEmailTemplate)"
      >
        <i class="bi bi-plus" />
        Update Template
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'
import { useUiConfiguration } from '@/store/uiConfiguration.js'
import { useEmailTemplates } from '@/store/emailTemplates.js'
import { useNotificationsStore } from '@/store/notifications'
import ConfirmationModal from '@/components/modals/ConfirmationModal.vue'

const uiConfigurationStore = useUiConfiguration()
const emailTemplateStore = useEmailTemplates()
const notifications = useNotificationsStore()

const templateName = ref('')
const isTemplateNameValidClass = ref('')
const htmlEditorData = ref('')
const textareaData = ref('')
const allEmailTemplates = ref([])
const currentNavbarViewNumber = ref(0)

const openModalResetEmailTemplate = ref(false)
const openModalUpdateEmailTemplate = ref(false)

onMounted(() => {
  emailTemplateStore.retrieveEmailTemplates().then((response) => {
    allEmailTemplates.value = response
  })
})
const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme || ''
})

function resetEmailTemplate() {
  emailTemplateStore.creatEmailTemplateReset(templateName.value)
}

function updateEmailTemplate() {
  emailTemplateStore.updateEmailTemplate(templateName.value, textareaData.value)
}

function openModal(modal) {
  if (isTemplateNameValid()) {
    modal.click()
  }
}

function openTemplate(name) {
  templateName.value = name
  emailTemplateStore.retrieveEmailTemplateByName(name).then((response) => {
    textareaData.value = response
    currentNavbarViewNumber.value = 1
  })
}

function isTemplateNameValid() {
  if (templateName.value.length > 0) {
    isTemplateNameValidClass.value = ''
    return true
  } else {
    isTemplateNameValidClass.value = 'is-invalid'
    notifications.setNotification('Please enter template name!', 'danger')
    return false
  }
}
</script>

<style scoped>
.quill-editor :deep(.ql-editor) {
  min-height: 200px;
  max-height: 350px;
  overflow: auto;
}

.textarea-editor textarea {
  min-height: 200px;
  max-height: 350px;
  overflow: auto;
}

.table {
  line-height: 25px;
  min-height: 25px;
  max-height: 250px;
  overflow: scroll;
  height: 25px;
}

</style>

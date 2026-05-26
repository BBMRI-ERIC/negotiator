<template>
  <div class="access-forms-section">
    <AdminSettingsPageHeader title="Access Forms">
      <template #actions>
        <button class="btn btn-sm btn-outline-primary" @click="addAccessForm">
          Add Access Form
        </button>
      </template>
      <template #description>
        Manage your access forms below. You can add a new form, view all existing forms, or update
        them as needed.
      </template>
    </AdminSettingsPageHeader>
  </div>
  <table v-if="!isLoading && allAccessForms" class="table table-hover table-sm mt-3">
    <thead>
      <tr>
        <th scope="col">ID</th>
        <th scope="col">Name</th>
      </tr>
    </thead>
    <tbody v-for="(accessForm, index) in allAccessForms" :key="index">
      <tr class="clickable-row" @click="editAccessForm(accessForm.id)">
        <td>{{ accessForm.id }}</td>
        <td>{{ accessForm.name }}</td>
      </tr>
    </tbody>
  </table>
  <LoadingIndicator v-else-if="isLoading" />
  <div v-else class="alert alert-light my-5">All Access Forms unavailable!</div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useNegotiationFormStore } from '../store/negotiationForm'
import AdminSettingsPageHeader from '@/components/AdminSettingsPageHeader.vue'
import LoadingIndicator from '@/components/LoadingIndicator.vue'
import { useRouter } from 'vue-router'

const negotiationFormStore = useNegotiationFormStore()
const allAccessForms = ref(null)
const isLoading = ref(false)
const router = useRouter()

onMounted(async () => {
  isLoading.value = true
  try {
    const response = await negotiationFormStore.retrieveAccessForms()
    allAccessForms.value = Object.values(response)[0]
  } finally {
    isLoading.value = false
  }
})

function addAccessForm() {
  router.push({ name: 'admin-access-form-create' })
}

function editAccessForm(id) {
  router.push({ name: 'admin-access-form-edit', params: { accessFormId: id } })
}
</script>

<style scoped>
.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: #f1f1f1;
}
</style>

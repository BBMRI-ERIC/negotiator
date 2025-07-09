<template>
  <div class="access-forms-section">
    <h2 class="mb-1 text-left">Access Forms</h2>
    <div class="text-muted mb-3">
      Manage your access forms below. You can add a new form, view all existing forms, or update
      them as needed:
    </div>
    <button class="btn btn-sm btn-outline-primary" @click="addAccessForm">Add Access Form</button>
    <div class="text-muted my-3">All Access Forms:</div>
  </div>
  <table v-if="allAccessForms" class="table table-sm mt-3">
    <thead>
      <tr>
        <th scope="col">id</th>
        <th scope="col">Name</th>
        <th scope="col"></th>
      </tr>
    </thead>
    <tbody v-for="(accessForm, index) in allAccessForms" :key="index">
      <tr>
        <td>{{ accessForm.id }}</td>
        <td>{{ accessForm.name }}</td>
        <td class="col-3">
          <span class="mx-1">
            <button
              type="button"
              class="btn float-end btn-sm"
              @click="duplicateAccessForm(accessForm.id)"
            >
              <i class="bi bi-copy"></i>
              Duplicate
            </button>
          </span>
          <button type="button" class="btn float-end btn-sm" @click="editAccessForm(accessForm.id)">
            <i class="bi bi-pencil-square"></i>
            Edit
          </button>
        </td>
      </tr>
    </tbody>
  </table>
  <div v-else class="alert alert-light my-5">All Access Forms unavailable!</div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useNegotiationFormStore } from '../store/negotiationForm'
import { useRouter } from 'vue-router'

const negotiationFormStore = useNegotiationFormStore()
const allAccessForms = ref(null)
const router = useRouter()

onMounted(() => {
  negotiationFormStore.retrieveAccessForms().then((response) => {
    allAccessForms.value = Object.values(response)[0]
  })
})

function addAccessForm() {
  router.push('/createAccessForm/')
}

function editAccessForm(id) {
  router.push('/editAccessForm/' + id)
}

function duplicateAccessForm(id) {
  router.push('/duplicateAccessForm/' + id)
}
</script>

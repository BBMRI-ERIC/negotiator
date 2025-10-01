<template>
  <div class="access-forms-section">
    <div class="d-flex justify-content-between align-items-center mb-1">
      <h2 class="text-left">Access Forms</h2>
      <button class="btn btn-success" @click="addAccessForm">Add Access Form</button>
    </div>
    <div class="text-muted mb-3">
      Manage your access forms below. You can add a new form, view all existing forms, or update
      them as needed.
    </div>
  </div>
  <table v-if="allAccessForms" class="table table-hover table-sm mt-3">
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
</script>

<style scoped>
.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  background-color: #f1f1f1;
}
</style>

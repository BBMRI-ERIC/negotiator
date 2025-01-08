<template>
  <div id="v-step-3" class="filter-by-status">
    <button class="btn btn-sm btn-outline-sort-filter-button-outline dropdown-toggle btn-primary" type="button"
            data-bs-toggle="dropdown" data-bs-auto-close="outside" aria-expanded="false">
      Select Network
    </button>
    <ul class="dropdown-menu" aria-labelledby="dropdownSortingButton" role="menu">
      <div v-for="(network, index) in userNetworks" :key="index" class="form-check mx-2 my-2">
        <input :value="network.id" class="form-check-input" type="radio" v-model="userNetworksSelected"
               :checked="isChecked(network.id)"
               @change="emitUserNetworksSelected(), emitUserNetworksSelectedName()">
        <label class="form-check-label text-sort-filter-dropdown-text">
          {{ network.name }}
        </label>
      </div>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const emit = defineEmits(["userNetworksSelected", "userNetworksSelectedName"])

const props = defineProps({
  userNetworks: {
    type: Array,
    default: () => ([])
  }
})

const userNetworksSelected = ref("")
onMounted(() => {
  userNetworksSelected.value = props.userNetworks[0].id
  emit('userNetworksSelected', props.userNetworks[0].id)
  emit('userNetworksSelectedName', props.userNetworks[0].name)
})

function emitUserNetworksSelected() {
  emit('userNetworksSelected', userNetworksSelected.value)
}
function emitUserNetworksSelectedName(name) {
  emit('userNetworksSelectedName', name)
}

function isChecked(value) {
  return userNetworksSelected.value === value
}
</script>
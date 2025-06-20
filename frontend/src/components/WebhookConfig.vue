<template>
  <form @submit.prevent="$emit('submit')">
    <div class="mb-3">
      <label for="webhookUrl" class="form-label">Webhook URL</label>
      <input
        type="url"
        class="form-control"
        id="webhookUrl"
        v-model="localForm.url"
        required
        :class="{ 'is-invalid': !urlIsValid }"
        title="Must be a valid URL starting with http:// or https://"
      />
      <small class="form-text text-muted">
        Enter the full URL where you want to receive webhook notifications.
      </small>
      <div v-if="!urlIsValid" class="invalid-feedback">
        Please enter a valid URL starting with http:// or https://
      </div>
    </div>
    <div class="mb-3">
      <label class="form-label">SSL Verification</label>
      <div>
        <div class="form-check form-check-inline">
          <input
            class="form-check-input"
            type="radio"
            name="sslVerification"
            id="sslEnabled"
            :value="true"
            v-model="localForm.sslVerification"
          />
          <label class="form-check-label" for="sslEnabled"> Enabled </label>
        </div>
        <div class="form-check form-check-inline">
          <input
            class="form-check-input"
            type="radio"
            name="sslVerification"
            id="sslDisabled"
            :value="false"
            v-model="localForm.sslVerification"
          />
          <label class="form-check-label" for="sslDisabled"> Disabled </label>
        </div>
      </div>
      <small class="form-text text-warning" v-if="localForm.sslVerification === false">
        Disabling SSL verification is not recommended.
      </small>
    </div>
    <div class="mb-3 form-check">
      <input type="checkbox" class="form-check-input" id="active" v-model="localForm.active" />
      <label class="form-check-label" for="active">
        Active - Toggle to enable or disable webhook notifications.
      </label>
    </div>
  </form>
</template>

<script setup>
import { reactive, watch } from 'vue'

const props = defineProps({
  form: { type: Object, required: true },
  urlIsValid: { type: Boolean, required: true },
})
const emit = defineEmits(['updateForm', 'submit'])

// Create a local reactive copy of the form prop
const localForm = reactive({ ...props.form })

// When localForm changes, emit an update to inform the parent.
watch(
  localForm,
  (newVal) => {
    emit('updateForm', newVal)
  },
  { deep: true },
)

// If the parent updates the form prop, update our local copy.
watch(
  () => props.form,
  (newForm) => {
    Object.assign(localForm, newForm)
  },
  { deep: true },
)
</script>

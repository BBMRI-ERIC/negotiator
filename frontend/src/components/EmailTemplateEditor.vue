<template>
  <div class="email-template-editor border">
    <codemirror
      v-model="emailTemplateData"
      placeholder="Enter xml code here..."
      :style="{ height: '400px' }"
      :autofocus="true"
      :indent-with-tab="true"
      :tab-size="2"
      :extensions="extensions"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Codemirror } from 'vue-codemirror'
import { vue } from '@codemirror/lang-vue'

const emailTemplateData = defineModel('emailTemplateData')

const extensions = ref([vue()])
</script>

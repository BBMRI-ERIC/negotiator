<template>
  <pre><code :class="languageClass" v-html="content"></code></pre>
</template>

<script setup>
import {onMounted, ref, toRef, watch} from 'vue'

const props = defineProps({
  path: {
    type: String,
    required: true,
  },
  language: {
    type: String,
    default: 'plaintext', // Specify the language class for syntax highlighting
  },
})

const content = ref('')
const languageClass = ref(`language-${props.language}`)

const loadFileContent = async () => {
  try {
    if (props.path) {
      const response = await fetch(props.path)
      if (!response.ok) {
        content.value = 'Error: File not found or unable to load.'
        console.error(`Failed to load file at ${props.path}`, response.statusText)
      } else {
        content.value = await response.text()
      }
    }
  } catch (error) {
    content.value = 'Error loading file content.'
    console.error('Fetch error:', error)
  }
}

onMounted(loadFileContent)
watch(toRef(props, 'path'), loadFileContent)
</script>

<style>
/* Light mode */
pre {
  background: #f5f5f5;
  padding: 1em;
  border-radius: 4px;
  overflow-x: auto;
  color: #333;
}

code {
  white-space: pre-wrap;
  display: block;
}

/* Dark mode */
html.dark pre {
  background: #333;
  color: #f5f5f5;
}
</style>

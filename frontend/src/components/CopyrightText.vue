<template>
  <p v-if="isVisible" data-test="copyright-text">{{ copyrightText }}</p>
</template>

<script setup>
import { computed } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'

const DEFAULT_COPYRIGHT_TEXT = '© {year} BBMRI-ERIC'

const uiConfigurationStore = useUiConfiguration()

const footerConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.footer)

const isVisible = computed(() => footerConfiguration.value?.isFooterCopyRightVisible !== false)

const copyrightText = computed(() => {
  const text = footerConfiguration.value?.footerCopyRightText || DEFAULT_COPYRIGHT_TEXT
  return text.replaceAll('{year}', String(new Date().getFullYear()))
})
</script>

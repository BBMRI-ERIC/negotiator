<template>
  <p v-if="isVisible" data-test="copyright-text">{{ copyrightText }}</p>
</template>

<script setup>
import { computed } from 'vue'
import { useUiConfiguration } from '../store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()

const footerConfiguration = computed(() => uiConfigurationStore.uiConfiguration?.footer)

const isVisible = computed(
  () =>
    footerConfiguration.value?.isFooterCopyRightVisible === true &&
    !!footerConfiguration.value?.footerCopyRightText,
)

const copyrightText = computed(() =>
  (footerConfiguration.value?.footerCopyRightText ?? '').replaceAll(
    '{year}',
    String(new Date().getFullYear()),
  ),
)
</script>

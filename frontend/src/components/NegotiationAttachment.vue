<template>
  <div class="border rounded d-flex p-2 w-50">
    <div id="file-type" class="me-2">
      <i class="fs-2 bi" :class="getFileTypeIconClass(contentType)" />
    </div>
    <div id="file-info" style="max-width: 71%;">
      <div class="text-truncate" :title="name" >{{ name }}</div>
      <div class="text-info">
        {{ getFileTypeName(contentType) + ' ' + getHumanFileSize(size) }}
      </div>
    </div>
    <div v-if="!downloadable" id="file-reset" class="ms-auto">
      <button type="button" class="btn-close" @click.prevent="$emit('removed')" />
    </div>
    <div v-if="downloadable" class="ms-auto align-self-center pe-2 cursor-pointer">
      <i class="bi bi-download" @click="$emit('download')" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
const props = defineProps({
  id: {
    type: String,
    default: undefined,
  },
  name: {
    type: String,
    required: true,
  },
  contentType: {
    type: String,
    required: true,
  },
  // Array of possible recipients for messages.
  size: {
    type: Number,
    required: true,
  },
})

const downloadable = computed(() => {
  return props.id !== undefined
})

function getHumanFileSize(bytes, dp = 1) {
  const thresh = 1024

  if (Math.abs(bytes) < thresh) {
    return bytes + ' B'
  }

  const units = ['kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB']
  let u = -1
  const r = 10 ** dp

  do {
    bytes /= thresh
    ++u
  } while (Math.round(Math.abs(bytes) * r) / r >= thresh && u < units.length - 1)
  return bytes.toFixed(dp) + ' ' + units[u]
}

function getFileTypeIconClass(fileType) {
  if (fileType === 'application/pdf') {
    return { 'bi-file-pdf': true }
  } else if (
    [
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
      'application/msword',
    ].includes(fileType)
  ) {
    return { 'bi-file-word': true }
  } else {
    return { 'bi-file-earmark': true }
  }
}

function getFileTypeName(fileType) {
  if (fileType === 'application/pdf') {
    return 'PDF'
  } else if (
    fileType === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ) {
    return 'DOCX'
  } else if (fileType === 'application/msword') {
    return 'DOC'
  }
}
</script>

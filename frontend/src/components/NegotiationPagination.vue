<template>
  <nav
      v-if="negotiations?.length > 0 && pagination.totalPages > 1"
      aria-label="Page navigation"
  >
    <ul class="pagination pagination-sm justify-content-center mt-2">
      <li
          class="page-item"
          :class="currentPageNumber === 1 ? 'disabled' : ''"
      >
        <a
            class="page-link text-primary-text"
            href="#"
            tabindex="-1"
            aria-disabled="true"
            @click="changeCurrentPage(currentPageNumber - 1)"
        >Previous</a>
      </li>
      <li
          v-for="page in pagination.totalPages"
          :key="page"
          class="page-item"
          :class="page === (pagination.number + 1) ? 'disabled' : ''"
      >
        <a
            class="page-link text-primary-text"
            href="#"
            @click="changeCurrentPage(page)"
        >
          {{ page }}
        </a>
      </li>
      <li
          class="page-item"
          :class="(pagination.number + 1) < pagination.totalPages ? '' : 'disabled'"
      >
        <a
            class="page-link text-primary-text"
            href="#"
            @click="changeCurrentPage(currentPageNumber + 1)"
        >Next</a>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import {onMounted, ref} from "vue"
import {useRoute} from "vue-router"

const route = useRoute()

const props = defineProps({
  negotiations: {
    type: Array,
    default: undefined
  },
  pagination: {
    type: Object,
    default: undefined
  }
})

const emit = defineEmits(["currentPageNumber"])

const currentPageNumber = ref(1)

onMounted(() => {
  if (route?.query.currentPageNumber) {
    currentPageNumber.value = parseInt(route?.query.currentPageNumber)
  }
})

function changeCurrentPage(pageNumber) {
  emit("currentPageNumber", pageNumber)
  currentPageNumber.value = pageNumber
}
</script>

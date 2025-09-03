<template>
  <div>
    <div class="container-fluid">
      <!-- Content -->
      <div class="content">
        <!-- Organizations -->
        <div
          id="organizations"
        >
          <RepresentedOrganizationSection />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user.js'
import RepresentedOrganizationSection from '@/components/governance/RepresentedOrganizationSection.vue'

const userStore = useUserStore()

onMounted(async () => {
  // Ensure user data is loaded for admin verification
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})
</script>

<style scoped>

.container-fluid {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

.content {
  background-color: #ffffff;
  padding: 0;
  min-height: 500px;
}

@media (max-width: 768px) {
  .container-fluid {
    padding: 1rem 0.5rem;
  }
}
</style>

<template>
  <div class="governance-page">
    <div class="container-fluid">
      <h1 class="mb-5 text-center">Governance Management</h1>

      <!-- Navigation Tabs -->
      <ul class="nav nav-tabs mb-4" id="governanceTab" role="tablist">
        <li class="nav-item" role="presentation">
          <button
            class="nav-link active"
            id="organizations-tab"
            data-bs-toggle="tab"
            data-bs-target="#organizations"
            type="button"
            role="tab"
            aria-controls="organizations"
            aria-selected="true"
          >
            <i class="bi bi-building me-2"></i>
            Organizations
          </button>
        </li>
        <li class="nav-item" role="presentation">
          <button
            class="nav-link"
            id="resources-tab"
            data-bs-toggle="tab"
            data-bs-target="#resources"
            type="button"
            role="tab"
            aria-controls="resources"
            aria-selected="false"
          >
            <i class="bi bi-database me-2"></i>
            Resources
          </button>
        </li>
        <li class="nav-item" role="presentation">
          <button
            class="nav-link"
            id="networks-tab"
            data-bs-toggle="tab"
            data-bs-target="#networks"
            type="button"
            role="tab"
            aria-controls="networks"
            aria-selected="false"
          >
            <i class="bi bi-diagram-3 me-2"></i>
            Networks
          </button>
        </li>
      </ul>

      <!-- Tab Content -->
      <div class="tab-content" id="governanceTabContent">
        <!-- Organizations Tab -->
        <div
          class="tab-pane fade show active"
          id="organizations"
          role="tabpanel"
          aria-labelledby="organizations-tab"
        >
          <OrganizationsSection />
        </div>

        <!-- Resources Tab -->
        <div
          class="tab-pane fade"
          id="resources"
          role="tabpanel"
          aria-labelledby="resources-tab"
        >
          <div class="placeholder-content">
            <div class="placeholder-icon">
              <i class="bi bi-database"></i>
            </div>
            <h4 class="placeholder-title">Resources Management</h4>
            <p class="placeholder-description">Manage resources that belong to organizations</p>
            <div class="alert alert-info">
              <i class="bi bi-info-circle me-2"></i>
              Resources functionality will be implemented here
            </div>
          </div>
        </div>

        <!-- Networks Tab -->
        <div
          class="tab-pane fade"
          id="networks"
          role="tabpanel"
          aria-labelledby="networks-tab"
        >
          <div class="placeholder-content">
            <div class="placeholder-icon">
              <i class="bi bi-diagram-3"></i>
            </div>
            <h4 class="placeholder-title">Networks Management</h4>
            <p class="placeholder-description">Manage networks and their connections to resources</p>
            <div class="alert alert-info">
              <i class="bi bi-info-circle me-2"></i>
              Networks functionality will be implemented here
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/store/user.js'
import OrganizationsSection from '@/components/OrganizationsSection.vue'

const userStore = useUserStore()

onMounted(async () => {
  // Ensure user data is loaded for admin verification
  if (Object.keys(userStore.userInfo).length === 0) {
    await userStore.retrieveUser()
  }
})
</script>

<style scoped>
.governance-page {
  background: #ffffff;
  min-height: 100vh;
  padding: 0;
}

.container-fluid {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem 1rem;
}

h1 {
  font-size: 2rem;
  font-weight: 600;
  color: #212529;
  margin-bottom: 2rem;
}

.nav-tabs {
  border-bottom: 1px solid #e8ecef;
  margin-bottom: 2rem;
}

.nav-tabs .nav-item {
  margin-bottom: -1px;
}

.nav-tabs .nav-link {
  border: none;
  border-radius: 0;
  color: #6c757d;
  font-weight: 500;
  font-size: 0.95rem;
  padding: 1rem 1.5rem;
  transition: all 0.2s ease;
  background: transparent;
  text-decoration: none;
}

.nav-tabs .nav-link:hover {
  color: #495057;
  background-color: #f8f9fa;
  border-bottom: 2px solid #0d6efd;
}

.nav-tabs .nav-link.active {
  color: #0d6efd;
  background-color: #ffffff;
  border-bottom: 2px solid #0d6efd;
  font-weight: 600;
}

.nav-tabs .nav-link:focus {
  box-shadow: none;
  outline: none;
}

.tab-content {
  background-color: #ffffff;
  padding: 0;
  min-height: 500px;
}

.placeholder-content {
  text-align: center;
  padding: 4rem 2rem;
}

.placeholder-icon {
  margin-bottom: 1.5rem;
}

.placeholder-icon i {
  font-size: 4rem;
  color: #6c757d;
  opacity: 0.6;
}

.placeholder-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: #495057;
  margin-bottom: 1rem;
}

.placeholder-description {
  font-size: 1rem;
  color: #6c757d;
  margin-bottom: 2rem;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.alert {
  max-width: 500px;
  margin: 0 auto;
  border-radius: 0.375rem;
  font-size: 0.95rem;
}

.alert-info {
  background-color: #d1ecf1;
  border-color: #bee5eb;
  color: #0c5460;
}

@media (max-width: 768px) {
  .container-fluid {
    padding: 1rem 0.5rem;
  }

  h1 {
    font-size: 1.75rem;
  }

  .nav-tabs .nav-link {
    padding: 0.75rem 1rem;
    font-size: 0.875rem;
  }

  .placeholder-content {
    padding: 2rem 1rem;
  }

  .placeholder-icon i {
    font-size: 3rem;
  }

  .placeholder-title {
    font-size: 1.25rem;
  }
}

@media (max-width: 576px) {
  .nav-tabs .nav-link {
    padding: 0.5rem 0.75rem;
    font-size: 0.8rem;
  }

  .nav-tabs .nav-link i {
    display: none;
  }
}
</style>

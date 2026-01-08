<template>
  <div class="organization-item mb-4">
    <!-- Organization Header -->
    <div class="organization-header p-3 bg-light border rounded-top">
      <div class="d-flex justify-content-between align-items-center">
        <div>
          <h5 class="mb-1" :class="{ 'text-muted': organization.withdrawn }">
            {{ organization.name }}
            <small class="text-muted">({{ organization.externalId }})</small>
            <UiBadge
              v-if="organization.withdrawn"
              :class="'bg-warning text-dark ms-2'"
              :icon="'bi bi-x-octagon-fill me-1'"
              >WITHDRAWN</UiBadge
            >
          </h5>
          <p class="mb-0 text-muted small">{{ organization.description }}</p>
        </div>
        <div class="organization-status">
          <i
            v-if="organization.withdrawn"
            class="bi bi-x-octagon-fill text-warning"
            title="This organization is withdrawn"
          ></i>
          <i
            v-else-if="allResourcesHaveRepresentatives"
            class="bi bi-check-circle-fill text-success"
            title="All resources have representatives"
          ></i>
          <i
            v-else
            class="bi bi-exclamation-triangle-fill text-warning"
            title="Some resources are missing representatives"
          ></i>
        </div>
      </div>

      <!-- Organization Contact Info -->
      <div class="organization-contacts mt-2">
        <small class="text-muted">
          <i class="bi bi-envelope me-1"></i>
          <a
            :href="'mailto:' + organization.contactEmail"
            :class="{ 'text-muted': organization.withdrawn }"
          >
            {{ organization.contactEmail }}
          </a>
          <span class="mx-2">|</span>
          <i class="bi bi-globe me-1"></i>
          <a
            :href="organization.uri"
            target="_blank"
            :class="{ 'text-muted': organization.withdrawn }"
          >
            {{ organization.uri }}
          </a>
        </small>
      </div>
    </div>

    <!-- Resources List -->
    <div class="resources-container border border-top-0 rounded-bottom">
      <div
        class="resources-header p-2 bg-white border-bottom cursor-pointer"
        @click="toggleResources"
        data-bs-toggle="collapse"
        :data-bs-target="'#resources-' + organization.id"
        aria-expanded="false"
      >
        <div class="d-flex justify-content-between align-items-center">
          <small class="text-muted fw-bold">
            <i
              class="bi bi-chevron-right transition-icon"
              :class="{ 'rotate-90': resourcesExpanded }"
            ></i>
            Resources ({{ organization.resources.length }})
            <span v-if="resourceStats.active !== organization.resources.length">
              - {{ resourceStats.active }} active, {{ resourceStats.withdrawn }} withdrawn
            </span>
          </small>
          <small class="text-muted">
            Click to {{ resourcesExpanded ? 'collapse' : 'expand' }}
          </small>
        </div>
      </div>

      <div
        :id="'resources-' + organization.id"
        class="collapse resources-list"
        :class="{ show: resourcesExpanded }"
      >
        <div
          v-for="resource in organization.resources"
          :key="resource.id"
          class="resource-item p-3 border-bottom"
          :class="{
            'resource-withdrawn': resource.withdrawn,
            'resource-no-reps': !resource.withdrawn && resource.representatives.length === 0,
            'org-withdrawn': organization.withdrawn,
          }"
        >
          <div class="d-flex justify-content-between align-items-start">
            <div class="resource-info flex-grow-1">
              <h6
                class="mb-1"
                :class="{ 'text-muted': resource.withdrawn || organization.withdrawn }"
              >
                {{ resource.name }}
                <small class="text-muted">({{ resource.sourceId }})</small>

                <!-- Resource Status Badges -->
                <UiBadge
                  v-if="resource.withdrawn"
                  :class="'bg-warning text-dark ms-2'"
                  :icon="'bi bi-x-octagon-fill me-1'"
                  >WITHDRAWN</UiBadge
                >
                <UiBadge
                  v-else-if="organization.withdrawn"
                  :class="'bg-secondary ms-2'"
                  :icon="'bi bi-x-octagon-fill me-1'"
                  >ORG WITHDRAWN</UiBadge
                >
                <UiBadge
                  v-else-if="resource.representatives.length === 0"
                  :class="'bg-danger ms-2'"
                  :icon="'bi bi-exclamation-triangle-fill me-1'"
                  >NO REPRESENTATIVES</UiBadge
                >
                <UiBadge v-else :class="'bg-success ms-2'" :icon="'bi bi-check-circle-fill me-1'"
                  >ACTIVE</UiBadge
                >
              </h6>

              <p
                class="mb-2 text-muted small"
                :class="{ 'text-muted': resource.withdrawn || organization.withdrawn }"
              >
                {{ resource.description }}
              </p>

              <!-- Resource Contact Info -->
              <div class="resource-contacts mb-2">
                <small class="text-muted">
                  <i class="bi bi-envelope me-1"></i>
                  <a
                    :href="'mailto:' + resource.contactEmail"
                    :class="{ 'text-muted': resource.withdrawn || organization.withdrawn }"
                  >
                    {{ resource.contactEmail }}
                  </a>
                  <span class="mx-2">|</span>
                  <i class="bi bi-globe me-1"></i>
                  <a
                    :href="resource.uri"
                    target="_blank"
                    :class="{ 'text-muted': resource.withdrawn || organization.withdrawn }"
                  >
                    {{ resource.uri }}
                  </a>
                </small>
              </div>

              <!-- Representatives -->
              <div class="representatives">
                <small class="text-muted">
                  <strong>Representatives: </strong>
                  <span v-if="resource.representatives.length === 0" class="text-warning">
                    None assigned
                  </span>
                  <span v-else>
                    {{
                      resource.representatives
                        ?.map(
                          (rep) =>
                            `${rep.name || 'name not provided'} (${rep.email || 'email not provided'})`,
                        )
                        .join(', ')
                    }}
                  </span>
                </small>
              </div>
            </div>

            <!-- Resource Status Icon -->
            <div class="resource-status ms-3">
              <i
                v-if="organization.withdrawn"
                class="bi bi-x-octagon-fill text-warning"
                title="Organization is withdrawn"
              ></i>
              <i
                v-else-if="resource.withdrawn"
                class="bi bi-x-octagon-fill text-warning"
                title="Resource is withdrawn"
              ></i>
              <i
                v-else-if="resource.representatives.length > 0"
                class="bi bi-check-circle-fill text-success"
                title="Resource has representatives"
              ></i>
              <i
                v-else
                class="bi bi-exclamation-triangle-fill text-danger"
                title="Resource has no representatives"
              ></i>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import UiBadge from '@/components/ui/UiBadge.vue'

const props = defineProps({
  organization: {
    type: Object,
    required: true,
  },
})

const resourcesExpanded = ref(false)

const toggleResources = () => {
  resourcesExpanded.value = !resourcesExpanded.value
}

const allResourcesHaveRepresentatives = computed(() => {
  return props.organization.resources.every(
    (resource) => resource.representatives.length > 0 || resource.withdrawn === true,
  )
})

const resourceStats = computed(() => {
  const active = props.organization.resources.filter((r) => !r.withdrawn).length
  const withdrawn = props.organization.resources.filter((r) => r.withdrawn).length
  return { active, withdrawn }
})
</script>

<style scoped>
.organization-item {
  border: none;
}

.organization-header {
  background-color: #f8f9fa;
}

.organization-header.withdrawn {
  background-color: #fff3cd;
}

.resources-container {
  background-color: #ffffff;
}

.resource-item {
  transition: background-color 0.2s ease;
}

.resource-item:hover {
  background-color: #f8f9fa;
}

.resource-item:last-child {
  border-bottom: none;
}

.resource-withdrawn {
  background-color: #fff8f0;
}

.resource-no-reps {
  background-color: #ffebee;
}

.org-withdrawn {
  background-color: #f5f5f5;
  opacity: 0.8;
}

.badge {
  font-size: 0.7rem;
  padding: 0.25em 0.5em;
}

.resource-status i {
  font-size: 1.2rem;
}

.organization-status i {
  font-size: 1.5rem;
}

a {
  color: #0366d6;
  text-decoration: none;
}

a:hover {
  text-decoration: underline;
}

.text-muted a {
  color: #6c757d !important;
}

.fw-bold {
  font-weight: 600;
}

.small {
  font-size: 0.875rem;
}

.flex-grow-1 {
  flex-grow: 1;
}

.cursor-pointer {
  cursor: pointer;
}

.resources-header:hover {
  background-color: #f8f9fa !important;
}

.transition-icon {
  transition: transform 0.2s ease;
  display: inline-block;
  margin-right: 0.5rem;
}

.rotate-90 {
  transform: rotate(90deg);
}

.collapse {
  transition: height 0.35s ease;
}
</style>

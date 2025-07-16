<template>
  <div class="accordion-item" :class="{ 'resource-withdrawn': resource.withdrawn, 'org-withdrawn': organizationWithdrawn }">
    <h2 class="accordion-header" :id="'heading_' + resource.id">
      <button
        :class="[
          'accordion-button',
          'collapsed',
          { 'gray-text': resource.withdrawn || organizationWithdrawn },
          { 'withdrawn-resource': resource.withdrawn },
          { 'org-withdrawn-resource': organizationWithdrawn }
        ]"
        type="button"
        data-bs-toggle="collapse"
        :data-bs-target="'#collapse_' + resource.id"
        aria-expanded="false"
        :aria-controls="'collapse_' + resource.id"
      >
        {{ resource.name }} ({{ resource.sourceId }})

        <!-- Organization withdrawn notice -->
        <span v-if="organizationWithdrawn && !resource.withdrawn" class="ms-2 text-muted small">
          (Organization withdrawn)
        </span>

        <!-- Resource Status Icon -->
        <i
          v-if="organizationWithdrawn"
          class="bi bi-building-x text-warning ms-1"
          title="This resource's organization is withdrawn"
        ></i>
        <i
          v-else-if="resource.withdrawn === true"
          class="bi bi-x-octagon-fill text-warning ms-1"
          title="This resource is no longer active and cannot be a part of any request."
        ></i>
        <i
          v-else-if="resource.representatives.length > 0 && resource.withdrawn === false"
          class="bi bi-check-circle-fill text-success ms-1"
          title="This resource has representatives"
        ></i>
        <i
          v-else-if="resource.representatives.length === 0 && resource.withdrawn === false"
          class="bi bi-exclamation-triangle-fill text-warning ms-1"
          title="This resource has no representatives"
        ></i>
      </button>
    </h2>

    <!-- Resource Body -->
    <div
      :id="'collapse_' + resource.id"
      class="accordion-collapse collapse"
      :aria-labelledby="'heading_' + resource.id"
    >
      <div class="accordion-body" :class="{ 'withdrawn-content': resource.withdrawn || organizationWithdrawn }">

        <!-- Withdrawn notices -->
        <div v-if="organizationWithdrawn && !resource.withdrawn" class="alert alert-info mb-3" role="alert">
          <i class="bi bi-info-circle-fill me-2"></i>
          This resource is part of a withdrawn organization and cannot participate in new negotiations.
        </div>

        <div v-if="resource.withdrawn" class="alert alert-warning mb-3" role="alert">
          <i class="bi bi-exclamation-triangle-fill me-2"></i>
          This resource is withdrawn and no longer active.
        </div>

        <p :class="{ 'text-muted': resource.withdrawn || organizationWithdrawn }">{{ resource.description }}</p>
        <ul class="list-unstyled p-2">
          <li v-if="resource.withdrawn === true">
            <i class="bi bi-activity"></i>
            <span class="text-warning">Withdrawn, no longer active</span>
          </li>
          <li v-else-if="organizationWithdrawn">
            <i class="bi bi-building-x"></i>
            <span class="text-warning">Organization withdrawn</span>
          </li>
          <li>
            <i class="bi bi-envelope p-2"></i>
            <a
              :href="'mailto:' + resource.contactEmail"
              :class="{ 'text-muted': resource.withdrawn || organizationWithdrawn }"
            >
              {{ resource.contactEmail }}
            </a>
          </li>
          <li>
            <i class="bi bi-globe p-2"></i>
            <a
              :href="resource.uri"
              target="_blank"
              :class="{ 'text-muted': resource.withdrawn || organizationWithdrawn }"
            >
              {{ resource.uri }}
            </a>
          </li>
        </ul>
        <h6 :class="{ 'text-muted': resource.withdrawn || organizationWithdrawn }">Representatives:</h6>
        <ul>
          <li
            v-for="rep in resource.representatives"
            :key="rep"
            :class="{ 'text-muted': resource.withdrawn || organizationWithdrawn }"
          >
            <i class="bi bi-person"></i> {{ rep }}
          </li>
        </ul>
        <!-- Warning if no representatives -->
        <div
          v-if="resource.representatives.length === 0 && !resource.withdrawn && !organizationWithdrawn"
          class="text-warning mt-2"
        >
          <i class="bi bi-exclamation-triangle"></i> This resource has no representatives.
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  resource: {
    type: Object,
    required: true,
  },
  organizationWithdrawn: {
    type: Boolean,
    default: false,
  },
})
</script>

<style scoped>
.gray-text {
  color: gray;
}

.accordion-item {
  border: 1px solid #dee2e6;
  margin-bottom: 0.5rem;
}

.accordion-button {
  background-color: #f8f9fa;
  border: none;
  padding: 1rem;
  text-align: left;
  width: 100%;
  position: relative;
}

.accordion-button:focus {
  box-shadow: none;
  border-color: #dee2e6;
}

.accordion-button.collapsed {
  background-color: #ffffff;
}

.accordion-body {
  padding: 1rem;
  background-color: #ffffff;
}

.text-warning {
  color: #ffc107 !important;
}

.text-success {
  color: #198754 !important;
}

.list-unstyled {
  list-style: none;
  padding-left: 0;
}

.p-2 {
  padding: 0.5rem;
}

.ms-1 {
  margin-left: 0.25rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

.accordion-body a {
  color: #0366d6;
  text-decoration: none;
}

.accordion-body a:hover {
  text-decoration: underline;
}

.accordion-body li {
  margin-bottom: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.accordion-body h6 {
  margin-top: 1rem;
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.accordion-body p {
  margin-bottom: 1rem;
  color: #6c757d;
}

.resource-withdrawn {
  background-color: #fff8f0;
  border-color: #ffc107;
}

.org-withdrawn {
  background-color: #f8f9fa;
  border-color: #6c757d;
}

.withdrawn-resource {
  background-color: #fff3cd !important;
  color: #664d03;
}

.org-withdrawn-resource {
  background-color: #e9ecef !important;
  color: #495057;
}

.withdrawn-content {
  background-color: #fafafa;
}

.alert-info {
  background-color: #d1ecf1;
  border-color: #bee5eb;
  color: #0c5460;
}

.small {
  font-size: 0.875rem;
}
</style>

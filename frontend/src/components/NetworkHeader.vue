<template>
  <div class="organization-details">
    <div class="avatar">
      <i class="bi bi-people-fill mt-2" />
    </div>
    <div class="organization-info ms-3">
      <h1 class="h2 lh-condensed">
        {{ network?.name?.toUpperCase() }}
      </h1>
      <ul class="list-style-none">
        <li>
          <i class="bi bi-globe" />
          <a :href="network.uri" class="ms-2">{{ network.uri }}</a>
        </li>
        <li>
          <i class="bi bi-envelope" />
          <a :href="'mailto:' + network.contactEmail" class="ms-2">{{ network.contactEmail }}</a>
        </li>
        <li>
          <i class="bi bi-clipboard" />
          <div class="text-muted ms-2 text-nowrap">
            {{ network.externalId }}
          </div>
        </li>
      </ul>
    </div>
    <div>
      <div class="d-flex flex-column">
        <a
          type="button"
          class="btn btn-sm mb-2"
          :href="externalLinks.auth_management_link"
          :style="{
            'background-color': uiConfiguration?.buttonColor,
            color: '#ffffff',
          }"
        >
          <span>
            <i class="bi bi-gear"></i>
            Manage Resource Representatives
          </span>
        </a>
        <a
          type="button"
          class="btn btn-sm"
          href="https://bbmri-eric.github.io/negotiator/network_manager"
          :style="{
            'background-color': uiConfiguration?.buttonColor,
            color: '#ffffff',
          }"
        >
          <span>
            <i class="bi bi-book"></i>
            Guide
          </span>
        </a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import externalLinks from '@/config/externalLinks'
import { useUiConfiguration } from '@/store/uiConfiguration.js'

const uiConfigurationStore = useUiConfiguration()
const uiConfiguration = computed(() => {
  return uiConfigurationStore.uiConfiguration?.theme
})
</script>

<style scoped>
.avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background-color: #e1e4e8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 50px;
  color: #ffffff;
}

.organization-details {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  margin-top: 1rem;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
}

.organization-details img {
  margin-bottom: 1rem;
  margin-right: 1.5rem;
}

.organization-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.organization-info h1 {
  font-size: 1.75rem;
  font-weight: 600;
}

.organization-info .description {
  color: #586069;
}

.organization-info ul {
  list-style: none;
  padding: 0;
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  margin-top: 1rem;
  margin-bottom: 0;
}

.organization-info ul li {
  max-width: 230px;
  display: flex;
  align-items: center;
  color: #586069;
}

.organization-info ul li a {
  color: #0366d6;
  text-decoration: none;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  max-width: 100%;
}

.organization-info ul li a:hover {
  text-decoration: underline;
}

.organization-info ul li svg {
  margin-right: 0.5rem;
}

.btn {
  background-color: #007bff;
  color: #ffffff;
  transition:
    background-color 0.3s ease,
    transform 0.2s ease;
  text-decoration: none;
}

.btn:hover {
  background-color: #0056b3;
  transform: scale(1.05);
}

.btn:active {
  transform: scale(1);
}
</style>

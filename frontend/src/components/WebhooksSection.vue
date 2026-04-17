<template>
  <div class="specific-area panel panel-default border-">
    <div class="d-flex justify-content-between align-items-center mb-1">
      <h2 class="text-left">Webhooks</h2>
      <RouterLink class="btn btn-outline-primary" to="/settings/webhooks"
        >Manage Webhooks</RouterLink
      >
    </div>
    <div class="text-muted mb-3">
      Webhooks allow external services to be notified when certain events happen. When the specified
      events occur, we send a POST request to each URL provided. Learn more in our
      <a
        href="https://bbmri-eric.github.io/negotiator/administrator#webhooks"
        target="_blank"
        rel="noopener noreferrer"
        >Webhooks Documentation</a
      >.
    </div>
  </div>
</template>

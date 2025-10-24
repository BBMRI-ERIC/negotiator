<template>
  <div class="guide-page">
    <h1>Welcome to the Negotiator Guide</h1>
    <p>This guide will walk you through how to use the app effectively from a user perspective.</p>

    <section>
      <h2>Key Features</h2>
      <ul>
        <li><strong>Menu:</strong> Navigating the BBMRI-ERIC Negotiator</li>

        <p class="mb-0">
          The navigation bar at the top of the screen is your primary tool for getting around the
          platform.
        </p>
        <button class="btn btn-sm btn-outline-dark my-3">
          Take the Tour
          <i class="bi bi-person-raised-hand"></i>
        </button>

        <li>
          <strong>Search:</strong> Use the search bar to quickly find what you're looking for.
        </li>
        <li><strong>Notifications:</strong> Stay updated with real-time alerts and updates.</li>
        <li><strong>Settings:</strong> Customize your preferences and manage your account.</li>
      </ul>
    </section>

    <section>
      <h2>Access Documentation</h2>
      <p>Learn more about the Negotiator by visiting the official documentation.</p>
      <a
        href="https://bbmri-eric.github.io/negotiator/what-is-negotiator"
        target="_blank"
        class="doc-link"
      >
        <i class="bi bi-book"></i> Documentation
      </a>
    </section>
  </div>
</template>

<style scoped>
.guide-page {
  font-family: Arial, sans-serif;
  line-height: 1.6;
  padding: 20px;
}

h1 {
  color: #2c3e50;
}

h2 {
  color: #34495e;
  margin-top: 20px;
}

ul {
  list-style-type: disc;
  margin-left: 20px;
}

p {
  margin-bottom: 15px;
}
</style>

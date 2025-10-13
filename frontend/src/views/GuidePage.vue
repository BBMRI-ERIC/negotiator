<template>
  <div class="guide-page">
    <h1>Welcome to the App Guide</h1>
    <p>This guide will walk you through how to use the app effectively from a user perspective.</p>

    <section>
      <h2>Getting Started</h2>
      <p>
        To begin, log in to your account using your credentials. If you're new, you can sign up by
        clicking the "Sign Up" button on the login page.
      </p>
    </section>

    <section>
      <h2>Key Features</h2>
      <ul>
        <li><strong>Dashboard:</strong> View an overview of your activities and progress.</li>
        <li>
          <strong>Search:</strong> Use the search bar to quickly find what you're looking for.
        </li>
        <li><strong>Notifications:</strong> Stay updated with real-time alerts and updates.</li>
        <li><strong>Settings:</strong> Customize your preferences and manage your account.</li>
      </ul>
    </section>

    <section>
      <h2>How to Navigate</h2>
      <p>
        Use the navigation bar at the top of the screen to access different sections of the app.
        Each section is designed to help you accomplish specific tasks efficiently.
      </p>
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

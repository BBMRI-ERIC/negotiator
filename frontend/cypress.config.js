import { defineConfig } from 'cypress'

export default defineConfig({
  chromeWebSecurity: false,
  e2e: {
    setupNodeEvents() {
      // implement node event listeners here
    },
    retries: 2
  }
})

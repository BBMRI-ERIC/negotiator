import { defineConfig } from 'cypress'

export default defineConfig({
  chromeWebSecurity: false,
  env: {
    // eslint-disable-next-line no-undef
    FEATURE_FLAG_PDF_EXPORT_ENABLED: process.env.FEATURE_FLAG_PDF_EXPORT_ENABLED === 'true'
  },
  e2e: {
    setupNodeEvents() {
      // implement node event listeners here
    },
    retries: 2
  }
})

import { defineConfig } from 'cypress'

export default defineConfig({
  fixturesFolder: 'tests/assets/',
  viewportWidth: 1280,
  viewportHeight: 720,
  video: false,
  e2e: {
    // We've imported your old cypress plugins here.
    // You may want to clean this up later by importing these.
    setupNodeEvents(on, config) {
      return require('./tests/cypress/plugins/index.js')(on, config)
    },
    baseUrl: 'http://localhost:8081',
    specPattern: 'tests/cypress/**/*.cypress.js',
    supportFile: 'tests/cypress/support/index.js',
  },
})

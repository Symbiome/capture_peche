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
    // Par défaut : uniquement les parcours fonctionnels (gating CI).
    // Les bancs CV/perf (tests/cypress/bench) se lancent à part (script cypress:bench).
    specPattern: 'tests/cypress/e2e/**/*.cypress.js',
    supportFile: 'tests/cypress/support/index.js',
  },
  env: {
    // Base de l'API backend, utilisée par cy.loginAngler() (mode liveBackend).
    apiUrl: 'http://localhost:8081/api',
    // Identifiant pêcheur de recette — À RENSEIGNER une fois le seed #67 en place.
    anglerEmail: 'pecheur-recette@fishola.test',
    anglerPassword: 'azerty',
    // false => les e2e nécessitant un backend seedé sont ignorés (skip).
    // true  => e2e réels contre un backend démarré + seedé (#67).
    liveBackend: false,
  },
})

const { defineConfig } = require("cypress");

module.exports = defineConfig({
  viewportWidth: 1280,
  viewportHeight: 720,
  video: false,
  e2e: {
    baseUrl: "http://localhost:8082",
    specPattern: "tests/cypress/e2e/**/*.cypress.js",
    supportFile: "tests/cypress/support/e2e.js",
    fixturesFolder: "tests/cypress/fixtures",
  },
  env: {
    // Base de l'API backend, utilisée par le mode E2E réel (liveBackend=true).
    // À aligner sur l'environnement ciblé (dev admin = 8080, tests Quarkus = 8081).
    apiUrl: "http://localhost:8080/api",
    // Compte national du seed de recette (scripts/seed_recette.sql).
    staffEmail: "national.recette@fishola.test",
    staffPassword: "Recette2026!",
    // false => smoke stubbé (aucun backend requis, vert en CI headless).
    // true  => E2E réel contre un backend démarré + seedé (#67).
    liveBackend: false,
  },
});

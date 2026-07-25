// Typage des commandes Cypress personnalisées (confort éditeur ; sans impact à l'exécution).
declare namespace Cypress {
  interface Chainable {
    /**
     * Connecte un compte staff via l'écran de connexion et attend la home.
     * Identifiants par défaut : `Cypress.env('staffEmail' | 'staffPassword')`.
     */
    loginStaff(options?: { email?: string; password?: string }): Chainable<void>;
  }
}

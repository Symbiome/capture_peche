# Module front (mobile) de Fishola

## Démarrer en mode dev

Il suffit de lancer la commande suivante :

```bash
npm run serve
```

L'application tourne sur le port `8081` : [http://localhost:8081](http://localhost:8081).

## Lancer les tests

Il existe deux types de tests pour Fishola : 

* Les tests unitaires avec Vitest
* les tests d'intégration avec Cypress

### Tests unitaires (vitest)

Les tests sont situés dans tests/unit (*.spec.ts).
Pour lancer la TestSuite :

```bash
npm run tests
```

En mode surveillance pendant le développement :

```bash
npm run tests:watch
```

La configuration vit dans `vitest.config.js`, qui réutilise telle quelle celle de
`vite.config.js` : alias `@`, plugin Vue 2, préprocesseur Less et substitution
des `import.meta.env` sont donc identiques à ceux de l'application. Les tests
s'exécutent dans un environnement jsdom.

### Tests d'intégration (cypress)

Les tests sont situés dans tests/cypress/ (*.cypress.js).
Pour lancer la test suite : 

* lancez le front de fishola en local
* modifiez le fichier cypress.json pour indiquer l'url du front (par défaut 8081)
* ouvrir cypress
```bash
npm run cypress
```
* cliquer sur le bouton "play" à droite de la liste des tests pour jouer les scénarios
* pour générer un rapport html
```bash
npm run cypress-report
```

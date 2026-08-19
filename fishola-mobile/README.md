# Module front (mobile) de Fishola

## Démarrer en mode dev

Il suffit de lancer la commande suivante :

```bash
npm run serve
```

L'application tourne sur le port `8081` : [http://localhost:8081](http://localhost:8081).

## Contrôle de types

Le build Vite passe par esbuild, qui retire les annotations de type **sans les
vérifier** : une erreur de typage ne se voit donc ni au `npm run serve`, ni au
`npm run build:*`. Elle n'apparaît qu'en lançant explicitement le compilateur :

```bash
npm run type-check
```

Cette commande (`tsc --noEmit`) ne produit aucun fichier, elle ne fait que
vérifier. Elle est aussi jouée par `./run_tests.sh mobile` à la racine du dépôt,
avant les tests unitaires, pour qu'une régression de typage ne puisse pas passer
inaperçue.

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

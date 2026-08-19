# Carto — version de `maplibre-gl`

## Décision : on reste en 4.x, version épinglée `4.7.1`

`fishola-mobile/package.json` déclare `"maplibre-gl": "4.7.1"` — **sans caret**.
Le fichier de verrouillage fixait déjà 4.7.1, mais le `^` laissait un
`npm install` dériver dans la branche 4.x sans que personne ne l'ait décidé.
L'épinglage rend l'intention lisible dans le fichier que les gens lisent.

Trois raisons de ne pas monter en v5 :

1. **Aucun besoin fonctionnel.** Les apports de la v5 tournent autour de la
   projection globe et du rendu 3D. L'application affiche des lacs français
   entre les niveaux de zoom 10 et 16.
2. **Le coût n'est pas la montée, c'est la re-validation.** Cinq fichiers
   importent `maplibre-gl` directement :
   - `src/components/common/MapLibreMap.vue`
   - `src/components/common/MapLibreMarkersMap.vue`
   - `src/components/common/MapLibrePositionMap.vue`
   - `src/components/common/maplibreStyle.ts`
   - `src/components/my-trips/MyTripsMap.vue`

   Il faudrait revérifier trois écrans de carte, le clustering natif, le survol
   hydro, les pins de capture, la sélection depuis les listes et le mode dégradé
   hors-ligne. Une demi-journée de recette.
3. **Le risque est asymétrique.** Si la v5 abandonne le repli WebGL1, la carte
   devient **noire sans message d'erreur** sur les appareils anciens : invisible
   côté serveur, non remonté par l'utilisateur, indistinguable d'un problème de
   réseau.

## Porte de contrôle — à franchir *avant* toute montée en v5

À dérouler le jour où quelqu'un ouvre le sujet ; tant que les trois cases ne
sont pas cochées, on ne monte pas.

- [ ] **Le repli WebGL1 est-il toujours présent dans le paquet installé ?**
      En 4.7.1 il l'est — le contexte est créé ainsi :

      ```bash
      grep -o 'getContext("webgl2"[^;]*' node_modules/maplibre-gl/dist/maplibre-gl.js
      ```

      doit montrer `getContext("webgl2",t)||this._canvas.getContext("webgl",t)`.
      Si la version visée ne tente plus que `webgl2`, la montée est **bloquée**
      tant qu'on supporte des appareils sans WebGL2.
- [ ] **La spec end-to-end de carte passe-t-elle ?**
      `tests/cypress/e2e/maplibre-map.cypress.js` — initialisation du canvas,
      bascule Plan/Satellite, bouton de géolocalisation.
- [ ] **Essai sur un appareil ancien réel**, si disponible. Le banc Cypress
      tourne sur un moteur de bureau : il ne dit rien du parc WebView.

## Voir aussi

- Parc navigateur visé : `fishola-mobile/.browserslistrc`. C'est lui qui décide
  du niveau de moteur qu'on prétend supporter, donc de la pertinence de la
  première case ci-dessus.

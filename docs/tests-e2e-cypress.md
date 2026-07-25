# Tests E2E Cypress (#76)

Base commune des tests end-to-end Cypress pour **fishola-mobile** (app pêcheur) et
**fishola-admin** (back-office). Ce document décrit la structure, les conventions et
les commandes ; il correspond au socle posé par l'issue #76 (cadrage + nettoyage +
amorce CI), **avant** la refonte complète des scénarios.

## Principe : deux familles de tests

| Dossier | Contenu | Gating CI |
| --- | --- | --- |
| `tests/cypress/e2e/` | Parcours **fonctionnels** (régression métier) | Oui (bloquant) |
| `tests/cypress/bench/` | **Bancs** de mesure CV / perf (précision détection, stress) | Non (outillage local) |

Les bancs (`fish-detection`, `marker-detection`, `stress-test`) mesurent un **algorithme**
(détection de poisson / marqueur, tenue en charge) : ce ne sont pas des tests de
non-régression fonctionnelle, ils ne doivent donc pas bloquer la CI. Ils restent utiles
pour le réglage de la vision par ordinateur et produisent un rapport dédié (voir plus bas).

## Arborescence

### fishola-mobile (port dev `8081`)

```
tests/cypress/
├── e2e/
│   ├── offline-revalidation.cypress.js   # mode offline & re-validation (#10)
│   └── maplibre-map.cypress.js           # smoke carte MapLibre (#33)
├── bench/
│   ├── fish-detection.cypress.js         # banc mesure poisson
│   ├── marker-detection.cypress.js       # banc détection marqueur
│   ├── stress-test.cypress.js            # banc perf
│   └── cypress-test-utils.js             # helpers partagés des bancs
├── support/
│   ├── index.js                          # charge commands + reporter + ignore 401
│   └── commands.js                       # cy.loginAngler()
└── plugins/
    └── index.js                          # tâche log + reporter mochawesome (conditionnel)
```

### fishola-admin (port dev `8082`)

```
tests/cypress/
├── e2e/
│   └── auth-smoke.cypress.js             # smoke connexion staff (#76)
└── support/
    ├── e2e.js                            # charge commands
    ├── commands.js                       # cy.loginStaff()
    └── index.d.ts                        # typage de cy.loginStaff
```

## Commandes d'authentification

Chaque application expose **une** commande de connexion réutilisable. Le mobile
authentifie des **pêcheurs**, l'admin du **staff** (administrateurs / opérateurs) — d'où
deux noms distincts.

### `cy.loginStaff(options?)` — admin

Connexion **par l'interface** (visite `/login`, remplit le formulaire, attend `/home`).
Choix de l'UI plutôt que d'un appel HTTP direct : `cy.request` **contourne** `cy.intercept`,
donc un login programmatique ne serait pas *stubbable* ; en UI la commande fonctionne aussi
bien contre un backend réel que contre un backend simulé par la spec.

```js
cy.loginStaff();                                   // identifiants par défaut (Cypress.env)
cy.loginStaff({ email: "a@b.fr", password: "…" }); // override
```

### `cy.loginAngler(options?)` — mobile

Connexion **programmatique** (`cy.request POST /v1/security/login`) mise en cache avec
`cy.session`. Ici on veut une ouverture de session rapide en amont des scénarios (les e2e
mobiles ciblent de toute façon un vrai backend seedé), pas rejouer l'écran de connexion.

```js
cy.loginAngler();
cy.loginAngler({ email: "pecheur@ex.fr", password: "…" });
```

## Le drapeau `liveBackend`

Les deux configs exposent des variables `env`, dont `liveBackend` (défaut `false`) :

- **`false`** (défaut, CI headless) : les scénarios qui exigent un backend seedé sont
  soit **stubbés** (`cy.intercept`), soit **ignorés** (`this.skip()` dans un `beforeEach`).
  Rien à démarrer côté serveur applicatif.
- **`true`** : e2e **réels** contre un backend démarré et seedé. À activer une fois le
  **seed de recette (#67)** en place :

```bash
npx cypress run --env liveBackend=true
```

Exemples :
- admin `auth-smoke` : le parcours de connexion tourne **stubbé** par défaut (vert sans
  backend) ; un cas supplémentaire, gardé derrière `liveBackend`, valide la connexion réelle.
- mobile `offline-revalidation` : les AC sont **skippés** tant que `liveBackend` est faux ;
  activés, ils ouvrent une session via `cy.loginAngler()` puis jouent la synchro offline.

> `maplibre-map` ne dépend pas d'un backend seedé : il vérifie le rendu de la carte et
> tourne dès que le **serveur front** (`npm run serve`) est lancé.

## Variables d'environnement (`env`)

| Clé | Mobile | Admin | Rôle |
| --- | --- | --- | --- |
| `apiUrl` | `http://localhost:8081/api` | `http://localhost:8080/api` | Base API (mode `liveBackend`) |
| `liveBackend` | `false` | `false` | Bascule stub/réel |
| `anglerEmail` / `anglerPassword` | ✓ | — | Identifiants pêcheur de recette |
| `staffEmail` / `staffPassword` | — | ✓ | Identifiants staff de recette |

Les identifiants par défaut sont des **placeholders de recette** : à renseigner avec le
seed #67. Ne jamais committer de secret réel — surcharger via `--env` ou `CYPRESS_*`.

## Scripts npm

### Mobile

```bash
npm run serve          # serveur de dev (port 8081), requis pour les e2e
npm run cypress        # ouvre l'UI Cypress
npm run cypress:run    # e2e headless (dossier e2e/ uniquement)
npm run cypress:bench  # bancs CV/perf (dossier bench/)
npm run cypress-report # bancs + rapport HTML mochawesome (tests/cypress/reports/)
```

### Admin

```bash
npm install            # Cypress n'est pas encore installé côté admin
npm run serve          # serveur de dev (port 8082)
npm run cypress:open   # ouvre l'UI Cypress
npm run cypress:run    # e2e headless
```

## Rapport mochawesome (bancs)

Le plugin `cypress-mochawesome-reporter` n'est enregistré **que** si le run utilise ce
reporter (`plugins/index.js` teste `config.reporter`). Sans ce garde-fou, le hook
`after:run` échoue sur un run en reporter `spec` (`cypress:run`) faute de JSON à fusionner.
Le rapport HTML n'est donc produit que par `cypress-report`, dans `tests/cypress/reports/`
(dossier gitignoré).

## Choix techniques notables

- **Admin en JavaScript, pas TypeScript** : l'app admin est en `typescript` 6, dont le
  `ts-node` embarqué par Cypress refuse de compiler une config `.ts` (`moduleResolution`
  déprécié → erreur). Les fichiers Cypress admin sont donc en `.js`, ce qui est aussi
  cohérent avec la suite mobile (100 % `.js`). Le typage de `cy.loginStaff` est conservé
  via `support/index.d.ts`.
- **Séparation e2e / bench** portée par `specPattern` : la config par défaut ne discovers
  que `tests/cypress/e2e/**` ; les bancs se lancent via un script dédié (`--spec bench/**`).

## Prérequis d'exécution

- **Node** + dépendances installées (`npm install`). Côté admin, `cypress` est déclaré
  mais pas encore installé.
- Pour les e2e mobiles : **serveur front lancé** (`npm run serve`, port 8081).
- Pour `liveBackend=true` : **stack backend démarrée et seedée** (#67).

## Amorce CI (à faire)

Aucun job e2e n'existe encore dans `.gitlab-ci.yml`. Piste : un job dédié, en headless,
gaté sur `tests/cypress/e2e/` uniquement (les bancs restent hors pipeline), avec artefacts
de rapport. La politique de gating (bloquant / informatif) reste à décider.

## Hors de ce socle

La **refonte complète des scénarios** E2E (couverture des écrans opérateur : gestion,
import CSV, saisie manuelle, changement de mot de passe ; parcours pêcheur) fait l'objet
d'une issue dédiée. Ce document ne couvre que le cadrage, le nettoyage et l'amorce.

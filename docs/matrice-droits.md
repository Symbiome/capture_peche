# Matrice des droits par profil (#188)

**Version :** 2.0-draft — septembre 2026 — remplace `matrice_droits_nasse_vMO.ods` (v1.1 MO, août 2026)
**Statut :** arbitrages A1 à A9 reçus le 29/09/2026 (commentaire sur #188) ; A8 (Superset) reporté
**Donnée d'entrée de :** #79 (Volet B — profils paramétrables)

La colonne « Administrateur » de la v1.1 est scindée en **Administrateur régional**
(« administrateur local » dans les remarques UFBRMC) et **Administrateur national**,
conformément aux rôles déjà codés dans `fishola-backend`.

## Rôles et gardes

| Profil | Données en base (`fishola_admin`) | Garde minimale | Périmètre |
|---|---|---|---|
| Pêcheur | `fishola_user` (pas de compte staff) | `getUserIdOrRenew()` | ses propres données |
| Opérateur | `is_operator = true` | `checkIsStaff()` | `fishola_admin_departments` |
| Administrateur régional | `is_operator = false`, `is_national_admin = false` | `checkIsAdmin()` | `fishola_admin_departments` |
| Administrateur national | `is_national_admin = true` | `checkIsNationalAdmin()` | aucun (tout) |

Gardes : `AbstractFisholaResource.java` (`checkIsStaff` / `checkIsAdmin` /
`checkIsNationalAdmin` / `getAllowedAdminDepartments`). Un administrateur régional
ne gère les comptes que s'il a `can_create_admin = true`.

**Légende.** `Oui` autorisé · `Oui (dép.)` borné aux départements du compte ·
`Non` refusé · `N/A` non applicable · `?` à arbitrer.
Statut : ✅ conforme au code · ❌ écart (ticket de correction séparé) · ⏳ reporté.
La colonne « L » renvoie au numéro de ligne de la v1.1 MO.

## Matrice

### Compte & profil

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 6 | Créer son compte | Oui | Non | Non | Non | `PUT /security/register` (public) ; comptes staff créés par un admin | ✅ |
| 7 | Modifier son propre profil (mot de passe, consentement RGPD) | Oui | Oui | Oui | Oui | `PUT /security/profile`, `/settings`, `/password` ; staff : `PUT /admin/password` (`checkIsStaff`) | ✅ |
| 8 | Voir la liste des pêcheurs | Non | Oui (dép.) | Oui (dép.) | Oui | `GET /security/users` : `checkIsStaff` + pêcheurs ayant pêché dans le périmètre ou dont le code postal y est ; `GET /admin/competitions/search-users` : `checkIsStaff`, recherche nationale pour les concours (voulu) | ✅ |
| 9 | Créer un compte opérateur | Non | Non | Oui (dép., si `can_create_admin`) | Oui | `POST /admin/operators` : `checkIsAdmin` + `can_create_admin` ; opérateur rattaché au périmètre du créateur (#164) | ✅ |
| 10 | Modifier un compte opérateur | Non | Non | Oui (dép., si `can_create_admin`) | Oui | `PUT /admin/operators/{id}`, `PUT /admin/{id}` : compte cible inclus dans le périmètre, périmètre vide refusé | ✅ |
| 11 | Désactiver un compte | Non | Non | ? | Oui | pêcheur : `DELETE /security/users/{id}` (`checkIsNationalAdmin`) ; staff : aucun endpoint | ❌ E6 |
| 12 | Voir code postal / année de naissance | Non | Oui (dép.) | Oui (dép.) | Oui | exposés par `GET /security/users`, borné comme L8 | ✅ |
| 13 | Accéder au journal d'audit | Non | Non | Non | Oui | `GET /admin/audit-log` : `checkIsNationalAdmin` | ✅ |

### Sessions de pêche

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 15 | Créer une session | Oui | N/A | N/A | N/A | `POST /trips/` | ✅ |
| 16 | Voir ses propres sessions | Oui | N/A | N/A | N/A | `GET /trips/` | ✅ |
| 17 | Voir toutes les sessions | Non | Oui (dép.) | Oui (dép.) | Oui | `GET /trips/export[/…]` : `checkIsStaff` + périmètre | ✅ |
| 18 | Modifier / supprimer une session | Oui (les siennes) | Non | ? | Oui | pêcheur : `PUT`/`DELETE /trips/{id}` ; staff : aucun endpoint | ❌ E7 |

### Prises

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 20 | Saisir une prise | Oui | N/A | N/A | N/A | via `POST`/`PUT /trips/` | ✅ |
| 21 | Voir ses propres prises | Oui | N/A | N/A | N/A | `GET /trips/{id}` | ✅ |
| 22 | Voir toutes les prises | Non | Oui (dép.) | Oui (dép.) | Oui | liste : export (cf. L17) ; unitaire : `GET /trips/catches/{id}` ; `checkIsStaff` + dép. | ✅ |
| 23 | Modifier / supprimer une prise | Oui (les siennes) | Non | Oui (dép.) | Oui | `PUT /trips/catches/{id}` : `checkIsStaff` + dép. ; l'opérateur est limité aux prises à valider (cf. L26) ; suppression staff = exclusion des exports | ✅ |
| 24 | Saisir une note de certitude | Oui | N/A | N/A | N/A | champ de la prise | ✅ |
| 25 | Voir les prises à valider | Non | Oui (dép.) | Oui (dép.) | Oui | `GET /trips/catches/pending-validation/…` : `checkIsStaff` + dép. | ✅ |
| 26 | Valider / corriger une prise incertaine | Non | Oui (dép.) | Oui (dép.) | Oui | `PUT /trips/catches/{id}` : `checkIsStaff` + dép. (#87) ; opérateur : identification non certaine et pas encore validée | ✅ |

### Entités hydrographiques

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 28 | Voir les lacs / cours d'eau | Oui | Oui (dép.) | Oui (dép.) | Oui | public : `/waterEntities/*` ; staff : `GET /referential/waterEntities` borné | ✅ |
| 29 | Ajouter un lac / cours d'eau | Non | Non | Non | Non | supprimé de l'interface (#3), référentiel importé (BD TOPO) | ✅ |
| 30 | Modifier un lac / cours d'eau (référence : BD TOPO) | Non | Non | Non | Non | pas d'édition de l'entité (A1) | ✅ |
| 30b | Paramétrer un plan d'eau (tailles autorisées, alias d'espèces) | Non | Non | Oui (dép.) | Oui | `PUT /referential/authorized-samples` (`checkIsAdmin` + dép.), `PUT /referential/species-aliases-per-waterEntity` (`checkIsAdmin` **sans périmètre**) | ❌ E9 |
| 31 | Supprimer un lac / cours d'eau | Non | Non | Non | Non | aucun endpoint (référentiel BD TOPO) | ✅ |

### Espèces

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 33 | Voir la liste des espèces | Oui | Oui | Oui | Oui | `GET /referential/species` (public) | ✅ |
| 34 | Ajouter / modifier / supprimer une espèce | Non | Non | Non | Oui | `POST`/`PUT`/`DELETE /referential/raw-species` : `checkIsNationalAdmin` (A2) | ✅ |
| 35 | Activer la déclaration obligatoire (renvoi vers un outil externe) | Non | Non | Non | Oui | `species.mandatory_report` via `PUT /referential/raw-species` (A3) | ✅ |
| 36 | Renseigner le lien de déclaration | Non | Non | Non | Oui | `species.report_link`, idem (A3) | ✅ |

### Fédérations & départements

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 38 | Voir ses départements | Non | Oui (les siens) | Oui (les siens) | Oui | `GET /admin/check` (`departmentCodes`) (A4) | ✅ |
| 39 | Créer / modifier une fédération | N/A | N/A | N/A | N/A | sans objet : pas d'entité Fédération (A4) | ✅ |
| 40 | Attribuer des départements à un opérateur | Non | Non | Oui (tout son périmètre, #164) | Oui | `PUT /admin/operators/{id}` ; une prise relève du département où tombe son point GPS (A5) | ✅ |

### Récompenses & badges

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 42 | Voir ses badges | Oui | N/A | N/A | N/A | `GET /gamification/me/badges` | ✅ |
| 43 | Partager un badge | Oui | N/A | N/A | N/A | côté app mobile | ✅ |
| 44 | Créer un concours / attribuer le badge concours | Non | Oui (dép.) | Oui (dép.) | Oui | `/admin/competitions/*` : `checkIsStaff` + dép. (#90) | ✅ A6 |
| 45 | Créer / modifier les définitions de badges ; attribuer un badge quelconque | Non | Non | Non | Oui | catalogue en migrations ; `GET /admin/gamification/badges`, `POST …/attribute` : `checkIsNationalAdmin` | ✅ |

### Déclarations obligatoires

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 47 | Accéder au lien de déclaration (externe) | Oui | N/A | N/A | N/A | alerte app après validation d'une prise (#91) | ✅ |
| 48 | Voir les déclarations des pêcheurs | N/A | N/A | N/A | N/A | sans objet : la déclaration est faite sur l'outil externe (A3) | ✅ |

### Import / export & statistiques

| L | Fonctionnalité | Pêcheur | Opérateur | Adm. régional | Adm. national | Code actuel | Statut |
|---|---|---|---|---|---|---|---|
| 50 | Voir ses propres statistiques | Oui | N/A | N/A | N/A | `GET /dashboard`, `/evolution/personal/*` | ✅ |
| 51 | Importer les données (hors périmètre bloqué) | Non | Oui (dép.) | Oui (dép.) | Oui | `/admin/imports/*`, `/admin/manual-entries/*` : `checkIsStaff` ; ligne hors périmètre rejetée (`REF_WATER_ENTITY_SCOPE`) (A7) | ✅ |
| 52 | Exporter les données (CSV) | Non | Oui (dép.) | Oui (dép.) | Oui | `GET /trips/export` : `checkIsStaff` + dép. | ✅ |
| 53 | Accéder aux tableaux de bord Superset | Non | Oui (?) | Oui (?) | Oui | hors `fishola-backend` (DWH anonymisé) | ⏳ A8 |
| 54 | Export RGPD de ses données personnelles | Oui | N/A | N/A | N/A | `GET /dashboard/export` | ✅ |
| 55 | Supprimer un compte : données personnelles identifiantes seulement | Oui (le sien) | Non | Non | Oui | pêcheur : `DELETE /security/profile` anonymise (identité, contact, code postal, permis de pêche) ; national : `DELETE /security/users/{id}` supprime le compte (permis en cascade), détache les sorties (`unsetOwner`) (A9) | ✅ |

## Écarts constatés (un ticket de correction par écart)

| Id | Gravité | Écart | Lignes |
|---|---|---|---|
| ~~E4~~ | Corrigé | **Périmètre vide = national (fail-open).** Un compte non national sans département voyait tout (reproduit : régional 74 → `PUT /admin/{son id}` avec `departmentCodes: []` → l'export contenait le 73). Corrigé dans #188 : `getAllowedAdminDepartments()` renvoie un département fictif (`NO_DEPARTMENT_PERIMETER`) qui ne correspond à aucune donnée, et `PUT /admin/{id}` refuse (400) un périmètre vide pour un compte non national (`DepartmentalScopeTest#regionalCannotEmptyItsOwnPerimeter`, `#staffWithoutDepartmentSeesNothing`). | 10, 40 |
| ~~E3~~ | Corrigé | `PUT /admin/{id}` et `PUT /admin/operators/{id}` ne vérifiaient pas que le compte **cible** était dans le périmètre d'un régional. Corrigé dans #188 : un modificateur non national ne modifie qu'un compte non national dont **tous** les départements sont dans son périmètre, sinon 403 (`DepartmentalScopeTest#regionalCannotEditOperatorOutsidePerimeter`, `#regionalCannotEditNationalAdmin`). | 10, 40 |
| ~~E2~~ | Sans objet | `GET /admin/competitions/search-users` renvoie tout pêcheur à tout opérateur : **comportement voulu** (arbitrage du 29/09/2026 : tout pêcheur peut être trouvé par l'opérateur pour un concours). Verrouillé par `DepartmentalScopeTest#operatorFindsAnglersOfAllDepartmentsForCompetitions`. | 8 |
| ~~E1~~ | Corrigé | Liste des pêcheurs (et CP / année de naissance) réservée au national. Corrigé dans #188 : `GET /security/users` ouvert à tout le staff ; hors national, un pêcheur relève d'un département s'il y a au moins une sortie **ou** si son code postal y est (`Departments.fromPostalCode` : deux premiers chiffres, Corse 2A/2B, DOM 97x). Modification et suppression restent nationales ; l'écran « Pêcheurs » est en lecture seule hors national (`DepartmentalScopeTest#anglerListIsScopedToDepartments`, `DepartmentsTest`). | 8, 12 |
| ~~E5~~ | Corrigé | Export des sorties/prises en `checkIsAdmin` : l'opérateur n'avait ni la liste ni l'export CSV. Corrigé dans #188 : `GET /trips/export` et `/trips/export/{…}` passent en `checkIsStaff`, toujours bornés au périmètre (`DepartmentalScopeTest#operatorExportIsScopedToItsDepartments`). | 17, 22, 52 |
| ~~E8~~ | Corrigé | L'opérateur pouvait corriger **toute** prise de son périmètre. Corrigé dans #188 : `PUT /trips/catches/{id}` refuse (403) à l'opérateur une prise certaine ou déjà validée ; les administrateurs corrigent toute prise de leur périmètre. La page de détail passe en consultation seule pour l'opérateur (`DepartmentalScopeTest#operatorCorrectsOnlyCatchesPendingValidation`). | 23, 26 |
| E9 | Moyenne | `PUT /referential/species-aliases-per-waterEntity` : `checkIsAdmin` sans contrôle de périmètre — un régional modifie alias/absences d'espèces de n'importe quel plan d'eau. | 30 |
| E6 | Basse | Aucun endpoint de désactivation d'un compte staff. | 11 |
| E7 | Basse | Aucun endpoint staff de modification/suppression d'une sortie. | 18 |
| ~~E10~~ | Corrigé | L'anonymisation (`UsersDao.safeDeleteByAnonymiseUser`) ne vide pas `fishola_user.postal_code` et ne supprime pas les permis de pêche téléversés (`fishola_user_licences.content`), alors que A9 impose de supprimer toutes les données personnelles identifiantes. Corrigé dans #188 (`LicenceDaoTest#testAnonymisingUserDeletesLicencesAndPostalCode`). | 55 |

## Arbitrages UFBRMC (commentaire du 29/09/2026 sur #188)

| Id | L | Question | Décision | Effet sur le code |
|---|---|---|---|---|
| A1 | 30-31 | Modification d'un lac / cours d'eau par l'opérateur | Aucun droit pour le moment ; la référence est la BD TOPO | Conforme : aucun endpoint d'édition |
| A2 | 34 | Espèces : portée nationale ou ajout localisé ? | L'opérateur n'a aucun droit d'ajout, de modification ni de suppression sur la liste des espèces | Conforme : national uniquement |
| A3 | 35-36, 48 | « Activer déclaration obligatoire » | Renvoyer vers un outil externe dont le lien est renseigné par un administrateur | Conforme : `species.mandatory_report` + `report_link` (#91) ; L48 sans objet |
| A4 | 38-39 | « Voir sa fédération » | Voir ses départements | Conforme : `GET /admin/check` ; pas d'entité Fédération |
| A5 | 40 | Zones limitrophes entre fédérations | L'opérateur gère à l'échelle du département ; une prise relève du département où tombe son point GPS | Conforme : `catch.department` par jointure spatiale (#159). Sans GPS (imports, saisie manuelle), repli sur le département du plan d'eau |
| A6 | 44 | Concours / badge pour l'opérateur | Déjà tranché par #90 : attribution manuelle du badge concours oui, définitions de badges non | Conforme |
| A7 | 51 | Import dépassant le périmètre | Bloquer les données hors périmètre | Conforme : chaque ligne hors périmètre est rejetée ; en mode « tout ou rien », rien n'est inséré |
| A8 | 53 | Superset : cloisonnement et création de dashboards | À arbitrer et implémenter plus tard | Reporté |
| A10 | 44 | Quels pêcheurs l'opérateur peut-il trouver pour un concours ? | Tous les pêcheurs, sans limite de département | Conforme : `search-users` est national |
| A9 | 55 | La suppression de compte supprime-t-elle les données ? | Seulement les données personnelles identifiantes | Conforme : sorties et prises conservées ; code postal et permis de pêche supprimés à l'anonymisation (E10 corrigé) |

## Couverture de tests

Tests dans `fishola-backend/src/test/java/fr/inrae/fishola/rest/security/` — un endpoint
représentatif par domaine et par rôle, limité aux lignes **conformes** (les écarts seront
couverts par leurs tickets de correction).

| Domaine | Opérateur (`OperatorAccessTest`, `DepartmentalScopeTest`) | Adm. régional (`RegionalAdminScopeTest`, `DepartmentalScopeTest`) |
|---|---|---|
| Compte & profil | audit 403, création d'opérateur 403 | liste des opérateurs 200, liste des pêcheurs 200 (bornée), suppression de pêcheur 403, audit 403 |
| Sessions / prises | export et liste paginée bornés (`operatorExportIsScopedToItsDepartments`) ; prises à valider 200 ; prise hors dép. lecture/validation 403, dans dép. 200 | export 200 (borné) ; prises à valider 200 ; prise hors dép. 403 |
| Entités hydrographiques | liste bornée aux départements | liste bornée aux départements |
| Espèces | création 403 | création 403 ; météo 403 |
| Fédérations & départements | attribution de départements 403 | rattachement forcé au périmètre (#164) |
| Récompenses | concours 200 ; définitions de badges 403 | concours 200 ; définitions de badges 403 |
| Import / export | saisie manuelle accessible (400 de validation) | export borné (`DepartmentalScopeTest`) |

## Reste à faire (hors code)

- [ ] Arbitrer A8 (Superset) avec l'UFBRMC.
- [ ] Ouvrir les tickets E6, E7 et E9.
- [ ] Référencer ce document depuis #79.

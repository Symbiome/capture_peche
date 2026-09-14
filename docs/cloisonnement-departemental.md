# Cloisonnement départemental (#159)

Suite de #156 (table `departement` + contours). Migration `V2.0.0`.

## Périmètre d'un compte staff

Le périmètre d'un `fishola_admin` (administrateur régional **ou** opérateur) est
une **liste de codes département INSEE**, table `fishola_admin_departments
(fishola_admin_id, department_code)` — plus une liste explicite d'entités hydro
(l'ancienne `fishola_admin_water_entities`, supprimée par `V2.0.0`, dont les
périmètres sont repris via `water_entity.department`).

- Pas de FK sur `department_code` : le référentiel est la constante Java
  `Departments.NAMES` (même choix que `species_by_department`, #133) ; validation
  applicative par `Departments.isValidCode()`.
- Un compte **national** (`is_national_admin = true`) a un périmètre vide, qui
  signifie « aucune restriction ».
- Côté back-end : `AbstractFisholaResource.getAllowedAdminDepartments()` renvoie
  `Set<String>` (vide = national). `AdminDao.getAllowedDepartments(id)` et
  `getAllowedWaterEntityIds(id)` (entités dérivées du périmètre, pour l'import).
- Claims JWT / beans : `departmentCodes` (au lieu de `waterEntityIds`).
- UI : `Operators.vue` / `Admins.vue` — multi-select alimenté par
  `GET /v1/referential/departments` (`[{code, name}]`, les 101 départements) ; ne
  charge plus le référentiel hydro complet (antipattern OOM #154).

## Département d'une sortie / d'une prise

`trip.department` et `catch.department` (`varchar(3)`, indexés) mémorisent le
**département où l'action a eu lieu**, calculé à chaque **ajout / édition** :

1. jointure spatiale `ST_Contains(departement.geom, <point>)` — `trip` :
   `snapped_position` puis `begin_position` puis `end_position` ; `catch` :
   `position` ;
2. repli sur `water_entity.department` (`trip`) / `trip.department` (`catch`)
   quand il n'y a pas de position : imports opérateur, saisie manuelle, carnet
   volontaire, sorties « a posteriori » sans GPS.

`trip.water_entity_id` / `catch.trip_id` restent la source du rattachement hydro :
le département est une donnée **complémentaire**.

Points d'appel : `TripsDao.stampDepartment` / `CatchsDao.stampDepartment`
(`TripResource` création/édition sortie et prise, `putCatch`), et
`ImportDao.stampDepartment` (import CSV, saisie manuelle, carnet).

## Lecture / export cloisonnés

`TripResource` — `/export`, `/export/{...}`, `GET`/`PUT /catches/{id}` : passés de
`checkIsNationalAdmin()` à `checkIsAdmin()`.

- Un staff **régional** ne voit / n'exporte / n'édite que les prises de ses
  départements (`catchs_openadom_export.departement = ANY(périmètre)`, `403` sur
  une prise hors périmètre).
- Un **national** voit tout (périmètre vide → pas de filtre).
- Les vues d'export `catchs_export`, `catchs_openadom_export`,
  `personal_catchs_export` exposent la colonne `departement` (dernière position).

Écrans admin `Catches.vue` / `CatchEditionPage.vue` : ouverts à tout le staff
(le back-end borne).

## Non couvert

- **Dashboard admin par département** : pas d'écran admin de dashboard
  aujourd'hui (`/global-dashboard` est anonyme, par entité) — à câbler quand cet
  écran existera.
- Entité `Federation` (rattachement opérateur → fédération) : hors périmètre.

# Backlog — User stories

Backlog de user stories issues des échanges avec le MO, à créer comme issues GitHub
dans `Symbiome/capture_peche` et à rattacher au GitHub Project #1.

---

## #1 — Point GPS de fin de session

**Titre :** `[Feature] Ajout d'un point GPS de fin de session de pêche`
**Labels :** `enhancement`, `fishola-mobile`, `fishola-backend`

### User Story
En tant que **pêcheur**, je veux pouvoir enregistrer un point GPS
de fin de session afin de délimiter géographiquement ma zone de pêche.

### Contexte
Le point GPS de début de session existe déjà. Il faut ajouter
le point de fin sur le même modèle.

### Critères d'acceptance
- [ ] Le formulaire de fin de session affiche un bouton
      « Enregistrer ma position de fin »
- [ ] La position GPS est capturée au moment du clic (même
      mécanique que le point de début)
- [ ] Le point de fin est optionnel (non bloquant pour valider
      la session)
- [ ] Le point de fin est stocké en base (latitude + longitude)
- [ ] Les deux points sont affichés sur la carte de la session
      (début en vert, fin en rouge)
- [ ] L'API expose le point de fin dans le endpoint de session

### Notes techniques
- Chercher le champ `startLatitude` / `startLongitude` dans
  le modèle Session — dupliquer pour `endLatitude` /
  `endLongitude`
- Migration Flyway à créer pour ajouter les colonnes en base
- Mettre à jour le DTO et le mapper côté backend Java/Quarkus
- Mettre à jour le formulaire Vue.js côté fishola-mobile

---

## #2 — Note de certitude sur l'identification d'une prise

**Titre :** `[Feature] Note de certitude sur l'identification d'une prise`
**Labels :** `enhancement`, `fishola-mobile`, `fishola-backend`, `fishola-admin`

### User Story
En tant que **pêcheur**, je veux pouvoir indiquer mon niveau de
certitude sur l'identification de l'espèce capturée, afin que les
données incertaines soient revues par les techniciens des fédérations.

### Critères d'acceptance
- [ ] Lors de la saisie d'une prise, le pêcheur peut choisir
      une note de certitude parmi :
      Certain / Probable / Incertain (ou équivalent 1-3)
- [ ] La valeur par défaut est « Certain »
- [ ] La note est stockée en base sur le modèle Catch/Prise
- [ ] Dans l'espace administrateur, les prises avec certitude
      « Incertain » sont visibles dans une vue dédiée
      « Prises à valider »
- [ ] Un opérateur peut corriger l'espèce et marquer la prise
      comme validée
- [ ] Les prises non validées sont exclues des statistiques
      publiques (ou marquées comme « à confirmer »)

### Notes techniques
- Ajouter un champ `identificationCertainty` de type ENUM
  (`CERTAIN`, `PROBABLE`, `UNCERTAIN`) sur l'entité Catch
- Migration Flyway avec valeur par défaut `CERTAIN` pour
  l'existant
- Nouvelle vue dans fishola-admin :
  « Prises à valider » filtrée sur `UNCERTAIN` + `PROBABLE`
- Ajouter un champ `validatedBy` (userId) et `validatedAt`
  (timestamp) pour tracer les validations opérateur

### Rôles concernés
- Pêcheur : saisit la certitude
- Opérateur (technicien de fédération) : valide ou corrige

---

## #3 — Supprimer l'ajout de lac/plan d'eau dans l'admin

**Titre :** `[Chore] Supprimer la fonctionnalité d'ajout de lac/plan d'eau dans l'espace administrateur`
**Labels :** `cleanup`, `fishola-admin`

### User Story
En tant que **développeur**, je veux supprimer la fonctionnalité
d'ajout manuel de lac/plan d'eau dans l'espace administrateur, car
les milieux aquatiques sont désormais gérés via la base de données
géographique IGN (BD TOPO).

### Critères d'acceptance
- [ ] Le bouton / formulaire d'ajout de lac est supprimé de
      l'interface admin
- [ ] L'endpoint API correspondant est supprimé ou désactivé
      (retourner 410 Gone)
- [ ] La route Vue.js associée est supprimée
- [ ] Aucune régression sur les autres fonctionnalités
      de l'espace admin

### Notes techniques
- Rechercher dans fishola-admin les composants liés à
  la création de lac (probablement `LakeCreate`, `LakeForm`
  ou similaire)
- Ne pas supprimer la liste des lacs ni la consultation —
  uniquement la création/modification
- Vérifier qu'aucun autre composant n'appelle l'endpoint
  de création avant de le supprimer

---

## #4 — Supprimer la page « Prises par plan d'eau »

**Titre :** `[Chore] Supprimer la page "Prises par plan d'eau" de l'espace administrateur`
**Labels :** `cleanup`, `fishola-admin`

### User Story
En tant que **développeur**, je veux supprimer la page
« Prises par plan d'eau » de l'espace administrateur car cette
fonctionnalité n'est plus d'actualité.

### Critères d'acceptance
- [ ] La page « Prises par plan d'eau » est supprimée de l'admin
- [ ] Le lien de navigation vers cette page est retiré du menu
- [ ] La route Vue.js associée est supprimée
- [ ] L'endpoint API dédié est supprimé ou désactivé si
      non utilisé ailleurs
- [ ] Aucune régression sur les autres pages de l'admin

### Notes techniques
- Chercher dans fishola-admin la route et le composant
  correspondants (probablement `CatchesByLake`,
  `FishByPond` ou similaire)
- Vérifier dans le router Vue.js toutes les références
  avant suppression
- Supprimer également les appels API côté backend si
  l'endpoint n'est utilisé nulle part ailleurs

---

## #5 — Badge « Participation à un concours de pêche »

**Titre :** `[Feature] Badge de participation à un concours de pêche`
**Labels :** `enhancement`, `gamification`, `fishola-admin`, `fishola-mobile`

### User Story
En tant que **opérateur (technicien de fédération)**, je veux
pouvoir attribuer un badge de participation à un concours à des
pêcheurs sélectionnés, afin de valoriser leur engagement dans
les événements du réseau associatif.

### Critères d'acceptance
- [ ] Il existe un type de badge « Concours » dans le système
      de récompenses
- [ ] Un opérateur peut créer un concours (nom, date,
      plan d'eau ou cours d'eau, fédération organisatrice)
- [ ] Un opérateur peut associer des pêcheurs à un concours
      (attribution manuelle du badge)
- [ ] Le badge apparaît sur le profil du pêcheur dans
      l'app mobile avec le nom du concours et la date
- [ ] Le badge est partageable sur les réseaux sociaux
      (carte de partage existante)

### Notes techniques
- S'appuyer sur le système de récompenses (AchievementEngine)
  déjà prévu dans le cadrage
- Créer une entité `Competition` (nom, date, lieu,
  fédérationId) liée à `Achievement`
- L'attribution est manuelle (déclenchée par un opérateur),
  pas automatique — différent des badges auto
- Distinguer dans le modèle les badges auto (règles JSON)
  des badges manuels (attribués par un opérateur)
- Ajouter dans fishola-admin une section
  « Gestion des concours » sous le menu opérateur

---

## #6 — Déclaration obligatoire par espèce

**Titre :** `[Feature] Déclaration obligatoire et lien de déclaration par espèce`
**Labels :** `enhancement`, `fishola-backend`, `fishola-mobile`

### User Story
En tant que **pêcheur**, lorsque je saisis une prise d'une espèce
soumise à déclaration obligatoire, je veux être redirigé vers le
formulaire officiel de déclaration afin de respecter la réglementation.

### Critères d'acceptance
- [ ] La table `species` (ou équivalent) dispose de deux
      nouvelles colonnes :
      - `mandatory_report` BOOLEAN NOT NULL DEFAULT FALSE
      - `report_link` TEXT NULL
- [ ] Après validation d'une prise d'une espèce avec
      `mandatory_report = true`, l'app affiche une alerte
      claire : « Cette espèce nécessite une déclaration
      obligatoire » avec un bouton « Déclarer » ouvrant
      `report_link` dans le navigateur
- [ ] Si `report_link` est null, afficher un message
      générique invitant à contacter sa fédération
- [ ] Dans l'espace admin, un opérateur peut activer/désactiver
      `mandatory_report` et renseigner `report_link`
      pour chaque espèce

### Notes techniques
- Migration Flyway : ajouter `mandatory_report` et
  `report_link` sur la table espèces existante
- Mettre à jour le DTO `SpeciesDto` et le mapper
- Côté fishola-mobile : intercepter la validation de
  saisie de prise et vérifier le flag avant de naviguer
  vers l'écran suivant
- Le lien s'ouvre via `window.open(url, '_blank')` ou
  le deep link natif selon la plateforme

---

## #7 — Classification taxonomique complète des espèces (Darwin Core)

**Titre :** `[Feature] Classification taxonomique complète des espèces selon Darwin Core`
**Labels :** `enhancement`, `fishola-backend`, `fishola-admin`, `data`

### User Story
En tant que **opérateur** et **administrateur**, je veux que chaque
espèce de poisson soit décrite avec sa classification taxonomique
complète (nom scientifique, genre, espèce, famille, sous-famille)
selon le standard Darwin Core, afin d'assurer l'interopérabilité
des données avec les référentiels scientifiques (GBIF, Hub'eau).

### Critères d'acceptance
- [ ] La table `species` est enrichie avec les colonnes suivantes
      (nommage Darwin Core) :
      - `vernacular_name` TEXT — nom commun (existant, renommer
        si nécessaire)
      - `scientific_name` TEXT — nom binomial complet
        ex : *Esox lucius*
      - `genus` TEXT — genre ex : *Esox*
      - `specific_epithet` TEXT — épithète spécifique ex : *lucius*
      - `family` TEXT — famille ex : *Esocidae*
      - `subfamily` TEXT NULL — sous-famille (optionnelle)
      - `taxon_rank` TEXT DEFAULT 'SPECIES'
      - `taxon_id` TEXT NULL — identifiant GBIF/INPN si disponible
- [ ] L'affichage dans l'app mobile montre le nom vernaculaire
      en principal et le nom scientifique en secondaire (italique)
- [ ] Dans l'espace admin, le formulaire d'édition d'espèce
      expose tous ces champs
- [ ] La recherche d'espèce (saisie de prise) fonctionne sur
      le nom vernaculaire ET le nom scientifique

### Notes techniques
- Migration Flyway : ALTER TABLE sur la table espèces existante
- Ne pas casser les données existantes : les noms vernaculaires
  déjà en base restent dans `vernacular_name`
- Référence standard : https://dwc.tdwg.org/terms/#taxon
- Pour le jeu de données initial, les espèces de poissons
  d'eau douce de France sont disponibles sur INPN/GBIF —
  prévoir un script de migration des données (CSV → SQL)
- Mettre à jour `SpeciesDto`, le mapper et tous les endpoints
  qui exposent les espèces

### Données de référence
Prévoir un fichier `scripts/species_taxon_seed.sql` ou
`scripts/species_taxon_seed.csv` pour alimenter les nouvelles
colonnes sur les espèces déjà présentes en base.

---

## #8 — Gestion multi-opérateurs par fédération avec périmètre géographique

**Titre :** `[Feature] Gestion des opérateurs par fédération avec périmètre départemental`
**Labels :** `enhancement`, `fishola-backend`, `fishola-admin`, `auth`

### User Story
En tant que **administrateur**, je veux pouvoir créer des comptes
opérateurs rattachés à une fédération de pêche et leur attribuer
un ou plusieurs départements, afin que chaque opérateur ne puisse
agir que sur les entités hydrographiques de sa zone géographique.

En tant que **opérateur**, je veux que mes actions soient
automatiquement restreintes aux entités de mes départements,
sans avoir à filtrer manuellement.

### Critères d'acceptance

#### Modèle de données
- [ ] Entité `Federation` : id, nom, code (ex : FDAAPPMA 74)
- [ ] Entité `Department` : id, code INSEE (ex : « 74 »), nom
- [ ] Table de liaison `federation_departments` :
      federation_id, department_id
- [ ] Table de liaison `operator_departments` :
      user_id, department_id (un opérateur peut couvrir
      plusieurs départements)
- [ ] Un utilisateur de rôle `OPERATOR` est obligatoirement
      rattaché à une `Federation`

#### Interface administrateur
- [ ] Page « Fédérations » : liste, création, modification
      (nom, code, départements couverts)
- [ ] À la création/édition d'un compte opérateur,
      l'admin peut sélectionner :
      - La fédération de rattachement
      - Les départements attribués (multi-select,
        limité aux départements de sa fédération)
- [ ] Un opérateur peut avoir N comptes par fédération
      (pas de limite)

#### Contrôle d'accès
- [ ] Toutes les requêtes d'un opérateur sur les entités
      hydrographiques (lacs, cours d'eau, tronçons) sont
      filtrées automatiquement par ses départements
- [ ] Tentative d'accès à une entité hors périmètre →
      HTTP 403 avec message explicite
- [ ] Les sessions de pêche et prises restent visibles
      par tous les opérateurs (données scientifiques
      mutualisées) — seule la modification des entités
      hydrographiques est restreinte

#### Interface opérateur
- [ ] L'opérateur voit ses départements affectés dans
      son profil
- [ ] Les listes d'entités hydrographiques sont
      pré-filtrées sur ses départements

### Notes techniques

#### Modèle BDD (migrations Flyway)
```sql
CREATE TABLE federation (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT UNIQUE NOT NULL  -- ex: FDAAPPMA_74
);

CREATE TABLE department (
    id BIGSERIAL PRIMARY KEY,
    insee_code CHAR(3) NOT NULL UNIQUE,  -- 01 à 976
    name TEXT NOT NULL
);

CREATE TABLE federation_departments (
    federation_id BIGINT REFERENCES federation(id),
    department_id BIGINT REFERENCES department(id),
    PRIMARY KEY (federation_id, department_id)
);

CREATE TABLE operator_departments (
    user_id BIGINT REFERENCES fishola_user(id),
    department_id BIGINT REFERENCES department(id),
    PRIMARY KEY (user_id, department_id)
);

ALTER TABLE fishola_user
    ADD COLUMN federation_id BIGINT REFERENCES federation(id) NULL;
```

#### Contrôle d'accès (backend Java/Quarkus)
- Créer un `DepartmentSecurityFilter` ou intercepteur JAX-RS
  qui enrichit chaque requête OPERATOR avec ses department_ids
- Modifier les requêtes sur les entités hydrographiques
  pour ajouter un `WHERE department_code = ANY(:dept_codes)`
- Les entités hydrographiques (lacs, cours d'eau) doivent
  avoir un champ `department_code` INSEE — vérifier s'il
  existe déjà ou à ajouter via migration

#### Données de référence
- Prévoir `scripts/departments_seed.sql` avec les 101
  départements français (codes INSEE + noms)
- Prévoir `scripts/federations_seed.sql` avec les
  fédérations du bassin RM&C

### Note sur la terminologie
Les « opérateurs » sont les techniciens des fédérations
départementales de pêche (FDAAPPMA). Un opérateur ≠
un administrateur système.

---

## #9 — Code postal et année de naissance à l'inscription

**Titre :** `[Feature] Code postal et année de naissance à l'inscription`
**Labels :** `enhancement`, `backend`, `mobile`, `data`

### User Story
En tant que **pêcheur**, je veux renseigner mon code postal et mon
année de naissance lors de mon inscription, afin de permettre aux
fédérations d'analyser les données par zone géographique et
tranche d'âge.

### Critères d'acceptance
- [ ] Le formulaire d'inscription (fishola-mobile) comporte
      deux nouveaux champs obligatoires :
      - Code postal (5 chiffres, validation format français)
      - Année de naissance (entier, entre 1900 et année courante - 6)
- [ ] Les deux champs bloquent la soumission du formulaire
      s'ils sont absents ou invalides
- [ ] Les deux champs sont stockés en base sur l'entité
      `fishola_user`
- [ ] Les deux champs sont exposés dans l'espace admin
      (lecture seule) sur la fiche pêcheur
- [ ] Les données existantes en base ne sont pas affectées
      (colonnes NULLable pour les comptes existants,
      obligatoire uniquement à la création)

### Notes techniques
- Migration Flyway :
```sql
ALTER TABLE fishola_user
    ADD COLUMN postal_code CHAR(5) NULL,
    ADD COLUMN birth_year SMALLINT NULL;
```
- Contrainte applicative uniquement (pas de NOT NULL en base
  pour préserver les comptes existants)
- Validation côté backend :
  - `postal_code` : regex `^\d{5}$`
  - `birth_year` : entre 1900 et `YEAR(NOW()) - 6`
- Mettre à jour `UserDto`, le mapper et l'endpoint
  d'inscription (`POST /api/users` ou équivalent)
- Côté fishola-mobile : ajouter les champs au formulaire
  d'inscription avec messages d'erreur explicites

### Note RGPD
L'année de naissance (et non la date complète) est
suffisante pour l'analyse par tranche d'âge et limite
l'exposition de données personnelles. Ne pas collecter
le jour et le mois de naissance.

---

## #10 — Format horaire français (24h) sur tous les champs de saisie d'heure

**Titre :** `[Bug] Forcer le format horaire français (24h) sur les champs de type "time"`
**Labels :** `bug`, `ux`, `fishola-mobile`, `fishola-admin`

### User Story
En tant que **pêcheur** (ou **opérateur**), je veux saisir une heure au
format français 24h (ex : `13:00`, `22:15`) plutôt qu'au format anglais
avec AM/PM, afin de saisir mes heures de pêche facilement et sans erreur
d'interprétation.

### Contexte
Les champs `<input type="time">` HTML natifs affichent leur format
(24h ou 12h AM/PM) selon la locale du système d'exploitation ou du
navigateur de l'utilisateur, et non selon la langue de l'application.
Sur mobile en particulier, si la locale de l'appareil est en anglais
(ex : `en-US`), le sélecteur d'heure natif s'affiche en AM/PM même si
toute l'app est en français — l'attribut `lang="fr"` du document ne
suffit pas à forcer un format 24h de façon fiable sur tous les
navigateurs/OS (notamment iOS Safari, qui suit toujours la région de
l'appareil).
Le champ est utilisé à plusieurs endroits :
- `fishola-mobile/src/components/common/FormInput.vue` — composant
  générique utilisé avec `type="time"` par :
  - `fishola-mobile/src/components/trip/SomeTripSummary.vue`
    (heure de début / heure de fin de sortie)
  - `fishola-mobile/src/views/trip/TripMeta.vue` (idem, saisie a posteriori)
  - `fishola-mobile/src/views/trip/EditCatch.vue` (heure de capture)
- `fishola-admin/src/views/operator/ManualEntry.vue` — `<input type="time">`
  natif, saisi directement sans passer par `FormInput` (heure de début /
  heure de fin lors de la saisie manuelle opérateur)

### Critères d'acceptance
- [ ] Sur mobile et web, quel que soit le paramètre de langue/région
      de l'appareil de l'utilisateur, tous les champs de saisie d'heure
      de l'app pêcheur affichent et acceptent un format 24h
      (`HH:mm`, ex : `06:00`, `13:00`, `22:15`) — jamais de AM/PM
- [ ] Le comportement est identique sur le picker natif mobile
      (Android/iOS) et sur la saisie clavier
- [ ] La saisie manuelle au clavier n'accepte que des heures valides
      (`00:00` à `23:59`), avec un message d'erreur clair si le format
      est invalide
- [ ] Le même correctif est appliqué au champ « Heure de début » /
      « Heure de fin » de la saisie manuelle opérateur dans
      `fishola-admin`
- [ ] Aucune régression sur la valeur stockée en base ni sur le format
      transmis à l'API (reste `HH:mm` ou `HH:mm:ss` selon l'existant)
- [ ] Testé sur au moins un appareil/navigateur configuré en anglais
      (`en-US`) pour valider que le format reste bien 24h

### Notes techniques
- La solution robuste consiste à **ne plus dépendre du rendu natif
  du navigateur** pour le format d'affichage, celui-ci n'étant pas
  garanti par la locale de la page :
  - Remplacer `<input type="time">` par un composant de saisie d'heure
    dédié (ex : masque de saisie texte `HH:mm` avec
    `inputmode="numeric"` + validation par regex
    `^([01]\d|2[0-3]):[0-5]\d$`, ou une petite librairie Vue de
    time-picker configurable en 24h uniquement type
    `vue3-timepicker`)
  - Centraliser ce comportement dans `FormInput.vue` (gérer
    `type="time"` comme un cas spécifique plutôt que de le transmettre
    tel quel au `<input>` natif), pour que
    `SomeTripSummary.vue`, `TripMeta.vue` et `EditCatch.vue`
    bénéficient du correctif sans modification côté appelant
  - Appliquer le même composant/masque dans `ManualEntry.vue`
    (fishola-admin), qui n'utilise pas `FormInput` actuellement
- Conserver le binding `v-model` existant (string `HH:mm`) pour ne
  pas impacter le format envoyé à l'API Quarkus
- Vérifier qu'aucun style CSS actuel de `FormInput.vue` ne dépend du
  rendu natif du picker (ex : icône horloge fournie par le navigateur)

---

## #11 — Saisie opérateur des « carnets volontaires » (formulaire + import en masse)

**Suivi GitHub :** [#143](https://github.com/Symbiome/capture_peche/issues/143)

**Titre :** `[Feature] Import & saisie opérateur — format « carnet volontaire » (étendre #71/#72 au format dédié)`
**Labels :** `feat`, `backend`, `frontend`, `csv`, `data`

### User Story
En tant que **opérateur (technicien de fédération)**, je veux
pouvoir saisir les données de captures issues des carnets
volontaires remplis par les pêcheurs **soit via un formulaire
sortie par sortie, soit par import en masse d'un fichier**, afin
d'intégrer ces relevés terrain dans la base quel que soit le
volume reçu — un carnet isolé transmis par un pêcheur, ou un lot
de carnets collectés par la fédération — sans ressaisie manuelle
superflue ni obligation de passer par un fichier pour une seule
sortie.

### Contexte
Le « carnet volontaire » est l'un des deux modes de collecte
terrain (l'autre étant l'enquête, cf. #12). Le pêcheur volontaire
consigne lui-même ses sorties ; la fédération récupère ces
carnets (papier ou tableur) et les transmet à l'opérateur pour
saisie.

Deux volumétries coexistent en pratique : quelques carnets reçus
au fil de l'eau (saisie unitaire plus rapide au formulaire qu'un
export fichier) et des lots conséquents transmis en une fois
(import en masse indispensable). Les deux modes doivent donc
rester disponibles et appliquer **les mêmes règles de
validation**.

Le socle d'import CSV opérateur existe déjà **côté Quarkus**
(`fishola-backend/.../rest/imports/` — `ImportSchema`,
`ImportService`, `ImportDao`, issue #71 ; pipeline structurel →
référentiel → métier, idempotence, rapport ligne à ligne
`import_row_error`, `collection_method` déjà porté sur `trip`),
ainsi que l'écran de saisie manuelle générique
(`ManualEntryResource`/`ManualTripBean`, issue #72,
`fishola-admin/src/views/operator/ManualEntry.vue`). Ce socle ne
couvre aujourd'hui qu'un **format générique unique** à 28
colonnes — pas encore le format « carnet volontaire » tel que
transmis par le client. Cette issue **étend ce socle** au format
décrit dans `20260824_Formats de données et infos pecheurs
v2.xlsx` (onglet *Données_carnets volontaires*), pour l'import
**et** le formulaire de saisie unitaire.

> ⚠️ Le socle historique `fishola-backoffice/backoffice/imports/`
> (Django) référencé dans une version antérieure de cette US est
> **décommissionné** (arbitrage Symbiome du 04/09/2026, cf. #73 :
> redondant avec l'espace admin existant, aucune ossature
> conservée) : le code cible est exclusivement Quarkus (#71/#72),
> pas Django.

### Deux modes de saisie
- **Formulaire** : l'opérateur saisit une sortie (et ses
  captures) via un écran dédié dans `fishola-admin`
  (extension de `ManualEntry.vue`), équivalent fonctionnel d'une
  ligne (+ ses lignes capture) du fichier d'import — mêmes
  champs, mêmes obligations, mêmes listes déroulantes (espèce,
  technique, milieu résolu par recherche plutôt que par
  nom+commune en texte).
- **Import en masse** : dépôt d'un fichier CSV/XLSX couvrant
  plusieurs sorties (cf. format cible ci-dessous).

Les deux modes partagent la même couche de validation métier
(mêmes codes d'erreur, mêmes bornes d'aberration par espèce) afin
qu'une sortie saisie au formulaire et une sortie importée par
fichier soient traitées de façon strictement identique en base.

### Format cible — une ligne = une capture (ou un lot)
Regroupement des lignes en sorties par un identifiant de sortie
commun (`session_ref` ou équivalent). Une sortie sans capture
(`bredouille = oui`) tient sur une seule ligne sans champs
capture.

**Niveau sortie de pêche :**

| Champ | Oblig. | Modalités / notes |
|-------|--------|-------------------|
| Secteur pêché | oui | entité hydro résolue par nom + commune |
| Date | oui | `JJ/MM/AAAA` |
| Mode de pêche | oui | bateau / float tube-canoë / bord itinérant / bord statique |
| Heure de début | oui | `HH:mm` 24h |
| Heure de fin | oui | `HH:mm` 24h, > heure de début |
| Technique principale | oui | pêche au coup / au leurre / aux appâts naturels / à la mouche / de la carpe / au vif |
| Technique secondaire | non | même liste (mode « Avancé ») |
| Nombre de lignes | oui | entier > 0 |
| Appât / type de leurre | non | texte / liste (mode « Avancé ») |
| Espèce recherchée | oui | nom vernaculaire liste simplifiée + modalité `Aucune` |
| Observations diverses | non | liste : cormorans, harle bièvre, pollution… (multi-valeurs) |
| Bredouille | oui | `oui`/`non` — si `oui`, aucune ligne capture attendue |

**Niveau capture :**

| Champ | Oblig. | Modalités / notes |
|-------|--------|-------------------|
| Espèce | oui | vernaculaire → code espèce en base |
| Origine TRF (si espèce = TRF) | non | naturelle / déversement / inconnue (mode « Avancé ») |
| Technique | oui | même liste, pré-rempli d'après la sortie |
| Appât / type de leurre | non | pré-rempli d'après la sortie (mode « Avancé ») |
| Heure de la prise | non | `HH:mm` (mode « Avancé ») |
| Taille | oblig. pour espèces à définir | taille exacte **ou** classe de taille, en cm |
| Poids | non | grammes |
| Nombre | oui | ≥ 1 ; > 1 ⇒ saisie par lot |
| Poisson conservé | oui | `oui`/`non` |
| Taille min / Taille max | si lot | cm, obligatoires si `Nombre > 1` |
| Poisson marqué ou bagué | non | `oui`/`non` ou texte libre |
| N° / identifiant de marquage | oblig. si marqué | texte libre |

### Critères d'acceptance

#### Formulaire de saisie unitaire
- [ ] Un écran « Nouvelle sortie — carnet volontaire » permet à
      l'opérateur de saisir une sortie et ses captures sans
      passer par un fichier
- [ ] Les mêmes champs, obligations et modalités que le tableau
      « Format cible » ci-dessous s'appliquent : secteur (résolu
      par recherche d'entité hydro), date, mode de pêche,
      horaires, technique(s), nombre de lignes, espèce
      recherchée, observations, bredouille
- [ ] Comportement dynamique : `Bredouille = oui` masque le bloc
      capture ; `Nombre > 1` sur une capture affiche `Taille min`
      / `Taille max` ; `Espèce = TRF` affiche `Origine TRF` ; les
      champs du mode « Avancé » (technique secondaire, appât/leurre,
      heure de la prise…) sont repliés par défaut
- [ ] L'opérateur peut ajouter plusieurs captures à la même
      sortie avant d'enregistrer
- [ ] La validation (référentielle et métier, mêmes codes
      d'erreur que l'import) s'exécute à la saisie/soumission du
      formulaire ; les erreurs bloquantes empêchent
      l'enregistrement, les signalements (ex. taille aberrante)
      s'affichent en avertissement non bloquant
- [ ] La sortie créée porte `collection_method = 'carnet_volontaire'`
      et `source = 'operator_form'` (à distinguer de
      `operator_import`)
- [ ] Une sortie saisie au formulaire reste modifiable/supprimable
      par un opérateur
- [ ] L'interface opérateur « Carnets volontaires » propose les
      deux modes dès l'entrée (ex. deux onglets « Saisir une
      sortie » / « Importer un fichier »)

#### Import en masse
- [ ] Un modèle de fichier `template-carnet-volontaire.csv`
      (en-tête figé, séparateur `;`, UTF-8) est téléchargeable
      depuis l'interface d'import opérateur
- [ ] L'import accepte le CSV **et** le XLSX (première feuille)
- [ ] Les lignes d'un même identifiant de sortie sont
      regroupées en une seule `Trip` ; chaque ligne capture
      devient un `Catch` rattaché
- [ ] Validation structurelle : en-tête conforme, dates
      `JJ/MM/AAAA`, heures `HH:mm`, `heure_fin > heure_debut`,
      `mode_peche` ∈ liste, `bredouille` ∈ {oui, non},
      `nombre` entier ≥ 1
- [ ] Validation référentielle : `secteur pêché` résolu en
      entité hydro (nom + commune), `espèce` et `technique`
      résolues sur les référentiels ; échec ⇒ ligne rejetée
      avec code d'erreur (`REF_WATER_ENTITY`, `REF_SPECIES`,
      `REF_TECHNIQUE`)
- [ ] Validation métier : `bredouille = oui` ⇒ pas de capture
      (sinon `METIER_BREDOUILLE`) ; `bredouille = non` ⇒ au
      moins une capture ; taille hors bornes espèce ⇒
      `METIER_SIZE_ABERRANT` (ligne signalée, non bloquante,
      cf. détection valeurs aberrantes du cadrage) ;
      `Nombre > 1` sans `taille min`/`taille max` ⇒
      `METIER_QUANTITY`
- [ ] Champ `Origine TRF` ignoré si l'espèce n'est pas la
      truite fario ; stocké sinon
- [ ] `Espèce recherchée = Aucune` accepté et stocké tel quel
- [ ] Import idempotent : réimporter le même fichier ne crée
      pas de doublon (empreinte SHA-256 dans `import_job`)
- [ ] Mode « simulation » (dry-run) : l'opérateur voit le
      rapport de validation (lignes OK / rejetées / signalées)
      **avant** d'écrire en base
- [ ] Rapport d'import téléchargeable (CSV) : n° de ligne,
      colonne, étage, code, message
- [ ] Toutes les sorties/captures créées portent
      `collection_method = 'carnet_volontaire'` et
      `source = 'operator_import'`
- [ ] L'action d'import est tracée dans le journal d'activité
      (opérateur, fichier, nombre de lignes, date)

### Notes techniques
- Le formulaire de saisie unitaire construit en mémoire la même
  structure de ligne (+ lignes capture) que le parseur de fichier,
  puis appelle les mêmes validations référentiel/métier que
  l'import (`ImportService`, `fishola-backend/.../rest/imports/`)
  avant écriture (`ImportDao`) — un seul chemin de
  validation/persistance pour les deux modes, le formulaire
  n'ajoute qu'une saisie côté UI en amont du parseur
- Étendre `ImportSchema.EXPECTED_HEADER` (en-tête dédié « carnet
  volontaire » ou variante du format générique existant — à
  trancher, cf. #65) plutôt que de mélanger les deux formats
  (ils divergent trop — cf. #12)
- Support XLSX en plus du CSV : conversion en lignes puis même
  pipeline
- Champs sans colonne de destination en base à ce stade
  (technique secondaire, appât/leurre, observations diverses,
  origine TRF, marquage + n°, heure de prise, taille min/max
  de lot) : dépendent des extensions de modèle #13 — les
  valider dès maintenant, ne les persister qu'une fois #13
  livré (même logique que les champs `enquete_*` déjà validés
  sans être persistés dans le socle #71)
- `Nombre`, `size_class` : colonnes `catch.quantity` /
  `catch.size_class` déjà présentes
- Regroupement en `Trip` : clé = identifiant de sortie du
  fichier ; `Trip.name` = `secteur pêché` ; `Trip.mode` =
  `mode_peche`

**Détail complet (schéma de fichier précis, tâches, fichiers concernés) : voir l'issue GitHub [#143](https://github.com/Symbiome/capture_peche/issues/143).**

### Rôles concernés
- Opérateur : dépose le fichier, relit le rapport, confirme
- Administrateur : accès aux mêmes imports, tous périmètres

---

## #12 — Saisie opérateur des « enquêtes terrain » (formulaire guidé + import en masse)

**Suivi GitHub :** [#144](https://github.com/Symbiome/capture_peche/issues/144)

**Titre :** `[Feature] Import & saisie opérateur — format « enquête terrain » (multi-tables, sous-module survey)`
**Labels :** `feat`, `backend`, `frontend`, `csv`, `data`

### User Story
En tant que **opérateur (technicien de fédération)**, je veux
saisir les données recueillies lors des enquêtes terrain — où un
enquêteur se rend sur un site et interroge les pêcheurs présents
sur leur sortie en cours, leurs techniques et leur dernière
sortie — **soit via un formulaire guidé pendant ou juste après la
campagne, soit par import en masse d'un fichier une fois
l'enquête dépouillée**, afin de consolider ces campagnes
d'enquête dans la base quel que soit le moment et le volume de
saisie.

### Contexte
L'enquête est le second mode de collecte terrain (cf. #11 pour
le carnet volontaire). Contrairement au carnet, le format
d'enquête est **relationnel multi-tables**, décrit dans
`20260824_Formats de données et infos pecheurs v2.xlsx`
(onglet *Données_enquêtes*) :

```
Session d'enquête (1)
  └── Sortie de pêche (n)          [Code session → Code sortie]
        └── Capture pêcheur (n)    [Code sortie, Code pêcheur]
              └── captures de la sortie en cours
Session souvenir (n)  — bloc facultatif, sortie passée du même pêcheur
      [Code pêcheur]
```

Un même **Code sortie** regroupe plusieurs pêcheurs (bateau ou
spot partagé). Un même **Code pêcheur** relie la sortie en cours
et sa « session souvenir » (dernière sortie du pêcheur, décrite
de mémoire).

### Deux modes de saisie
- **Formulaire guidé (assistant)** : l'opérateur (ou l'enquêteur
  lui-même, sur tablette/terrain) déroule la hiérarchie
  interactivement plutôt que de remplir un fichier :
  1. ouvrir/choisir une **session d'enquête** (secteur, date,
     comptages non-enquêtés) ;
  2. ajouter une **sortie** au sein de cette session (horaires) ;
  3. ajouter un ou plusieurs **pêcheurs interrogés** à cette
     sortie, chacun avec ses réponses (mode/technique/espèce
     recherchée, bredouille, captures) ;
  4. pour chaque pêcheur, ajouter en option le bloc **session
     souvenir** (sa dernière sortie, décrite de mémoire).
  Les identifiants `Code session` / `Code sortie` / `Code
  pêcheur` sont générés automatiquement par l'application (plus
  besoin de les saisir/inventer comme dans le fichier).
- **Import en masse** : dépôt d'un classeur/jeu de fichiers
  couvrant une ou plusieurs sessions d'enquête complètes (cf.
  format cible ci-dessous) — adapté à une campagne dépouillée
  a posteriori sur tableur.

Les deux modes reconstituent la même structure de graphe
(`Session d'enquête` → `Sortie` → `Capture pêcheur` [+ `Session
souvenir`]) et partagent la même validation métier.

### Format cible

**Table `Session d'enquête` :**

| Champ | Oblig. | Notes |
|-------|--------|-------|
| Code session | oui | identifiant unique de la session (clé de liaison) |
| Secteur | oui | menu déroulant des sites pré-enregistrés par la FD → entité hydro |
| Date | oui | `JJ/MM/AAAA` |
| Nb pêcheurs carnassiers du bord observés non-enquêtés | non | entier ≥ 0 |
| Nb pêcheurs carnassiers en bateau observés non-enquêtés | non | entier ≥ 0 |

**Table `Sortie de pêche` :**

| Champ | Oblig. | Notes |
|-------|--------|-------|
| Code session | oui | FK vers `Session d'enquête` |
| Code sortie | oui | regroupe plusieurs pêcheurs d'une même embarcation / d'un même spot |
| Heure du contrôle | oui | `HH:mm` |
| Heure de début de pêche | oui | `HH:mm` |
| Heure de fin de pêche prévue | oui | `HH:mm` |

**Table `Capture pêcheur` (sortie en cours) :**

| Champ | Oblig. | Notes |
|-------|--------|-------|
| Code sortie | oui | FK vers `Sortie de pêche` |
| Code pêcheur | oui | identifiant de l'enquêté au sein de la sortie |
| Département origine | non | liste départements FR, ou pays si non-résident |
| Espèce recherchée | oui | vernaculaire + modalités `Toutes espèces` et `Aucune` |
| Mode de pêche | oui | bateau / float tube / bord itinérant / bord statique |
| Technique | oui | pêche au coup / au leurre / aux appâts naturels / à la mouche / de la carpe de nuit / au vif |
| Nombre de lignes | oui | entier > 0 |
| Appât / type de leurre | non | mode « Avancé » |
| Bredouille | oui | `oui`/`non` — si `oui`, pas de capture |
| Espèce (capture) | oui si non bredouille | vernaculaire → code espèce |
| Taille | oui | taille exacte ou classe de taille, cm |
| Nombre | oui | ≥ 1 ; > 1 ⇒ lot |
| Poisson conservé | oui | `oui`/`non` |
| Taille min / Taille max | si lot | cm (ex. « 5 BRO à 74 cm ») |

**Table `Session souvenir` (sortie passée — bloc facultatif) :**
mêmes champs que `Capture pêcheur` mais rattachés au `Code
pêcheur` seul, plus :

| Champ | Oblig. (si souvenir) | Notes |
|-------|----------------------|-------|
| Site pêché | oui | localisation ou saisie guidée → entité hydro |
| Date de la sortie | oui | `JJ/MM/AAAA` |
| Période de la journée | oui | matin / après-midi / journée entière / soirée (à confirmer : durée estimée ?) |

### Critères d'acceptance

#### Formulaire guidé (assistant)
- [ ] Un assistant « Nouvelle session d'enquête » permet de créer
      une session (secteur, date, comptages non-enquêtés) sans
      passer par un fichier
- [ ] Depuis une session ouverte, l'opérateur ajoute une ou
      plusieurs sorties, puis pour chaque sortie un ou plusieurs
      pêcheurs interrogés (mêmes champs/obligations que le
      tableau « Capture pêcheur » ci-dessous), avec la même
      logique dynamique que #11 (`Bredouille = oui` masque le
      bloc capture, `Nombre > 1` affiche les bornes de taille)
- [ ] Pour chaque pêcheur, un bloc « Session souvenir » facultatif
      peut être ajouté (mêmes champs que le tableau dédié
      ci-dessous)
- [ ] `Code session`, `Code sortie`, `Code pêcheur` sont attribués
      automatiquement par l'application (non saisis par
      l'opérateur) ; pas de risque de collision/erreur de saisie
      sur ces identifiants en mode formulaire
- [ ] La session reste modifiable (ajout d'une sortie/d'un
      pêcheur supplémentaire) tant qu'elle n'a pas été exportée
      vers le DWH — utile en enquête terrain où les pêcheurs
      interrogés s'enchaînent sur la journée
- [ ] La validation référentielle et métier (mêmes codes d'erreur
      que l'import) s'applique à chaque ajout
- [ ] Les sorties/captures créées via l'assistant portent
      `source = 'operator_form'` (vs `operator_import` pour le
      fichier), `collection_method` inchangé (`'enquete'` /
      `'enquete_souvenir'`)
- [ ] L'interface opérateur « Enquêtes terrain » propose les deux
      modes dès l'entrée (« Nouvelle session (assistant) » /
      « Importer un fichier »)

#### Import en masse
- [ ] Modèles de fichiers téléchargeables depuis l'interface
      d'import opérateur — au choix retenu à l'implémentation
      (cf. Notes techniques) :
      - soit un classeur XLSX à 4 feuilles
        (`session_enquete`, `sortie`, `capture_pecheur`,
        `session_souvenir`)
      - soit 4 CSV séparés reliés par les codes
- [ ] L'import reconstitue le graphe :
      `Session d'enquête` → `Sortie` → `Capture pêcheur`,
      plus les `Session souvenir` rattachées par `Code pêcheur`
- [ ] Chaque `Sortie` (code sortie) devient une `Trip` ;
      chaque `Capture pêcheur` d'un pêcheur distinct de la même
      sortie devient **une `Trip` distincte** rattachée à la
      même session d'enquête (un pêcheur = une expérience de
      pêche propre : mode, technique, nb lignes, espèce
      recherchée peuvent différer)
- [ ] Chaque `Session souvenir` devient une `Trip` datée de la
      sortie passée, avec `collection_method = 'enquete_souvenir'`
- [ ] Validation d'intégrité référentielle du fichier :
      tout `Code session` cité dans `Sortie` doit exister ;
      tout `Code sortie` cité dans `Capture pêcheur` doit
      exister ; sinon rejet de la ligne avec code
      `REF_CODE_SESSION` / `REF_CODE_SORTIE`
- [ ] Validation structurelle : dates, heures 24h,
      `mode_peche` ∈ liste, `bredouille` ∈ {oui, non},
      entiers ≥ 0 pour les comptages « non-enquêtés »
- [ ] Validation référentielle : `Secteur` / `Site pêché`
      résolus en entité hydro ; `Espèce` et `Technique`
      résolues ; `Espèce recherchée` accepte `Toutes espèces`
      et `Aucune`
- [ ] Validation métier : `bredouille = oui` ⇒ aucune capture ;
      taille hors bornes espèce ⇒ signalement
      `METIER_SIZE_ABERRANT` ; `Nombre > 1` sans bornes ⇒
      `METIER_QUANTITY`
- [ ] Les comptages « pêcheurs observés non-enquêtés » (bord /
      bateau) sont stockés au niveau de la session d'enquête
      (utilisés pour redresser l'effort de pêche à l'analyse)
- [ ] `Département origine` : validé (liste FR ou pays),
      stocké **sans lien nominatif** avec un compte pêcheur
      (l'enquêté n'a pas de compte) — donnée d'analyse, pas
      donnée personnelle directe
- [ ] Mode simulation (dry-run) + rapport de validation avant
      écriture, comme #11
- [ ] Import idempotent (empreinte SHA-256 sur l'ensemble du
      dépôt)
- [ ] Rapport d'import téléchargeable (par table, n° de ligne,
      code, message)
- [ ] Action tracée dans le journal d'activité

### Notes techniques
- L'assistant de saisie appelle les mêmes fonctions de
  validation/persistance que l'import (`rest/imports/survey/`,
  cf. ci-dessous), une session/sortie/pêcheur à la fois, plutôt
  qu'un lot de lignes — même logique que le formulaire de #11
  vis-à-vis du pipeline d'import
- S'appuyer sur le socle Quarkus existant
  (`fishola-backend/.../rest/imports/`, issue #71) mais dans un
  module dédié `rest/imports/survey/` : le format relationnel ne
  rentre pas dans le pipeline « une ligne = une capture » du
  format générique actuel. Cadrage initial : #32 (« esquisse du
  sous-module enquêtes offline »)
- **Décision de format à trancher avec le MO / l'équipe** :
  classeur XLSX multi-feuilles (plus lisible pour l'opérateur,
  nécessite `openpyxl`) vs. 4 CSV (plus simple à diff/versionner).
  Recommandation Symbiome : XLSX multi-feuilles, un onglet par
  table, en-têtes figés
- Ordre de résolution : parser et valider les 4 tables en
  mémoire → contrôler l'intégrité des codes → résoudre les
  référentiels → étage métier → `_persist` transactionnel
  (rollback global si `mode = full`, partiel si `mode = partial`)
- `Code pêcheur` / `Code sortie` : identifiants **locaux au
  fichier**, ne pas les confondre avec les `owner_id` /
  `trip.id` de la base. Les conserver dans un mapping le temps
  de l'import ; éventuellement les tracer dans une colonne
  `trip.external_ref` (migration légère) pour l'auditabilité
- Table `session d'enquête` : pas d'équivalent direct en base
  aujourd'hui. Option minimale v1 : stocker `code session`,
  `secteur`, `date` et les 2 comptages dans une table
  `survey_session` (nouvelle) et rattacher les `Trip` via une
  FK `trip.survey_session_id` nullable — cf. #13
- Champs sans destination tant que #13 n'est pas livré
  (appât/leurre, période de journée, département origine
  détaillé) : validés, non persistés
- `collection_method` : `'enquete'` pour la sortie en cours,
  `'enquete_souvenir'` pour la session passée — ajouter ces
  valeurs à `ImportSchema.COLLECTION_METHODS`

**Détail complet (schéma de fichier précis, tâches, fichiers concernés) : voir l'issue GitHub [#144](https://github.com/Symbiome/capture_peche/issues/144).**

### Rôles concernés
- Opérateur : dépose le classeur, relit le rapport, confirme
- Administrateur : idem, tous périmètres

---

## #13 — Extensions du modèle de données pour les imports terrain

**Suivi GitHub :** [#145](https://github.com/Symbiome/capture_peche/issues/145)

**Titre :** `[Feature] Extensions du schéma BDD Quarkus pour les formats carnet volontaire / enquête`
**Labels :** `feat`, `backend`, `data`

### User Story
En tant que **développeur**, je veux étendre le schéma de la BDD
applicative pour accueillir les champs des formats « carnet
volontaire » (#11) et « enquête » (#12) qui n'ont pas de
colonne de destination aujourd'hui, afin que ces imports
persistent l'intégralité des données terrain et non un
sous-ensemble.

### Contexte
Le socle d'import Quarkus (#71/#72, `V1.2.0__Import_csv_socle.sql`)
valide déjà des champs qu'il ne stocke pas encore, en attendant
les colonnes cibles. Les formats client « carnet volontaire »
(#11 / [#143](https://github.com/Symbiome/capture_peche/issues/143))
et « enquête » (#12 / [#144](https://github.com/Symbiome/capture_peche/issues/144))
ajoutent d'autres champs dans le même cas. Cette issue regroupe
ces extensions de modèle, prérequis à la persistance complète
des deux imports.

### Périmètre — champs à héberger

**Sur la sortie (`trip`) :**
- [ ] `expected_species_id` / modalités `Aucune` et
      `Toutes espèces` (espèce recherchée)
- [ ] `secondary_technique_id` (technique secondaire — carnet)
- [ ] `bait_or_lure` TEXT (appât / type de leurre)
- [ ] `rod_count` SMALLINT (nombre de lignes) — cf. issue I-G
      si déjà tracée
- [ ] `fishing_mode` ENUM (bateau / float tube-canoë /
      bord itinérant / bord statique) — cf. issue I-G
- [ ] `trip_observations` (observations diverses : cormorans,
      harle bièvre, pollution… — liste multi-valeurs, carnet)
- [ ] `day_period` ENUM (matin / après-midi / journée / soirée
      — session souvenir enquête)
- [ ] `external_ref` TEXT (code sortie / code pêcheur du
      fichier source, auditabilité)

**Sur la capture (`catch`) :**
- [ ] `trout_origin` ENUM NULL (naturelle / déversement /
      inconnue — uniquement si espèce = TRF)
- [ ] `lot_min_size_cm` / `lot_max_size_cm` SMALLINT NULL
      (bornes de taille d'un lot, distinctes des bornes
      d'aberration par espèce `species_size_bounds`)
- [ ] `is_tagged` BOOLEAN + `tag_reference` TEXT NULL
      (poisson marqué ou bagué + n°)
- [ ] `bait_or_lure` TEXT (pré-rempli d'après la sortie)

**Nouvelle table `survey_session` (enquête) :**
- [ ] `code` TEXT UNIQUE, `water_entity_id` FK, `day` DATE
- [ ] `unsurveyed_shore_anglers` SMALLINT — pêcheurs
      carnassiers du bord observés non-enquêtés
- [ ] `unsurveyed_boat_anglers` SMALLINT — idem en bateau
- [ ] `trip.survey_session_id` FK NULL vers `survey_session`

**Profil de l'enquêté (non nominatif) :**
- [ ] Réutiliser / créer `surveyed_angler` (cf. issue I-F si
      tracée) : `origin_department` CHAR(3) NULL,
      `origin_country` TEXT NULL, `code` (local au fichier)
- [ ] `trip.surveyed_angler_id` FK NULL
- [ ] **Aucune donnée personnelle directe** : l'enquêté n'a
      pas de compte, seul le département (ou pays) d'origine
      est conservé pour l'analyse

### Critères d'acceptance
- [ ] Migration Flyway `fishola-backend/src/main/resources/db/migration/`
      (prochaine version après `V1.7.0`) créant les colonnes et
      tables ci-dessus, toutes **NULLables** — aucun impact sur
      les données existantes
- [ ] Les enums sont documentés et alignés avec les modalités
      du fichier `20260824_Formats de données et infos
      pecheurs v2.xlsx`
- [ ] `ImportDao`/`ManualEntryService` (Quarkus,
      `fishola-backend/.../rest/imports/`) exposent les nouveaux
      champs
- [ ] `DATABASE.md` mis à jour
- [ ] Les imports #11 et #12 persistent alors la totalité des
      champs validés (retrait des mentions « validé mais non
      persisté »)

**Détail complet : voir l'issue GitHub [#145](https://github.com/Symbiome/capture_peche/issues/145).**

### Notes techniques
- Vérifier l'existence préalable des issues I-F
  (`surveyed_angler`) et I-G (`fishing_mode` / `rod_count`)
  citées dans `imports/service.py` : si elles existent, cette
  issue ne fait que les référencer ; sinon elle les absorbe
- Coordonner le nommage avec la cible Darwin Core du DWH
  (ex. `trout_origin` → `establishmentMeans` / `pathway` selon
  DwC) pour limiter les transformations ETL ultérieures
- Ne pas dupliquer `species_size_bounds` : les bornes de lot
  (`lot_min/max_size_cm`) sont une donnée saisie, pas une règle
  de validation

---

## #14 — Conserver le nom réel de l'entité hydrographique (désambiguïsation hors libellé)

**Titre :** `[Feature] Afficher le nom réel de l'entité hydrographique, désambiguïser par l'identifiant et non par le libellé`
**Labels :** `enhancement`, `fishola-backend`, `fishola-mobile`, `data`
**Lié à :** #117 (cours d'eau suffixés), #107 (plans d'eau homonymes, clos), #115

### User Story
En tant que **pêcheur**, je veux voir le nom réel de mon plan d'eau
ou de mon cours d'eau (« Ruisseau du Moulin », « Lac Blanc »), et
non un libellé technique (« Ruisseau du Moulin_COURDEAU0000002491701923 »,
« Lac Blanc (Valloire) » collé de force), afin de retrouver et
reconnaître facilement le milieu où je pêche.

### Contexte
Aujourd'hui, `water_entity.name` et `water_entity.export_as` portent
chacun une contrainte `UNIQUE`. Comme les toponymes BD TOPO ne sont
pas uniques (« Lac Blanc » = 17 plans d'eau, « Ruisseau des Combes »
= 27 cours d'eau homonymes), l'import
(`scripts/import_hydro_gpkg.sql`) construit un « escalier de
nommage » qui **modifie le nom lui-même** pour le rendre unique :
toponyme nu → `toponyme (commune)` → `toponyme_cleabs`. Résultat :
16,4 % des cours d'eau des 7 départements chargés portent un
identifiant technique dans leur libellé (#117), illisible pour le
pêcheur.

Le nom affiché ne devrait **jamais** être l'endroit où l'on assure
l'unicité. La base dispose déjà de deux identifiants uniques et
stables pour cela :
- `water_entity.id` (UUID, PK) — la sélection côté app se fait
  déjà par `id` partout (autocomplétion, carte, proximité,
  attribution) ;
- `water_entity.bdtopo_cleabs` (identifiant national BD TOPO,
  `UNIQUE`, renseigné à 100 % sur les données importées) ;
- `water_entity.water_entity_code` (`varchar(10)`, `UNIQUE`,
  aujourd'hui quasi inutilisé) reste disponible pour un code
  métier (ex. `code_hydrographique` SANDRE).

### Principe retenu
Séparer le **nom d'affichage** (brut, non contraint) de la
**clé d'unicité** (identifiant technique) et du **libellé d'export**
(stable, tracé) :

| Champ | Rôle | Unicité |
|-------|------|---------|
| `name` | nom affiché au pêcheur = toponyme BD TOPO brut | **non unique** (contrainte supprimée) |
| `id` / `bdtopo_cleabs` | identité technique, sélection, ré-import | déjà `UNIQUE` |
| `export_as` | libellé stable pour les exports Darwin Core (`nom_du_site`, `nom_de_la_plateforme`) | reste `UNIQUE`, garde l'escalier de désambiguïsation |

La désambiguïsation **pour l'œil du pêcheur** ne passe plus par le
nom mais par une information de contexte affichée **à côté** du nom
(commune du centroïde, code postal) — déjà exposée par l'API
(`WaterEntitySearchResult.commune` / `codePostal`, US #6/#15) et
déjà rendue dans `LakeSelection.vue` (`.suggestion-commune`).

### Critères d'acceptance
- [ ] `water_entity.name` ne porte plus de suffixe technique ni de
      qualifiant entre parenthèses : il vaut exactement le toponyme
      BD TOPO (`coalesce(nullif(toponyme, ''), cleabs)`)
- [ ] La contrainte `UNIQUE` sur `water_entity.name`
      (`water_entity_name_key`) est supprimée par migration Flyway
- [ ] `water_entity.export_as` reste unique et conserve l'escalier
      de désambiguïsation (nu → `(commune)` → `_cleabs`) ; les vues
      et exports Darwin Core existants sont inchangés
- [ ] `scripts/import_hydro_gpkg.sql` (sections 1 plan d'eau et
      2 cours d'eau) écrit le toponyme brut dans `name` et n'applique
      l'escalier que sur `export_as`
- [ ] L'import reste **idempotent** : rejeu multi-passes et
      multi-départements sans oscillation de libellé ni violation de
      contrainte (cf. avertissement #117)
- [ ] Dans l'autocomplétion « Plan d'eau » / cours d'eau, deux
      entités homonymes s'affichent avec le même nom + la commune en
      sous-texte pour les distinguer ; la sélection reste faite par
      `id` (aucune ambiguïté fonctionnelle)
- [ ] Aucune régression sur : recherche texte (accent/typo),
      recherche par commune, « autour de moi », attribution
      hydrographique d'une sortie, tuiles vectorielles
- [ ] Les sorties (`trip`) existantes conservent leur rattachement
      (`water_entity_id` inchangé) — seul le libellé affiché change
- [ ] Les ~2 000 cours d'eau et les plans d'eau actuellement
      suffixés retrouvent leur nom nu après ré-import

### Notes techniques
- Migration Flyway `fishola-backend/src/main/resources/db/migration/`
  (prochaine version) :
  ```sql
  ALTER TABLE water_entity DROP CONSTRAINT water_entity_name_key;
  ```
  Conserver `water_entity_export_as_key`, `water_entity_bdtopo_cleabs_key`,
  `water_entity_water_entity_code_key`.
- `scripts/import_hydro_gpkg.sql` :
  - section 1 (`plan_d_eau`) et section 2 (`cours_d_eau`) : dans le
    `INSERT ... SELECT`, remplacer `final_name` par `base_name` pour
    la colonne `name`, garder `final_name` pour `export_as` ;
  - le `ON CONFLICT (bdtopo_cleabs) DO UPDATE` met `name = base_name`,
    `export_as = final_name` ;
  - l'escalier de la section 2 (cours d'eau) reste sur `export_as`
    uniquement ; le choix du discriminant lisible (commune d'une
    extrémité, cf. #117) peut être traité séparément puisqu'il ne
    concerne plus que `export_as`.
- Backend Java : `HydroSearchDao` renvoie déjà `we.name` tel quel et
  joint la commune — rien à changer côté SQL de recherche. Vérifier
  qu'aucun code ne reconstruit un libellé à partir de `name` +
  parenthèses.
- Frontend `LakeSelection.vue` : `formatCommune` / `.suggestion-commune`
  existent déjà ; s'assurer que la commune est affichée **aussi**
  dans la liste « autour de moi » (`NearbyList`) et dans le libellé
  de l'entité sélectionnée quand un homonyme existe.
- Mettre à jour `DATABASE.md` (règle : `name` = toponyme brut non
  unique, `export_as` = libellé désambiguïsé unique).

### Rôles concernés
- Pêcheur : voit le nom réel
- Opérateur : distingue les homonymes par la commune / `export_as`
  dans l'admin

---

## #15 — Marqueurs des sorties visibles à tout niveau de zoom sur la carte « Mes sorties »

**Titre :** `[Bug] Marqueurs des sorties invisibles quand la carte « Mes sorties » est complètement dézoomée`
**Labels :** `bug`, `fishola-mobile`, `carto`, `frontend`

### User Story
En tant que **pêcheur**, sur la page « Mes sorties » onglet
« Carte », je veux voir les marqueurs de mes sorties à **tous les
niveaux de zoom, y compris carte complètement dézoomée**, afin de
garder une vue d'ensemble de tous les lieux où j'ai pêché.

### Contexte
Composant `fishola-mobile/src/components/my-trips/MyTripsMap.vue`
(carte MapLibre, #33), affiché par
`fishola-mobile/src/views/TripsListAndMap.vue` (onglet « Carte »).
Les sorties sont rendues via une source GeoJSON `catches` avec
clustering natif (`clusterRadius: 50`, `clusterMaxZoom: 14`),
au-dessus du fond IGN + réseau hydro (`buildFisholaStyle`).
À l'ouverture de l'onglet, `fitToMarkers()` cadre la vue sur
l'emprise de tous les marqueurs (`fitBounds`, `maxZoom: 13`).

Quand l'utilisateur dézoome à la main au-delà de ce cadrage
initial, les marqueurs (pins isolés et/ou clusters) ne sont plus
visibles : la carte paraît vide.

### Critères d'acceptance
- [ ] Sur l'onglet « Carte » de « Mes sorties », à **n'importe quel
      niveau de zoom atteignable**, au moins un marqueur ou cluster
      représentant les sorties reste affiché tant qu'il existe ≥ 1
      sortie géolocalisée
- [ ] Carte complètement dézoomée, les sorties dispersées se
      regroupent en un ou plusieurs clusters **visibles et
      cliquables** (le clic re-zoome sur l'amas), compteur inclus
- [ ] Un dézoom (pincement ou bouton −) ne peut pas amener la carte
      dans un état où l'emprise des marqueurs sort entièrement du
      cadre sans moyen d'y revenir
- [ ] Une action « recadrer sur mes sorties » (bouton sur la carte)
      ramène la vue sur l'emprise de tous les marqueurs, comme le
      fait `fitToMarkers()` à l'ouverture
- [ ] Le fond de carte reste lisible (pas d'écran gris) au zoom
      minimal autorisé
- [ ] Vérifié avec : 1 sortie ; plusieurs sorties proches ;
      plusieurs sorties très éloignées (ex. Haute-Savoie + un autre
      bassin) ; aucune sortie géolocalisée (message inchangé)
- [ ] Aucune régression sur le cadrage initial, le clic de cluster
      (`getClusterExpansionZoom`), le clic de pin isolé et
      l'infobulle

### Notes techniques (hypothèses à confirmer)
- Définir un `minZoom` explicite sur la carte (options du
  constructeur `maplibregl.Map`), cohérent avec une emprise
  nationale (≈ 4–5), pour empêcher un dézoom au-delà de l'utile et
  garder le fond IGN lisible. Le fond `ign-plan` (`maxzoom: 19`,
  pas de `minzoom`) a peu de contenu aux très bas zooms.
- Vérifier que les couches `catch-clusters`, `catch-cluster-count`
  et `catch-unclustered` n'ont pas de borne de zoom basse et
  rendent au-dessus du fond à tous les zooms. La source `catches`
  (GeoJSON) n'a pas de `minzoom`, mais l'index `supercluster`
  n'indexe que l'intervalle `[0, clusterMaxZoom]` : borner le
  `minZoom` carte dans cet intervalle évite un niveau sans aucun
  cluster.
- Ajouter un contrôle « recadrer » (bouton MapLibre personnalisé
  réutilisant `fitToMarkers()`), et/ou rappeler `fitToMarkers()`
  sur `zoomend`/`moveend` lorsque l'emprise des marqueurs devient
  entièrement hors champ.
- Contrôler le comportement de `renderWorldCopies` (défaut) aux bas
  zooms : selon la largeur du conteneur, les marqueurs peuvent être
  projetés hors de la bande visible.
- Reproduire sur mobile réel (le composant est aussi cité dans la
  piste mémoire #128 — ne pas réintroduire de fuite en
  ré-instanciant la carte).

### Rôles concernés
- Pêcheur : consulte la carte de ses sorties

---

## #16 — Bouton de confirmation de position hors écran sur mobile (carte de saisie)

**Titre :** `[Bug] Le bouton de confirmation de la position saisie sur la carte est hors écran sur mobile`
**Labels :** `bug`, `fishola-mobile`, `carto`, `frontend`

### User Story
En tant que **pêcheur sur mobile**, quand je place un point GPS sur
la carte pour situer ma **session** ou ma **prise**, je veux que le
bouton de validation reste visible et atteignable, afin de pouvoir
confirmer la localisation.

### Contexte
Sur mobile, après un tap sur la carte de saisie, le bouton de
confirmation apparaît **trop bas** : il est masqué par la barre de
navigation basse et/ou sort de la zone visible du navigateur. La
sélection de la localisation devient **impossible à valider**, aussi
bien pour une session que pour une prise.

Deux parcours concernés :
- **Session** : `LakeSelection.vue` ouvre `MapLibreMap.vue`
  (`.map` en `position: fixed`, `bottom: @footer-height`,
  `max-height: calc(100dvh - @header-height - @secondary-header-height
  - @footer-height - 10px)`), puis le tap déclenche `onMapClick`
  → `AttributionConfirmSheet.vue`. Cette bottom-sheet est un overlay
  `position: fixed; inset: 0; align-items: flex-end`, feuille en
  `max-height: 80vh; overflow-y: auto`, actions **Annuler / Confirmer**
  en dernière ligne.
- **Prise** : `EditCatch.vue` affiche `MapLibrePositionMap.vue`
  en ligne dans le formulaire (marqueur déplaçable, clic-pour-placer) ;
  la validation est le bouton « Enregistrer » du formulaire, repoussé
  sous la ligne de flottaison quand la carte est haute.

Causes probables (à confirmer) :
- l'overlay `AttributionConfirmSheet` couvre `inset: 0` sans retirer
  `@footer-height` : sa ligne d'actions en `flex-end` passe **derrière
  la barre de navigation** fixe (`@footer-height = 76px +
  safe-area-inset-bottom`) ;
- `max-height: 80vh` : sur mobile `vh` ignore les barres dynamiques du
  navigateur → la feuille dépasse la zone réellement visible
  (utiliser `dvh` / `svh`, comme déjà fait pour `.map`, cf. #97) ;
- pas de marge basse `env(safe-area-inset-bottom)` sur la feuille ;
- côté prise, la hauteur de `.map` dans `EditCatch` n'est pas bornée
  pour les petits écrans, poussant le bouton d'enregistrement hors vue.

### Critères d'acceptance
- [ ] Sur mobile (y compris petit écran, ex. hauteur ~640 px, et
      navigateur avec barre d'URL visible), après un tap sur la carte
      pour situer une **session**, la bottom-sheet d'attribution
      s'affiche entièrement : proposition, alternatives **et** la ligne
      **Annuler / Confirmer** sont visibles sans être masquées par la
      barre de navigation
- [ ] Si le contenu de la feuille dépasse la hauteur disponible, il
      défile à l'intérieur de la feuille, la ligne d'actions restant
      **fixée en bas et visible** (actions non défilantes)
- [ ] Même garantie pour la saisie de la **position d'une prise**
      (`EditCatch`) : après avoir placé/déplacé le marqueur, le bouton
      d'enregistrement reste atteignable sans scroll hasardeux
- [ ] La carte de saisie reste correctement cadrée sur le point placé
      sur écran étroit (le pin visé n'est pas caché sous la sheet ni
      hors cadre)
- [ ] Respect des zones sûres iOS/Android
      (`env(safe-area-inset-bottom)`) : aucun chevauchement avec la
      barre système ni la barre de navigation de l'app
- [ ] Aucune régression desktop (la sheet reste centrée,
      `@media (min-width: @desktop-min-width)`)
- [ ] Vérifié sur iOS Safari et Android Chrome, en création **et** en
      édition

### Notes techniques
- `AttributionConfirmSheet.vue` :
  - contraindre l'overlay au-dessus de la barre de navigation
    (`bottom: @footer-height` sur mobile, ou `padding-bottom`) et
    remplacer `80vh` par une unité dynamique
    (`max-height: min(80svh, calc(100dvh - @footer-height))`) ;
  - rendre `.actions` non défilant : `position: sticky; bottom: 0` (+
    fond blanc) à l'intérieur de `.attribution-sheet`, le corps
    au-dessus en `overflow-y: auto` ;
  - ajouter `padding-bottom: env(safe-area-inset-bottom)`.
- `MapLibreMap.vue` : quand la sheet est ouverte sur mobile, réduire /
  recentrer la carte pour que le pin posé reste visible au-dessus de
  la sheet (`map.easeTo` avec un `padding.bottom` égal à la hauteur de
  la sheet, ou `flyTo` sur le point).
- `EditCatch.vue` : borner la hauteur de `.map` (ex.
  `max-height: 45svh`) et garder la barre d'action du formulaire
  visible (déjà `position: absolute` ligne ~1152 — vérifier le
  `bottom` vs `@footer-height` et le safe-area).
- Réutiliser le fix `dvh`/`vh` de #97 (`App.vue`) : les feuilles et
  modales doivent suivre la même règle que `.map`.
- Ajouter un test e2e / capture sur viewport court validant que le
  bouton « Confirmer » est dans le viewport après un tap carte.

### Rôles concernés
- Pêcheur : place et confirme la position d'une session ou d'une prise

---

## Récap

| # | Titre court | Complexité | Priorité |
|---|-------------|-----------|---------|
| 1 | GPS fin de session | Faible | Sprint 0 |
| 2 | Note de certitude sur identification | Moyenne | Sprint 0 |
| 3 | Supprimer ajout lac dans admin | Faible | Sprint 0 |
| 4 | Supprimer page « prises par plan d'eau » | Faible | Sprint 0 |
| 5 | Badge concours de pêche | Moyenne | Sprint 1 |
| 6 | Déclaration obligatoire par espèce | Faible | Sprint 0 |
| 7 | Classification taxonomique Darwin Core | Haute | Sprint 0 |
| 8 | Multi-opérateurs + périmètre départemental | Haute | Sprint 1 |
| 9 | Code postal + année de naissance à l'inscription | Faible | Sprint 0 |
| 10 | Format horaire français (24h) sur champs "time" | Moyenne | Sprint 0 |
| 11 | Carnets volontaires : formulaire + import en masse | Moyenne | Sprint 4 |
| 12 | Enquêtes terrain : formulaire guidé + import en masse (multi-tables) | Haute | Sprint 4 |
| 13 | Extensions du modèle pour les imports terrain | Moyenne | Sprint 4 |
| 14 | Nom réel de l'entité hydro, désambiguïsation hors libellé | Moyenne | Sprint 0 |
| 15 | Marqueurs des sorties visibles à tout niveau de zoom | Faible | Sprint 0 |
| 16 | Bouton de confirmation de position hors écran sur mobile | Faible | Sprint 0 |

Les issues #3 et #4 sont les plus rapides à merger (implémentées).
Les issues #7 et #8 sont les plus structurantes (BDD) et doivent
être faites avant les features qui en dépendent.

Les issues #11, #12 et #13 concernent la saisie opérateur du
Sprint 4. Chacune couvre désormais deux modes de saisie
complémentaires — formulaire (unitaire pour #11, assistant guidé
pour #12) et import en masse — partageant la même validation
métier. #13 (modèle de données) est un prérequis de la
persistance complète de #11 et #12, quel que soit le mode de
saisie utilisé : les deux peuvent démarrer sur le socle #32
existant et ne persister l'intégralité des champs qu'une fois #13
livré. #12 (format relationnel multi-tables) est nettement plus
lourd que #11 (format à plat), le formulaire assistant l'étant
également par rapport au formulaire unitaire de #11.

L'issue #14 propose la résolution de fond de #117 (et rouvre le
sujet de #107) : elle sort la désambiguïsation du champ `name`
plutôt que de chercher un meilleur suffixe. À cadrer avant tout
nouveau chargement hydrographique national (#51).

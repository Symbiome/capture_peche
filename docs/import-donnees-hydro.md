# Import des données hydrographiques (BD TOPO IGN)

Le référentiel des plans d'eau et cours d'eau vient de la **BD TOPO® de l'IGN**,
thème Hydrographie. Ce document décrit comment le charger, département par
département ou à l'échelle nationale.

## En bref

```bash
# Prérequis : la base doit tourner
fishola-backend/start_db.sh

# Charger la Savoie, la Haute-Savoie et la Loire
./scripts/import_hydro_france.sh ./data 73 74 42
```

Le script se charge de tout : il résout la dernière livraison publiée pour
chaque département, la télécharge, en extrait les couches utiles, charge les
communes nécessaires au nommage, puis alimente la base.

## Ce que fait la chaîne

Trois scripts se composent, du plus général au plus unitaire :

| Script | Rôle |
|---|---|
| `import_hydro_france.sh` | Orchestration : boucle sur les départements, journalise, reprend après erreur |
| `download_hydro_ign.sh` | Récupère une livraison IGN et en extrait les 4 couches hydro |
| `import_hydro_gpkg.sh` | Charge un dossier départemental en base (+ `import_hydro_gpkg.sql`) |

Chacun s'utilise seul. `download_hydro_ign.sh` ne nécessite même pas que la base
tourne, ce qui permet de préparer les données à l'avance.

Les données alimentent trois tables applicatives : `water_entity` (plans d'eau et
cours d'eau), `river_section` (tronçons) et `water_surface` (surfaces en eau).

## Où viennent les données

La Géoplateforme IGN expose des URLs de téléchargement direct, sans
authentification, décrites par un flux Atom interrogeable par zone. Il n'y a donc
rien à récupérer à la main.

Deux conséquences de la structure des livraisons, qui expliquent la volumétrie :

- **La BD TOPO 3.x n'est plus livrée par thème** (sauf TRANSPORT). Il faut donc
  rapatrier l'archive « tous thèmes » du département — environ 300 Mo — et son
  GeoPackage décompressé — environ 1,7 Go — pour n'en garder que l'hydrographie.
  Les deux sont supprimés après extraction : prévoir **~2,5 Go de disque
  transitoire**, sans cumul d'un département à l'autre.
- **La Géoplateforme limite à environ une requête par seconde.** Les appels au
  flux sont temporisés en conséquence ; c'est normal si la résolution d'une
  livraison prend quelques secondes.

Aucune dépendance 7-Zip n'est nécessaire : GDAL lit l'archive directement. Toute
la chaîne passe par Docker, comme le reste du dépôt — aucun binaire client à
installer.

## Usage détaillé

```
./scripts/import_hydro_france.sh [options] <racine_data> [dept...]
```

`racine_data` est le dossier qui accueille les sous-dossiers `hydro_<dept>/`.
Sans liste de départements, tous les dossiers `hydro_*` déjà présents sont
importés — mais rien n'est téléchargé, faute de pouvoir deviner ce qui est voulu.

Les codes acceptés sont ceux de l'INSEE : `74`, `01`, `2A`, `971`. Un chiffre
isolé est complété (`1` vaut `01`).

### Options

| Option | Effet |
|---|---|
| `--download=auto` | *(défaut)* télécharge ce qui manque, conserve l'existant |
| `--download=never` | n'appelle jamais l'IGN ; échoue si des fichiers manquent |
| `--download=force` | retélécharge la dernière édition et réimporte, même si le département était marqué importé |
| `--communes=auto` | *(défaut)* charge les communes manquantes avant l'import |
| `--communes=never` | n'appelle pas geo.api.gouv.fr ; prévient si elles manquent |
| `--keep-archives` | conserve les archives `.7z` téléchargées |

### Exemples

```bash
# Trois départements, tout automatique
./scripts/import_hydro_france.sh ./data 73 74 42

# Rafraîchir un département vers la dernière édition publiée
./scripts/import_hydro_france.sh --download=force ./data 74

# Réimporter des données déjà sur disque, sans réseau
./scripts/import_hydro_france.sh --download=never ./data

# France métropolitaine entière
./scripts/import_hydro_france.sh ./data $(seq -w 1 95) 2A 2B
```

## Reprise et rejeu

L'opération est conçue pour être relancée sans précaution particulière.

Chaque département importé laisse un marqueur dans
`<racine_data>/.hydro_import_state/`, et un relancement saute ce qui est déjà
fait. Le journal complet est écrit dans `<racine_data>/import_hydro_france.log`.
Un import interrompu se reprend donc simplement en relançant la même commande ;
seul `--download=force` réexamine un département déjà marqué.

Le rejeu est sûr par construction, parce que l'identifiant national de la BD TOPO
— le `cleabs` — sert de clé naturelle. Une entité partagée entre deux
départements limitrophes n'est donc pas dupliquée : elle est mise à jour. C'est
aussi ce qui rend un réimport de la même édition sans effet.

## Nommage des plans d'eau

Les toponymes de la BD TOPO ne sont pas uniques : « Lac Blanc » désigne 17 plans
d'eau distincts sur sept départements, « Étang Neuf » 41. Or les colonnes `name`
et `export_as` portent une contrainte d'unicité.

Les homonymes sont donc départagés par leur commune :

```
Lac Blanc (Courchevel)
Lac Blanc (Valloire)
Lac Blanc (Bonneval-sur-Arc)
```

C'est pour cela que les communes sont chargées automatiquement avant l'import. Si
le référentiel manque, l'import réussit quand même, mais les homonymes portent
alors leur identifiant technique — `Lac Blanc_PLANDEAU0000002000776820` — et
**seul un réimport complet du département corrige ces libellés**. D'où la
vérification en amont : à l'échelle nationale, l'erreur ne se constaterait
qu'après plusieurs heures de traitement.

Deux limites assumées :

- **Les cours d'eau restent sur l'identifiant.** Une entité `cours_d_eau` porte
  le cours entier — l'Arve traverse 26 communes de la seule Haute-Savoie — et un
  qualificatif communal y serait faux autant qu'inutile.
- **Certains plans d'eau partagent toponyme *et* commune** : les darses du port
  de Dunkerque, les étangs de la Dombes, les chapelets de lacs de montagne.
  Aucune commune ne peut les départager, ils conservent leur identifiant.

## Communes seules

Le référentiel commune s'alimente aussi indépendamment, depuis geo.api.gouv.fr :

```bash
./scripts/import_communes_geoapi.sh 74
```

Utile pour préparer le terrain avant un gros import, ou lorsque l'hydrographie a
été chargée avec `--communes=never`. L'opération est idempotente et prend
quelques secondes par département.

## Diagnostic

**L'import s'arrête sur « Conteneur postgres-18-fishola introuvable ou arrêté ».**
La base n'est pas démarrée : `fishola-backend/start_db.sh`.

**Un département est systématiquement sauté.** Son marqueur existe dans
`.hydro_import_state/`. Le supprimer, ou passer `--download=force`.

**Les plans d'eau portent des identifiants au lieu de noms de communes.**
Le référentiel commune ne couvrait pas la zone au moment de l'import. Charger les
communes puis réimporter le département avec `--download=force`.

**Le téléchargement échoue ou s'interrompt.** Les archives sont reprises là où
elles s'étaient arrêtées, et leur empreinte MD5 est vérifiée : relancer la même
commande suffit.

## Volumétrie observée

Mesures relevées sur sept départements (01, 38, 42, 59, 62, 73, 74) :

| | Valeur |
|---|---|
| Durée par département | ~2 min (téléchargement compris) |
| Base après 7 départements | ~740 Mo |
| Plans d'eau / cours d'eau | 3 376 / 12 302 |
| Tronçons / surfaces | 244 830 / 82 655 |

Extrapolé à la France métropolitaine : compter 3 à 4 heures, environ 25 Go
téléchargés au total et une base de 6 à 8 Go. Les marqueurs de reprise rendent
l'opération relançable sans perte, elle peut donc tourner sans surveillance.

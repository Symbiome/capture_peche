# Backend « gestion interne » (Django)

Back-office du projet **capture_peche** : **administration** (comptes du personnel,
profils & droits, journal d'activité) et **saisie opérateur** (import de masse CSV).
L'interface est un **Django Admin** personnalisé, thémé « AquaAdmin » (django-unfold).

C'est le second backend du projet ; le premier, **Fishola/Quarkus**, porte l'application
pêcheur. Les deux partagent la même base de données.

## Architecture — points clés

- **BDD partagée.** Les deux backends écrivent la **même** base PostgreSQL/PostGIS.
- **Le schéma métier appartient à Fishola/Flyway.** Ici, tous les modèles métier sont
  **`managed = False`** : Django les **lit et écrit** via l'ORM, mais ne les **crée ni
  migre** jamais (la migration initiale est un *no-op* SQL pour ces tables — vérifiable
  via `manage.py sqlmigrate backoffice 0001`).
- **Django ne gère que ses propres tables** (`auth_*`, `django_*`, sessions, admin),
  créées dans la base partagée par `migrate` (séparation par préfixe).
- **Authentification du personnel = django-auth.** Les profils sont des **Groups**
  (`operator`, `admin_regional`, `admin_national`). Le pêcheur reste géré par Fishola.
- **Journal d'activité partagé.** Django écrit dans la table `audit_log` (actions admin
  et opérateur) ; Fishola y écrit les actions pêcheur.
- **Pas de GeoDjango.** Les colonnes géométriques ne sont pas mappées (l'import résout la
  localisation en `water_entity_id`) → pas de dépendance GDAL.
- **Vue « Carte » (consultation).** Une page carte (MapLibre + fonds IGN) affiche les
  sessions géolocalisées (marqueurs + regroupement) avec une liste latérale ; un clic
  ouvre une fiche synthétique. Le **réseau hydro** est produit ici même en tuiles
  vectorielles (MVT) depuis la base partagée (survol / clic → nom + type du cours d'eau),
  sans dépendre du backend Fishola. Les données pêcheur ne sont **pas** éditables ici.

## Stack

- **Python 3.12**, **Django 5.2 LTS**, **django-unfold**, **django-import-export 4.x**,
  **psycopg 3**, **gunicorn** (prod).

## Démarrage (dev)

Le Python local (`python3.12`) est géré par **uv**.

**Raccourci** (recommandé) :

```bash
cd fishola-backoffice
./setup.sh                # env + dépendances + check (ne touche PAS la base)
./setup.sh --init-db      # + migrate (tables Django) + profils staff
# ou, en cibles Make :  make bootstrap   |   make help
```

**Étapes détaillées équivalentes :**

```bash
uv venv --python 3.12 .venv
uv pip install --python .venv -r requirements.txt
cp .env.example .env          # ajuster la connexion à la base partagée
.venv/bin/python manage.py check
```

- `manage.py check` ne nécessite **pas** de base.
- `manage.py migrate` crée les tables **framework Django** dans la base partagée
  (auth/admin/sessions) — à faire une fois. Les tables métier ne sont **pas** touchées
  (managed=False).
- Serveur de dev : `make run` (`manage.py runserver`) → admin sur `/admin/`.
  **En production : gunicorn** (cf. Docker), jamais `runserver`.

### Compte administrateur

```bash
make superuser            # ou : .venv/bin/python manage.py createsuperuser
```

Crée un compte d'accès à l'admin. On lui attribue ensuite un profil (groupe) dans
**« Profils & Droits »** — ou on le laisse super-utilisateur pour l'exploitation.
`./setup.sh --init-db` a déjà créé les groupes `operator` / `admin_regional` /
`admin_national` (commande `manage.py setup_groups`).

### Jeu de données de démonstration (dev / recette)

Sur une base fraîche, les référentiels (espèces, techniques, météo, états de relâche)
sont vides : les menus de saisie sont vides et les imports ne se résolvent pas. Pour
poser un jeu réaliste minimal (eau douce, lacs alpins) :

```bash
make seed                 # ou : .venv/bin/python manage.py seed_referential_dev
```

**Idempotent** (n'insère que si la table est vide) et **non destructif**. Ce n'est **pas**
le référentiel officiel — il est destiné au développement et à la recette, à remplacer
par le référentiel fourni par l'UFBRMC.

## Conteneurisation

Convention du repo : chaque module a son `docker/Dockerfile.<env>` (pas de compose
racine, déploiement Ansible). Image runtime `python:3.12-slim`, lancement via **gunicorn**
(jamais `runserver`), utilisateur **non-root**, config par variables d'environnement.

```bash
# depuis fishola-backoffice/
docker build -f docker/Dockerfile.demo -t symbiome/fishola-backoffice:demo .
docker run -i --rm -p 8000:8000 --env-file .env symbiome/fishola-backoffice:demo
```

`collectstatic` est exécuté au build. La connexion à la base partagée est fournie au run
par `--env-file` / variables d'environnement.

## Décisions ouvertes (cf. issue #56)

1. **Sort de `fishola_admin`** : les comptes du personnel existants migrent-ils vers
   `auth_user` (django-auth) ou mappe-t-on l'existant ?
2. **`audit_log.actor_type`** : étendre la contrainte à `'operator'` (migration côté
   Fishola) ou garder `'admin'` + rôle précis dans `details` ?
3. **`migrate` sur la base partagée** : quand/comment créer les tables framework Django
   sans perturber Fishola.
4. **SSO / Keycloak (#49)** : différé — l'authentification repose sur django-auth.

# Backend « gestion interne » (Django)

Backend d'administration et de saisie opérateur du projet **capture_peche** (Symbiome / UFBRMC).
Il réalise, côté Phase 1 :

- **§3.1 — Module administrateur** : gestion des comptes staff, profils & droits, journal d'activité (« Historique »), via **Django Admin** personnalisé (« AquaAdmin »).
- **§3.3 — Module de saisie opérateur** : import de masse (CSV) via **django-import-export**.

C'est le second backend de l'architecture figée par Symbiome (mémoire technique, Figure 1). Le premier, **Fishola/Quarkus**, porte l'application pêcheur.

## Architecture — points clés

- **BDD partagée.** Les deux backends écrivent la **même** base PostgreSQL/PostGIS (« BDD applicative Extension Fishola »).
- **Flyway/Fishola est l'autorité du schéma métier.** Ici, tous les modèles métier sont **`managed = False`** : Django les **lit et écrit** via l'ORM, mais ne les **crée ni migre** jamais (la migration initiale est un *no-op* SQL pour ces tables — vérifiable via `manage.py sqlmigrate backoffice 0001`).
- **Django ne gère que ses propres tables** (`auth_*`, `django_*`, sessions, admin). Elles sont créées dans la base partagée par `migrate` (séparation par préfixe de tables).
- **Auth staff = django-auth.** Profils = **Groups** (`operator`, `admin_regional`, `admin_national`). Le pêcheur reste côté Fishola (non géré ici).
- **Journal d'activité partagé.** Django écrit dans la table `audit_log` de Fishola (actions admin/opérateur, `actor_type='admin'` — le CHECK DB n'autorise que `admin|user|system`, le rôle précis va dans `details`). Fishola y écrit les actions pêcheur.
- **Pas de GeoDjango.** Les colonnes géométriques ne sont pas mappées (l'import résout la localisation en `water_entity_id`) → pas de dépendance GDAL.

## Stack

- **Python 3.12**, **Django 5.2 LTS** (support long, adapté marché public), **django-import-export 4.x**, **psycopg 3**, **gunicorn** (prod).

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
cp .env.example .env          # ajuster la connexion à la BDD Fishola partagée
.venv/bin/python manage.py check
```

- `manage.py check` ne nécessite **pas** de base.
- `manage.py migrate` crée les tables **framework Django** dans la base partagée (auth/admin/sessions) — à faire une fois, en coordination (cf. décisions ci-dessous). Les tables métier ne sont **pas** touchées (managed=False).
- Serveur de dev : `manage.py runserver` → admin sur `/admin/`. **En production : gunicorn** (cf. Docker), jamais `runserver`.

## Conteneurisation

Convention du repo : chaque module a son `docker/Dockerfile.<env>` (pas de compose racine, déploiement Ansible). Image runtime `python:3.12-slim`, lancement via **gunicorn** (jamais `runserver`), utilisateur **non-root**, config par variables d'environnement.

```bash
# depuis fishola-backoffice/
docker build -f docker/Dockerfile.demo -t symbiome/fishola-backoffice:demo .
docker run -i --rm -p 8000:8000 --env-file .env symbiome/fishola-backoffice:demo
```

`collectstatic` est exécuté au build. La connexion à la BDD Fishola partagée est fournie au run par `--env-file` / variables d'environnement.

## Décisions ouvertes (à acter — cf. issue #56)

1. **Sort de `fishola_admin`** : les comptes staff migrent-ils vers `auth_user` (django-auth) ou mappe-t-on l'existant ?
2. **`audit_log.actor_type`** : étendre le CHECK à `'operator'` (migration Flyway Fishola) ou garder `'admin'` + rôle dans `details` ?
3. **`migrate` sur la base partagée** : quand/comment créer les tables framework Django sans perturber Fishola.
4. **Keycloak (#49)** : repoussé — le mémoire prescrit django-auth.

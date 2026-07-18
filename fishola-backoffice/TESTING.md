# Tester le backend « gestion interne »

## 0. Prérequis

- **BDD Fishola de recette démarrée** (conteneur `postgres-18-fishola`, port hôte **15432**, base `fishola`, user `postgres`, mdp `whatever`). C'est la BDD **partagée** — le backend Django la lit/écrit en `managed=False`.
- `uv` installé (le `python3.12` local est géré par uv).

## 1. Installation

```bash
cd fishola-backoffice
uv venv --python 3.12 .venv
uv pip install --python .venv -r requirements.txt
cp .env.example .env        # valeurs par défaut = recette (localhost:15432)
```

## 2. Vérifications sans base

```bash
.venv/bin/python manage.py check                       # -> System check identified no issues
.venv/bin/python manage.py makemigrations --check      # -> No changes detected
.venv/bin/python manage.py sqlmigrate backoffice 0001 | grep -c no-op   # tables métier = no-op (Flyway garde la main)
```

## 3. Vérifier le branchement ORM ↔ BDD Fishola partagée (le test clé)

Lit de vraies données Fishola via les modèles `managed=False` :

```bash
.venv/bin/python manage.py shell -c "from backoffice.models import Species, WaterEntity, Trip; print('espèces:', Species.objects.count(), '| entités hydro:', WaterEntity.objects.count(), '| sorties:', Trip.objects.count())"
```

→ doit afficher des comptes cohérents avec la base de recette.

## 4. Lancer l'admin « AquaAdmin »

⚠️ `migrate` crée les tables **framework Django** (`auth_*`, `django_*`, sessions) **dans la base partagée**. C'est **additif** (préfixe `auth_`/`django_`, ne touche pas les tables Fishola) et réversible (drop de ces tables). Décision d'intégration à acter (cf. README §Décisions ouvertes).

```bash
.venv/bin/python manage.py migrate                 # crée les tables Django (auth/admin/sessions)
.venv/bin/python manage.py setup_groups            # crée les Groups operator/admin_regional/admin_national
.venv/bin/python manage.py createsuperuser         # un compte pour se connecter
.venv/bin/python manage.py runserver               # DEV uniquement
# -> http://127.0.0.1:8000/admin/  (Utilisateurs / Profils & Droits / Historique + données de pêche)
```

## 5. Tester l'image Docker

```bash
docker build -f docker/Dockerfile.demo -t symbiome/fishola-backoffice:demo .
# la BDD est sur l'hôte : depuis le conteneur, l'hôte = host.docker.internal
docker run -i --rm -p 8000:8000 \
  -e DB_HOST=host.docker.internal -e DB_PORT=15432 \
  -e DB_NAME=fishola -e DB_USER=postgres -e DB_PASSWORD=whatever \
  -e DJANGO_SECRET_KEY=demo -e DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1 \
  symbiome/fishola-backoffice:demo
# -> http://localhost:8000/admin/
```

## Nettoyage éventuel (annuler le migrate sur la base de recette)

```sql
-- dans la base fishola, si tu veux retirer les tables Django ajoutées :
DROP TABLE IF EXISTS auth_permission, auth_group, auth_group_permissions,
  auth_user, auth_user_groups, auth_user_user_permissions,
  django_admin_log, django_content_type, django_migrations, django_session CASCADE;
```

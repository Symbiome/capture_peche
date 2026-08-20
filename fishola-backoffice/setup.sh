#!/usr/bin/env bash
#
# Bootstrap du backend « gestion interne » : environnement virtuel + dépendances,
# puis (optionnel) initialisation de la base partagée.
#
#   ./setup.sh              # environnement + dépendances + vérification (ne touche PAS la base)
#   ./setup.sh --init-db    # + migrate (tables framework Django) + profils staff
#
set -euo pipefail
cd "$(dirname "$0")"

if ! command -v uv >/dev/null 2>&1; then
    echo "Erreur : 'uv' est requis (le Python local est géré par uv)." >&2
    exit 1
fi

echo "==> Environnement virtuel + dépendances (uv)"
uv venv --python 3.12 .venv
uv pip install --python .venv -r requirements.txt

if [ ! -f .env ]; then
    cp .env.example .env
    echo "==> .env créé depuis .env.example (ajuster la connexion à la BDD si besoin)"
fi

echo "==> Vérification Django"
# --skip-checks : sur une base neuve (auth_permission pas encore créée), les
# checks admin de django-unfold interrogent cette table et échouent avant même
# que migrate n'ait pu la créer (poule/œuf). Une fois la base initialisée,
# runserver n'a plus besoin de ce flag.
.venv/bin/python manage.py check --skip-checks

if [ "${1:-}" = "--init-db" ]; then
    echo "==> Initialisation de la base PARTAGÉE (ajoute les tables Django auth_*/django_*)"
    .venv/bin/python manage.py migrate --skip-checks
    .venv/bin/python manage.py setup_groups
    echo "==> Créez un compte admin :  .venv/bin/python manage.py createsuperuser"
fi

echo ""
echo "OK. Démarrer le serveur de dev :  .venv/bin/python manage.py runserver"
echo "    puis http://127.0.0.1:8000/admin/"

# Backend « gestion interne » — raccourcis dev & exploitation.
# `make help` pour la liste.

PYTHON := .venv/bin/python
IMAGE  := symbiome/fishola-backoffice:demo

.DEFAULT_GOAL := help
.PHONY: help install check migrate groups superuser seed run bootstrap docker-build docker-run

help:  ## Affiche cette aide
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
	  awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-14s\033[0m %s\n", $$1, $$2}'

install:  ## Environnement virtuel + dépendances (uv)
	uv venv --python 3.12 .venv
	uv pip install --python .venv -r requirements.txt

check:  ## Vérifications Django (sans base)
	$(PYTHON) manage.py check

migrate:  ## Crée les tables framework Django dans la base PARTAGÉE (additif)
	$(PYTHON) manage.py migrate

groups:  ## Crée / met à jour les profils staff (operator, admin_regional, admin_national)
	$(PYTHON) manage.py setup_groups

superuser:  ## Crée un compte administrateur
	$(PYTHON) manage.py createsuperuser

seed:  ## Charge un référentiel de démonstration (dev/recette, idempotent)
	$(PYTHON) manage.py seed_referential_dev

run:  ## Serveur de développement (NE PAS utiliser en production)
	$(PYTHON) manage.py runserver

bootstrap: install check  ## Setup complet sans toucher la base (= ./setup.sh)

docker-build:  ## Construit l'image Docker
	docker build -f docker/Dockerfile.demo -t $(IMAGE) .

docker-run:  ## Lance le conteneur (BDD de recette via host.docker.internal)
	docker run -i --rm -p 8000:8000 \
	  -e DB_HOST=host.docker.internal -e DB_PORT=15432 \
	  -e DB_NAME=fishola -e DB_USER=postgres -e DB_PASSWORD=whatever \
	  -e DJANGO_SECRET_KEY=demo -e DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1 \
	  $(IMAGE)

# Image Postgres de la stack déployée (#279) : PostGIS + pgBackRest.
#
# pgBackRest doit être dans ce conteneur pour l'archivage continu des WAL
# (archive_command, cf. docker-compose.yml) ; les sauvegardes sont lancées
# depuis ce même conteneur (docker compose exec, cron de l'hôte géré par
# Ansible) : même version, mêmes chemins, même utilisateur (postgres, UID 999).
#
# Image de base Debian uniquement (dépôt PGDG déjà configuré). Version de
# pgBackRest épinglée : une même version doit servir à archive-push et aux
# sauvegardes/restaurations.
ARG POSTGRES_IMAGE=postgis/postgis:18-3.6
FROM ${POSTGRES_IMAGE}

ARG PGBACKREST_VERSION=2.59.3-1.pgdg13+1

RUN apt-get update \
 && apt-get install -y --no-install-recommends "pgbackrest=${PGBACKREST_VERSION}" \
 && rm -rf /var/lib/apt/lists/* \
 && mkdir -p /var/lib/pgbackrest /var/log/pgbackrest /var/spool/pgbackrest \
 && chown postgres:postgres /var/lib/pgbackrest /var/log/pgbackrest /var/spool/pgbackrest \
 && chmod 750 /var/lib/pgbackrest

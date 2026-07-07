#!/bin/bash

DB=fishola
CONTAINER=postgres-12-${DB}

# Sur macOS, /home est réservé à l'automonteur : on utilise un volume Docker
# nommé. Sur Linux, on conserve le bind mount historique.
if [ "$(uname)" = "Darwin" ]; then
  DBvolume=fishola-pgdata
else
  DBdir=/home/postgresql-12
  DBvolume=${DBdir}/${DB}
fi

# Idempotent : ne rien faire si la base tourne déjà, la redémarrer si le
# conteneur existe mais est arrêté.
if [ -n "$(docker ps -q -f name=^/${CONTAINER}$)" ]; then
  echo "La base ${DB} tourne déjà (conteneur ${CONTAINER})."
  exit 0
fi
if [ -n "$(docker ps -aq -f name=^/${CONTAINER}$)" ]; then
  echo "Redémarrage du conteneur existant ${CONTAINER}."
  docker start ${CONTAINER}
  exit $?
fi

docker run \
  --name ${CONTAINER} \
  --restart always \
  -v ${DBvolume}:/var/lib/postgresql/data \
  -e POSTGRES_DB=${DB} \
  -e POSTGRES_PASSWORD=whatever \
  -p 15432:5432 \
  -d postgres:12

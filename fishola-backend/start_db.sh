#!/bin/bash

DBdir=/home/postgresql-18
DB=fishola
CONTAINER=postgres-18-${DB}

# Choix de l'image selon l'ARCHITECTURE CPU (détectée à la volée) : les images
# officielles postgis/postgis ne sont publiées qu'en amd64. Sur arm64 (Apple
# Silicon, ARM Linux) on bascule sur imresamu/postgis, le build multi-arch
# recommandé par le projet PostGIS. Surchargeable via $FISHOLA_PG_IMAGE.
case "$(uname -m)" in
  arm64 | aarch64) DEFAULT_IMAGE=imresamu/postgis:18-3.6-alpine ;;
  *)               DEFAULT_IMAGE=postgis/postgis:18-3.6 ;;
esac
IMAGE=${FISHOLA_PG_IMAGE:-${DEFAULT_IMAGE}}

# Choix du stockage selon l'OS : sur macOS, /home est réservé à l'automonteur,
# on utilise donc un volume Docker nommé. Sur Linux, on conserve le bind mount
# historique.
if [ "$(uname)" = "Darwin" ]; then
  # Volume dédié PG18 : l'ancien volume fishola-pgdata (PG12) n'est pas
  # réutilisable par PostgreSQL 18 (ni par l'uid des images PG18).
  DBvolume=fishola-pg18-data
else
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

echo "Démarrage de ${CONTAINER} avec l'image ${IMAGE} ($(uname -m))."
docker run \
  --name ${CONTAINER} \
  --restart always \
  -v ${DBvolume}:/var/lib/postgresql \
  -e POSTGRES_DB=${DB} \
  -e POSTGRES_PASSWORD=whatever \
  -p 15432:5432 \
  -d ${IMAGE}

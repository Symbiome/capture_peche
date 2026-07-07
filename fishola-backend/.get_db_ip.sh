#!/bin/bash
DB=$1
if [ "${DB}" = "" ]; then
   DB=fishola
fi
# .NetworkSettings.IPAddress n'est plus renseigné par les versions récentes de
# Docker : on passe par la liste des réseaux du conteneur.
IP=`docker inspect --format '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' postgres-12-${DB}`
echo ${IP}

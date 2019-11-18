#!/bin/bash
DB=$1
if [ "${DB}" = "" ]; then
   DB=fishola
fi
IP=`docker inspect --format '{{ .NetworkSettings.IPAddress }}' postgres-12-${DB}`
echo ${IP}

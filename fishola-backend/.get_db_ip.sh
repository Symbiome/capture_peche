#!/bin/bash
DB=$1
if [ "${DB}" = "" ]; then
   DB=fishola
fi
IP=`docker inspect --format '{{ .NetworkSettings.IPAddress }}' postgres-11-${DB}`
echo ${IP}

#!/bin/bash
DB=fishola
IP=`./.get_db_ip.sh ${DB}`
#echo "Starting psql on database ${DB} at ${IP} with command: docker run -it --rm postgres psql -h ${IP} -U postgres -d ${DB}"
#docker run -it --rm postgres psql -h ${IP} -U postgres -d ${DB}

# On spécifie le MDP sur la ligne de commande car .pgpass non accessible
echo "Starting psql on database ${DB} at ${IP} with command: docker run -it --rm postgres:12 psql \"host=172.17.0.2 port=5432 dbname=fishola user=postgres password=whatever\""
docker run -it --rm postgres:12 psql "host=172.17.0.2 port=5432 dbname=fishola user=postgres password=whatever"

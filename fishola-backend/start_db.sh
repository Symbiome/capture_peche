#!/bin/bash

DBdir=/home/postgresql-18
DB=fishola
docker run \
  --name postgres-18-${DB} \
  --restart always \
  -v ${DBdir}/${DB}:/var/lib/postgresql \
  -e POSTGRES_DB=${DB} \
  -e POSTGRES_PASSWORD=whatever \
  -p 15432:5432 \
  -d postgis/postgis:18-3.6-alpine

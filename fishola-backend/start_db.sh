#!/bin/bash

DBdir=/home/postgresql-12
DB=fishola
docker run \
  --name postgres-12-${DB} \
  --restart always \
  -v ${DBdir}/${DB}:/var/lib/postgresql/data \
  -e POSTGRES_DB=${DB} \
  -e POSTGRES_PASSWORD=whatever \
  -d postgres:12

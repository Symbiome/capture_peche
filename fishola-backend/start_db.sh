#!/bin/bash

DBdir=/home/postgresql-12
DB=fishola
docker run \
  --name postgres-12-${DB} \
  --restart always \
  -v ${DBdir}/${DB}:/var/lib/postgresql/data \
  -e POSTGRES_DB=${DB} \
  -d postgres:12

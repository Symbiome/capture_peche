#!/bin/bash

DBdir=/home/postgresql-11
DB=fishola
docker run \
  --name postgres-11-${DB} \
  --restart always \
  -v ${DBdir}/${DB}:/var/lib/postgresql/data \
  -e POSTGRES_DB=${DB} \
  -d postgres:11

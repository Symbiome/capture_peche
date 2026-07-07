#!/bin/bash
DB=fishola
# Sur macOS l'IP du conteneur n'est pas routable depuis l'hôte :
# on passe par le port publié sur localhost.
if [ "$(uname)" = "Darwin" ]; then
  HOST=localhost
  PORT=15432
else
  HOST=`./.get_db_ip.sh ${DB}`
  PORT=5432
fi
echo "Starting psql on database ${DB} at ${HOST}:${PORT} with command: psql -h ${HOST} -p ${PORT} -U postgres ${DB}"
psql -h ${HOST} -p ${PORT} -U postgres ${DB}

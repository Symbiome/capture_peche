#!/bin/bash
DB=fishola
IP=`./.get_db_ip.sh ${DB}`
echo "Starting psql on database ${DB} at ${IP} with command: psql -h ${IP} -U postgres ${DB}"
psql -h ${IP} -U postgres ${DB}

#!/bin/sh
set -ue

if echo "" | nc localhost 50053 >/dev/null ;then
  echo "kill the node first"
  exit 1
fi
if echo "" | nc localhost 50052 >/dev/null ;then
  echo "kill the wallet first"
  exit 1
fi
if echo "" | nc localhost 50051 >/dev/null ;then
  echo "kill the connector first"
  exit 1
fi

if ! pwd | grep "credentials-verification-web$" >/dev/null ; then
  echo "run me on the webapp directory (credentials-verification-web)"
  exit 1
fi

containers="$(docker ps | grep '\bpostgres\b' | cut -f 1 -d ' ')"
if [ -n "${containers}" ] ; then 
  echo "killing current database, containers: ${containers}"
  # shellcheck disable=SC2086
  docker stop ${containers} # I want the different container ids to be different arguments
  # shellcheck disable=SC2086
  docker rm ${containers} || echo "some container exited by being stopped" # if you ran a container with --rm this might fail, it doesn't matter
else
  echo "warning: there were no docker containers running postgres"
fi

echo "running a new database instance"
docker run -d -p 5432:5432 postgres

# compose? never heard of it
echo "waiting for the database to start"
sleep 2

echo "creating databases"
echo "create database geud_node_db; create database geud_connector_db;" \
  |docker run -i --rm postgres psql -h 172.17.0.1 -U postgres

echo "deleting keys"
rm -r ../credentials-verification/.cvpwallet* || echo "no wallet data found"

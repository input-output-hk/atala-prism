#!/bin/bash

docker network create --driver bridge postgres_network || true

docker run -it --network postgres_network  --rm postgres:11.5 psql -h node_postgres -U postgres node_db


#!/bin/bash

docker network create --driver bridge postgres_network || true

docker run --network postgres_network -p 5432:5432 --rm --name node_postgres  -e POSTGRES_DB=node_db postgres:11.5


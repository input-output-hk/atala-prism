#!/bin/bash

docker run -p 5432:5432 -e POSTGRES_DB=geud_node_db postgres:11.5


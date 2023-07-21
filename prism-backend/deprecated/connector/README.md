## Running
In order to run the connector, you must first run the [node](../node/README.md).

The connector, like node, requires the Postgres database to run.
You can follow the same recipe as in node, by adjusting the database name
and the port the container binds to.

```sh
docker run -it --rm -e POSTGRES_DB=connector_db -p 5433:5432 postgres:11.5
```

Note that we use port `5433` instead of the usual `5432` because we assume the node db
is already running and is bound to the latter. Now you are ready to run the connector:

```sh
CONNECTOR_PSQL_HOST=localhost:5433 sbt connector/run
```
For example, you can run connector with `logback-dev.xml` using console by:
``` sh
CONNECTOR_PSQL_HOST=localhost:5433 sbt "connector/run" -Dlogback.configurationFile=logback-dev.xml
```


## Connector client

The connector comes with simple CLI client, which right now is used for testing complex features manually.

### Register DID

```
sbt "connectorClient/run register"
```

This operation will prepare a request to publish a DID to the node, which is used as an argument while registering on the connector.

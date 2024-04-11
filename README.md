# PRISM Node

This project represents a reference implementation of the PRISM Node described in the PRISM DID method specification.
This code is not recommended for production environments. The project supports:
- the indexing of the blockchain
- The interpretation of the DID method events (Creation, updates and deactivation)
- batch submission for DID PRISM operations

### Flow of commits

Commits land on `develop` from Pull Requests, as soon as they are approved. There is no coordination happening there.


#### Future investigations

## How to run

This implementation relays on a Postgres database, a Cardano wallet and an instance of db-sync.
For development and illustration purposes, the node supports an "in memory" mode that doesn't interact with db-sync nor wallet.

### Database

Run the Postgres server inside docker container

```bash
docker run -it --rm -e POSTGRES_DB=node_db -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres
```

This command will start the Postgres service on port 5432. It will also create a database "connector_db" if does not exist.

the `--rm` command line option will remove the container when you stop it, this means that once you stop the database service all the data you had there will be lost. In case you want stay persistent you need to run the database like this:

```bash
docker run \
    --name prism-db \
    -e POSTGRES_HOST_AUTH_METHOD=trust \
    -it \
    --rm \
    -p 5432:5432 \
    -v $PWD/.docker-volumes/postgres/data:/var/lib/postgresql/data \
    postgres
```

This command will bind volume to `.docker-volumes/postgres/data`, so all your database date will be stored there.

You can connect to the database through your favorite RDBMS, like DataGrip for example, or use command line tool `psql` if you prefer this way.
For psql, you can run. Note that the tables are created by the node on its first run, so be sure to first run the node (see below) in order to find data in the database.

```bash
$ psql node_db \
      -U postgres \
      -h localhost \
      -p 5432
```

### Running the node

In the top folder of this project just run:

```
$ sbt node/run
```

This will start the node server. You can interact with it using gRPC calls at port 50053.
By default, the node is running on "in memory" mode, which means that any operation submitted to it will be instantly confirmed and processed.
Alternatively, you can configure to run it against a Cardano network by setting values for the db-sync and Cardano wallet services.

```bash
export NODE_CARDANO_DB_SYNC_HOST="db-sync-instance.example.com"
export NODE_CARDANO_DB_SYNC_DATABASE="db name"
export NODE_CARDANO_DB_SYNC_USERNAME="the username"
export NODE_CARDANO_DB_SYNC_PASSWORD="your password"

export NODE_CARDANO_WALLET_API_HOST="cardanowallet.example.com"
export NODE_CARDANO_WALLET_API_PORT="port number"

export NODE_CARDANO_PAYMENT_ADDRESS="wallet address"
export NODE_CARDANO_WALLET_ID="a wallet id"
export NODE_CARDANO_WALLET_PASSPHRASE="wallet pathphrase"
```

For development purposes, you may want to reduce the number of blocks to wait for confirmations. Note that this parameter is fixed for mainnet. Hence, only modify then for tests if needed.

```
export NODE_CARDANO_CONFIRMATION_BLOCKS="1"
export NODE_LEDGER="cardano"
```

For more configuration options, please refer to `node/src/main/resources/application.conf`. Note that environment values override the configuration values. You can change locally the `application.conf` instead of exporting environment variables as we did above.

## Working with the codebase

In order to keep the code format consistent, we use scalafmt and git hooks, follow these steps to configure it accordingly (otherwise, your changes are going to be rejected by CircleCI):

- Install [coursier](prism-backend/README.md#Install-coursier), the `cs` command must work.
- install `scalafmt`

   ```bash
   cs install scalafmt
   ```
- `cp pre-commit .git/hooks/pre-commit`

## Known limitations

- This reference implementation does not support Cardano rollbacks' management

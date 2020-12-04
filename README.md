# Prism

Prism is a Self-Sovereign Identity (SSI), and Verifiable Credentials (VC) platform.

[![CircleCI](https://circleci.com/gh/input-output-hk/atala/tree/develop.svg?style=svg&circle-token=1a9dcf544cec8cb581fa377d8524d2854cfb10e9)](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop)

## Branches

Two main branches will be maintained: `develop` and `master`. `master` contains the latest version of the code that was tested end-to-end. `develop` contains the latest version of the code that runs all the tests (unit and integration tests). Integration tests don't test all the integrations. Hence, any version in `develop` might have bugs when deployed for an end-to-end test.


## Working with the codebase

This is a monorepo and each of the `credentials-verification-XYZ` folders refers to a different part of the platform. Check the specific READMEs for more details.

Be sure to follow our [contributing guidelines](CONTRIBUTING.md).

In order to keep the code format consistent, we use scalafmt and git hooks, follow these steps to configure it accordingly (otherwise, your changes are going to be rejected by CircleCI):

- Install [coursier](https://github.com/coursier/coursier#command-line), the `coursier` command must work.
- `./install-scalafmt.sh` (might require sudo).
- `cp pre-commit .git/hooks/pre-commit`

## More docs

* Documentation about operational aspects of the team and the services we use can be found in [Confluence](https://input-output.atlassian.net/wiki/spaces/CE/pages/606371843/Code+and+Infrastructure+Setup).
* The general guideline and ultimate goal is to have the [repository](credentials-verification/docs/README.md) as the source of truth for all technical documentation.

## How to run

### Front-end

We use [nvm](https://github.com/nvm-sh/nvm) to handle the node versions.

To ensure the node version standardized:

```
$ cd credentials-verification-web
```

and run 

```
credentials-verification-web$ nvm use
```

which will set the node version to 10.16.3, if installed, otherwise install it and then run the command again.

If the file `.env.local` is not present, copy `.env` file to `.env.local` (it should work without further modifications).
Then, install dependencies with 

```
credentials-verification-web$ npm install
```

and run the front-end with

```
credentials-verification-web$ npm start
```

### Back-end and PRISM wallet

We need now to run the different backend services

The steps in this document were replicated in an Ubuntu environment with the following versions

- Postgres version: 12.2
- psql version: 12.3
- envoy: 1.12.1

Steps

1. First, we will start the Postgres server. If you already have this up and running, you can skip this step.
   ``` 
   $ docker run -it --rm -e POSTGRES_DB=connector_db -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres
   ```

   The above command will start a docker Postgres container with a database necessary for the connector.
   Given that we will also need a database for the node, open a postgres client and create the needed database as follows:

   ``` 
   $ psql connector_db \
       -U postgres \
       -h localhost \
       -p 5432
   ```
   This will open an sql shell connected to the docker container.
   Execute the query:

   ```
   connector_db=# CREATE DATABASE node_db;
   ```
   and keep the sql shell open because we will need it for next steps.

2. Now, we should run the node, connector and envoy locally.

   You will need three more terminals/tabs to run this.
   Run the commands in sequence (i.e. wait for each one to finish before running the next one).

   **NOTE:** If you are a *linux* user, you could alternatively go to `credentials-verification-webextension`, and run
   `$ ./run_local.sh`, and move directly to step 4.

   Connector
   ```
   [terminal 1]
   atala$ cd credentials-verification
   credentials-verification$ sbt "connector/run"
   ```

   Node
   ```
   [terminal 2]
   atala$ cd credentials-verification
   credentials-verification$ sbt "node/run"
   ```

   Envoy

   **NOTE FOR MAC USERS:** There seems to be a problem to run envoy on docker.
   There is an `envoy.yaml` file in `credentials-verification-web/envoy` that instruct in comments an attempt to
   fix the problem. Some Mac users suggested to install envoy locally and avoid docker completely.
   ```
   [terminal 3]
   atala$ cd credentials-verification-web/envoy
   envoy$ docker run --rm -ti --net=host -v $PWD/envoy.yaml:/etc/envoy/envoy.yaml envoyproxy/envoy:v1.12.1
   ```

3. Now it is time to compile the web extension (the wallet)

   ``` 
   atala$ cd credentials-verification-webextension
   credentials-verification-webextension$ sbt chromeUnpackedFast
   ```

4. Now let's install the wallet on chrome. Open chrome and go to [chrome://extensions/](chrome://extensions/)

Activate the developer mode (top right corner of the page) and click on `Load unpacked` (top left visible in developer
mode), in the dialog opened go to `atala/credentials-verification-webextension/target/chrome`, select `unpacked-fast`
folder and click `Open`. Now the wallet should be found in your plugins. Do not open the wallet yet.

At this point, you have all the needed components up to run the wallet locally.


# Prism

Prism is a Self-Sovereign Identity (SSI), and Verifiable Credentials (VC) platform.

[![CircleCI](https://circleci.com/gh/input-output-hk/atala/tree/develop.svg?style=svg&circle-token=1a9dcf544cec8cb581fa377d8524d2854cfb10e9)](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop)

## Branches, deployment, and stability

Three main branches will be maintained:

* `develop`
* `qa`
* `master`

All development happens against `develop`, which contains the latest version of the code that runs all the tests (unit and some integration tests). Integration tests don't test all the integrations. Hence, any version in `develop` might have bugs when deployed for an end-to-end test.

The `qa` branch is where we can guarantee that code is stable end-to-end, meaning that all components can work flawlessly together when deployed. Periodically, `qa` is rebased onto `develop` and a series of tests (contained in [`atala-qa-automation`](https://github.com/input-output-hk/atala-qa-automation/)) that take all components into account are executed.

The `master` branch contains milestones that have been tested end-to-end and have been demonstrated to work properly. The idea is to be able to create PRISM releases out of `master` and also be able to create demo environments. We expect that milestones and, especially, releases will be represented by git tags. For instance, [vAlpha-01](https://github.com/input-output-hk/atala/tree/vAlpha-01) and [vAlpha-02](https://github.com/input-output-hk/atala/tree/vAlpha-02) are existing tags that represent milestones.

### Flow of commits

Commits land on `develop` from Pull Requests, as soon as they are approved. There is no coordination happening there.

Commits land on `qa` from `develop` periodically, currently once in a day. This is an automated process.

Commits land on `master` from `qa` manually, according to the PRISM roadmap, when we have to represent a stable milestone or cut a release.

### Working assumptions

The current situation regarding the engineering & QA flows and processes has the following characteristics:

* Running the QA pipeline is costly. Given the fact that our Pull Requests can evolve `develop` very frequently, it means that running the QA pipeline against `develop` can be ineffective: while in the middle of the QA pipeline, a new commit can appear in `develop` and, as a consequence, the so far QA work can be wasted.

* Launching deployments is costly. Our CircleCI configuration declares precisely under which circumstances a deployment is created and updated.

* Periodically rebasing `qa` onto `develop` almost cancels out the volatility present in the latter, so we expect QA results with less interference between changing features.

* _Some_ manual testing may still be needed in `qa`.

* Commits flow from `develop` to `qa` periodically in an automated way but we may choose to manually cherry-pick a few latest commits from `develop`, in case a hot fix is needed to stabilize `qa`. In any case, we may move to a full manual process when bringing patches from `develop` in `qa`.

#### Future investigations

As a consequence of the above, we may want to look into ways to make:

* Deployments lighter-weight.
* The QA pipeline lighter-weight.
* The QA pipeline even more automatic.

These could bring more QA automation into `develop`, could ease testing the Pull Requests, and probably bridge the gap between `develop` and `master`.


## Working with the codebase

This is a monorepo and each of the `prism-XYZ` folders refers to a different part of the platform. Check the specific READMEs for more details.

Be sure to follow our [contributing guidelines](CONTRIBUTING.md).

In order to keep the code format consistent, we use scalafmt and git hooks, follow these steps to configure it accordingly (otherwise, your changes are going to be rejected by CircleCI):

- Install [coursier](https://github.com/coursier/coursier#command-line), the `coursier` command must work.
- `./install-scalafmt.sh` (might require sudo).
- `cp pre-commit .git/hooks/pre-commit`

## More docs

* Documentation about operational aspects of the team and the services we use can be found in [Confluence](https://input-output.atlassian.net/wiki/spaces/CE/pages/606371843/Code+and+Infrastructure+Setup).
* The general guideline and ultimate goal is to have the [repository](prism-backend/docs/README.md) as the source of truth for all technical documentation.

## How to run

### Front-end

We use [nvm](https://github.com/nvm-sh/nvm) to handle the node versions.

To ensure the node version standardized:

```
$ cd prism-management-console-web
```

and run 

```
prism-management-console-web$ nvm use
```

which will set the node version to 10.16.3, if installed, otherwise install it and then run the command again.

If the file `.env.local` is not present, copy `.env` file to `.env.local` (it should work without further modifications).
Then, install dependencies with 

```
prism-management-console-web$ npm install
```

and run the front-end with

```
prism-management-console-web$ npm start
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

   **NOTE:** If you are a *linux* user, you could alternatively go to `prism-web-wallet`, and run
   `$ ./run_local.sh`, and move directly to step 4.

   Connector
   ```
   [terminal 1]
   atala$ cd prism-backend
   prism-backend$ sbt "connector/run"
   ```

   Node
   ```
   [terminal 2]
   atala$ cd prism-backend
   prism-backend$ sbt "node/run"
   ```

   Envoy

   **NOTE FOR MAC USERS:** There seems to be a problem to run envoy on docker.
   There is an `envoy.yaml` file in `prism-management-console-web/envoy` that instruct in comments an attempt to
   fix the problem. Some Mac users suggested to install envoy locally and avoid docker completely.
   ```
   [terminal 3]
   atala$ cd prism-management-console-web/envoy
   envoy$ docker run --rm -ti --net=host -v $PWD/envoy.yaml:/etc/envoy/envoy.yaml envoyproxy/envoy:v1.16-latest
   ```

3. Now it is time to compile the web extension (the wallet)

   ``` 
   atala$ cd prism-web-wallet
   prism-web-wallet$ sbt chromeUnpackedFast
   ```

4. Now let's install the wallet on chrome. Open chrome and go to [chrome://extensions/](chrome://extensions/)

Activate the developer mode (top right corner of the page) and click on `Load unpacked` (top left visible in developer
mode), in the dialog opened go to `atala/prism-web-wallet/target/chrome`, select `unpacked-fast`
folder and click `Open`. Now the wallet should be found in your plugins. Do not open the wallet yet.

At this point, you have all the needed components up to run the wallet locally.


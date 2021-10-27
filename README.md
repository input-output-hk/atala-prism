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



## How to run

### Front-end

See the instructions on how to set up all the necessary dependencies to run the frontend management console in dedicated [readme](prism-management-console-web/README.md)

### Database

Run the Postgres server inside docker container

```bash
docker run -it --rm -e POSTGRES_DB=connector_db -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres
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

You will also need to create another database with the name "node_db" (and optionally additional one named "management_console_db"). You can connect to the database through your favorite RDBMS, like DataGrip for example, and create it, or use command line tool `psql` if you prefer this way 


```bash
$ psql connector_db \
      -U postgres \
      -h localhost \
      -p 5432
```

and then

```
connector_db=# CREATE DATABASE node_db;
```

If you are setting up backend for prism-management-console-web app, then create the "management_console_db" too

```
connector_db=# CREATE DATABASE management_console_db;
```

### Back-end

See the instructions on how to set up all the necessary dependencies to run the backend in dedicated [readme](prism-backend/README.md)

### Envoy

You need to have envoy proxy server running to proxy the grpc-web calls to the backend services, if you don't have it set up, refer to [Install Envoy](prism-management-console-web/README.md#Install-Envoy)

### Chrome extension

A chrome extension wallet which is necessary to log into web management console.

Compile the chrome extension
```bash
atala$ cd prism-web-wallet
prism-web-wallet$ sbt chromeUnpackedFast
```

In order to install the wallet on chrome. Open chrome and go to [chrome://extensions/](chrome://extensions/)

Activate the developer mode (top right corner of the page) and click on `Load unpacked` (top left visible in developer
mode), in the dialog opened go to `atala/prism-web-wallet/target/chrome`, select `unpacked-fast`
folder and click `Open`. Now the wallet should be found in your extensions.

At this point, you have all the needed components up to run the wallet locally.

## Working with the codebase

This is a monorepo and each of the `prism-XYZ` folders refers to a different part of the platform. Check the specific READMEs for more details.

Be sure to follow our [contributing guidelines](CONTRIBUTING.md).

In order to keep the code format consistent, we use scalafmt and git hooks, follow these steps to configure it accordingly (otherwise, your changes are going to be rejected by CircleCI):

- Install [coursier](prism-backend/README.md#Install-coursier), the `cs` command must work.
- install `scalafmt`

   ```bash
   cs install scalafmt
   ```
- `cp pre-commit .git/hooks/pre-commit`

## More docs

* Documentation about operational aspects of the team and the services we use can be found in [Confluence](https://input-output.atlassian.net/wiki/spaces/CE/pages/606371843/Code+and+Infrastructure+Setup).
* The general guideline and ultimate goal is to have the [repository](prism-backend/docs/README.md) as the source of truth for all technical documentation.
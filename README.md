# PRISM Node

This project represents a reference implementation of the PRISM Node described in the [PRISM DID method specification](https://github.com/input-output-hk/prism-did-method-spec/blob/main/w3c-spec/PRISM-method.md).
This code is not recommended for production environments. The project supports:
- the indexing of the blockchain
- The interpretation of the DID method events (Creation, updates and deactivation)
- batch submission for DID PRISM operations

## Dependencies

### Install coursier

Coursier is a package manager through which we will install all java/scala related dependencies. Follow the [instructions](https://get-coursier.io/docs/cli-installation.html) for your operating system

### Install JDK 11

```bash
cs java --jvm adopt:1.11.0-11 --setup
```
after that `java -version` should yield

```bash
openjdk version "11.0.11" 2021-04-20
OpenJDK Runtime Environment (build 11.0.11+9)
OpenJDK 64-Bit Server VM (build 11.0.11+9, mixed mode)
```

### Install sbt

```bash
cs install sbt
```

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
Alternatively, you can configure to run it against a Cardano network by setting values for the db-sync and Cardano wallet services. See [this documentation](https://github.com/hyperledger/identus-cloud-agent/blob/main/docs/guides/deploying-node.md) for more details

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
- `chmod +x .git/hooks/pre-commit`


## IDE / Editor support

If you intend to work on scala code, you should set up build server

first, generate bsp config

```bash
sbt -bsp
```
You can exit sbt as soon as you see `sbt:prism>` greeter.

This will generate `.bsp/sbt.json`, it will look similar to this
```json
{
  "name": "sbt",
  "version": "1.4.2",
  "bspVersion": "2.0.0-M5",
  "languages": [
    "scala"
  ],
  "argv": [
    "/Users/shota/Library/Caches/Coursier/jvm/adopt@1.11.0-8/Contents/Home/bin/java",
    "-Xms100m",
    "-Xmx100m",
    "-classpath",
    "/Users/shota/Library/Application Support/Coursier/bin/sbt:/Users/shota/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/sbt/sbt-runner/0.2.0/sbt-runner-0.2.0.jar:/Users/shota/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/sbt-launch/1.4.6/sbt-launch-1.4.6.jar",
    "xsbt.boot.Boot",
    "-bsp"
  ]
}
```

Once the file has been generated, edit `.bsp/sbt.json` and increase -Xmx setting to 4096m so it looks like this

```json
{
  "name": "sbt",
  "version": "1.4.2",
  "bspVersion": "2.0.0-M5",
  "languages": [
    "scala"
  ],
  "argv": [
    "/Users/shota/Library/Caches/Coursier/jvm/adopt@1.11.0-8/Contents/Home/bin/java",
    "-Xms100m",
    "-Xmx4096m",
    "-classpath",
    "/Users/shota/Library/Application Support/Coursier/bin/sbt:/Users/shota/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/io/get-coursier/sbt/sbt-runner/0.2.0/sbt-runner-0.2.0.jar:/Users/shota/Library/Caches/Coursier/v1/https/repo1.maven.org/maven2/org/scala-sbt/sbt-launch/1.4.6/sbt-launch-1.4.6.jar",
    "xsbt.boot.Boot",
    "-bsp"
  ]
}
```

### IntelliJ IDEA

You can open the project with IntelliJ IDEA.

When IntelliJ IDEA asks you what project configuration to use, pick "Open as BSP project". Once the project has been imported, make sure your project SDK is set to JDK 11.

#### Troubleshooting

If you get errors while the project gets imported by IntelliJ IDEA, try running IntelliJ from the terminal:
- `./bin/idea.sh` from the IntelliJ directory, for Linux.
- `open -a "IntelliJ IDEA"` or `open -a "IntelliJ IDEA CE"` for Mac.


This occurs commonly when dealing with scalajs, because npm is required, and, sometimes IntelliJ fails to get the proper `PATH`, example error log:

```bash
[error] (ProjectRef(uri("file:/home/dell/iohk/repo/cardano-enterprise/prism-sdk/"), "sdkJS") / ssExtractDependencies) java.io.IOException: Cannot run program "npm" (in directory "/home/dell/iohk/repo/cardano-enterprise/prism-sdk/js/target/scala-2.13/scalajs-bundler/main"): error=2, No such file or directory
```
### VSCode

If you use VSCode, you need to first install [metals](https://scalameta.org/metals/docs/editors/vscode.html#installation) extension.

Metals by default uses bloop as its build server, sometimes bloop has a problem importing this projects build. if you encounter the problem with that, you can switch to bsp.

1. Open the project with vscode (`build.sbt` has to be in the root of the project)
2. Run "Metals: import build", this might take a minute or two, depending on your machine.

#### Troubleshooting

If you encounter an error while importing the build, run "Metals: switch build server", you will be given an option to choose between multiply build servers. choose `bsp`, then re-import the build.

run "Metals: run doctor" to see if all sub-project builds have been imported

run "Metals: restart build server" to restart the build server, if editor is acting weird.


# PRISM backend

This is the server side of PRISM.

## Install all necessary dependencies

### Install coursier 

Coursier is a package manager through which we will install all java/scala related dependencies. Follow the [instructions](https://get-coursier.io/docs/cli-installation.html#linux-macos) for your operating system

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

## Run services

You need to run connector and node services

*NOTE:* If it is the first time you are running `sbt`, it will take a couple of minutes to start. the reason being is that `sbt` is actually an "sbt launcher", it has to download the appropriate version of sbt, (defined in `project/build.properties`) first time it is used. all the subsequent runs should take a couple of seconds.

```bash
sbt "connector/run"
```

In a different terminal tab/window, run node service

```bash
sbt "node/run"
```


### Testnet
The node can run against the Cardano Testnet by providing these environment variables before running it:

```bash
export NODE_CARDANO_DB_SYNC_HOST="credentials-database-test.co3l80tftzq2.us-east-2.rds.amazonaws.com"
export NODE_CARDANO_DB_SYNC_DATABASE="prism-test-cardano"
export NODE_CARDANO_DB_SYNC_USERNAME="prism-test-cardano"
export NODE_CARDANO_DB_SYNC_PASSWORD="ask-for-it"
export NODE_CARDANO_WALLET_API_HOST="3.21.97.253"
export NODE_CARDANO_WALLET_API_PORT="8090"

export NODE_CARDANO_PAYMENT_ADDRESS="addr1qrlh7p9th5c9ps938ry05vq96j92lzuhqr29v46caydf2wzkvlatzplcfr8afde6wsr6weskqr8k3u80e957ecmkvkhqe4n2hn"
export NODE_CARDANO_WALLET_ID="b8b1d9cba6582a2730a09ea704e84712dc6c1167"
export NODE_CARDANO_WALLET_PASSPHRASE="ask-for-it"

export NODE_CARDANO_CONFIRMATION_BLOCKS="1"
export NODE_LEDGER="cardano"
```

## Pushing Docker images
To build and push a Docker image for a backend module run the following command in your terminal:

```bash
sbt <module>/dockerBuildAndPush
```

By default, `sbt` pushes to AWS Elastic Container Registry. Alternatively, you can push to GitHub Packages Container registry. First, you need to set up your login details for GitHub. Assuming that your GitHub personal access token with write rights for packages is stored in `GITHUB_TOKEN`, run this:

```bash
 echo $GITHUB_TOKEN | docker login ghcr.io -u <USERNAME> --password-stdin
```

Next, by setting `GITHUB` environment variable to `1`, you can tell `sbt` that you want the images to be pushed to GitHub. For example, to push connector in such fashion run this:

```bash
GITHUB=1 sbt connector/dockerBuildAndPush
```

You can also use `TAG` environment variable to specify a custom Docker container tag:

```bash
TAG=test-container sbt connector/dockerBuildAndPush
```

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

### Running the whole project

See our main [README](../README.md#How-to-run) for instructions on how to run the whole project, including the frontend management console and the chrome extension (which is necessary to log into the management console).

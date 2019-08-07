# Atala
The Atala Framework and Sample Applications

[![CircleCI](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop.svg?style=svg&circle-token=1a9dcf544cec8cb581fa377d8524d2854cfb10e9)](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop)


## Branches

Two main branches will be maintained: `develop` and `master`. `master` contains the latest version of the code that was tested end-to-end. `develop` contains the latest version of the code that runs all the tests (unit and integration tests). Integration tests don't test all the integrations. Hence, any version in `develop` might have bugs when deployed for an end-to-end test.


## Working with the codebase

To build the codebase, we use [bazel](https://bazel.build/).

In order to keep the code format consistent, we use scalafmt and git hooks, follow these steps to configure it accordingly (otherwise, your changes are going to be rejected by CircleCI):
- Install [coursier](https://github.com/coursier/coursier#command-line), the `coursier` command must work.
- `./install-scalafmt.sh` (might require sudo).
- `cp pre-commit .git/hooks/pre-commit`

### A note on bazel

Bazel distributes the codebase in two types of entities:
 - **package**: a folder containing a `BUILD` file. A package is a collection of files and *rules*
 - **rule**: a rule is something that can be built. A rule is a pure function (in the more 'functional programming' sense) that given some inputs (tipically source files and some kind of compiler(s)) produces an output (an executable, a library, the result of running some tests...). It's important to note that rules are **pure**, that is, given some concrete inputs the whole thing can be replaced with what bazel would build. That is, bazel caches what it produces and if nothing in the input changes it's always going to use the cached version. And that's why tests in bazel must be _idempotent_.

Note: The BUILD file lists the rules of a package (usually one library and/or binary, and a test suite). If you want to see what a package can build, just look at the BUILD file.

Note 2: Bazel is designed from the grown-up for absolutelly reproducible builds, that is: once you have built something you should **_NEVER_** want/need to clean. You can clean (with `bazel clean`, or it's extreme version `bazel clean --expunge`), but you shouldn't.

### Using Bazel from the terminal

I'm going to explain different things that can be done, using this sample situation: we have a build file in `atala/obft/BUILD`, with two rules, an scala library named `obft` and a set of tests named `tests`. Where the `main` folder is a subfolder of the `workspace`. The workspace is the folder containing the `WORKSPACE` file.

All rules in bazel have a label (similar to a full name in Java/Scala). This labels when writen in full are something like this:

```
//<package_name>:<rule_name>
```

Where `package_name` is the path containing the `BUILD` file. In our example the package_name of our package is `atala/obft`. So the label for the `obft` rule is `//atala/obft:obft`. And the label for the `tests` rule is `//atala/obft:tests`.

If the last bit of `package_name` (that is `obft` in our example) is the same than the rule name, the rule name can be omited. That is, we can label our two packages `//atala/obft` and `//atala/obft:tests` which is quite clean.

There are three relevant command in bazel `build`, `run` and `test`. Usually run this way:

```bash
bazel <command> <label> [<label>...]
```

For example, to build `obft` you need to run this:

```bash
bazel build //atala/obft
```

Or, to run it's associated tests (that is the rule `tests`)

```bash
bazel test //atala/obft
```

Note that, by default only shows a summary of the test results, but not the whole thing. If you want the whole thing, you need this:

```bash
bazel test //atala/obft --test_output=all
```

Or, if you are only interested on the tests that fail:

```bash
bazel test //atala/obft --test_output=errors
```

The `run` command only accepts one label:

```bash
bazel run atala/apps/cluster:cluster
```

But the other two accept as many as you need. Or even better, you can use the `...` wildcard. This will run all the tests below `main` (recursively):

```bash
bazel test //main/...
```

Or this will build and test everything:

```bash
bazel test //...
```

#### Running the scala repl with bazel

To start a repl with the content of an `scala_library` or an `scala_binary`, you only need to run an special rule created automaticaly for all of them. This rule has the same name than the library/binary with `_repl` added at the end. For example, to run the repl for `atala/obft` you would run:

```bash
bazel run //atala/obft:obft_repl
```

Or, if you want to run the repl for `atala` (the library that contains almost everything), you would run:

```bash
bazel run //atala:atala_repl
```

#### Locality

One nice thing about bazel is that you don't need to work from the workspace folder. Using the same example than before, you can do this things:

```bash
cd atala/obft
bazel build obft          # No need to specify the whole label in here
bazel test tests          # The first `test` is the command, followed by the rule name `tests`
vim OuroborosBFT.scala    # Edit something local
bazel test tests          # and test again
```

#### Bazel on windows

Bazel does not really work on windows. You can however build the source on windows using a tool called `dazel`.
Dazel runs a docker container with bazel.

If you are using a professional or enterprise edition of Windows, the steps are fairly straightforward. You install
docker then install and run dazel (https://github.com/nadirizr/dazel).

If you using a home edition of windows, there is no virtualization support and you should use the legacy `docker toolbox`
from [https://docs.docker.com/toolbox/toolbox_install_windows/].

## Sample applications

### Cluster

This application emulates a whole cluster of Atala nodes (basically OBFT nodes) and a client application that communicates with that cluster. You can start the whole thing with this command:

```bash
bazel run //atala/apps/cluster:cluster
```

This simple command-line application, implements a key-value store where the keys are integers and the values are strings. Also, it forbids changing values once in the store

### KvNode

This application starts a single node of a cluster of Atala nodes. The topology of the cluster is goberned by the configuration file loaded by the application on start up. The default configuration supposes a cluster of a single node (itself) and can be started with this command:

```bash
bazel run //atala/apps/kvnode:kvnode
```

#### Interacting with the cluster

To interact with the cluster, you only need to perform REST calls to a single node of the cluster. This examples are going to suppose the REST interface of the node is running on `localhost:9090`

***STORE STATE:*** query the current state in the store

```bash
curl http://localhost:9090/state
```

***ADD:*** add a key value pair to the store

```bash
curl http://localhost:9090/add/42/towel
```

#### Spinning up a cluster

To spin up a cluster with `n` nodes, you only need to start the application `n` times, each with it's own configuration specifying where the other `n - 1` nodes are

In [atala/apps/kvnode/resources/](atala/apps/kvnode/resources/) can be found three sample configuration files that can be used to start up a three nodes cluster with the following commands (run from the root of this repo)

```bash
bazel run atala/apps/kvnode -- --jvm_flag="-Dconfig.file=$PWD/atala/apps/kvnode/resources/applicationC3A.conf"
```

```bash
bazel run atala/apps/kvnode -- --jvm_flag="-Dconfig.file=$PWD/atala/apps/kvnode/resources/applicationC3B.conf"
```

```bash
bazel run atala/apps/kvnode -- --jvm_flag="-Dconfig.file=$PWD/atala/apps/kvnode/resources/applicationC3C.conf"
```

## Infrastructure and Integrations

All documentation related to our integrations and infrastructure should be found [here](https://input-output.atlassian.net/wiki/spaces/CE/pages/606371843/Code+and+Infrastructure+Setup)

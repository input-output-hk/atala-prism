# cardano-enterprise
The Cardano Enterprise Framework and Reference Applications

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

I'm going to explain different things that can be done, using this sample situation: we have a build file in `main/io/iohk/cef/codecs/BUILD`, with two rules, an scala library named `codecs` and a set of tests named `tests`. Where the `main` folder is a subfolder of the `workspace`. The workspace is the folder containing the `WORKSPACE` file.

All rules in bazel have a label (similar to a full name in Java/Scala). This labels when writen in full are something like this:

```
//<package_name>:<rule_name>
```

Where `package_name` is the path containing the `BUILD` file. In our example the package_name of our package is `main/io/iohk/cef/codecs`. So the label for the `codecs` rule is `//main/io/iohk/cef/codecs:codecs`. And the label for the `tests` rule is `//main/io/iohk/cef/codecs:tests`.

If the last bit of `package_name` (that is `codecs` in our example) is the same than the rule name, the rule name can be omited. That is, we can label our two packages `//main/io/iohk/cef/codecs` and `//main/io/iohk/cef/codecs:tests` which is quite clean.

There are three relevant command in bazel `build`, `run` and `test`. Usually run this way:

```bash
bazel <command> <label> [<label>...]
```

For example, to build `codecs` you need to run this:

```bash
bazel build //main/io/iohk/cef/codecs
```

Or, to run it's associated tests (that is the rule `tests`)

```bash
bazel test //main/io/iohk/cef/codecs
```

Note that, by default only shows a summary of the test results, but not the whole thing. If you want the whole thing, you need this:

```bash
bazel test //main/io/iohk/cef/codecs --test_output=all
```

Or, if you are only interested on the tests that fail:

```bash
bazel test //main/io/iohk/cef/codecs --test_output=errors
```

The `run` command only accepts one label:

```bash
bazel run src/main/scala/io/iohk/cef/main
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

To start a repl with the content of an `scala_library` or an `scala_binary`, you only need to run an special rule created automaticaly for all of them. This rule has the same name than the library/binary with `_repl` added at the end. For example, to run the repl for `main/io/iohk/cef/codecs` you would run:

```bash
bazel run //main/io/iohk/cef/codecs:codecs_repl
```

Or, if you want to run the repl for `cef` (the library that contains almost everything), you would run:

```bash
bazel run //:cef_repl
```

#### Locality

One nice thing about bazel is that you don't need to work from the workspace folder. Using the same example than before, you can do this things:

```bash
cd main/io/iohk/cef/codecs
bazel build codecs   # No need to specify the whole label in here
bazel test tests     # The first `test` is the command, followed by the rule name `tests`
vim package.scala    # Edit something local
bazel test tests     # and test again
```


## Sample application

NOTE: This bit of the documentation needs to be improved/corrected

A sample application is provided that configures and starts up an Identity Ledger. It is located in `io.iohk.cef.main.IndentityTxMain`. To run this application, simply execute `bazel run src/main/scala/io/iohk/cef/main`.

When setting up a cluster, you need to configure each node's properties. 
The configuration files used in `IndentityTxMain` are 
* `src/main/resources/reference.conf` for configuring the library.
* `src/main/resources/identity-tx-main.conf` for configuring the app.

Below is a sample of a config of the node "1111" that belongs to a three-node cluster (1111,2222,3333): 

```
db {
  default {
    driver = "org.h2.Driver"
    url = "jdbc:h2:file:./db/default"
    user = "sa"
    password = ""
    poolInitialSize = 5
    poolMaxSize = 7
    poolConnectionTimeoutMillis = 1000
    poolValidationQuery = "select 1 as one"
    poolFactoryName = "commons-dbcp2"
  }
}

peer-config {
  node-id = "1111"
  capabilities = "01"
  network-config {
    tcp-transport-config {
      bind-address = "127.0.0.1:9011"
      nat-address = "127.0.0.1:9011"
    }
  }
}

discovery-config {
  discovery-enabled = true
  interface = "127.0.0.1"
  port = 8011
  bootstrap-nodes = [
    {
      id = "2222"
      discovery-address = "127.0.0.1:8022"
      server-address = "127.0.0.1:9022"
      capabilities = "01"
    },
    {
      id = "3333"
      discovery-address = "127.0.0.1:8033"
      server-address = "127.0.0.1:9033"
      capabilities = "01"
    }
  ]
  discovered-nodes-limit = 10
  scan-nodes-limit = 10
  concurrency-degree = 20
  scan-initial-delay = 10000
  scan-interval = 15000
  message-expiration = 100000
  max-seek-results = 10
  multiple-connections-per-address = true
  blacklist-default-duration = 30000
}

ledger-config {
  id = "1"
  max-block-size = 10000
  default-transaction-expiration = "1 minute"
  block-creator-initial-delay = "10 seconds"
  block-creator-interval = "1 minute"
}

consensus-config {
  raft-config {
    node-id = ${peer-config.node-id}
    cluster-member-ids = ["2222", "3333"]
    election-timeout-range = ["150 millis", "300 millis"]
    heartbeat-timeout-range = ["75 millis", "75 millis"]
  }
}
```
## Agreements Demo
For agreements demo you need to run minimum 2 nodes
Currently the Agreements demo app is based on data type String only
Below scripts can be used to start the 3 nodes manually with respective configurations

```bash
bazel run -- //src/main/scala/io/iohk/cef/main:AgreementsMain agreements-node-1-app

bazel run -- //src/main/scala/io/iohk/cef/main:AgreementsMain agreements-node-2-app

bazel run -- //src/main/scala/io/iohk/cef/main:AgreementsMain agreements-node-3-app


```
## curl sample request for Agreements Demo
```
curl -d '{"correlationId": "agreementId","data": "it is raining","to": ["2222","3333"]}' -XPOST -H "Content-Type: application/json" http://localhost:8000/agreements/weather/propose
   
curl -d '{"correlationId": "agreementId","data": "it is raining"}' -XPOST -H "Content-Type: application/json" http://localhost:9000/agreements/weather/agree

curl -d '{"correlationId": "agreementId"}' -XPOST -H "Content-Type: application/json" http://localhost:7000/agreements/weather/decline

```

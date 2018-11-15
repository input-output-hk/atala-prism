# cardano-enterprise
The Cardano Enterprise Framework and Reference Applications

[![CircleCI](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop.svg?style=svg&circle-token=1a9dcf544cec8cb581fa377d8524d2854cfb10e9)](https://circleci.com/gh/input-output-hk/cardano-enterprise/tree/develop)

#### Working with the codebase

To be able to build or test the codebase, `sbt-verify` needs to be properly setup.

`sbt-verify` is used in order to check the validity of the checksums of all the downloaded libraries.

`sbt-verify` can be downloaded from our read only repository by typing

 `git clone  https://github.com/input-output-hk/sbt-verify`

Then in order to make `sbt-verify` available to our build type

```
cd sbt-verify
git checkout sbt-1.x
sbt publishLocal
```

This installs the `sbt-verify` library to your local repository.

After installing the `sbt-verify` library to your local repository checkout this repository from github and, for example, type

```
sbt test
```

in the root of the project.

#### Branches

Two main branches will be maintained: `develop` and `master`. `master` contains the latest version of the code that was tested end-to-end. `develop` contains the latest version of the code that runs all the tests (unit and integration tests). Integration tests don't test all the integrations. Hence, any version in `develop` might have bugs when deployed for an end-to-end test.

#### Sample application

A sample application is provided that configures and starts up an Identity Ledger. It is located in `io.iohk.cef.main.IndentityTxMain`. To run this application, simply execute `sbt "runMain io.iohk.cef.main.IndentityTxMain"`.

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

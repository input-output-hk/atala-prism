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

When setting up a cluster, you need to configure each node's properties. The configuration file used in `IndentityTxMain` is found on `src/main/resources/reference.conf`. Below is a sample of a config of the node "1111" that belongs to a three-node cluster (1111,2222,3333): 

```
akka {
  loglevel = "DEBUG"
  actor.debug.unhandled = on
}

db {
  default {
    driver = "org.h2.Driver"
    url="jdbc:h2:file:./db/default"
    user="sa"
    password=""
    poolInitialSize=5
    poolMaxSize=7
    poolConnectionTimeoutMillis=1000
    poolValidationQuery="select 1 as one"
    poolFactoryName="commons-dbcp2"
  }
}

network {
  discovery {
    enabled = true
    interface = "172.31.7.11"
    port = 8011
    bootstrapNodes = [
      {
        discoveryUri = "udp://172.31.10.15:8022"
        p2pUri = "enode://2222@172.31.10.15:9022"
        capabilities = "01"
      },
      {
        discoveryUri = "udp://172.31.13.9:8033"
        p2pUri = "enode://3333@172.31.13.9:9033"
        capabilities = "01"
      }
    ]
    discoveredNodesLimit = 10
    scanNodesLimit = 10
    concurrencyDegree = 20
    scanInitialDelay = 10000
    scanInterval = 15000
    messageExpiration = 100000
    maxSeekResults = 10
    multipleConnectionsPerAddress = true
    blacklistDefaultDuration = 30000
  }
  server {
    interface = "172.31.7.11"
    port = 9011
  }
}

node {
  id = "1111"
  capabilities = "01"
}

ledger {
  id = 1
  maxBlockSizeInBytes = 10000
  defaultTransactionExpiration = "1 minute"
  blockCreatorInitialDelay = "10 seconds"
  blockCreatorInterval = "1 minute"
}

frontend {
  rest {
    port = 8888
    interface = "172.31.7.11"
  }
}

telemetry {
  nodeTag = ""
  datadog {
    apiKey = "d1c1eec86cec73406295f84f7e69da41"
    duration = 10000 //milli
  }
}

consensus {
  raft {
    clusterMemberIds = ["2222","3333"]
    electionTimeoutRangeStart = "1 minute"
    electionTimeoutRangeEnd = "1 minute"
    heartbeatTimeoutRangeStart = "1 minute"
    heartbeatTimeoutRangeEnd = "1 minute"
  }
}
```



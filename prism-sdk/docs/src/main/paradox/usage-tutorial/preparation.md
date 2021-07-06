# Integrating the Atala PRISM SDK Into a Scala Project Using SBT

## Prerequisites

This section explains how to integrate the Atala PRISM SDK into a Scala project using the Scala Build Tool (SBT). 

**Note:** This tutorial assumes that you are proficient in setting up/working with [SBT](https://www.scala-sbt.org/) projects. You can create your own project or open an existing one.


## Adding Dependencies

After creating (or opening) the SBT project, add the necessary dependencies to your `build.sbt`:

```scala
libraryDependencies += "io.iohk" %% "prism-protos" % "@VERSION@" // needed for the credential payloads defined in protobuf
libraryDependencies += "io.iohk" %% "prism-crypto" % "@VERSION@" // needed to get a crypto implementation
libraryDependencies += "io.iohk" %% "prism-identity" % "@VERSION@" // needed to deal with DIDs
libraryDependencies += "io.iohk" %% "prism-credentials" % "@VERSION@" // needed to deal with credentials
```

## Importing Atala PRISM Modules

For the rest of the tutorial, you can use the Scala Read-Evaluate-Print-Loop (REPL) tool to test changes (`sbt console`), or take the code snippets into a proper file.

Let's import the Atala PRISM modules required to complete the next steps:

```scala mdoc
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity._
```

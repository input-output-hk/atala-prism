# Preparation steps
These preparation steps are necessary to complete the usage tutorial.

This section gives you the necessary steps to integrate the PRISM SDK in a Scala project, you are expected to know how to set up an [sbt](https://www.scala-sbt.org/) project.

Proceed by creating a new project, or opening an existing one.


## Dependencies

Once you have created the sbt project, add the necessary dependencies to your `build.sbt`:

```scala
libraryDependencies += "io.iohk" %% "prism-protos" % "@VERSION@" // needed for the credential payloads defined in protobuf
libraryDependencies += "io.iohk" %% "prism-crypto" % "@VERSION@" // needed to get a crypto implementation
libraryDependencies += "io.iohk" %% "prism-identity" % "@VERSION@" // needed to deal with DIDs
libraryDependencies += "io.iohk" %% "prism-credentials" % "@VERSION@" // needed to deal with credentials
```

## Imports
For the rest of the tutorial, you can use the Scala REPL to try every change (`sbt console`), or take the code snippets into a proper file.

Let's import the PRISM modules which are required to complete the next steps:

```scala mdoc
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity._
```


## Next

By now, you have setup a Scala project with SBT that includes the PRISM SDK, you are ready to proceed to the next section.

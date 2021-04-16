## Prerequisites

This section explains how to integrate the Atala PRISM SDK into a Kotlin project using the Gradle Build Tool.

**Note:** This tutorial assumes that you are proficient in setting up/working with [Gradle](https://gradle.org/) projects. You can create your own project or open an existing one.

It is worth saying that the examples use the `runBlocking` clause to make the examples simpler (i.e. not having to worry about coroutine contexts). If you use Kotlin in your integration, you aren't expected to use `runBlocking` directly.

## Adding Dependencies

After creating (or opening) the Gradle project, add the necessary dependencies to your `build.gradle`:

```kotlin
implementation("io.iohk.atala.prism:protos:$VERSION") // needed for the credential payloads defined in protobuf as well as to interact with our backend services
implementation("io.iohk.atala.prism:crypto:$VERSION") // needed for cryptography primitives implementation
implementation("io.iohk.atala.prism:identities:$VERSION") // needed to deal with DIDs
implementation("io.iohk.atala.prism:credentials:$VERSION") // needed to deal with credentials
implementation("io.iohk.atala.prism:extras:$VERSION") // used to avoid some boilerplate

// needed for the credential content, bring the latest version
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")

// needed for dealing with dates, bring the latest version
implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.1")
```


Also, include these repositories to be able to resolve all the dependencies:

```kotlin
repositories {
    mavenCentral()
    mavenLocal()
    google()
    maven("https://plugins.gradle.org/m2/")
    maven("https://dl.bintray.com/itegulov/maven")
    maven("https://kotlin.bintray.com/kotlinx/")
    maven("https://dl.bintray.com/acinq/libs")
}
```


## Importing Atala PRISM Modules

For the rest of the tutorial, you can take the code snippets into a Kotlin file.

Let's import the Atala PRISM modules required to complete the next steps:

```kotlin:ank
import io.iohk.atala.prism.kotlin.credentials.BatchData
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.credentials.CredentialBatches
import io.iohk.atala.prism.kotlin.credentials.CredentialVerification
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.Hash
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.crypto.util.toByteArray
import io.iohk.atala.prism.kotlin.extras.ProtoClientUtils
import io.iohk.atala.prism.kotlin.extras.ProtoUtils
import io.iohk.atala.prism.kotlin.extras.RequestUtils
import io.iohk.atala.prism.kotlin.extras.findPublicKey
import io.iohk.atala.prism.kotlin.extras.toTimestampInfoModel
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pbandk.decodeFromByteArray
import pbandk.encodeToByteArray
```

## Backend clients

At last, let's create the clients for our backend services involved in this tutorial, the Connector and the Node:

```kotlin:ank
val connector = ProtoClientUtils.connectorClient("localhost", 50051)
val node = ProtoClientUtils.nodeClient("localhost", 50053)
```

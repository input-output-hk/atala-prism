This section explains how to integrate the **Atala PRISM SDK** into a **Kotlin** project using the **Gradle Build Tool**. It also shows how to set up **Atala PRISM** environment locally.

**NOTE:** This tutorial assumes proficiency in setting up/working with [Gradle](https://gradle.org/) projects. Create a new project or open an existing one.

It is worth saying that the examples use the `runBlocking` clause to make the examples simpler (i.e. not having to worry about coroutine contexts). If **Kotlin** is used as integration there is no need to use `runBlocking` directly.

## Services
If there is no dedicated environment where all services are up and running there is a possibility to run all required services locally. To do it follow the next steps. 

**NOTE:** If a working environment (where services are running) is already deployed feel free to skip these steps.

### Prerequisites

1. Install [docker](https://www.docker.com/).
2. Install [Java 8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html).
3. Install [sbt](https://www.scala-sbt.org/).
4. Install [psql](https://www.postgresql.org/download/).

### Steps
1. Run **Postgres** inside docker container and leave it running:
```bash
docker run -it --rm -e POSTGRES_DB=connector_db -e POSTGRES_HOST_AUTH_METHOD=trust -p 5432:5432 postgres
```
2. Login to database in a new terminal window:
```bash
psql connector_db \
       -U postgres \
       -h localhost \
       -p 5432
```
3. Create a new database called `node_db` for **Node**:
```bash
connector_db=# CREATE DATABASE node_db;
```

4. In a new terminal window clone [Atala repository](https://github.com/input-output-hk/atala) and navigate to `prism-backend` directory:
```bash
git clone git@github.com:input-output-hk/atala.git
cd atala/prism-backend/
```

5. Start **Node** and leave it running:
```bash
sbt node/run
```

6. Start **Connector**  and leave it running:
```bash
sbt connector/run
```

That's it! All services are up and running locally.

## Adding dependencies

After creating (or opening) the **Gradle** project, add the necessary dependencies to your `build.gradle`:

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

For the rest of the tutorial insert the code snippets into a **Kotlin** file.

Let's import the **Atala PRISM** modules required to complete the next steps:

```kotlin:ank
import io.iohk.atala.prism.kotlin.credentials.BatchData
import io.iohk.atala.prism.kotlin.credentials.CredentialBatchId
import io.iohk.atala.prism.kotlin.credentials.CredentialBatches
import io.iohk.atala.prism.kotlin.credentials.CredentialVerification
import io.iohk.atala.prism.kotlin.credentials.content.CredentialContent
import io.iohk.atala.prism.kotlin.credentials.json.JsonBasedCredential
import io.iohk.atala.prism.kotlin.credentials.utils.toTimestampInfoModel
import io.iohk.atala.prism.kotlin.crypto.EC
import io.iohk.atala.prism.kotlin.crypto.Hash
import io.iohk.atala.prism.kotlin.crypto.MerkleInclusionProof
import io.iohk.atala.prism.kotlin.extras.ProtoClientUtils
import io.iohk.atala.prism.kotlin.extras.ProtoUtils
import io.iohk.atala.prism.kotlin.extras.RequestUtils
import io.iohk.atala.prism.kotlin.extras.findPublicKey
import io.iohk.atala.prism.kotlin.identity.DID
import io.iohk.atala.prism.kotlin.protos.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import pbandk.decodeFromByteArray
import pbandk.encodeToByteArray
```

## Backend clients

At last, let's create the clients for backend services involved in this tutorial, the **Connector** and the **Node**:

```kotlin:ank
val environment = "localhost" // If exists, replace 'localhost' with an url to your dedicated environment. 
val connector = ProtoClientUtils.connectorClient(environment, 50051)
val node = ProtoClientUtils.nodeClient(environment, 50053)
```

**NOTE:** If a production-like environment with **Cardano** integration is used keep in mind that additional waits may be needed between the calls to backend services.
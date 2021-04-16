This section explains how to integrate the Atala PRISM SDK into a Kotlin project using the Gradle Build Tool.

**Note:** This tutorial assumes that you are proficient in setting up/working with [Gradle](https://gradle.org/) projects. You can create your own project or open an existing one.


## Adding dependencies

After creating (or opening) the Gradle project, add the necessary dependencies to your `build.gradle`:

```kotlin
implementation("io.iohk.atala.prism:protos:$VERSION") // needed for the credential payloads defined in protobuf
implementation("io.iohk.atala.prism:crypto:$VERSION") // needed to get a crypto implementation
implementation("io.iohk.atala.prism:identities:$VERSION") // needed to deal with DIDs
implementation("io.iohk.atala.prism:credentials:$VERSION") // needed to deal with credentials

// needed for the credential content, bring the latest version
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
```

## Importing Atala PRISM modules

For the rest of the tutorial, you can take the code snippets into a Kotlin file.

Let's import the Atala PRISM modules required to complete the next steps:

```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.*
import io.iohk.atala.prism.kotlin.identity.*
import io.iohk.atala.prism.kotlin.credentials.*
import io.iohk.atala.prism.kotlin.credentials.content.*
import io.iohk.atala.prism.kotlin.credentials.json.*
import kotlinx.datetime.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
```

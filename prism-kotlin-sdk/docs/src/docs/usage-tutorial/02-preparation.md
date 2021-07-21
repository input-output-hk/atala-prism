import PrismDependencies from '../../src/components/PrismDependencies';

# Preparation

This section explains how to integrate the **Atala PRISM SDK** into a **Kotlin** project using the **Gradle Build Tool**.

**NOTE:** This tutorial assumes proficiency in setting up/working with [Gradle](https://gradle.org/) projects. Fee free to create a new project or open an existing one.


## Adding dependencies

After creating (or opening) the **Gradle** project, add the necessary dependencies to your `build.gradle`:

<PrismDependencies/>

## Importing Atala PRISM modules

For the rest of the tutorial, you can take the code snippets into a **Kotlin** file.

Let's import the **Atala PRISM** modules required to complete the next steps:

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

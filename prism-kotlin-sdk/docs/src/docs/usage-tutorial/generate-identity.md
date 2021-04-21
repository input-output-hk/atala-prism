Once you have a project that includes the **Atala PRISM SDK**, you can generate a **Decentralized Identifier (DID)**. This section explains how to generate a **DID** and extract some information from it, which you will need later.

**NOTE:** Extensive knowledge or understanding of **DIDs** is not required to complete this tutorial. Check the official [DID specifications](https://w3c-ccg.github.io/did-spec/) or [Decentralized identifiers on Wiki](https://en.wikipedia.org/wiki/Decentralized_identifiers) for reference.

## Recap
This is what we have done in previous steps:

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

## Generating a DID

Generating a **DID** requires an associated public key. This example derives a public key from an [elliptic-curve](https://en.wikipedia.org/wiki/Elliptic-curve_cryptography).

```kotlin:ank
val masterKeyPair = EC.generateKeyPair()
val did = DID.createUnpublishedDID(masterKeyPair.publicKey)
```

**DID** can be created without *any network interaction nor blockchain transaction*. These **DIDs** are called **unpublished DIDs**.

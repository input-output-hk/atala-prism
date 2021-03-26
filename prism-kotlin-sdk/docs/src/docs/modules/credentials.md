To install the module add the following to your `build.gradle`:
```kotlin
implementation("io.iohk.atala.prism:credentials:$VERSION")

// needed for the credential content, bring the latest version
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")

```

## Verifiable Credentials (VCs)

This module provides the necessary tools to work with Verifiable Credentials (VCs).

Add these imports to run the examples listed on this module:

```kotlin:ank
import io.iohk.atala.prism.kotlin.crypto.* // necessary to sign a credential
import io.iohk.atala.prism.kotlin.credentials.*
import io.iohk.atala.prism.kotlin.credentials.content.*
import io.iohk.atala.prism.kotlin.credentials.json.*

import kotlinx.serialization.json.* // necessary to construct the credential content

```
Follow these steps to create, sign, and verify a credential.

1. Define the credential's content:

```kotlin:ank
val credentialContent = CredentialContent(
    JsonObject(
        mapOf(
            Pair("credentialSubject", JsonObject(
                mapOf(
                    Pair("name", JsonPrimitive("José López Portillo")),
                    Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
                )
            ))
        )
    )
)
```

2. Create the `Credential` object:

```kotlin:ank
val credential = JsonBasedCredential(credentialContent)
```

3. You can sign a credential by using the Crypto module:

```kotlin:ank
val masterKeyPair = EC.generateKeyPair()
val signedCredential = credential.sign(masterKeyPair.privateKey)
```

4. Embed the credential in a batch, which leaves you with a proof of inclusion:

```kotlin:ank
val (merkleRoot, merkleProofs) = CredentialBatches.batch(listOf(signedCredential))
```

5. Verify that a credential is valid:

```kotlin
CredentialVerification.verify(
    keyData = TODO, // get this from Cardano
    batchData = TODO, // get this from Cardano
    credentialRevocationTime = TODO, // get this from Cardano
    merkleRoot = merkleRoot,
    inclusionProof = merkleProofs.first(), // the batch includes a single credential
    signedCredential = signedCredential
)
```

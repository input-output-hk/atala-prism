To install the module add the following to your `build.gradle`:
```kotlin
implementation("io.iohk.atala.prism:credentials:$VERSION")

// needed for the credential content, bring the latest version
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")

```

## Verifiable Credentials (VCs)

This module provides the necessary tools to work with [Verifiable Credentials - VCs](https://en.wikipedia.org/wiki/Verifiable_credentials).

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

3. You can sign a credential by using the `Crypto` module:

```kotlin:ank
val masterKeyPair = EC.generateKeyPair()
val signedCredential = credential.sign(masterKeyPair.privateKey)
```

4. Embed the credential in a batch, which leaves you with a proof of inclusion:

```kotlin:ank
val (merkleRoot, merkleProofs) = CredentialBatches.batch(listOf(signedCredential))
```

5. Mock Cardano data:
```kotlin:ank
// assume there is a block in Cardano that includes the DID, and the credential, which was confirmed 1 minute ago
val didBlockInfo = TimestampInfo(
        atalaBlockTimestamp = Clock.System.now().minus(60, DateTimeUnit.SECOND).epochSeconds,
        atalaBlockSequenceNumber = 1,
        operationSequenceNumber = 1
)
val batchBlockInfo = TimestampInfo(
        atalaBlockTimestamp = Clock.System.now().minus(20, DateTimeUnit.SECOND).epochSeconds,
        atalaBlockSequenceNumber = 2,
        operationSequenceNumber = 2
)

// this metadata about the DID key should be retrieved from the Cardano network
val keyData = KeyData(publicKey = masterKeyPair.publicKey, addedOn = didBlockInfo, revokedOn = null)

// this credential batch metadata should be retrieved from the Cardano network
val batchData = BatchData(issuedOn = batchBlockInfo, revokedOn = null)
```

6. Verify that a credential is valid:

```kotlin:ank
CredentialVerification.verify(
    keyData = keyData, // mocked for now, will get this from Cardano in next iteration
    batchData = batchData, // mocked for now, will get this from Cardano in next iteration
    credentialRevocationTime = null, // mocked for now, will get this from Cardano in next iteration
    merkleRoot = merkleRoot,
    inclusionProof = merkleProofs.first(), // the batch includes a single credential
    signedCredential = signedCredential
)
```

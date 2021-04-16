This section explains how to verify a credential's validity.

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

val masterKeyPair = EC.generateKeyPair()
val did = DID.createUnpublishedDID(masterKeyPair.publicKey)

val credentialContent = CredentialContent(
    JsonObject(
        mapOf(
            Pair("issuerDid", JsonPrimitive(did.value)),
            Pair("issuanceKeyId", JsonPrimitive("Issuance-0")),
            Pair("credentialSubject", JsonObject(
                mapOf(
                    Pair("name", JsonPrimitive("José López Portillo")),
                    Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
                )
            )),
        )
    )
)
val credential = JsonBasedCredential(credentialContent)
val signedCredential = credential.sign(masterKeyPair.privateKey)
val (merkleRoot, merkleProofs) = CredentialBatches.batch(listOf(signedCredential))
```


## Mocking Network Data

For simplicity, this tutorial does not interact with the Cardano network. A real implementation would require the retrieval of some data from the network, so in this tutorial, we will mock that data.

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

## Verifying the Credential

Taking the data from the previous steps, we can now verify the credential. The code returns no errors, so the credential is valid:

```kotlin:ank
CredentialVerification.verify(
    keyData = keyData,
    batchData = batchData,
    credentialRevocationTime = null,
    merkleRoot = merkleRoot,
    inclusionProof = merkleProofs.first(), // the batch includes a single credential
    signedCredential = signedCredential
)
```

## More

By now, check the [Integration Tutorial](../integration-tutorial) if you are ready to integrate PRISM.

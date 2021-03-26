# Issuing a Credential

This section explains how to issue a credential.


## Recap

This is what we have done from previous versions:

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
```

## Defining the Credential Claims

Before issuing a credential, we need to define its contents:

```kotlin:ank
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
```

In this case, `credentialSubject` represents the claims involved in the credential, while the other fields are metadata specifying *who* is issuing the credential.


## Deriving a Credential From its Claims

We can use the previous claims to derive a `Credential` object:

```kotlin:ank
val credential = JsonBasedCredential(credentialContent)
```

## Signing the Credential

Having the `Credential` model allows to easily sign it, which results in a signed credential:

```kotlin:ank
val signedCredential = credential.sign(masterKeyPair.privateKey)
```

**Note:** These are the keys related to the DID generated in the previous section.


## Issuing the Credential

In our protocol, issuing a credential involves creating batches of signed credentials (so that you only pay one Cardano fee). Batches are timestamped in the Cardano blockchain. For simplicity, we'll get the relevant data without touching the Cardano network:

```kotlin:ank
val (merkleRoot, merkleProofs) = CredentialBatches.batch(listOf(signedCredential))
```

The `merkleRoot` and `merkleProofs` are necessary to verify the credential validity.

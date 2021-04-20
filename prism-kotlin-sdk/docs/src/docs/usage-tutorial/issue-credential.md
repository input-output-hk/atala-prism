Issuing a credential on a blockchain provides proof of when, where and by whom that credential was issued and can’t be changed.

This section explains how to do it using **Atala PRISM SDK**.

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
```

## Defining the credential claims

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


## Deriving a credential from its claims

We can use the previous claims to derive a `Credential` object:

```kotlin:ank
val credential = JsonBasedCredential(credentialContent)
```

## Signing the credential

Having the `Credential` model allows to easily sign it, which results in a signed credential:

```kotlin:ank
val signedCredential = credential.sign(masterKeyPair.privateKey)
```

**NOTE:** These are the keys related to the DID generated in the previous section.


## Issuing the credential

In our protocol, issuing a credential involves creating batches of signed credentials to reduce the time and costs of publishing them. Batches are timestamped and for simplicity, we'll get the relevant data without touching any external network:

```kotlin:ank
val (merkleRoot, merkleProofs) = CredentialBatches.batch(listOf(signedCredential))
```

The `merkleRoot` and `merkleProofs` are necessary to verify the credential validity.

**NOTE:** To get more info about `Merkle tree` visit [Merkle tree wiki page](https://en.wikipedia.org/wiki/Merkle_tree) or [this page](https://www.investopedia.com/terms/m/merkle-root-cryptocurrency.asp) to see how these techniques are related to blockchain.

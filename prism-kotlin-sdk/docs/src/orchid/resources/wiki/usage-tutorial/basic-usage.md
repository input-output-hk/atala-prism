In this section, you will find how to use the PRISM SDK for issuing simple credentials, all the operations done locally.

This assumes you already know how to setup an [gradle](https://gradle.org/) project.

## Dependencies

First of all, add the necessary dependencies to your `gradle.build.kts`:

```kotlin
implementation("io.iohk.atala.prism:protos:$VERSION") // needed for the credential payloads defined by protobuf
implementation("io.iohk.atala.prism:crypto:$VERSION") // needed to get a crypto implementation
implementation("io.iohk.atala.prism:identity:$VERSION") // needed to deal with DIDs
implementation("io.iohk.atala.prism:credentials:$VERSION") // needed to deal with credentials
```

## Imports
Import the PRISM modules:

```kotlin
import io.iohk.atala.prism.kotlin.credentials.Credential
import io.iohk.atala.prism.kotlin.credentials.content.*
import io.iohk.atala.prism.kotlin.credentials.json.*
import io.iohk.atala.prism.kotlin.crypto.*
import io.iohk.atala.prism.kotlin.identity.*
import io.iohk.atala.prism.kotlin.protos.*
```

## Generate an identity
In order to use the PRISM ecosystem, you will need to generate an identity, PRISM provides [Decentralized Identifiers](https://w3c-ccg.github.io/did-spec/), for this section, we use an Unpublished DID. On such DID, the operations don't require network access, it is handy for trying the SDK, or, for use cases where you don't want others to discover such DID.


Generating an Unpublished DID is simple, we start generating a master key, taking the public key to generate the DID:

```kotlin
val masterKeyPair = EC.generateKeyPair()
val did = DID.createUnpublishedDID(masterKeyPair.publicKey)
```


Then, we need to get some details before being able to sign a credential, the [DID Document](https://w3c.github.io/did-core/#dfn-did-documents), and the id for the master key we used while creating our DID:

```kotlin
// The DID Document is necessary to find the master key id, used to sign a credential
val didDocument =
    (did.asLongForm()?.getInitialState()?.operation as? AtalaOperation.Operation.CreateDid)?.value!!

// we have created the DID with a single public key
val firstPublicKey = didDocument.didData?.publicKeys?.firstOrNull()!!
```


## Generate a credential
Then, generating a credential requires defining credential content (with optional credentialSubject), 
let's define a simple one:

```kotlin
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

val credentialContent = buildCredentialContent {
    putJsonArray("type") {
        add("VerifiableCredential")
        add("RedlandIdCredential")
    }
    put("id", DID.buildPrismDID("123456678abcdefg").value)
    put("keyId", "Issuance-0")
    putJsonObject("credentialSubject") {
        put("name", "Jorge Lopez Portillo")
        put("degree", "Bachelor's in Self-Sovereign Identity Development")
    }
}
```

You can add additional claims as credential subject.

We're ready to create credential now:


```kotlin
val credential: Credential = JsonBasedCredential(credentialContent)
```


At last, we can proceed to sign the actual credential:

```kotlin
val signedCredential = credential.sign(masterKeyPair.privateKey)
```

That's it, until know you were able to create and sign a credential, on the next steps, you will learn how to get it published to the Cardano network.

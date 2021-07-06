# Creating an Identity
Once you have a project that includes the Atala PRISM SDK, you can generate a decentralized identifier (DID). This section explains how to generate a DID and extract some information from it, which you will need later.

**Note:** You do not require extensive knowledge or understanding of DIDs to complete this tutorial. Check the official [DID specifications](https://w3c-ccg.github.io/did-spec/) for reference.

## Recap
This is what we have done from previous versions:

```scala mdoc
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity._
```

## Generating a DID

Generating a DID requires an associated public key. This example derives a public key from an elliptic-curve.

```scala mdoc
  val masterKeyPair = EC.generateKeyPair()
  val did = DID.createUnpublishedDID(masterKeyPair.publicKey)
```

We can create a DID without *any network interaction nor blockchain transaction*. We call these *unpublished* DIDs.

## Extracting DID Information

We need to extract some details from the generated DID, which are required to deal with credentials later:

```scala mdoc
  // The DID Document is necessary to find the master key id that is used to sign credentials
  val didDocument = did.asLongForm
    .flatMap(_.getInitialState.toOption)
    .flatMap(_.operation.createDid)
    .getOrElse(throw new RuntimeException("Impossible as we just created an unpublished DID"))

  // We have created the DID with a single public key
  val firstPublicKey = didDocument.didData
    .flatMap(_.publicKeys.headOption)
    .getOrElse(throw new RuntimeException("Impossible as we used a key to create the DID"))
```

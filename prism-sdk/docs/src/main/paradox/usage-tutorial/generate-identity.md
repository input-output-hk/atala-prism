# Generate an identity
Once you have a project that includes the PRISM SDK, you can proceed to generate a decentralized identifier (DID).

Don't worry If the DIDs term is new to you, understanding DIDs is not necessary to complete the tutorial, you can check the official [spec](https://w3c-ccg.github.io/did-spec/) if you like.

By the end of this section, you will have generated a DID, and extracted some useful details from it, which we'll use for the next sections.

## Recap
This is what we have done from previous versions:

```scala mdoc
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity._
```

## Generate a DID

In order to generate our first DID, we will need a public key to associate to it. Hence, in this example we derive one from an elliptic-curve.


```scala mdoc
  val masterKeyPair = EC.generateKeyPair()
  val did = DID.createUnpublishedDID(masterKeyPair.publicKey)
```

Note that we were able to create a DID without any network interaction nor blockchain transaction. We call these DIDs, *unpublished*.

Before moving to the next section, let's extract some details from the generated DID, which are required to deal with credentials later:

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


## Next

By now, we have generated a DID which will be used in the next section.

# Issuing a Credential

This section explains how to issue a credential.


## Recap

This is what we have done from previous versions:

```scala mdoc
import io.iohk.atala.prism.credentials.content.CredentialContent
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._
import io.iohk.atala.prism.identity._

val masterKeyPair = EC.generateKeyPair()
val did = DID.createUnpublishedDID(masterKeyPair.publicKey)

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

## Defining the Credential Claims

Before issuing a credential, we need to define its contents:

```scala mdoc
  val credentialContent: CredentialContent = CredentialContent(
    CredentialContent.JsonFields.CredentialType.field -> CredentialContent
      .Values("VerifiableCredential", "PRISM-certified"),
    CredentialContent.JsonFields.IssuerDid.field -> did.value,
    CredentialContent.JsonFields.IssuanceKeyId.field -> "master-0",
    CredentialContent.JsonFields.CredentialSubject.field -> CredentialContent.Fields(
      "name" -> "José López Portillo",
      "certificate" -> "Certificate of PRISM SDK tutorial completion"
    )
  )
```

In this case, `CredentialSubject` represents the claims involved in the credential, while the other fields are metadata specifying *who* is issuing the credential.


## Deriving a Credential From its Claims

We can use the previous claims to derive a `Credential` object:

```scala mdoc
  val credential: Credential = Credential.fromCredentialContent(credentialContent)
```

## Signing the Credential

Having the `Credential` model allows to easily sign it, which results in a signed credential:

```scala mdoc
  val signedCredential = credential.sign(masterKeyPair.privateKey)(EC)
```

**Note:** These are the keys related to the DID generated in the previous section.


## Issuing the Credential

In our protocol, issuing a credential involves creating batches of signed credentials (so that you only pay one Cardano fee). Batches are timestamped in the Cardano blockchain. For simplicity, we'll get the relevant data without touching the Cardano network:

```scala mdoc
  val (merkleRoot, merkleProof) = CredentialBatches.batch(List(signedCredential)) match {
    case (root, List(proof)) => (root, proof)
  }
```

The `merkleRoot` and `merkleProof` are necessary to verify the credential validity.

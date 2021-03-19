# Atala PRISM SDK - Credentials

To install the module:

```scala
libraryDependencies += "io.iohk" %% "prism-credentials" % "@VERSION@"
```

## Verifiable Credentials

Atala PRISM Credentials module provides the necessary tools to work with Verifiable Credentials (VC).

To run the following examples, first you will need to add some imports:

```scala mdoc
import io.iohk.atala.prism.credentials.content._
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._ // necessary to sign a credential

```

Define the content for a credential:

```scala mdoc
  val credentialContent: CredentialContent = CredentialContent(
    CredentialContent.JsonFields.CredentialType.field -> CredentialContent
      .Values("VerifiableCredential", "PRISM-certified"),
    CredentialContent.JsonFields.CredentialSubject.field -> CredentialContent.Fields(
      "name" -> "José López Portillo",
      "certificate" -> "Certificate of PRISM SDK tutorial completion"
    )
  )
```

Create the `Credential` object:

```scala mdoc
  val credential: Credential = Credential.fromCredentialContent(credentialContent)
```

By using the crypto module, you can sign a credential:

```scala mdoc
  val masterKeyPair = EC.generateKeyPair()
  val signedCredential = credential.sign(masterKeyPair.privateKey)(EC)
```

Embed the credential in a batch, leaving you with a proof of inclusion:

```scala mdoc
  val (merkleRoot, merkleProof) = CredentialBatches.batch(List(signedCredential)) match {
    case (root, List(proof)) => (root, proof)
  }
```

At last, you can verify that a credential is valid:

```scala
  PrismCredentialVerification
    .verify(
      keyData = ???, // get this from Cardano
      batchData = ???, // get this from Cardano
      credentialRevocationTime = ???, // get this from Cardano
      merkleRoot = merkleRoot,
      inclusionProof = merkleProof,
      signedCredential = signedCredential
    )(EC)
```
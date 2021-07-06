# Credentials

Use this code to install the Credentials module:

```scala
libraryDependencies += "io.iohk" %% "prism-credentials" % "@VERSION@"
```

## Verifiable Credentials (VCs)

This module provides the necessary tools to work with Verifiable Credentials (VCs).

Add these imports to run the examples listed on this module:

```scala mdoc
import io.iohk.atala.prism.credentials.content._
import io.iohk.atala.prism.credentials.content.syntax._
import io.iohk.atala.prism.credentials._
import io.iohk.atala.prism.crypto._ // necessary to sign a credential

```
Follow these steps to create, sign, and verify a credential.

1. Define the credential's content:

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

2. Create the `Credential` object:

```scala mdoc
  val credential: Credential = Credential.fromCredentialContent(credentialContent)
```

3. You can sign a credential by using the Crypto module:

```scala mdoc
  val masterKeyPair = EC.generateKeyPair()
  val signedCredential = credential.sign(masterKeyPair.privateKey)(EC)
```

4. Embed the credential in a batch, which leaves you with a proof of inclusion:

```scala mdoc
  val (merkleRoot, merkleProof) = CredentialBatches.batch(List(signedCredential)) match {
    case (root, List(proof)) => (root, proof)
  }
```

5. Verify that a credential is valid:

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

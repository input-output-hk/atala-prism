# Verifying the Credential

This section explains how to verify a credential's validity.

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

val credentialContent: CredentialContent = CredentialContent(
  CredentialContent.JsonFields.CredentialType.field -> CredentialContent
    .Values("VerifiableCredential", "PRISM-certified"),
  CredentialContent.JsonFields.IssuerDid.field -> did.value,
  CredentialContent.JsonFields.IssuanceKeyId.field -> "master-0",
  CredentialContent.JsonFields.CredentialSubject.field -> CredentialContent.Fields(
    "name" -> "José López Portillo",
    "certificate" -> "Certificate of PRISM SDK tutorial completion"
  ))
val credential: Credential = Credential.fromCredentialContent(credentialContent)
val signedCredential = credential.sign(masterKeyPair.privateKey)(EC)
val (merkleRoot, merkleProof) = CredentialBatches.batch(List(signedCredential)) match {
  case (root, List(proof)) => (root, proof)
}
```


## Mocking Network Data

For simplicity, this tutorial does not interact with the Cardano network. A real implementation would require the retrieval of some data from the network, so in this tutorial, we will mock that data.

```scala mdoc
  // assume there is a block in Cardano that includes the DID, and the credential, which was confirmed 1 minute ago
  val didBlockInfo = TimestampInfo(
    atalaBlockTimestamp = java.time.Instant.now().minusSeconds(60),
    atalaBlockSequenceNumber = 1,
    operationSequenceNumber = 1
  )
  val batchBlockInfo = TimestampInfo(
    atalaBlockTimestamp = java.time.Instant.now().minusSeconds(20),
    atalaBlockSequenceNumber = 2,
    operationSequenceNumber = 2
  )

  // this metadata about the DID key should be retrieved from the Cardano network
  val keyData = KeyData(publicKey = masterKeyPair.publicKey, addedOn = didBlockInfo, revokedOn = None)

  // this credential batch metadata should be retrieved from the Cardano network
  val batchData = BatchData(issuedOn = batchBlockInfo, revokedOn = None)
```

## Verifying the Credential

Taking the data from the previous steps, we can now verify the credential. The code returns no errors, so the credential is valid:

```scala mdoc
  PrismCredentialVerification
    .verify(
      keyData = keyData,
      batchData = batchData,
      credentialRevocationTime = None,
      merkleRoot = merkleRoot,
      inclusionProof = merkleProof,
      signedCredential = signedCredential
    )(EC)
```

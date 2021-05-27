Now, **Issuer** is able to issue a credential to **Holder**.

## Prepare credential

Let's create a simple certificate for **Holder** and sign it with the **Issuer's** key:

```kotlin
val holderCredentialContent = CredentialContent(
    JsonObject(
        mapOf(
            Pair("issuerDid", JsonPrimitive(issuerDID.value)),
            Pair("issuanceKeyId", JsonPrimitive(masterKeyId)),
            Pair(
                "credentialSubject",
                JsonObject(
                    mapOf(
                        Pair("name", JsonPrimitive("José López Portillo")),
                        Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
                    )
                )
            ),
        )
    )
)

val holderUnsignedCredential = JsonBasedCredential(holderCredentialContent)
val holderSignedCredential = holderUnsignedCredential.sign(issuerMasterKeyPair.privateKey)
```

## Include the credential in a batch

In **Atala PRISM**, credentials are published to **Cardano** in batches, for this tutorial, the batch includes a single credential:

```kotlin
val (holderCredentialMerkleRoot, holderCredentialMerkleProofs) = CredentialBatches.batch(listOf(holderSignedCredential))
val credentialBatchData = CredentialBatchData(
    issuerDID = issuerDID.suffix.value, // This requires the suffix only, as the node stores only suffixes
    merkleRoot = pbandk.ByteArr(holderCredentialMerkleRoot.hash.value.toByteArray())
)
val issueCredentialOperation = ProtoUtils.issueCredentialBatchOperation(credentialBatchData)
```

## Publish the credential to Cardano

Once we have prepared the batch, we can sign the operation and invoke **Atala PRISM Node** to publish it to **Cardano**:

```kotlin
val signedIssueCredentialOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issueCredentialOperation)
val issuedCredentialResponse = runBlocking {
    node.IssueCredentialBatch(IssueCredentialBatchRequest(signedIssueCredentialOperation))
}
```

Print out some details:

```kotlin
println(
    """
    Issuer: Credential issued to Holder, the transaction can take up to 10 minutes to be confirmed by the Cardano network
    - IssuerDID = $issuerDID
    - Cardano transaction id = ${issuedCredentialResponse.transactionInfo?.transactionId}
    - Credential content = $holderUnsignedCredential
    - Signed credential = ${holderSignedCredential.canonicalForm}
    - Inclusion proof (encoded) = ${holderCredentialMerkleProofs.first().encode()}
    - Batch id = ${issuedCredentialResponse.batchId}
    """.trimIndent()
)
```

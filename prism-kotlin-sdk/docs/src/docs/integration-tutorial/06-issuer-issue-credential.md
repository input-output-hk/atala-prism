# Issuer: Issue Credential

Now, **Issuer** is able to issue a credential to **Holder**.

## Include the credential in a batch

In **Atala PRISM**, credentials are published to **Cardano** in batches, for this tutorial, the batch includes a single credential that will be created from the given claim about **Holder** and signed using **Issuer's** key:

```kotlin
val issuerIssuingKeyPair = DID.deriveKeyFromFullPath(seed, 0, KeyType.ISSUING_KEY, 0)
val addIssuingKeyDIDContext = DID.updateDIDAtalaOperation(
    issuerMasterKeyPair.privateKey,
    masterKeyId,
    issuerDID,
    createDIDContext.operationHash,
    listOf(KeyInformation(issuingKeyId, KeyType.ISSUING_KEY, issuerIssuingKeyPair.publicKey))
)
val issueBatchContext = CredentialBatches.createBatchAtalaOperation(
    issuerDID = issuerDID,
    signingKeyId = issuingKeyId,
    issuingPrivateKey = issuerIssuingKeyPair.privateKey,
    credentialsClaims = listOf(
        JsonObject(
            mapOf(
                Pair("name", JsonPrimitive("José López Portillo")),
                Pair("certificate", JsonPrimitive("Certificate of PRISM SDK tutorial completion"))
            )
        ).toString()
    )
)
```

## Publish the credential to Cardano

Once we have prepared the batch, we can sign the operation and invoke **Atala PRISM Node** to publish it to **Cardano**:

```kotlin
val publishAsABlockResponse = runBlocking {
    node.PublishAsABlock(
        PublishAsABlockRequest(
            signedOperations = listOf(
                addIssuingKeyDIDContext.updateDIDSignedOperation,
                issueBatchContext.signedAtalaOperation
            )
        )
    )
}
val addDidKeyResponse = publishAsABlockResponse.outputs.first()
val issueCredentialBatchResponse = publishAsABlockResponse.outputs.last()
val holderSignedCredential = issueBatchContext.credentialsAndProofs.first().signedCredential
val holderCredentialMerkleProof = issueBatchContext.credentialsAndProofs.first().inclusionProof
```

Print out some details:

```kotlin
println(
    """
    Issuer: Credential issued to Holder, the transaction can take up to 10 minutes to be confirmed by the Cardano network
    - IssuerDID = $issuerDID
    - Add issuing key to DID operation identifier = ${addDidKeyResponse.operationId}
    - Issuer credential batch operation identifier = ${issueCredentialBatchResponse.operationId}
    - Credential content = ${holderSignedCredential.content}
    - Signed credential = ${holderSignedCredential.canonicalForm}
    - Inclusion proof (encoded) = ${holderCredentialMerkleProof.encode()}
    - Batch id = ${issueCredentialBatchResponse.batchOutput!!.batchId}
    """.trimIndent()
)
```

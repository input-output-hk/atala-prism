# Issuer: Revoke credential

This section illustrates how Issuer can revoke a credential, the API allows to:
1. Revoke a single credential.
2. Revoke all credentials involved in a batch.
3. Revoke many credentials from the same batch.

In this example, you'll see how to revoke a single credential, but, the other options are available by just switching the method arguments.

## Revoke a single credential

Given that you already have all the necessary data in place, the revocation is simple, just generate the revocation operation, and get it posted in Cardano by the Node:

```kotlin
val issuerRevokeCredentialOperation = ProtoUtils.revokeCredentialsOperation(
    batchOperationHash = Hash.compute(issueCredentialOperation.encodeToByteArray().asList()),
    batchId = CredentialBatchId.fromString(issuedCredentialResponse.batchId)!!,
    credentials = listOf(holderSignedCredential)
)
val issuerRevokeCredentialSignedOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issuerRevokeCredentialOperation)
val issuerCredentialRevocationResponse = runBlocking {
    node.RevokeCredentials(
        RevokeCredentialsRequest(issuerRevokeCredentialSignedOperation)
    )
}
println(
    """
    Issuer: Credential revoked, the transaction can take up to 10 minutes to be confirmed by the Cardano network
    - Cardano transaction id: ${issuerCredentialRevocationResponse.transactionInfo?.transactionId}
    """.trimIndent()
)
```
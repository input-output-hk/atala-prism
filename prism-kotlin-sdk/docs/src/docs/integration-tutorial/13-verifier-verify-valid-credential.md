# Verifier: Verify Valid Credential

The fun begins now. But before we able to verify a credential, we need to parse the signed credential to get the **Issuer's** details.

## Extract the credential data

Let's decode the signed credential to get the data relevant for the verification:

```kotlin
val verifierReceivedJsonCredential =
    JsonBasedCredential.fromString(verifierReceivedCredential.encodedCredential)
val verifierReceivedCredentialIssuerDID = verifierReceivedJsonCredential.content.getString("id")!!
val verifierReceivedCredentialIssuanceKeyId =
    verifierReceivedJsonCredential.content.getString("keyId")!!
val verifierReceivedCredentialMerkleProof =
    MerkleInclusionProof.decode(verifierReceivedCredential.encodedMerkleProof)
val verifierReceivedCredentialBatchId = CredentialBatches.computeCredentialBatchId(
    DID.fromString(verifierReceivedCredentialIssuerDID),
    verifierReceivedCredentialMerkleProof.derivedRoot()
)
println(
    """
    Verifier: Received credential decoded
    - Credential: ${verifierReceivedJsonCredential.content}
    - Issuer DID: $verifierReceivedCredentialIssuerDID
    - Issuer issuance key id: $verifierReceivedCredentialIssuanceKeyId
    - Merkle proof root: ${verifierReceivedCredentialMerkleProof.hash.hexValue()}
    """.trimIndent()
)
```

## Verify the credential

Having the necessary data, verifying the credential requires a single convenience method call that queries **Node** for the data relevant to **Issuer** and the credential:

```kotlin
println("Verifier: Verifying received credential using single convenience method")
val credentialVerificationServiceResult = runBlocking {
    CredentialVerificationService(node).verify(
        signedCredential = verifierReceivedJsonCredential,
        merkleInclusionProof = verifierReceivedCredentialMerkleProof
    )
}
println("Credential is valid: ${credentialVerificationServiceResult.verificationErrors.isEmpty()}")
```

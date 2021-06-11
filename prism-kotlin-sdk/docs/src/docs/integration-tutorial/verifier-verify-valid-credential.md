The fun begins now, before being able to verify a credential, follow these steps:
1. Parse the signed credential to get the **Issuer's** details.
2. Resolve the **Issuer's** details from the **Node** to make sure the key, and the signature are correct.
3. Resolve the credential state from the **Node** to make sure the proof exists in **Cardano**.
4. Resolve the credential revocation state from the Node to make sure the credential hasn't been revoked.

## Extract the credential data

Let's decode the signed credential to get the data relevant for the verification:

```kotlin
val verifierReceivedJsonCredential = JsonBasedCredential.fromString(verifierReceivedCredential.encodedCredential)
val verifierReceivedCredentialIssuerDID = verifierReceivedJsonCredential.content.getString("issuerDid")!!
val verifierReceivedCredentialIssuanceKeyId = verifierReceivedJsonCredential.content.getString("issuanceKeyId")!!
println(
    """
    Verifier: Received credential decoded
    - Credential: ${verifierReceivedJsonCredential.content}
    - Issuer DID: $verifierReceivedCredentialIssuerDID
    - Issuer issuance key id: $verifierReceivedCredentialIssuanceKeyId
    """.trimIndent()
)
```

## Resolve the issuer/credential details

It's time to query the **Node** for the data relevant to the **Issuer** and the credential, which is later on used to check the credential verification state:

```kotlin
println("Verifier: Resolving issuer/credential details from the node")
val verifierReceivedCredentialIssuerDIDDocument = runBlocking {
    node.GetDidDocument(GetDidDocumentRequest(did = verifierReceivedCredentialIssuerDID)).document!!
}
val verifierReceivedCredentialIssuerKey = verifierReceivedCredentialIssuerDIDDocument.findPublicKey(verifierReceivedCredentialIssuanceKeyId)
val verifierReceivedCredentialMerkleProof = MerkleInclusionProof.decode(verifierReceivedCredential.encodedMerkleProof)

val verifierReceivedCredentialBatchId = CredentialBatches.computeCredentialBatchId(
    DID.fromString(verifierReceivedCredentialIssuerDID),
    verifierReceivedCredentialMerkleProof.derivedRoot()
)

val verifierReceivedCredentialBatchState = runBlocking {
    node.GetBatchState(GetBatchStateRequest(batchId = Hash.fromHex(verifierReceivedCredentialBatchId.id).hexValue()))
}
val verifierReceivedCredentialBatchData = BatchData(
    issuedOn = verifierReceivedCredentialBatchState.publicationLedgerData?.timestampInfo?.toTimestampInfoModel()!!,
    revokedOn = verifierReceivedCredentialBatchState.revocationLedgerData?.timestampInfo?.toTimestampInfoModel()
)
val verifierReceivedCredentialRevocationTime = runBlocking {
    node.GetCredentialRevocationTime(
        GetCredentialRevocationTimeRequest(
            batchId = Hash.fromHex(verifierReceivedCredentialBatchId.id).hexValue(),
            credentialHash = pbandk.ByteArr(verifierReceivedJsonCredential.hash().value)
        )
    ).revocationLedgerData?.timestampInfo?.toTimestampInfoModel()
}
```

## Verify the credential

Having the necessary data, verifying the credential requires a single method call, which succeeds because the credential is valid:

```kotlin
// Verifier checks the credential validity (which succeeds)
println("Verifier: Verifying received credential")
CredentialVerification.verify(
    keyData = verifierReceivedCredentialIssuerKey!!,
    batchData = verifierReceivedCredentialBatchData,
    credentialRevocationTime = verifierReceivedCredentialRevocationTime,
    merkleRoot = verifierReceivedCredentialMerkleProof.derivedRoot(),
    inclusionProof = verifierReceivedCredentialMerkleProof,
    signedCredential = verifierReceivedJsonCredential
)
```

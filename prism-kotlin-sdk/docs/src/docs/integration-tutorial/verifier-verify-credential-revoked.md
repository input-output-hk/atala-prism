# Verifier: Verify revoked credential

Analogously to the last verification section, Verifier will verify the credential again. However, since it was revoked in the previous step, this time verification is supposed to fail.

**Note**: For simplicity, the only data being refreshed is the credential's revocation time, in your integration, you are expected to query all the details instead.

The operations take some minutes to be applied by Cardano, once the revoke operation is confirmed, you can query the revocation time and run the verification again, this time, an exception is thrown explaining that the credential is revoked:

```kotlin
println("Verifier: Checking the credential validity again, expect an error explaining that the credential is revoked")
val verifierReceivedCredentialRevocationTime2 = runBlocking {
    node.GetCredentialRevocationTime(
        GetCredentialRevocationTimeRequest(
            batchId = Hash.fromHex(verifierReceivedCredentialBatchId.id).hexValue(),
            credentialHash = pbandk.ByteArr(verifierReceivedJsonCredential.hash().value.toByteArray())
        )
    ).revocationLedgerData?.timestampInfo?.toTimestampInfoModel()
}

// Verifier checks the credential validity (which fails)
CredentialVerification.verify(
    keyData = verifierReceivedCredentialIssuerKey,
    batchData = verifierReceivedCredentialBatchData,
    credentialRevocationTime = verifierReceivedCredentialRevocationTime2,
    merkleRoot = verifierReceivedCredentialMerkleProof.derivedRoot(),
    inclusionProof = verifierReceivedCredentialMerkleProof,
    signedCredential = verifierReceivedJsonCredential
)
```

## More

That's it, congratulations on completing this long tutorial, by now, you should have a decent undertanding to replicate the work we did in our [interactive demo](https://atalaprism.io).

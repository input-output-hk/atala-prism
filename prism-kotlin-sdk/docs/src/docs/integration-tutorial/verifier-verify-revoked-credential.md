Analogously to the last verification section, **Verifier** will verify the credential again. However, since it was revoked in the previous step, this time verification **is supposed to fail**.

**Note**: For simplicity, the only data being refreshed is the credential's revocation time, in general querying all the details is required.

The operations take some minutes to be applied by **Cardano**, once the revoke operation is confirmed, query the revocation time and run the verification again, this time, an exception is thrown explaining that the credential is revoked:

```kotlin
println("Verifier: Checking the credential validity again, expect an error explaining that the credential is revoked")
Thread.sleep(2000) // give some time to the backend to apply the operation
val credentialVerificationServiceResult2 = runBlocking {
    CredentialVerificationService(node).verify(
        signedCredential = verifierReceivedJsonCredential,
        merkleInclusionProof = verifierReceivedCredentialMerkleProof
    )
}

println("Verification errors: ${credentialVerificationServiceResult2.verificationErrors.size}")
```

## Next steps

**That's it, congratulations on completing Integration Tutorial!**

By now, one should have a decent understanding how to replicate the work done on the [Atala PRISM Interactive Demo Website](https://atalaprism.io).

The **Holder's** goal in this tutorial is to receive a credential from **Issuer**, which will eventually be shared with **Verifier**.

In order to do that, you will need to do some preparation steps:
1. Generate an **unpublished DID** to communicate with **Issuer**.
2. Generate an **unpublished DID** to communicate with **Verifier**.

## Generating Holder DIDs
The **Holder DIDs** are generated similarly to how the **Issuer DID** was generated, the main difference is that there is no proof in **Cardano** about the existence of such **DIDs**.

```kotlin
val holderMasterKeyPair = EC.generateKeyPair()
val holderUnpublishedDID = DID.createUnpublishedDID(holderMasterKeyPair.publicKey)
println("Holder: First DID generated to connect with Issuer = $holderUnpublishedDID")

// Holder generates its identity to connect with verifier
// in PRISM, you are supposed to use different identities for every connection
val holderMasterKeyPair2 = EC.generateKeyPair()
val holderUnpublishedDID2 = DID.createUnpublishedDID(holderMasterKeyPair2.publicKey)
println("Holder: Second DID generated to connect with Verifier = $holderUnpublishedDID2")
```

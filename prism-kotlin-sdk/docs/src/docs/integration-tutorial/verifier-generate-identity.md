**Verifier's** goal in this tutorial is to receive a credential from **Holder** and verify its validity.

You have to follow the same steps done by **Issuer**:
1. Generate a **Decentralized Identifier (DID)** for **Verifier**.
2. Publish the **Verifier's DID** to **Cardano**.
3. Generate a token that will be used to connect to **Holder**. This step allows **Verifier** to receive credentials over the internet.

An astute reader might have noticed that **Verifier** is publishing its **DID** to the **Cardano** network, which is not strictly necessary. While it is usually a good practice to publish **DIDs** related to institutions, evaluate it depends on use case.

## Steps

Given that these are the same steps followed by **Issuer** in [this section](/wiki/integration-tutorial/issuer-generate-identity), the whole code for this section is left without much explanation:

```kotlin
val verifierMasterKeyPair = EC.generateKeyPair()
val verifierCreateDIDOperation = ProtoUtils.createDidAtalaOperation(verifierMasterKeyPair)
val verifierCreateDIDSignedOperation = ProtoUtils.signedAtalaOperation(verifierMasterKeyPair, verifierCreateDIDOperation)

val verifierRegisterDIDResponse = runBlocking {
    connector.RegisterDID(
        RegisterDIDRequest(
            registerWith = RegisterDIDRequest.RegisterWith.CreateDidOperation(verifierCreateDIDSignedOperation),
            name = "Verifier"
        )
    )
}
val verifierDID = DID.fromString(verifierRegisterDIDResponse.did)
val verifierUnpublishedDID = DID.createUnpublishedDID(verifierMasterKeyPair.publicKey)
println(
    """
    Verifier DID registered, the transaction can take up to 10 minutes to be confirmed by the Cardano network
    - DID: $verifierDID
    - Cardano transaction id: ${verifierRegisterDIDResponse.transactionInfo?.transactionId}
    """.trimIndent()
)
println()

// Verifier generates a token to connect with the credential subject
val verifierGenerateConnectionTokenRequest = GenerateConnectionTokenRequest(count = 1)
val verifierConnectionToken = runBlocking {
    connector.GenerateConnectionTokenAuth(
        verifierGenerateConnectionTokenRequest,
        RequestUtils.generateRequestMetadata(
            verifierUnpublishedDID.value,
            verifierMasterKeyPair.privateKey,
            verifierGenerateConnectionTokenRequest
        )
    ).tokens.first()
}
println("Verifier: Token for connecting with holder generated = $verifierConnectionToken")
```

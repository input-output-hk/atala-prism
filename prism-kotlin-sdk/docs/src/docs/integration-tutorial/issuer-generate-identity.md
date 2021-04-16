The **Issuer's** goal in this tutorial is to issue a credential to **Holder**, and eventually, revoke it.

In order to do that, some preparation steps need to be done:
1. Generate a **Decentralized Identifier (DID)** for **Issuer**.
2. Publish the **Issuer's DID** to **Cardano**, which is a necessary step for anyone willing to issue credentials.
3. Generate a that will be used to connect to **Holder**. This step allows **Issuer** to share the issued credentials over the internet.

During preparation, set up a project that includes **Atala PRISM SDK** and proceed to do the **Issuer** steps.

**Note:** You do not require extensive knowledge or understanding of **DIDs** to complete this tutorial. Check the official [DID specifications](https://w3c-ccg.github.io/did-spec/) for reference.

## Generating a DID

Generating a **DID** require associating a public key. This example generates a public key from an elliptic-curve and then publishes the **DID** to **Cardano** by invoking the **Connector's** service:

```kotlin
val issuerMasterKeyPair = EC.generateKeyPair()
val issuerCreateDIDOperation = ProtoUtils.createDidAtalaOperation(issuerMasterKeyPair)
val issuerCreatedDIDSignedOperation = ProtoUtils.signedAtalaOperation(issuerMasterKeyPair, issuerCreateDIDOperation)

// the issuer registers its identity to the node
// Usually the DID would be registered with the node, but, the connector can handle that as well
// val issuerDIDSuffix = node.CreateDID(CreateDIDRequest(signedOperation)).id
val issuerRegisterDIDResponse = runBlocking {
    connector.RegisterDID(
        RegisterDIDRequest(
            createDIDOperation = issuerCreatedDIDSignedOperation,
            name = "Issuer"
        )
    )
}
val issuerDID = DID.fromString(issuerRegisterDIDResponse.did)

// the DID takes some minutes to get confirmed by Cardano, in the mean time, the unpublished DID
// can be used to authenticate requests to the backend
val issuerUnpublishedDID = DID.createUnpublishedDID(issuerMasterKeyPair.publicKey)
println(
    """
            Issuer DID registered, the transaction can take up to 10 minutes to be confirmed by the Cardano network
            - DID: ${issuerRegisterDIDResponse.did}
            - Cardano transaction id: ${issuerRegisterDIDResponse.transactionInfo?.transactionId}
            """.trimIndent()
)
```

Also, `issuerUnpublishedDID` is created which is a **DID** without *any network interaction nor blockchain transaction*. We call these *unpublished DIDs*.


## Generating a connection token

Let's proceed to generate a token which will be used to get connected with **Holder**:

```kotlin
val issuerGenerateConnectionTokenRequest = GenerateConnectionTokenRequest(count = 1)
val issuerConnectionToken = runBlocking {
    connector.GenerateConnectionTokenAuth(
        issuerGenerateConnectionTokenRequest,
        RequestUtils.generateRequestMetadata(
            issuerUnpublishedDID.value,
            issuerMasterKeyPair.privateKey,
            issuerGenerateConnectionTokenRequest
        )
    ).tokens.first()
}
println("Issuer: Token for connecting with the holder generated = $issuerConnectionToken")
```

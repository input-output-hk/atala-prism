Now that **Holder** has generated some **DIDs**, it is ready to accept the **Issuer's** connection.

In your integration, there should a way for the **Issuer** to share the token with **Holder**, which is done outside the **Atala PRISM** ecosystem, there are many ways to do so, like sending a hyperlink to **Holder**, sharing a QR code, etc. Let's assume **Holder** already got the token.

## Accepting the Issuer's connection

Before accepting the **Issuer's** connection, its worth reviewing that we are connecting to the intended entity, for what let's use the token to get the entity details:

```kotlin
val issuerConnectionTokenDetails = runBlocking {
    connector.GetConnectionTokenInfo(
        GetConnectionTokenInfoRequest(token = issuerConnectionToken)
    )
}
println(
    """
        Holder: Check Issuer's connection token details:
        - Issuer name = ${issuerConnectionTokenDetails.creatorName}
        - Issuer DID  = ${issuerConnectionTokenDetails.creatorDID}
    """.trimIndent()
)
```

Once **Holder** is ready to accept the **Issuer's** connection, a request needs to be sent to the **Connector**, which returns a `Connection` object, which can be used to send messages to **Issuer**:

```kotlin
val holderAcceptsIssuerConnectionRequest = AddConnectionFromTokenRequest(token = issuerConnectionToken)
val holderIssuerConnection = runBlocking {
    connector.AddConnectionFromTokenAuth(
        holderAcceptsIssuerConnectionRequest,
        RequestUtils.generateRequestMetadata(
            holderUnpublishedDID.value,
            holderMasterKeyPair.privateKey,
            holderAcceptsIssuerConnectionRequest
        )
    ).connection!!
}
println("Holder (DID 1): Connected to Issuer, connectionId = ${holderIssuerConnection.connectionId}")
```
